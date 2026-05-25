package routing;

import core.Connection;
import core.Message;

public class Tuple<S1, S2> {
    private S1 key;
    private S2 value;

    public Tuple(S1 key, S2 value) {
        this.key = key;
        this.value = value;
    }

    public S1 getKey() {
        return key;
    }

    public S2 getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "(" + key + ", " + value + ")";
    }
}
