package _4类与接口.F类层次优于标签类.类层次;

/**
 * @author: 菜菜的后端私房菜
 * @create: 2024/3/22 8:35
 * @description:
 */
public class Rectangle extends AbstractFigure {
    private final double width;
    private final double height;

    public Rectangle(double width, double height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public double getArea() {
        return width * height;
    }
}
