package B_字节码与类加载篇.类文件结构;

import org.junit.Test;

import java.io.*;

/**
 * @author 菜菜的后端私房菜
 * @Date 2021/5/14
 * @Description:测试字节码指令
 */
public class ClassCodeTest {
    /**
     * 算术指令
     */
    @Test
    public void test1() {
        double d1 = 10 / 0.0;
        //Infinity
        System.out.println(d1);

        double d2 = 0.0 / 0.0;
        //NaN
        System.out.println(d2);

        //向0取整模式:浮点数转整数
        //5
        System.out.println((int) 5.9);
        //-5
        System.out.println((int) -5.9);


        //向最接近数舍入模式:浮点数运算
        //0.060000000000000005
        System.out.println(0.05 + 0.01);

        //抛出ArithmeticException: / by zero异常
        System.out.println(1 / 0);
    }


    /**
     * 宽化类型转换
     */
    @Test
    public void test2() {
        long l1 = 123412345L;
        long l2 = 1234567891234567899L;

        float f1 = l1;
        //结果: 1.23412344E8 => 123412344
        //                l1 =  123412345L
        System.out.println(f1);

        double d1 = l2;
        //结果: 1.23456789123456794E18 => 1234567891234567940
        //                          l2 =  1234567891234567899L
        System.out.println(d1);
    }

    /**
     * 窄化类型转换
     */
    @Test
    public void test3() {
        double d1 = Double.NaN;
        double d2 = Double.POSITIVE_INFINITY;

        int i1 = (int) d1;
        int i2 = (int) d2;
        //0
        System.out.println(i1);
        //true
        System.out.println(i2 == Integer.MAX_VALUE);

        long l1 = (long) d1;
        long l2 = (long) d2;
        //0
        System.out.println(l1);
        //true
        System.out.println(l2 == Long.MAX_VALUE);

        float f1 = (float) d1;
        float f2 = (float) d2;
        //NaN
        System.out.println(f1);
        //Infinity
        System.out.println(f2);


    }

    /**
     * 创建指令
     */
    @Test
    public void test4() {
        String[][] s = new String[10][];
        s[0] = new String[5];
    }


    /**
     * 字段访问指令
     */
    @Test
    public void test5() {
        int getStatic = Field.staticField;
        Field.staticField = 10;

        Field field = new Field();

        int getField = field.field;
        field.field = 10;
    }


    /**
     * 数组操作指令
     */
    @Test
    public void test6() {
        byte[] bytes = new byte[1024];
        bytes[10] = 100;
        byte b1 = bytes[10];
        int length = bytes.length;
    }

    /**
     * 类型检查指令
     */
    @Test
    public String test7(Object object) {
        if (object instanceof String) {
            return (String) object;
        }
        return "";
    }

    /**
     * 条件跳转语句
     */
    @Test
    public int test8(double d) {
        if (d > 100) {
            return 1;
        } else if (d < 100) {
            return -1;
        } else {
            return 0;
        }
    }

    /**
     * 比较条件跳转指令
     */
    @Test
    public void test9() {
        int i1 = 10;
        int i2 = 100;
        boolean b = i1 > i2;
    }

    /**
     * 多条件分支语句
     */
    @Test
    public void test10(int i) {
        int num;
        switch (i) {
            case 1:
                num = 123;
                break;
            case 2:
                num = 321;
                break;
            case 3:
                num = 213;
                break;
            default:
                num = 231;
        }
    }

    @Test
    public void test11(String s) {
        int num;
        switch (s) {
            case "s1":
                num = 123;
                break;
            case "s2222":
                num = 321;
                break;
            case "s33333333":
                num = 213;
                break;
            default:
                num = 231;
        }
    }

    @Test
    public void test12() {
        int i = 10;
        if (i == 0) {
            throw new RuntimeException("asd");
        }
    }

    @Test
    public void test13() {
        String fileName = "123";
        try (FileInputStream fis = new FileInputStream(fileName)) {

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public synchronized void test14() {
        int i = 100;
    }


    private Object obj = new Object();

    @Test
    public void test15() {
        int i0;
        String s = "123";
        synchronized (s) {
            i0 = 100;
        }
    }
}

class Field {
    static int staticField = 0;
    int field = 0;
}
