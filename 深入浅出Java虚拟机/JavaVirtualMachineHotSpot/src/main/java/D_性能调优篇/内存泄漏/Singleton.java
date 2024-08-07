package D_性能调优篇.内存泄漏;

/**
 * @author 菜菜的后端私房菜
 * @Date 2021/5/20
 * @Description: 饿汉式单例模式使单例对象生命周期变长
 */
public class Singleton {
    private static final Singleton INSTANCE = new Singleton();

    private Singleton(){
        if (INSTANCE!=null){
            throw new RuntimeException("not create instance");
        }
    }

    public static Singleton getInstance(){
        return INSTANCE;
    }
}
