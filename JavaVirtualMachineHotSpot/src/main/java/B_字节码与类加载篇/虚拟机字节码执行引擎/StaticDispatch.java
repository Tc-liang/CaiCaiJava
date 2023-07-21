package B_字节码与类加载篇.虚拟机字节码执行引擎;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Tc.l
 * @Date 2020/11/11
 * @Description:
 * Static dispatch静态分派
 */
public class StaticDispatch {
    public void test(List list){
        System.out.println("list");
    }

//    public void test(ArrayList arrayList){
//        System.out.println("arrayList");
//    }

    public static void main(String[] args) {
        ArrayList arrayList = new ArrayList();
        List list = new ArrayList();
        StaticDispatch staticDispatch = new StaticDispatch();
        staticDispatch.test(list);
        staticDispatch.test(arrayList);
    }
}
