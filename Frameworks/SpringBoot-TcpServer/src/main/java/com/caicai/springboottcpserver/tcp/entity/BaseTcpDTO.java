package com.caicai.springboottcpserver.tcp.entity;

import lombok.Data;

/**
 * @author: 菜菜的后端私房菜
 * @create: 2024/8/15 15:14
 * @description: TCP消息实体的基本父类 通用字段
 */
@Data
public class BaseTcpDTO {
    // 头部，用于标识消息格式
    private String header;

    // 协议版本，用于确定消息遵循的协议版本
    private String version;

    // 厂商标识，用于识别设备制造商
    private String manufacturerId;

    // 消息序列号，用于跟踪消息序列
    private short sequenceNumber;

    // 命令，用于指示消息类型或请求动作
    // 1注册 2事件上报 3指令下发 4下发指令回复 5长连接数据包 FF服务器回复
    private byte command;

    // 数据长度，用于指示有效载荷的大小
    private short dataLength;

    // 校验位，用于数据完整性检查
    private short crc;

    // 尾部，用于结束消息
    private String tail;

    //响应状态 0成功 1CRC校验失败 2数据解析失败 3厂商校验失败
    private int status;
}
