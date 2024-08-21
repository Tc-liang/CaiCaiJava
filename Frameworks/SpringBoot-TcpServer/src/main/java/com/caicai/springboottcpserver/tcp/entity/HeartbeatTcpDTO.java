package com.caicai.springboottcpserver.tcp.entity;

import lombok.Data;

/**
 * @author: 菜菜的后端私房菜
 * @create: 2024/8/15 17:25
 * @description:
 */
@Data
public class HeartbeatTcpDTO extends BaseTcpDTO {
    // 设备编号，用于唯一标识设备
    private String deviceId;
}
