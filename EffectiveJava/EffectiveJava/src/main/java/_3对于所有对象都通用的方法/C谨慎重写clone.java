package _3对于所有对象都通用的方法;

/**
 * @author: 菜菜的后端私房菜
 * @create: 2024/3/7 16:22
 * @description:
 */
public class C谨慎重写clone {

    static class CloneA implements Cloneable {
        private int num;

        public CloneA(int num) {
            this.num = num;
        }

        @Override
        protected CloneA clone() throws CloneNotSupportedException {
            return (CloneA) super.clone();
        }

        @Override
        public String toString() {
            return "Clone1{" +
                    "num=" + num +
                    '}';
        }
    }

    static class CloneObject implements Cloneable {
        private int num;
        private CloneA cloneA = new CloneA(99);

        public CloneObject(int num) {
            this.num = num;
        }

        @Override
        protected CloneObject clone() throws CloneNotSupportedException {
            CloneObject res = (CloneObject) super.clone();
            res.cloneA = cloneA.clone();
            return res;
        }

        @Override
        public String toString() {
            return "CloneObject{" +
                    "num=" + num +
                    ", cloneA=" + cloneA +
                    '}';
        }
    }

    public static void main(String[] args) throws CloneNotSupportedException {
        CloneObject c2 = new CloneObject(2);
        System.out.println(c2);

        CloneObject c2Clone = c2.clone();
        System.out.println(c2Clone);

        c2.cloneA = new CloneA(2);
        System.out.println(c2);
        System.out.println(c2Clone);
    }
}
