package A_内存管理篇.A1_HotSpot对象探秘;

import java.nio.ByteBuffer;
import java.util.Scanner;

/**
 * @author Tcl
 * @Date 2021/4/29
 * @Description:测试直接内存
 *
 * 直接内存用于NIO
 * 申请1G 后打开任务管理器找到占用1G的Java进程 （或通过JPS查找）
 * 输入任意字符释放内存后 手动full gc 回收直接内存 查看内存占用情况
 */
public class C_LocalMemory {
    //1GB
    private static final int BUFFER = 1024 * 1024 * 1024 ;

    public static void main(String[] args) {
        ByteBuffer buffer = ByteBuffer.allocateDirect(BUFFER);
        System.out.println("申请了1GB内存");

        System.out.println("输入任意字符释放内存");
        Scanner scanner = new Scanner(System.in);
        scanner.next();

        System.out.println("释放内存成功");
        buffer=null;
        System.gc();
        while (!scanner.next().equalsIgnoreCase("exit")){

        }
        System.out.println("退出程序");
    }
}




































