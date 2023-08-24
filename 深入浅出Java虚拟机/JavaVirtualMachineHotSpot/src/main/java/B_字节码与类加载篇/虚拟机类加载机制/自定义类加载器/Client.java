package B_字节码与类加载篇.虚拟机类加载机制.自定义类加载器;

import java.lang.reflect.Method;
import java.net.URL;
import java.util.concurrent.TimeUnit;

/**
 * @author Tcl
 * @Date 2021/5/17
 * @Description:
 */
public class Client {
    public static void main(String[] args) {
//        try {
//            Class<?> classLoader = myClassLoader.loadClass("B_字节码与类加载篇.虚拟机类加载机制.自定义类加载器.HotTest");
//            System.out.println("类加载器为:" + classLoader.getClassLoader().getClass().getName());
//            System.out.println("父类加载器为" + classLoader.getClassLoader().getParent().getClass().getName());
//        } catch (ClassNotFoundException e) {
//            e.printStackTrace();
//        }


        //测试热替换  修改HotTest 手动编译实现热替换
        try {
            while (true){
                MyClassLoader myClassLoader = new MyClassLoader("D:\\Tcl\\学习\\笔记\\Java\\书籍笔记\\深入理解Java虚拟机\\JavaVirtualMachineHotSpot\\src\\main\\java\\");
                Class<?> aClass = myClassLoader.findClass("B_字节码与类加载篇\\虚拟机类加载机制\\自定义类加载器\\HotTest");
                System.out.println(aClass.getClassLoader().getClass().getName());
                Method hot = aClass.getMethod("hot");
                Object instance = aClass.newInstance();
                Object invoke = hot.invoke(instance);
                TimeUnit.SECONDS.sleep(3);
            }
        } catch (Exception e){
            e.printStackTrace();
        }

//        解释如果类加载器不同那么它们肯定不是同一个类
//        MyClassLoader myClassLoader1 = new MyClassLoader("D:\\Code\\JavaVirtualMachineHotSpot\\src\\main\\java\\");
//        MyClassLoader myClassLoader2 = new MyClassLoader("D:\\Code\\JavaVirtualMachineHotSpot\\src\\main\\java\\");
//        try {
//            Class<?> aClass1 = myClassLoader1.findClass("Test");
//            Class<?> aClass2 = myClassLoader2.findClass("Test");
//            System.out.println(aClass1 == aClass2);//false
//        } catch (ClassNotFoundException e) {
//            e.printStackTrace();
//        }

    }
}
