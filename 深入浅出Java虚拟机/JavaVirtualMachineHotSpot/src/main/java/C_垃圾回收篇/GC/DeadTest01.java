package C_垃圾回收篇.GC;

import java.util.concurrent.TimeUnit;

/**
 * @author Tc.l
 * @Date 2020/11/20
 * @Description:
 * 测试不重写finalize方法是否会自救
 */
public class DeadTest01 {
    public  static DeadTest01 VALUE = null;
    public static void isAlive(){
        if(VALUE!=null){
            System.out.println("Alive in now!");
        }else{
            System.out.println("Dead in now!");
        }
    }
    public static void main(String[] args) {
        VALUE = new DeadTest01();

        VALUE=null;
        System.gc();
        try {
            //等Finalizer线程执行
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        isAlive();
    }
}
