package B_字节码与类加载篇.虚拟机类加载机制.自定义类加载器;

/**
 *  javac -encoding UTF-8 .\HotTest.java
 */
public class HotTest {
    public void hot(){
        System.out.println("热替换!!!!!!!");
    }

    public static void main(String[] args) {
        HotTest test = new HotTest();
        test.hot();
    }
}
