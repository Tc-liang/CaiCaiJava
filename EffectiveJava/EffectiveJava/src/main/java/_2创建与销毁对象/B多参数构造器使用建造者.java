package _2创建与销毁对象;

import lombok.Builder;

/**
 * @author: cl
 * @create: 2024/3/5 15:39
 * @description:
 */
public class B多参数构造器使用建造者 {

    @Builder
    static class Student {
        private String name;
        private int age;
        private String sex;
        private String address;
        private String phone;
        private String email;
        private String qq;
    }

    public static void main(String[] args) {
        Student student = new Student("张三", 18, "男", "北京", "13812345678", "", "12345678");

        Student.builder()
                .name("张三")
                .age(18)
                .sex("男")
                .address("北京")
                .phone("13812345678")
                .email("")
                .qq("12345678")
                .build();
    }
}
