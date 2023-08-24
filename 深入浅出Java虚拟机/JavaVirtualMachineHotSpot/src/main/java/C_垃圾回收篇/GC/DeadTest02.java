package C_垃圾回收篇.GC;

import java.util.concurrent.TimeUnit;

/**
 * @author Tc.l
 * @Date 2020/11/20
 * @Description:
 * 测试重写finalize方法是否会自救
 */
public class DeadTest02 {
    public  static DeadTest02 VALUE = null;
    public static void isAlive(){
        if(VALUE!=null){
            System.out.println("Alive in now!");
        }else{
            System.out.println("Dead in now!");
        }
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        System.out.println("搭上引用链的任一对象进行自救");
        VALUE=this;
    }

    public static void main(String[] args) {
        VALUE = new DeadTest02();
        System.out.println("开始第一次自救");
        VALUE=null;
        System.gc();
        try {
            //等Finalizer线程执行
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        isAlive();

        System.out.println("开始第二次自救");
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
