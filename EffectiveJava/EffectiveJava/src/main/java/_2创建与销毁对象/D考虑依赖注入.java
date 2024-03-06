package _2创建与销毁对象;

/**
 * @author: cl
 * @create: 2024/3/5 16:36
 * @description:
 */
public class D考虑依赖注入 {

    static class Util {
        public Util() {
        }
    }

    static class DependencyInjectionNotUse {
        private Util util = new Util();

        public DependencyInjectionNotUse() {
        }
    }

    static class DependencyInjectionUse {
        private Util util;

        public DependencyInjectionUse(Util util) {
            this.util = util;
        }
    }

    public static void main(String[] args) {
        //构造器注入
        DependencyInjectionUse dependencyInjectionUse = new DependencyInjectionUse(new Util());

        //不使用DI
        DependencyInjectionNotUse dependencyInjectionNotUse = new DependencyInjectionNotUse();
    }
}
