package _4类与接口.B复合优于继承;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

/**
 * @author: 菜菜的后端私房菜
 * @create: 2024/3/21 9:41
 * @description:
 */
public class CompositeHashSet implements Set{
    private Set set;
    private int addSize;

    public CompositeHashSet(Set set) {
        this.set = set;
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public boolean contains(Object o) {
        return false;
    }

    @Override
    public Iterator iterator() {
        return null;
    }

    @Override
    public Object[] toArray() {
        return new Object[0];
    }

    @Override
    public Object[] toArray(Object[] a) {
        return new Object[0];
    }

    public boolean add(Object o) {
        addSize++;
        return set.add(o);
    }

    @Override
    public boolean remove(Object o) {
        return false;
    }

    @Override
    public boolean containsAll(Collection c) {
        return false;
    }

    public boolean addAll(Collection c) {
        addSize += c.size();
        return set.addAll(c);
    }

    @Override
    public void clear() {

    }

    @Override
    public boolean removeAll(Collection c) {
        return false;
    }

    @Override
    public boolean retainAll(Collection c) {
        return false;
    }
}
