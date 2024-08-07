package _2创建与销毁对象;

/**
 * @author: 菜菜的后端私房菜
 * @create: 2024/3/6 15:07
 * @description:
 */
public class F消除过期对象引用 {
    static class MyObject {

    }

    public static void main(String[] args) {
        MyObject[] arr = new MyObject[]{new MyObject(), new MyObject()};
        System.out.println(arr.length);
        //删除操作

    }
}
