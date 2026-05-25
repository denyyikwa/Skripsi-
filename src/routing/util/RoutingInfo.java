package routing.util;

import java.util.ArrayList;
import java.util.List;

public class RoutingInfo {
    private String summary;
    private List<RoutingInfo> moreInfo;

    public RoutingInfo(String summary) {
        this.summary = summary;
        this.moreInfo = new ArrayList<>();
    }

    public void addMoreInfo(RoutingInfo info) {
        this.moreInfo.add(info);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(summary + "\n");
        for (RoutingInfo info : moreInfo) {
            sb.append("  ").append(info.toString()).append("\n");
        }
        return sb.toString();
    }
}
