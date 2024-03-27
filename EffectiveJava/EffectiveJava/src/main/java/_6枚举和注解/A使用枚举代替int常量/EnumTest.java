package _6枚举和注解.A使用枚举代替int常量;

public class EnumTest {
   public static void main(String[] args) {
      double mass = Planet.EARTH.mass();

      Planet[] values = Planet.values();
      for (Planet planet : values) {
         System.out.println(planet);
      }
   }
}