package _6枚举和注解.D使用EnumMap代替序数索引;

import java.util.*;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toSet;

class Plant {
    //植物成熟周期
    enum LifeCycle {
        ANNUAL,
        PERENNIAL,
        BIENNIAL
    }

    final String name;
    final LifeCycle lifeCycle;

    Plant(String name, LifeCycle lifeCycle) {
        this.name = name;
        this.lifeCycle = lifeCycle;
    }

    @Override
    public String toString() {
        return name;
    }

    public static void main(String[] args) {
        //不同植物成熟周期不同
        Plant[] garden = {
                new Plant("Basil", LifeCycle.ANNUAL),
                new Plant("Carroway", LifeCycle.BIENNIAL),
                new Plant("Dill", LifeCycle.ANNUAL),
                new Plant("Lavendar", LifeCycle.PERENNIAL),
                new Plant("Parsley", LifeCycle.BIENNIAL),
                new Plant("Rosemary", LifeCycle.PERENNIAL),
                new Plant("Rosemary", LifeCycle.PERENNIAL)
        };

        Set<Plant>[] plantsByLifeCycleArr =
                (Set<Plant>[]) new Set[Plant.LifeCycle.values().length];
        //初始化数组
        for (int i = 0; i < plantsByLifeCycleArr.length; i++){
            plantsByLifeCycleArr[i] = new HashSet<>();
        }
        //根据枚举的ordinal放入数组
        for (Plant p : garden){
            plantsByLifeCycleArr[p.lifeCycle.ordinal()].add(p);
        }

        //ANNUAL: [Basil, Dill]
        //PERENNIAL: [Lavendar, Rosemary]
        //BIENNIAL: [Parsley, Carroway]
        for (int i = 0; i < plantsByLifeCycleArr.length; i++) {
            System.out.printf("%s: %s%n", Plant.LifeCycle.values()[i], plantsByLifeCycleArr[i]);
        }

        //使用EnumMap替代
        Map<Plant.LifeCycle, Set<Plant>> plantsByLifeCycle = new EnumMap<>(Plant.LifeCycle.class);
        //初始化
        for (Plant.LifeCycle lc : Plant.LifeCycle.values()){
            plantsByLifeCycle.put(lc, new HashSet<>());
        }

        //1.手动添加
        for (Plant p : garden){
            plantsByLifeCycle.get(p.lifeCycle).add(p);
        }
        System.out.println(plantsByLifeCycle);

        //2.分组
        Map<LifeCycle, List<Plant>> map = Arrays.stream(garden).collect(groupingBy(p -> p.lifeCycle));
        System.out.println(map);

        //3.使用 EnumMap
        EnumMap<LifeCycle, Set<Plant>> enumMap = Arrays.stream(garden).collect(groupingBy(p -> p.lifeCycle, () -> new EnumMap<>(LifeCycle.class), toSet()));
        //{ANNUAL=[Basil, Dill], PERENNIAL=[Lavendar, Rosemary, Rosemary], BIENNIAL=[Parsley, Carroway]}
        enumMap.putIfAbsent(LifeCycle.BIENNIAL, new HashSet<>());
        System.out.println(enumMap);
    }
}