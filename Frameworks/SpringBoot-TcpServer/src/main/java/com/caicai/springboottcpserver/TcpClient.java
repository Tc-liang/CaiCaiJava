package com.caicai.springboottcpserver;

import com.alibaba.fastjson.JSON;
import com.caicai.springboottcpserver.tcp.ParseTCPUtil;
import com.caicai.springboottcpserver.tcp.entity.BaseTcpDTO;
import com.caicai.springboottcpserver.tcp.entity.RegisterTcpDTO;
import org.springframework.beans.BeanUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static ch.qos.logback.core.encoder.ByteArrayUtil.hexStringToByteArray;

public class TcpClient {

    public static String hexStringToAscii(String hexString) {
        StringBuilder output = new StringBuilder();
        for (int i = 0; i < hexString.length(); i += 2) {
            String str = hexString.substring(i, i + 2);
            output.append((char) Integer.parseInt(str, 16));
        }
        return output.toString();
    }

    public static String asciiStringToHex(String asciiString) {
        StringBuilder hexString = new StringBuilder();
        for (char ch : asciiString.toCharArray()) {
            String hex = Integer.toHexString(ch);
            if (hex.length() == 1) {
                hexString.append('0'); // Ensure two digits for each byte
            }
            hexString.append(hex);
        }
        return hexString.toString().toUpperCase(); // Convert to uppercase for consistency
    }

    public static void main(String[] args) {
        //注册
//        String asciiString = "57544B10687A0001013701383639393735303334343431303832343630313133313138373433373332383938363131323032323430313433393837363225156410687A454E44";

        //事件上报
//        String asciiString = "57544B10687A00010215033836393937353033343434313038320125156410687A454E44";

        //长连接数据包
        String asciiString = "57544B10687A0001050F383639393735303334343431303832687A454E44";
        byte[] bytes = getBytesByHex(asciiString);
        tcpClient(bytes);
    }

    private static byte[] getBytesByHex(String asciiString) {
        byte[] bytes = hexStringToByteArray(asciiString);
        System.out.println(bytes);
        System.out.println(new String(bytes));
        return bytes;
    }


    private static void tcpClient(byte[] bytes) {
        // 服务器的地址和端口
        String serverAddress = "localhost";
        int serverPort = 1111;

        try (
                // 创建一个新的Socket，连接到指定的服务器和端口
                Socket socket = new Socket(serverAddress, serverPort);
                // 创建PrintWriter用于向服务器发送数据
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                // 创建BufferedReader用于从服务器读取数据（这里仅用于示例，实际发送数据后可能不需要立即读取）
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        ) {
            System.out.println("Connected to the server.");

            // 向服务器发送字符串数据
            String res = new String(bytes);
            System.out.println(res);
            out.println(res);

            // 接收服务器的响应
            System.out.println(in.readLine());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private static String parse(byte[] bytes) {
        BaseTcpDTO dto = new BaseTcpDTO();
        int index = 0;
        index = paresDataBefore(index, bytes, dto);

        //根据命令位继续解析
        byte command = dto.getCommand();
        if (command == 1) {
            //注册
            RegisterTcpDTO registerTcpDTO = new RegisterTcpDTO();
            BeanUtils.copyProperties(dto, registerTcpDTO);
            registerParse(index, bytes, registerTcpDTO);
        }

        System.out.println(dto);
        return JSON.toJSONString(dto);
    }

    private static int registerParse(int index, byte[] bytes, RegisterTcpDTO registerTcpDTO) {
        // 设备类型
        byte deviceType = bytes[index++];
        registerTcpDTO.setDeviceType(deviceType);
        System.out.println("设备类型: " + deviceType);

        // 设备编号
        String deviceId = new String(Arrays.copyOfRange(bytes, index, index + 15), StandardCharsets.US_ASCII);
        index += 15;
        registerTcpDTO.setDeviceId(deviceId);
        System.out.println("设备编号: " + deviceId);

        // IMSI
        String imsi = new String(Arrays.copyOfRange(bytes, index, index + 15), StandardCharsets.US_ASCII);
        index += 15;
        registerTcpDTO.setImsi(imsi);
        System.out.println("IMSI: " + imsi);

        // ICCID
        String iccid = new String(Arrays.copyOfRange(bytes, index, index + 20), StandardCharsets.US_ASCII);
        index += 20;
        registerTcpDTO.setIccid(iccid);
        System.out.println("ICCID: " + iccid);

        // 电池电压
        byte batteryVoltage = bytes[index++];
        BigDecimal decimal = new BigDecimal(batteryVoltage)
                .multiply(new BigDecimal("0.1"))
                .setScale(1, RoundingMode.DOWN);
        String batteryVoltageStr = decimal.toString() + "V";
        registerTcpDTO.setBatteryVoltage(batteryVoltageStr);
        System.out.println("电池电压: " + batteryVoltageStr);

        // CSQ信号强度
        byte csq = bytes[index++];
        registerTcpDTO.setCsq(csq);
        System.out.println("CSQ信号强度: " + csq);

        // 电池电量
        byte batteryLevel = bytes[index++];
        String batteryLevelStr = batteryLevel + "%";
        registerTcpDTO.setBatteryLevel(batteryLevelStr);
        System.out.println("电池电量: " + batteryLevelStr);

        // 程序版本
        byte softwareVersion = bytes[index++];
        String versionStr = ParseTCPUtil.hexToVersionString(softwareVersion);
        registerTcpDTO.setSoftwareVersion(versionStr);
        System.out.println("程序版本: " + versionStr);


        return parseDataAfter(index, bytes, registerTcpDTO);
    }

    private static int parseDataAfter(int index, byte[] bytes, BaseTcpDTO dto) {
        // 校验位
        short crc = ByteBuffer.wrap(new byte[]{bytes[index], bytes[index + 2]}).getShort();
        index += 2;
        dto.setCrc(crc);
        System.out.println("校验位: " + crc);

        // 尾部
        String tail = new String(Arrays.copyOfRange(bytes, index, index + 3), StandardCharsets.US_ASCII);
        dto.setTail(tail);
        index += 3;
        System.out.println("尾部: " + tail);
        return index;
    }

    private static int paresDataBefore(int index, byte[] bytes, BaseTcpDTO entity) {
        // 头部
        String header = new String(bytes, index, 3, StandardCharsets.US_ASCII);
        index += 3;
        entity.setHeader(header);
        System.out.println("头部: " + header);

        // 协议版本
        byte version = bytes[index++];
        String versionStr = ParseTCPUtil.hexToVersionString(version);
        entity.setVersion(versionStr);
        System.out.println("协议版本: " + versionStr);

        // 厂商标识
        String manufacturerId = new String(bytes, index, 2, StandardCharsets.US_ASCII);
        index += 2;
        entity.setManufacturerId(manufacturerId);
        System.out.println("厂商标识: " + manufacturerId);

        // 消息序列号
        short sequenceNumber = ByteBuffer.wrap(new byte[]{bytes[index], bytes[index + 1]}).getShort();
        index += 2;
        entity.setSequenceNumber(sequenceNumber);
        System.out.println("消息序列号: " + sequenceNumber);

        // 命令
        byte command = bytes[index++];
        entity.setCommand(command);
        System.out.println("命令: " + command);

        // 数据长度
        short dataLength = ByteBuffer.wrap(new byte[]{bytes[index], bytes[index + 1]}).getShort();
        index += 1;
        entity.setDataLength(dataLength);
        System.out.println("数据长度: " + dataLength);
        return index;
    }


}