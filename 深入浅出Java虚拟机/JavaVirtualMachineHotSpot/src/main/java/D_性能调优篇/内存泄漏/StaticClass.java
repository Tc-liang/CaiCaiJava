package D_性能调优篇.内存泄漏;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Tcl
 * @Date 2021/5/20
 * @Description:
 */
public class StaticClass {
    private static final List<Object> list = new ArrayList<>();

    /**
     * 尽管这个局部变量Object生命周期非常短
     * 但是它被生命周期非常长的静态列表引用
     * 所以不会被GC回收 发生内存溢出
     */
    public void addObject(){
        Object o = new Object();
        list.add(o);
    }
}
