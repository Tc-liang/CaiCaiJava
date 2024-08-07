package B_字节码与类加载篇.虚拟机类加载机制;

import org.junit.Test;
import sun.misc.Launcher;

import java.net.URL;

/**
 * @author 菜菜的后端私房菜
 * @Date 2021/4/25
 * @Description:测试类加载器能够加载api的路径
 */
public class TestClassLoader {
    public static void main(String[] args) {
        URL[] urLs = Launcher.getBootstrapClassPath().getURLs();
        System.out.println("启动类加载器能加载的api路径:");
//        file:/D:/Environment/jdk1.8.0_191/jre/lib/resources.jar
//        file:/D:/Environment/jdk1.8.0_191/jre/lib/rt.jar
//        file:/D:/Environment/jdk1.8.0_191/jre/lib/sunrsasign.jar
//        file:/D:/Environment/jdk1.8.0_191/jre/lib/jsse.jar
//        file:/D:/Environment/jdk1.8.0_191/jre/lib/jce.jar
//        file:/D:/Environment/jdk1.8.0_191/jre/lib/charsets.jar
//        file:/D:/Environment/jdk1.8.0_191/jre/lib/jfr.jar
//        file:/D:/Environment/jdk1.8.0_191/jre/classes
        for (URL urL : urLs) {
            System.out.println(urL);
        }


        System.out.println("扩展类加载器能加载的api路径:");
        String property = System.getProperty("java.ext.dirs");
        //        D:\Environment\jdk1.8.0_191\jre\lib\ext;C:\WINDOWS\Sun\Java\lib\ext
        System.out.println(property);


        ClassLoader appClassLoader = TestClassLoader.class.getClassLoader();
        //sun.misc.Launcher$AppClassLoader@18b4aac2
        System.out.println(appClassLoader);

        ClassLoader extClassloader = appClassLoader.getParent();
        //sun.misc.Launcher$ExtClassLoader@511d50c0
        System.out.println(extClassloader);

        ClassLoader bootClassloader = extClassloader.getParent();
        //null
        System.out.println(bootClassloader);

        int[] ints = new int[10];
        //null
        System.out.println(ints.getClass().getClassLoader());

        String[] strings = new String[10];
        //null
        System.out.println(strings.getClass().getClassLoader());

        TestClassLoader[] testClassLoaderArray = new TestClassLoader[10];
        //sun.misc.Launcher$AppClassLoader@18b4aac2
        System.out.println(testClassLoaderArray.getClass().getClassLoader());

        //sun.misc.Launcher$AppClassLoader@18b4aac2
        System.out.println(Thread.currentThread().getContextClassLoader());
    }

    @Test
    public void test() {

    }
}

class User {
    private String name;
}
