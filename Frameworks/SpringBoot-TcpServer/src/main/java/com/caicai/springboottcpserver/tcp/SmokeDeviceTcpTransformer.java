package com.caicai.springboottcpserver.tcp;

import com.caicai.springboottcpserver.tcp.entity.BaseTcpDTO;
import com.caicai.springboottcpserver.tcp.entity.EventTcpDTO;
import com.caicai.springboottcpserver.tcp.entity.HeartbeatTcpDTO;
import com.caicai.springboottcpserver.tcp.entity.RegisterTcpDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.integration.transformer.AbstractPayloadTransformer;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * @author: 菜菜的后端私房菜
 * @create: 2024/8/15 14:11
 * @description: 烟感设备TCP消息转换
 */
@Slf4j
public class SmokeDeviceTcpTransformer extends AbstractPayloadTransformer<byte[], Object> {


    @Override
    protected Object transformPayload(byte[] bytes) {
        log.info("开始解析数据");
        BaseTcpDTO dto = new BaseTcpDTO();
        int index = 0;
        //解析通用数据
        index = paresDataBodyBefore(index, bytes, dto);

        //根据命令位继续解析
        byte command = dto.getCommand();
        if (command == 1) {
            //注册
            RegisterTcpDTO registerTcpDTO = new RegisterTcpDTO();
            BeanUtils.copyProperties(dto, registerTcpDTO);
            registerParse(index, bytes, registerTcpDTO);
            return registerTcpDTO;
        } else if (command == 2) {
            //事件上报
            EventTcpDTO eventTcpDTO = new EventTcpDTO();
            BeanUtils.copyProperties(dto, eventTcpDTO);
            eventParse(index, bytes, eventTcpDTO);
            return eventTcpDTO;
        } else if (command == 5) {
            //心跳
            HeartbeatTcpDTO heartbeatTcpDTO = new HeartbeatTcpDTO();
            BeanUtils.copyProperties(dto, heartbeatTcpDTO);
            heartbeatParse(index, bytes, heartbeatTcpDTO);
            return heartbeatTcpDTO;
        }

        return dto;
    }

    /**
     * 心跳解析
     * @param index
     * @param bytes
     * @param heartbeatTcpDTO
     */
    private void heartbeatParse(int index, byte[] bytes, HeartbeatTcpDTO heartbeatTcpDTO) {
        // 设备编号
        String deviceId = new String(Arrays.copyOfRange(bytes, index, index + 15), StandardCharsets.US_ASCII);
        index += 15;
        heartbeatTcpDTO.setDeviceId(deviceId);
        log.info("设备编号: " + deviceId);

        parseDataBodyAfter(index, bytes, heartbeatTcpDTO);
    }

    /**
     * 事件上报解析
     *
     * @param index       当前解析到的索引
     * @param bytes       要解析的数据
     * @param eventTcpDTO 注册消息实体
     */
    private void eventParse(int index, byte[] bytes, EventTcpDTO eventTcpDTO) {
        // 设备类型
        byte deviceType = bytes[index++];
        eventTcpDTO.setDeviceType(deviceType);
        log.info("设备类型: " + deviceType);

        // 设备编号
        String deviceId = new String(Arrays.copyOfRange(bytes, index, index + 15), StandardCharsets.US_ASCII);
        index += 15;
        eventTcpDTO.setDeviceId(deviceId);
        log.info("设备编号: " + deviceId);

        // 事件类型
        byte eventType = bytes[index++];
        eventTcpDTO.setEventType(eventType);
        log.info("事件类型: " + eventType);

        // 电池电压
        byte batteryVoltage = bytes[index++];
        String batteryVoltageStr = ParseTCPUtil.toBatteryVoltageString(batteryVoltage);
        eventTcpDTO.setBatteryVoltage(batteryVoltageStr);
        log.info("电池电压: " + batteryVoltageStr);

        // CSQ信号强度
        byte csq = bytes[index++];
        eventTcpDTO.setCsq(csq);
        log.info("CSQ信号强度: " + csq);

        // 电池电量
        byte batteryLevel = bytes[index++];
        String batteryLevelStr = batteryLevel + "%";
        eventTcpDTO.setBatteryLevel(batteryLevelStr);
        log.info("电池电量: " + batteryLevelStr);

        // 程序版本
        byte softwareVersion = bytes[index++];
        String versionStr = ParseTCPUtil.hexToVersionString(softwareVersion);
        eventTcpDTO.setSoftwareVersion(versionStr);
        log.info("程序版本: " + versionStr);

        parseDataBodyAfter(index, bytes, eventTcpDTO);
    }


