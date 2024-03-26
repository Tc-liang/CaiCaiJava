package _3对于所有对象都通用的方法;

/**
 * @author: 菜菜的后端私房菜
 * @create: 2024/3/7 16:12
 * @description:
 */
public class B始终要重写toString {

    static class Message {
        private String num;
        private String context;
        private String name;

        public Message(String num, String context, String name) {
            this.num = num;
            this.context = context;
            this.name = name;
        }

        //只关心消息编码与内容
        @Override
        public String toString() {
            return "Message{" +
                    "num='" + num + '\'' +
                    ", context='" + context + '\'' +
                    '}';
        }
    }


    public static void main(String[] args) {
        Message message = new Message("caicai1314", "菜菜的后端私房菜1314", "菜菜名称");
        System.out.println(message);
    }

}
