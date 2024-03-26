package _4类与接口.F类层次优于标签类.类层次;

/**
 * @author: 菜菜的后端私房菜
 * @create: 2024/3/22 8:37
 * @description:
 */
public class Circle extends AbstractFigure {
    private final double radius;

    public Circle(double radius) {
        this.radius = radius;
    }

    @Override
    public double getArea() {
        return radius * radius * Math.PI;
    }
}
