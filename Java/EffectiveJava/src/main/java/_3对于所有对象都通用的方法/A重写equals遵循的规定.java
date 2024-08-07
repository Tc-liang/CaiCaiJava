package _3对于所有对象都通用的方法;

import java.util.HashMap;
import java.util.Objects;

/**
 * @author: 菜菜的后端私房菜
 * @create: 2024/3/7 15:23
 * @description:
 */
public class A重写equals遵循的规定 {

    static class Student {
        private int a;
        private String b;

        @Override
        public String toString() {
            return "Student{" +
                    "a=" + a +
                    ", b='" + b + '\'' +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            //1.判断对象的引用地址是否相等
            if (this == o) return true;
            //2.判断两个对象是否为相同类型
            if (o == null || getClass() != o.getClass()) return false;
            //3.转换成相同类型后根据规定逻辑相等的关键字段进行比较
            Student student = (Student) o;
            return a == student.a && Objects.equals(b, student.b);
        }

        @Override
        public int hashCode() {
            return Objects.hash(a, b);
        }
    }

    public static void main(String[] args) {

        Student s1 = new Student();
        s1.a = 1;
        s1.b = "1";

        HashMap<Student, Integer> map = new HashMap<>();
        map.put(s1, 1);

        s1.a = 2;
        map.put(s1, 2);

        System.out.println(map);

        Student s2 = new Student();
        s2.a = 1;
        s2.b = "1";
        System.out.println(map.get(s2));
        System.out.println(map.get(s1));
    }
}
