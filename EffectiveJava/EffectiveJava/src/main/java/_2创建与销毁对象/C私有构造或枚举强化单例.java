package _2创建与销毁对象;

import java.lang.reflect.Constructor;

/**
 * @author: cl
 * @create: 2024/3/5 15:58
 * @description:
 */
public class C私有构造或枚举强化单例 {
    static class SingletonField {
        public static final SingletonField INSTANCE = new SingletonField();

        private SingletonField() {
        }
    }

    static class SingletonStaticFactory {
        private static final SingletonStaticFactory INSTANCE = new SingletonStaticFactory();

        private SingletonStaticFactory() {
        }

        public static SingletonStaticFactory getInstance() {
            return INSTANCE;
        }
    }

    enum SingletonEnum {
        INSTANCE;

        private SingletonEnum() {
        }
    }

    public static void main(String[] args) throws Exception {
        //私有构造
        SingletonField singletonField = SingletonField.INSTANCE;
        SingletonStaticFactory singletonStaticFactory = SingletonStaticFactory.getInstance();
        SingletonEnum singletonEnum = SingletonEnum.INSTANCE;



        System.out.println(singletonField);
        System.out.println(singletonStaticFactory);

        //反射破坏单例
        Constructor<SingletonField> singletonFieldConstructor = SingletonField.class.getDeclaredConstructor(null);
        singletonFieldConstructor.setAccessible(true);
        Object singletonFieldObj = singletonFieldConstructor.newInstance();
        System.out.println(singletonFieldObj);

        Constructor<SingletonStaticFactory> singletonStaticFactoryConstructor = SingletonStaticFactory.class.getDeclaredConstructor(null);
        singletonStaticFactoryConstructor.setAccessible(true);
        Object singletonStaticFactoryObj = singletonStaticFactoryConstructor.newInstance();
        System.out.println(singletonStaticFactoryObj);

        //枚举
        System.out.println(singletonEnum);
        //枚举没有无参构造，父类有2个参数的有参构造
//        Constructor<SingletonEnum> singletonEnumConstructor = SingletonEnum.class.getDeclaredConstructor(null);
        Constructor<SingletonEnum> singletonEnumConstructor = SingletonEnum.class.getDeclaredConstructor(String.class, int.class);
        singletonEnumConstructor.setAccessible(true);
        //构造前判断是枚举抛出异常
        SingletonEnum newInstance = singletonEnumConstructor.newInstance("caicai", 18);
        System.out.println(newInstance);
    }
}