    /**
     * 注册消息解析
     *
     * @param index          当前解析到的索引
     * @param bytes          要解析的数据
     * @param registerTcpDTO 注册消息实体
     * @return 解析后的索引位置
     */
    private static int registerParse(int index, byte[] bytes, RegisterTcpDTO registerTcpDTO) {
        // 设备类型
        byte deviceType = bytes[index++];
        registerTcpDTO.setDeviceType(deviceType);
        log.info("设备类型: " + deviceType);

        // 设备编号
        String deviceId = new String(Arrays.copyOfRange(bytes, index, index + 15), StandardCharsets.US_ASCII);
        index += 15;
        registerTcpDTO.setDeviceId(deviceId);
        log.info("设备编号: " + deviceId);

        // IMSI
        String imsi = new String(Arrays.copyOfRange(bytes, index, index + 15), StandardCharsets.US_ASCII);
        index += 15;
        registerTcpDTO.setImsi(imsi);
        log.info("IMSI: " + imsi);

        // ICCID
        String iccid = new String(Arrays.copyOfRange(bytes, index, index + 20), StandardCharsets.US_ASCII);
        index += 20;
        registerTcpDTO.setIccid(iccid);
        log.info("ICCID: " + iccid);

        // 电池电压
        byte batteryVoltage = bytes[index++];
        String batteryVoltageStr = ParseTCPUtil.toBatteryVoltageString(batteryVoltage);
        registerTcpDTO.setBatteryVoltage(batteryVoltageStr);
        log.info("电池电压: " + batteryVoltageStr);

        // CSQ信号强度
        byte csq = bytes[index++];
        registerTcpDTO.setCsq(csq);
        log.info("CSQ信号强度: " + csq);

        // 电池电量
        byte batteryLevel = bytes[index++];
        String batteryLevelStr = batteryLevel + "%";
        registerTcpDTO.setBatteryLevel(batteryLevelStr);
        log.info("电池电量: " + batteryLevelStr);

        // 程序版本
        byte softwareVersion = bytes[index++];
        String versionStr = ParseTCPUtil.hexToVersionString(softwareVersion);
        registerTcpDTO.setSoftwareVersion(versionStr);
        log.info("程序版本: " + versionStr);


        return parseDataBodyAfter(index, bytes, registerTcpDTO);
    }

    /**
     * 解析消息体前 解析通用消息
     *
     * @param index
     * @param bytes
     * @param dto
     * @return
     */
    private static int parseDataBodyAfter(int index, byte[] bytes, BaseTcpDTO dto) {
        // 校验位
        short crc = ByteBuffer.wrap(new byte[]{bytes[index], bytes[index + 2]}).getShort();
        index += 2;
        dto.setCrc(crc);
        log.info("校验位: " + crc);

        // 尾部
        String tail = new String(Arrays.copyOfRange(bytes, index, index + 3), StandardCharsets.US_ASCII);
        dto.setTail(tail);
        index += 3;
        log.info("尾部: " + tail);
        return index;
    }

    /**
     * 解析消息体后解析通用消息
     *
     * @param index
     * @param bytes
     * @param entity
     * @return
     */
    private static int paresDataBodyBefore(int index, byte[] bytes, BaseTcpDTO entity) {
        // 头部
        String header = new String(bytes, index, 3, StandardCharsets.US_ASCII);
        index += 3;
        entity.setHeader(header);
        log.info("头部: " + header);

        // 协议版本
        byte version = bytes[index++];
        String versionStr = ParseTCPUtil.hexToVersionString(version);
        entity.setVersion(versionStr);
        log.info("协议版本: " + versionStr);

        // 厂商标识
        String manufacturerId = new String(bytes, index, 2, StandardCharsets.US_ASCII);
        index += 2;
        entity.setManufacturerId(manufacturerId);
        log.info("厂商标识: " + manufacturerId);

        // 消息序列号
        short sequenceNumber = ByteBuffer.wrap(new byte[]{bytes[index], bytes[index + 1]}).getShort();
        index += 2;
        entity.setSequenceNumber(sequenceNumber);
        log.info("消息序列号: " + sequenceNumber);

        // 命令
        byte command = bytes[index++];
        entity.setCommand(command);
        log.info("命令: " + command);

        // 数据长度
        short dataLength = ByteBuffer.wrap(new byte[]{bytes[index], bytes[index + 1]}).getShort();
        index += 1;
        entity.setDataLength(dataLength);
        log.info("数据长度: " + dataLength);
        return index;
    }
}
