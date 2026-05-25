package routing;

import core.Connection;
import core.Message;

public class TupleDe<S1, S2> {
    private S1 first;
    private S2 second;

    public TupleDe(S1 first, S2 second) {
        this.first = first;
        this.second = second;
    }

    public S1 getFirst() {
        return first;
    }

    public S2 getSecond() {
        return second;
    }

    @Override
    public String toString() {
        return "(" + first + ", " + second + ")";
    }

    public Connection getValue() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getValue'");
    }

    public Message getKey() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getKey'");
    }
}
