package _4类与接口.F类层次优于标签类.标签类;

//标签类
class Figure {
    //标识类型：矩形、圆形
    enum Shape {RECTANGLE, CIRCLE}

    Shape shape;
    //宽高用于矩形计算
    double width, height;
    //半径用于圆形计算
    double radius;

    double getArea() {
        switch (shape) {
            case RECTANGLE:
                return width * height;
            case CIRCLE:
                return Math.PI * radius * radius;
            default:
                throw new IllegalArgumentException("Unknown shape");
        }
    }
}