package _4类与接口.A类和字段的可访问性最小化;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author: 菜菜的后端私房菜
 * @create: 2024/3/20 8:15
 * @description:
 */
public class PrivateFiledClass {

    private static final List<String> LIST = Collections.unmodifiableList(Arrays.asList("immutable", "values"));

    public static List<String> getList() {
        return LIST;
    }

}
