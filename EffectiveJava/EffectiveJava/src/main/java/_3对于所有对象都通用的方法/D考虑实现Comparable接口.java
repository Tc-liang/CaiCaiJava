package _3对于所有对象都通用的方法;

import java.util.*;

/**
 * @author: 菜菜的后端私房菜
 * @create: 2024/3/8 8:59
 * @description:
 */
public class D考虑实现Comparable接口 {

    static class Student {
        private int age;
        private int day;

        public int compareTo(Student o) {
            // int res = this.age - o.age;
            int res = Integer.compare(this.age, o.age);

            if (0 == res) {
                return Integer.compare(this.day, o.day);
            }

            return res;
        }

        public Student(int age, int day) {
            this.age = age;
            this.day = day;
        }

        public int getAge() {
            return age;
        }

        public int getDay() {
            return day;
        }

        @Override
        public String toString() {
            return "Student{" +
                    "age=" + age +
                    ", day=" + day +
                    '}';
        }

    }

    public static void main(String[] args) {
        //-1
        System.out.println("abc".compareTo("abcd"));

        TreeSet<String> set = new TreeSet<>();
        set.add("abcde");
        set.add("abc");
        set.add("abcd");
        System.out.println(set);

//        TreeSet<Student> students = new TreeSet<>();
        TreeSet<Student> students = new TreeSet<>(
                Comparator
                        .comparingInt(Student::getAge)
                        .thenComparingInt(Student::getDay)
        );
        students.add(new Student(18, 9));
        students.add(new Student(20, 9));
        students.add(new Student(20, 9));
        students.add(new Student(18, 19));
        System.out.println(students);

        Arrays.sort(new String[]{"abc", "abcd", "abcde"});


    }
}
