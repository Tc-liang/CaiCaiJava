package C_AQSComponent.test.proAndCon;

class Num {
    int count = 0;
    int max = 100;

    public int getFood() {
        return count;
    }

    public int addFood() {
        return ++count;
    }

    public int subFood() {
        return --count;
    }

    public int max() {
        return max;
    }
}