package A_内存管理篇.A2_Java内存区域与内存溢出.内存泄漏;

import java.util.HashMap;

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
 * @author 菜菜的后端私房菜
 * @Date 2022/2/25
 * @Description: 对象作为key加入map后，改变它哈希值，导致在哈希表中找不到了，从而内存泄漏
 */
public class Test {
    public static void main(String[] args) {
        HashMap<MemoryLeak, Integer> map = new HashMap<>();

        MemoryLeak leak = new MemoryLeak();
        leak.setAge(100);
        System.out.println(leak.hashCode());
        map.put(leak, 1);

        leak.setAge(200);
        System.out.println(leak.hashCode());
        //null
        System.out.println(map.get(leak));
    }
}
