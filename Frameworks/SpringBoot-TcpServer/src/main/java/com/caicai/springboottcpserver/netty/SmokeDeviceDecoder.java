package com.caicai.springboottcpserver.netty;

import com.caicai.springboottcpserver.tcp.ParseTCPUtil;
import com.caicai.springboottcpserver.tcp.entity.BaseTcpDTO;
import com.caicai.springboottcpserver.tcp.entity.EventTcpDTO;
import com.caicai.springboottcpserver.tcp.entity.HeartbeatTcpDTO;
import com.caicai.springboottcpserver.tcp.entity.RegisterTcpDTO;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * @author: 菜菜的后端私房菜
 * @create: 2024/8/16 10:07
 * @description: 烟感设备TCP编解码
 */
@Slf4j
public class SmokeDeviceDecoder extends MessageToMessageCodec<ByteBuf, BaseTcpDTO> {

    /**
     * 响应时TCP编码
     * @param ctx
     * @param msg
     * @param out
     * @throws Exception
     */
    @Override
    protected void encode(ChannelHandlerContext ctx, BaseTcpDTO msg, List<Object> out) throws Exception {
        //封装响应
        ByteBuf outByteBuf = ctx.alloc().buffer();

        //头部
        outByteBuf.writeBytes(new byte[]{0x57, 0x54, 0x4B});
        //协议版本
        outByteBuf.writeByte(0x10);
        //厂商标识 todo cl 未确定
        outByteBuf.writeBytes(new byte[]{0x68, 0x7A});
        //数据序列化 todo 未确定
        outByteBuf.writeBytes(new byte[]{0x68, 0x7A});
        //命令
        outByteBuf.writeByte(0xFF);
        //数据长度
        outByteBuf.writeByte(0x01);
        //回复标识 0x00成功 0x01CRC校验失败 0x02数据解析异常 0x03厂商校验失败
        outByteBuf.writeByte(0x00);
        //校验位 todo
        outByteBuf.writeBytes(new byte[]{0x68, 0x7A});
        //尾部
        outByteBuf.writeBytes(new byte[]{0x45, 0x4E, 0x44});

        outByteBuf.writeBytes("\r\n".getBytes(StandardCharsets.UTF_8));
        out.add(outByteBuf);

    }

