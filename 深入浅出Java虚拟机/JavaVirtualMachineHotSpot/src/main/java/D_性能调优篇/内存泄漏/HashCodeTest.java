package D_性能调优篇.内存泄漏;

import java.util.*;

/**
 * @author 菜菜的后端私房菜
 * @Date 2021/5/20
 * @Description:
 */
public class HashCodeTest {
    /**
     * 假设该对象实例变量a,d是关键域
     * a,d分别相等的对象逻辑相等
     */
    private final int a;
    private final double d;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HashCodeTest that = (HashCodeTest) o;
        return a == that.a &&
                Double.compare(that.d, d) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(a, d);
    }

    public HashCodeTest(int a, double d) {
        this.a = a;
        this.d = d;
    }


    @Override
    public String toString() {
        return "HashCodeTest{" +
                "a=" + a +
                ", d=" + d +
                '}';
    }

    public static void main(String[] args) {
        HashMap<HashCodeTest, Integer> map = new HashMap<>();

        HashCodeTest h1 = new HashCodeTest(1, 1.5);
        map.put(h1, 100);
        map.put(new HashCodeTest(2, 2.5), 200);
        //修改关键域 导致改变哈希值
        //h1.a=100;

        System.out.println(map.remove(h1));//null

        Set<Map.Entry<HashCodeTest, Integer>> entrySet = map.entrySet();
        for (Map.Entry<HashCodeTest, Integer> entry : entrySet) {
            System.out.println(entry);
        }
        //HashCodeTest{a=100, d=1.5}=100
        //HashCodeTest{a=2, d=2.5}=200

    }
}
