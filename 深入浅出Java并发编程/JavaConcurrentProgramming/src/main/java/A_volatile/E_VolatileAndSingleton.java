package A_volatile;

import java.util.Objects;

/**
 * @Author: Caicai
 * @Date: 2023-08-24 21:36
 * @Description:volatile与单例-双重检测锁
 *
 */
public class E_VolatileAndSingleton {
    private static volatile Singleton singleton;

    public static Singleton getSingleton(){
        if (Objects.isNull(singleton)){
            //有可能很多线程阻塞到拿锁,拿完锁再判断一次
            synchronized (Singleton.class){
                if (Objects.isNull(singleton)){
                    singleton = new Singleton();
                }
            }
        }

        return singleton;
    }

    static class Singleton{

    }
}
