package _4类与接口.E接口只用于定义类型;

/**
 * @author: 菜菜的后端私房菜
 * @create: 2024/3/21 17:56
 * @description:
 */
public class InterfaceConstanceTest implements InterfaceConstance{
    public static void main(String[] args) {
        InterfaceConstance test = new InterfaceConstanceTest();

        String aConstance = InterfaceConstance.A_CONSTANCE;
        System.out.println(aConstance);

        String bConstance = InterfaceConstance.B_CONSTANCE;
        System.out.println(bConstance);
    }
}
