package A_内存管理篇.A2_Java内存区域与内存溢出.逃逸分析;

import java.util.concurrent.TimeUnit;

/**
 * @author 菜菜的后端私房菜
 * @Date 2021/4/28
 * @Description:
 * 逃逸分析-测试标量替换
 * -XX:-EliminateAllocations 不开启标量替换
 */
public class ScalarSubstitution {
    static class Man{
        int age;
        int id;

        public Man() {
        }
    }

    public static void createInstance(){
        Man man = new Man();
        man.id = 123;
        man.age = 321;
    }

    public static void main(String[] args) {
        long start = System.currentTimeMillis();

        for (int i = 0; i < 10000000; i++) {
            createInstance();
        }

        System.out.println("花费时间:"+(System.currentTimeMillis()-start)+"ms");
    }
}
