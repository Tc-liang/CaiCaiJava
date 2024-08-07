package _6枚举和注解.B使用字段代替ordinal;

public enum Ensemble {
    DUET(2),
    SOLO(1),
    TRIO(3),
    QUINTET(5),
    QUARTET(4),
    SEXTET(6),
    SEPTET(7),
    OCTET(8),
    NONET(9),
    DOUBLE_QUARTET(8),
    DECTET(10),
    TRIPLE_QUARTET(12);

    //使用字段代替ordinal
    private final int numberOfMusicians;

    Ensemble(int size) {
        this.numberOfMusicians = size;
    }

    public static void main(String[] args) {
        for (Ensemble ensemble : Ensemble.values()) {
            System.out.println(ensemble.name() + ":" + ensemble.ordinal() + ":" + ensemble.numberOfMusicians);
        }
    }
}