package routing;

import java.util.ArrayList;
import java.util.List;

public class RoutingInfo {

    private String summary;
    private List<RoutingInfo> details;

    // Constructor untuk String
    public RoutingInfo(String summary) {
        this.summary = summary;
        this.details = new ArrayList<>();
    }

    // Constructor untuk Object (ini yang dibutuhkan!)
    public RoutingInfo(Object obj) {
        this.summary = String.valueOf(obj);
        this.details = new ArrayList<>();
    }

    public void addMoreInfo(RoutingInfo info) {
        details.add(info);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(summary + "\n");
        for (RoutingInfo info : details) {
            sb.append("  ").append(info.toString());
        }
        return sb.toString();
    }
}