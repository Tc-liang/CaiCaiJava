package _10异常;

/**
 * @author: 菜菜的后端私房菜
 * @create: 2024/4/7 8:49
 * @description:
 */
public class B对可恢复的情况使用受检异常 {
    static class MyException extends RuntimeException {
        public MyException(String message, Throwable cause) {
            super(message, cause);
        }

        public MyException(String message) {
            super(message);
        }
    }

    public static void main(String[] args) {
        try {

        } catch (Exception e) {
            throw new MyException("请稍后重试", e);
        }
    }
}
