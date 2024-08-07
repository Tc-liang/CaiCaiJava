package _2创建与销毁对象;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Collections;

/**
 * @author: 菜菜的后端私房菜
 * @create: 2024/3/5 14:50
 * @description:
 */
public class A静态工厂代替构造器 {
    public static void main(String[] args) throws SQLException, ClassNotFoundException {

        //1.可以自定义名称，见名知意
        //2.获取对象时，可以使用单例、享元等思想进行复用
        Boolean value = Boolean.valueOf(true);

        //3.可以返回原类型的子类
        Collections.singletonList(null);
        //4.返回对象可以随着入参发生改变

        //5.返回对象的类可以在编写静态工厂时不存在
        Class.forName("com.mysql.cj.jdbc.Driver");
        Connection conn = DriverManager.getConnection("url", "username", "password");

        //策略工厂可覆盖1-5
    }
}
