package B_字节码与类加载篇.类文件结构;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Tc.l
 * @Date 2020/11/6
 * @Description:
 */
public class Test {
    private int m;
    private final int CONSTANT=111;
    private final List list = new ArrayList();

    public int inc() throws Exception {
        int x;
        try {
            x = 1;
            return x;
        }catch (Exception e){
            x = 2;
            return  x;
        }finally{
            x = 3;
        }
    }

    public static void main(String[] args) {
        Test test = new Test();
        System.out.println(test.m);
    }
}
