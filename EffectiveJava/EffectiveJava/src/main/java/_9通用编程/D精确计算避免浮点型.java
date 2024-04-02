package _9通用编程;

import java.math.BigDecimal;

/**
 * @author: 菜菜的后端私房菜
 * @create: 2024/4/2 9:59
 * @description:
 */
public class D精确计算避免浮点型 {
    public static void main(String[] args) {
        doubleMethod();
        intMethod();
        bigDecimalMethod();
    }

    private static void bigDecimalMethod() {
        final BigDecimal TEN_CENTS = new BigDecimal(".10");

        int itemsBought = 0;
        BigDecimal funds = new BigDecimal("1.00");
        for (BigDecimal price = TEN_CENTS;
             funds.compareTo(price) >= 0;
             price = price.add(TEN_CENTS)) {
            funds = funds.subtract(price);
            itemsBought++;
        }
        System.out.println(itemsBought + " items bought.");
        System.out.println("Money left over: $" + funds);
    }

    private static void doubleMethod() {
        //拥有的钱 1.00
        double funds = 1.00;
        int itemsBought = 0;
        //每次都买商品递增 0.1
        //每次价格 0.1、0.2、0.3、0.4 期望买四个商品 剩下的钱为0
        for (double price = 0.10; funds >= price; price += 0.10) {
            funds -= price;
            itemsBought++;
        }
        //3 items bought.
        System.out.println(itemsBought + " items bought.");
        //Change: $0.3999999999999999
        System.out.println("Change: $" + funds);
    }

    private static void intMethod() {
        //去除小数、使用整形
        int funds = 100;
        int itemsBought = 0;
        for (int price = 10; funds >= price; price += 10) {
            funds -= price;
            itemsBought++;
        }
        //4 items bought.
        System.out.println(itemsBought + " items bought.");
        //Cash left over: 0 cents
        System.out.println("Cash left over: " + funds + " cents");
    }
}
