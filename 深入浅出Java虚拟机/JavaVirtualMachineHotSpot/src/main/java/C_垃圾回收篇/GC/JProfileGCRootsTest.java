package C_垃圾回收篇.GC;

import java.util.Scanner;

/**
 * @author 菜菜的后端私房菜
 * @Date 2021/5/2
 * @Description:使用Jprofile查看GCRoots
 */
public class JProfileGCRootsTest {
    public static void main(String[] args) {
        JProfileGCRootsTest test = new JProfileGCRootsTest();
        final Scanner scanner = new Scanner(System.in);
        //为了方便查看dump文件,"暂停程序"
        scanner.next();

        test = null;

        scanner.next();
    }
}
