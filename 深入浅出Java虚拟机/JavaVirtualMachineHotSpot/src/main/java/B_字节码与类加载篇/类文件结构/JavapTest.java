package B_字节码与类加载篇.类文件结构;

/**
 * @author 菜菜的后端私房菜
 * @Date 2021/5/13
 * @Description:
 */
public class JavapTest {
    private int a = 1;
    float b = 2.1F;
    protected double c = 3.5;
    public  int d = 10;

    private void test(int i){
        i+=1;
        System.out.println(i);
    }

    public void test1(){
        String s = "test1";
        System.out.println(s);
    }
}
