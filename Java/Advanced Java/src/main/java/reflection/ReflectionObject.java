package reflection;

import lombok.Data;

/**
 * @author: 菜菜的后端私房菜
 * @create: 2024/8/8 10:04
 * @description:
 */
public class ReflectionObject {
    private String name;
    private int age;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }


    @Override
    public String toString() {
        return "ReflectionObject{" +
                "name='" + name + '\'' +
                ", age=" + age +
                '}';
    }
}
