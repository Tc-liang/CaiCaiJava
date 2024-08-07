package _4类与接口.G静态内部类优于非静态内部类;

/**
 * @author: 菜菜的后端私房菜
 * @create: 2024/3/22 9:09
 * @description:
 */
public class InternalClass {

    nonStatic getNonStatic() {
        return new nonStatic();
    }

    final class nonStatic {

    }

    static staticClass getStaticClass() {
        return new staticClass();
    }

    static class staticClass {

    }

    public static void main(String[] args) {
        InternalClass internalClass = new InternalClass();
        nonStatic nonStatic = internalClass.new nonStatic();
        staticClass staticClass = new staticClass();
    }
}
