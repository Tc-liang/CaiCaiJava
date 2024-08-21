package com.caicai.springboottcpserver.tcp;

import io.netty.buffer.ByteBuf;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static io.netty.buffer.ByteBufUtil.appendPrettyHexDump;
import static io.netty.util.internal.StringUtil.NEWLINE;

/**
 * @author: 菜菜的后端私房菜
 * @create: 2024/8/15 15:56
 * @description: 解析TCP报文工具类
 */
public class ParseTCPUtil {
    /**
     * 十六进制转版本字符串
     * <p>
     * 初始版本 1.0，小版本修复更新，每次累加0.1，大版本更新，直接加 1，1.0 上报数据为0x10，1.1 对应0x11
     *
     * @param hexVersion 十六进制
     * @return 版本字符串
     */
    public static String hexToVersionString(byte hexVersion) {
        int intValue = hexVersion & 0xFF; // 将字节转换为无符号整数
        int majorVersion = intValue / 16; // 计算主版本号
        int minorVersion = intValue % 16; // 计算次版本号
        return majorVersion + "." + minorVersion; // 返回版本字符串
    }

    /**
     * 转化为字符串电压
     * 在值上乘0.1 加上单位V
     *
     * @param hexVersion
     * @return 电压
     */
    public static String toBatteryVoltageString(byte hexVersion) {
        return new BigDecimal("0.1")
                .multiply(new BigDecimal(hexVersion))
                .setScale(1, RoundingMode.DOWN)
                .toString() + "V";
    }


    public static void log(ByteBuf buffer) {
        int length = buffer.readableBytes();
        int rows = length / 16 + (length % 15 == 0 ? 0 : 1) + 4;
        StringBuilder buf = new StringBuilder(rows * 80 * 2)
                .append("read:").append(buffer.readerIndex())
                .append(" write:").append(buffer.writerIndex())
                .append(" capacity:").append(buffer.capacity())
                .append(NEWLINE);
        appendPrettyHexDump(buf, buffer);
        System.out.println(buf);
    }
}
