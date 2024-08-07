package _6枚举和注解.C使用EnumSet代替位运算;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

public class Text {
    public enum Style {BOLD, ITALIC, UNDERLINE, STRIKETHROUGH}

    // 打印集合中的内容
    public void applyStyles(Set<Style> styles) {
        System.out.printf("Applying styles %s to text%n",
                Objects.requireNonNull(styles));
    }

    public static void main(String[] args) {
        Text text = new Text();
        EnumSet<Style> enumSet = EnumSet.of(Style.BOLD, Style.ITALIC);
        enumSet.add(Style.UNDERLINE);
        text.applyStyles(enumSet);
    }
}