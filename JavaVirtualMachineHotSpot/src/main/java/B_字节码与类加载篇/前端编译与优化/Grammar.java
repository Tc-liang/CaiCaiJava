package B_字节码与类加载篇.前端编译与优化;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;

/**
 * @author Tcl
 * @Date 2021/5/24
 * @Description:
 */
public class Grammar {
    public static <T> T[] listToArray(List<T> list,Class<T> componentType){
        T[] instance = (T[]) Array.newInstance(componentType, list.size());
        return instance;
    }

    public static void main(String[] args) {
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);
        int sum = 0;
        for (Integer temp : list) {
            sum += temp;
        }
        System.out.println(sum);
    }

    public void test3(){
        if (true){
            System.out.println("ture");
        }else{
            System.out.println("false");
        }

        if (false){
            System.out.println("false");
        }else{
            System.out.println("true");
        }
    }
}
