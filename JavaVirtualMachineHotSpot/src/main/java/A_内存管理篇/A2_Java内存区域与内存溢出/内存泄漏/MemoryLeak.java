package A_内存管理篇.A2_Java内存区域与内存溢出.内存泄漏;

import java.util.Objects;

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
 * @Date 2022/2/25
 * @Description:
 */
public class MemoryLeak {
    private int age;

    public int getAge() {
        return age;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MemoryLeak that = (MemoryLeak) o;
        return age == that.age;
    }

    @Override
    public int hashCode() {
        return Objects.hash(age);
    }

    public void setAge(int age) {
        this.age = age;
    }
}
