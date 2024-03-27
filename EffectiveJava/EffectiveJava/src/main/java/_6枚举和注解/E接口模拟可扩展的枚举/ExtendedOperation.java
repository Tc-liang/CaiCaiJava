package _6枚举和注解.E接口模拟可扩展的枚举;

import java.util.*;

//扩展枚举 幂、模
public enum ExtendedOperation implements Operation {
    EXP("^") {
        public double apply(double x, double y) {
            return Math.pow(x, y);
        }
    },
    REMAINDER("%") {
        public double apply(double x, double y) {
            return x % y;
        }
    };
    private final String symbol;

    ExtendedOperation(String symbol) {
        this.symbol = symbol;
    }

    @Override
    public String toString() {
        return symbol;
    }

    public static void main(String[] args) {
        double x = Double.parseDouble("9.0");
        double y = Double.parseDouble("1.0");
        //9.000000 + 1.000000 = 10.000000
        //9.000000 - 1.000000 = 8.000000
        //9.000000 * 1.000000 = 9.000000
        //9.000000 / 1.000000 = 9.000000
        test(Arrays.asList(BasicOperation.values()), x, y);

        //9.000000 ^ 1.000000 = 9.000000
        //9.000000 % 1.000000 = 0.000000
        test(Arrays.asList(ExtendedOperation.values()), x, y);
    }

    private static void test(Collection<? extends Operation> opSet,
                             double x, double y) {
        for (Operation op : opSet)
            System.out.printf("%f %s %f = %f%n",
                    x, op, y, op.apply(x, y));
    }
}