package _8方法.B必要时进行保护性拷贝;

import java.util.*;

public class Attacks {
    public static void main(String[] args) {
        Date start = new Date();
        Date end = new Date();
        Period p = new Period(start, end);
        //修改入参
        start.setYear(76);
        //引用逃逸 被修改
        p.end().setYear(78);
        System.out.println(p);


        start = new Date();
        end = new Date();
        CopyPeriod cp = new CopyPeriod(start, end);
        //保护性拷贝 引用不会逃逸 修改无效
        start.setYear(76);
        cp.end().setYear(78);
        System.out.println(cp);
    }
}