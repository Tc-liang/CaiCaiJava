package _8方法.B必要时进行保护性拷贝;

import java.util.Date;

//可变周期
public final class Period {
    //起始时间使用可变对象Date
    private final Date start;
    private final Date end;

    public Period(Date start, Date end) {
        if (start.compareTo(end) > 0)
            throw new IllegalArgumentException(
                    start + " after " + end);
        this.start = start;
        this.end = end;
    }

    //获取起始时间(不安全)
    public Date start() {
        return start;
    }
    public Date end() {
        return end;
    }

    public String toString() {
        return start + " - " + end;
    }

}