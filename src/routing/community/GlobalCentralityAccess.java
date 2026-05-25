package routing.community;


import java.util.List;
import java.util.Map;
import core.DTNHost;


public interface GlobalCentralityAccess {
    public double[] getGlobal(Map<DTNHost, List<Duration>> connHistory);
    public List<Double> getGlobalPopularity();
}

