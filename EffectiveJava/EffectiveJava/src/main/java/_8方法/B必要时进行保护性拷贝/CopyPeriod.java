package _8方法.B必要时进行保护性拷贝;

import java.util.Date;

/**
 * @author: 菜菜的后端私房菜
 * @create: 2024/4/1 10:42
 * @description: 保护拷贝的周期
 */
public class CopyPeriod {
    private final Date start;
    private final Date end;
    //传入参数时进行拷贝
    public CopyPeriod(Date start, Date end) {
        this.start = new Date(start.getTime());
        this.end = new Date(end.getTime());
        if (this.start.compareTo(this.end) > 0)
            throw new IllegalArgumentException(
                    this.start + " after " + this.end);
    }
    //返回字段时进行拷贝
    public Date start() {
        return new Date(start.getTime());
    }
    public Date end() {
        return new Date(end.getTime());
    }

    public String toString() {
        return start + " - " + end;
    }
}
