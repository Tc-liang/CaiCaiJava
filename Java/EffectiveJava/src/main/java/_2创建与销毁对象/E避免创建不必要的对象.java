package _2创建与销毁对象;

/**
 * @author: 菜菜的后端私房菜
 * @create: 2024/3/5 16:43
 * @description:
 */
public class E避免创建不必要的对象 {

    public static void main(String[] args) {
        String a = new String("菜菜的后端私房菜");
        String b = "菜菜的后端私房菜";
        //自动拆装箱
        Long sum = 0L;

        for (long i = 0; i <= Integer.MAX_VALUE; i++) {
            sum += i;
//            Long.valueOf(sum = sum.longValue() + i);
        }
    }
}
