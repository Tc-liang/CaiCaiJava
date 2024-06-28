package I_SyncFuture;

/**
 * @author: 菜菜的后端私房菜
 * @create: 2024/6/27 15:10
 * @description: 消息结果
 */


public class MsgResponse {
    /**
     * 消息ID
     */
    private String msgId;

    /**
     * 消息体 使用JSON数据 通信
     */
    private String msgBodyJson;

    public String getMsgId() {
        return msgId;
    }

    public void setMsgId(String msgId) {
        this.msgId = msgId;
    }

    public String getMsgBodyJson() {
        return msgBodyJson;
    }

    public void setMsgBodyJson(String msgBodyJson) {
        this.msgBodyJson = msgBodyJson;
    }

    @Override
    public String toString() {
        return "MsgResponse{" +
                "msgId='" + msgId + '\'' +
                ", msgBodyJson='" + msgBodyJson + '\'' +
                '}';
    }
}
