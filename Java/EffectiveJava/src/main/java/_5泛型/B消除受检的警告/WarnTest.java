package _5泛型.B消除受检的警告;

import java.util.ArrayList;
import java.util.List;

/**
 * @author: 菜菜的后端私房菜
 * @create: 2024/3/25 22:15
 * @description:
 */
public class WarnTest {
    public static void main(String[] args) {
        List<Integer> integer = new ArrayList<>();
        //未检查赋值
        List<Integer> integers = new ArrayList();

        List list = new ArrayList();
        list.add(1);
        list.add(11);
        list.add(111);

        @SuppressWarnings("确保list中类型为Integer")
        List<Integer> integerList = list;
    }
}
