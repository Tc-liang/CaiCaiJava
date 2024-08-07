package BigDecimal;

import sun.security.krb5.internal.crypto.DesCbcMd5EType;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * @author 菜菜的后端私房菜
 * @create: 2024/8/7 9:37
 * @description:
 */
public class BigDecimalTest {
    public static void main(String[] args) {
//        createInstance();
//        toPlainString();
//        compare();
//        calc();
        priceCalc();


    }

    /**
     * valueOf 在数据大的情况下 toString 会使用科学计数法，可以使用toPlainString
     * <p>
     * 而string 构造方法不会使用科学计数法
     */
    private static void toPlainString() {
        BigDecimal d1 = new BigDecimal("123456789012345678901234567890.12345678901234567890");
        BigDecimal d2 = BigDecimal.valueOf(123456789012345678901234567890.12345678901234567890);

        //123456789012345678901234567890.12345678901234567890
        System.out.println(d1);
        //123456789012345678901234567890.12345678901234567890
        System.out.println(d1.toPlainString());

        //1.2345678901234568E+29
        System.out.println(d2);
        //123456789012345678901234567890.12345678901234567890
        System.out.println(d2.toPlainString());
    }


    /**
     * 多件商品平摊价格总和,除不尽时可以把余数加到最后一件商品进行兜底
     */
    private static void priceCalc() {
        //总价
        BigDecimal total = BigDecimal.valueOf(10.00);
        //商品数量
        int num = 3;
        BigDecimal count = BigDecimal.valueOf(num);
        //每件商品价格
        BigDecimal price = total.divide(count, 2, RoundingMode.HALF_UP);
        //3.33
        System.out.println(price);

        //剩余的价格 加到最后一件商品 兜底
        BigDecimal residue = total.subtract(price.multiply(count));
        //最后一件价格
        BigDecimal lastPrice = price.add(residue);
        //3.34
        System.out.println(lastPrice);
    }

    /**
     * 加减运算会选择两数中最大的小数位数作为结果的小数位数
     * <p>
     * 乘法运算会把两数的小数位数相加作为结果的小数位数
     * <p>
     * 除法运算由于算法没固定的结果小数位数,必须设置小数位数和舍入模式
     * <p>
     * 除了除法外其他运算也建议主动设置小数位数和舍入模式
     * <p>
     * 计算完要赋新值，否则结果会丢失（不可变对象）
     */
    private static void calc() {
        BigDecimal d1 = BigDecimal.valueOf(1.00);
        BigDecimal d2 = BigDecimal.valueOf(5.555);

        //1.0
        System.out.println(d1);
        //5.555
        System.out.println(d2);
        //6.555
        System.out.println(d1.add(d2));
        //-4.555
        System.out.println(d1.subtract(d2));
        //5.5550
        System.out.println(d1.multiply(d2));

        BigDecimal d3 = d2.divide(d1);
        BigDecimal d4 = d3.setScale(2, RoundingMode.HALF_UP);
        BigDecimal d5 = d2.divide(d1, 2, RoundingMode.HALF_UP);
        //5.555
        System.out.println(d3);
        //5.56
        System.out.println(d4);
        //5.56
        System.out.println(d5);
    }


    /**
     * equals会比较小数位,精度不同会不相等
     * <p>
     * 比较数值相同使用compareTo
     */
    private static void compare() {
        BigDecimal d1 = BigDecimal.valueOf(1);
        BigDecimal d2 = BigDecimal.valueOf(1.00);

        // false
        System.out.println(d1.equals(d2));
        // 0
        System.out.println(d1.compareTo(d2));
    }

    /**
     * 构造创建实例时不能用浮点型,会存在精度溢出
     * <p>
     * 可以使用字符串或valueOf
     */
    private static void createInstance() {
        BigDecimal d1 = new BigDecimal(6.66);
        BigDecimal d2 = new BigDecimal("6.66");
        BigDecimal d3 = BigDecimal.valueOf(6.66);

        //6.660000000000000142108547152020037174224853515625
        System.out.println(d1);
        //6.66
        System.out.println(d2);
        //6.66
        System.out.println(d3);
    }
}
