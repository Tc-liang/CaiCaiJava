package A_内存管理篇.A2_Java内存区域与内存溢出.字符串与字符串常量池;

import java.util.ArrayList;

/**
 * ████        █████        ████████     █████ █████
 * ░░███      ███░░░███     ███░░░░███   ░░███ ░░███
 * ░███     ███   ░░███   ░░░    ░███    ░███  ░███ █
 * ░███    ░███    ░███      ███████     ░███████████
 * ░███    ░███    ░███     ███░░░░      ░░░░░░░███░█
 * ░███    ░░███   ███     ███      █          ░███░
 * █████    ░░░█████░     ░██████████          █████
 * ░░░░░       ░░░░░░      ░░░░░░░░░░          ░░░░░
 * <p>
 * 大吉大利            永无BUG
 *
 * @author Tcl
 * @Date 2022/3/5
 * @Description: 测试字符串常量池OOM
 * JDK8 字符串常量池 在堆中
 * -Xmx10m
 * -Xms10m
 * OOM
 */
public class TestStringConstantPool {
    public static void main(String[] args) {
        String s = "1";
        ArrayList list = new ArrayList<String>();
        while (true) {
            s.intern();
            s += "i";
            list.add(s);
        }
    }
}