    /**
     * 收到请求后TCP解码
     *
     * @param channelHandlerContext
     * @param byteBuf
     * @param list
     */
    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) {
        log.info("TCP编解码器 开始解析TCP数据");

        BaseTcpDTO dto = new BaseTcpDTO();
        //解析通用数据
        parseDataBodyBefore(byteBuf, dto);

        //根据命令位继续解析
        byte command = dto.getCommand();
        if (command == 1) {
            //注册
            RegisterTcpDTO registerTcpDTO = new RegisterTcpDTO();
            BeanUtils.copyProperties(dto, registerTcpDTO);
            registerParse(byteBuf, registerTcpDTO);
            list.add(registerTcpDTO);
        } else if (command == 2) {
            //事件上报
            EventTcpDTO eventTcpDTO = new EventTcpDTO();
            BeanUtils.copyProperties(dto, eventTcpDTO);
            eventParse(byteBuf, eventTcpDTO);
            list.add(eventTcpDTO);
        } else if (command == 5) {
            //心跳
            HeartbeatTcpDTO heartbeatTcpDTO = new HeartbeatTcpDTO();
            BeanUtils.copyProperties(dto, heartbeatTcpDTO);
            heartbeatParse(byteBuf, heartbeatTcpDTO);
            list.add(heartbeatTcpDTO);
        }

        log.info("TCP编解码器 解析TCP数据结束");
    }


    /**
     * 解析注册消息
     *
     * @param byteBuf
     * @param registerTcpDTO
     */
    private static void registerParse(ByteBuf byteBuf, RegisterTcpDTO registerTcpDTO) {
        // 设备类型
        byte deviceType = byteBuf.readByte();
        registerTcpDTO.setDeviceType(deviceType);
        log.info("设备类型: " + deviceType);

        // 设备编号
        ByteBuf deviceIdBuffer = byteBuf.readSlice(15);
        String deviceId = deviceIdBuffer.toString(StandardCharsets.US_ASCII);
        registerTcpDTO.setDeviceId(deviceId);
        log.info("设备编号: " + deviceId);

        // IMSI
        ByteBuf imsiBuffer = byteBuf.readSlice(15);
        String imsi = imsiBuffer.toString(StandardCharsets.US_ASCII);
        registerTcpDTO.setImsi(imsi);
        log.info("IMSI: " + imsi);

        // ICCID
        ByteBuf iccidBuffer = byteBuf.readSlice(20);
        String iccid = iccidBuffer.toString(StandardCharsets.US_ASCII);
        registerTcpDTO.setIccid(iccid);
        log.info("ICCID: " + iccid);

        // 电池电压
        byte batteryVoltage = byteBuf.readByte();
        String batteryVoltageStr = ParseTCPUtil.toBatteryVoltageString(batteryVoltage);
        registerTcpDTO.setBatteryVoltage(batteryVoltageStr);
        log.info("电池电压: " + batteryVoltageStr);

        // CSQ信号强度
        byte csq = byteBuf.readByte();
        registerTcpDTO.setCsq(csq);
        log.info("CSQ信号强度: " + csq);

        // 电池电量
        byte batteryLevel = byteBuf.readByte();
        String batteryLevelStr = batteryLevel + "%";
        registerTcpDTO.setBatteryLevel(batteryLevelStr);
        log.info("电池电量: " + batteryLevelStr);

        // 程序版本
        byte softwareVersion = byteBuf.readByte();
        String versionStr = ParseTCPUtil.hexToVersionString(softwareVersion);
        registerTcpDTO.setSoftwareVersion(versionStr);
        log.info("程序版本: " + versionStr);

        parseDataBodyAfter(byteBuf, registerTcpDTO);
    }

    private static void heartbeatParse(ByteBuf in, HeartbeatTcpDTO heartbeatTcpDTO) {
        // 设备编号
        ByteBuf deviceIdBuffer = in.readSlice(15);
        String deviceId = deviceIdBuffer.toString(StandardCharsets.US_ASCII);
        heartbeatTcpDTO.setDeviceId(deviceId);
        log.info("设备编号: " + deviceId);

        parseDataBodyAfter(in, heartbeatTcpDTO);
    }

    private static void eventParse(ByteBuf in, EventTcpDTO eventTcpDTO) {
        // 设备类型
        byte deviceType = in.readByte();
        eventTcpDTO.setDeviceType(deviceType);
        log.info("设备类型: " + deviceType);

        // 设备编号
        ByteBuf deviceIdBuffer = in.readSlice(15);
        String deviceId = deviceIdBuffer.toString(StandardCharsets.US_ASCII);
        eventTcpDTO.setDeviceId(deviceId);
        log.info("设备编号: " + deviceId);

        // 事件类型
        byte eventType = in.readByte();
        eventTcpDTO.setEventType(eventType);
        log.info("事件类型: " + eventType);

        // 电池电压
        byte batteryVoltage = in.readByte();
        String batteryVoltageStr = ParseTCPUtil.toBatteryVoltageString(batteryVoltage);
        eventTcpDTO.setBatteryVoltage(batteryVoltageStr);
        log.info("电池电压: " + batteryVoltageStr);

        // CSQ信号强度
        byte csq = in.readByte();
        eventTcpDTO.setCsq(csq);
        log.info("CSQ信号强度: " + csq);

        // 电池电量
        byte batteryLevel = in.readByte();
        String batteryLevelStr = batteryLevel + "%";
        eventTcpDTO.setBatteryLevel(batteryLevelStr);
        log.info("电池电量: " + batteryLevelStr);

        // 程序版本
        byte softwareVersion = in.readByte();
        String versionStr = ParseTCPUtil.hexToVersionString(softwareVersion);
        eventTcpDTO.setSoftwareVersion(versionStr);
        log.info("程序版本: " + versionStr);

        parseDataBodyAfter(in, eventTcpDTO);
    }

    /**
     * 解析数据体前的通用数据
     *
     * @param byteBuf
     * @param dto
     */
    private static void parseDataBodyBefore(ByteBuf byteBuf, BaseTcpDTO dto) {
        // 头部
        ByteBuf headerBuffer = byteBuf.readSlice(3);
        String header = headerBuffer.toString(StandardCharsets.US_ASCII);
        dto.setHeader(header);
        log.info("头部: " + header);

        // 协议版本
        byte version = byteBuf.readByte();
        String versionStr = ParseTCPUtil.hexToVersionString(version);
        dto.setVersion(versionStr);
        log.info("协议版本: " + versionStr);

        // 厂商标识
        ByteBuf manufacturerIdBuffer = byteBuf.readSlice(2);
        String manufacturerId = manufacturerIdBuffer.toString(StandardCharsets.US_ASCII);
        dto.setManufacturerId(manufacturerId);
        log.info("厂商标识: " + manufacturerId);

        // 消息序列号
        short sequenceNumber = (short) byteBuf.readUnsignedShort();
        dto.setSequenceNumber(sequenceNumber);
        log.info("消息序列号: " + sequenceNumber);

        // 命令
        byte command = byteBuf.readByte();
        dto.setCommand(command);
        log.info("命令: " + command);

        // 数据长度
        byte dataLength = byteBuf.readByte();
        dto.setDataLength(dataLength);
        log.info("数据长度: " + dataLength);
    }

    /**
     * 解析消息体后的通用数据
     *
     * @param byteBuf
     * @param dto
     */
    private static void parseDataBodyAfter(ByteBuf byteBuf, BaseTcpDTO dto) {
        // 校验位
        short crc = byteBuf.readShortLE();
        dto.setCrc(crc);
        log.info("校验位: " + crc);

        // 尾部
        ByteBuf tailBuffer = byteBuf.readSlice(3);
        String tail = tailBuffer.toString(StandardCharsets.US_ASCII);
        dto.setTail(tail);
        log.info("尾部: " + tail);
    }
}
