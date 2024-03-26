package _4类与接口.B复合优于继承;

import java.util.Collection;
import java.util.HashSet;

public class ExtendsHashSet extends HashSet {
    private int addSize;

    @Override
    public boolean add(Object o) {
        addSize++;
        return super.add(o);
    }

    @Override
    public boolean addAll(Collection c) {
        addSize += c.size();
        return super.addAll(c);
    }
}