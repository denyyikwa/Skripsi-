package routing.community;

import java.util.*;
import core.*;

public class AverageWinCentrality1 implements Centrality, GlobalCentralityAccess {

    public static final String CENTRALITY_WINDOW_SETTING = "timeWindow";
    public static final String COMPUTATION_INTERVAL_SETTING = "computeInterval";
    public static final String EPOCH_COUNT_SETTING = "nrOfEpochsToAvg";

    protected static int COMPUTE_INTERVAL = 600;
    protected static int CENTRALITY_TIME_WINDOW = 86400;
    protected static int EPOCH_COUNT = 787;

    protected double globalCentrality;
    protected double localCentrality;

    protected int lastGlobalComputationTime;
    protected int lastLocalComputationTime;

    protected int[] globalCentralities = new int[EPOCH_COUNT];
    protected List<Double> globalPopularityList = new ArrayList<>();

    public AverageWinCentrality1(Settings s) {
        if (s.contains(CENTRALITY_WINDOW_SETTING))
            CENTRALITY_TIME_WINDOW = s.getInt(CENTRALITY_WINDOW_SETTING);
        if (s.contains(COMPUTATION_INTERVAL_SETTING))
            COMPUTE_INTERVAL = s.getInt(COMPUTATION_INTERVAL_SETTING);
        if (s.contains(EPOCH_COUNT_SETTING))
            EPOCH_COUNT = s.getInt(EPOCH_COUNT_SETTING);
    }

    public AverageWinCentrality1(AverageWinCentrality1 proto) {
        this.lastGlobalComputationTime = this.lastLocalComputationTime = -COMPUTE_INTERVAL;
    }

    @Override
    public double getGlobalCentrality(Map<DTNHost, List<Duration>> connHistory) {
        if (SimClock.getIntTime() - this.lastGlobalComputationTime < COMPUTE_INTERVAL)
            return globalCentrality;

        int epochCount = (int)Math.round(SimClock.getIntTime() / CENTRALITY_TIME_WINDOW + 0.5);
        int[] centralities = new int[epochCount];

        int timeNow = SimClock.getIntTime();
        Map<Integer, Set<DTNHost>> nodesCountedInEpoch = new HashMap<>();

        for (int i = 0; i < epochCount; i++)
            nodesCountedInEpoch.put(i, new HashSet<>());

        for (Map.Entry<DTNHost, List<Duration>> entry : connHistory.entrySet()) {
            DTNHost h = entry.getKey();
            for (Duration d : entry.getValue()) {
                int timePassed = (int) (timeNow - d.end);
                if (timePassed > CENTRALITY_TIME_WINDOW * epochCount)
                    break;

                int epoch = timePassed / CENTRALITY_TIME_WINDOW;
                Set<DTNHost> nodesAlreadyCounted = nodesCountedInEpoch.get(epoch);
                if (nodesAlreadyCounted.contains(h))
                    continue;

                centralities[epoch]++;
                nodesAlreadyCounted.add(h);
            }
        }

        int sum = 0;
        for (int i = 0; i < epochCount; i++)
            sum += centralities[i];

        this.globalCentrality = ((double) sum) / epochCount;
        this.lastGlobalComputationTime = SimClock.getIntTime();
        return this.globalCentrality;
    }

    @Override
    public double getLocalCentrality(Map<DTNHost, List<Duration>> connHistory, CommunityDetection cd) {
        if (SimClock.getIntTime() - this.lastLocalComputationTime < COMPUTE_INTERVAL)
            return localCentrality;

        int epochCount = (int)Math.round(SimClock.getIntTime() / CENTRALITY_TIME_WINDOW + 0.5);
        int[] centralities = new int[epochCount];

        int timeNow = SimClock.getIntTime();
        Map<Integer, Set<DTNHost>> nodesCountedInEpoch = new HashMap<>();

        for (int i = 0; i < epochCount; i++)
            nodesCountedInEpoch.put(i, new HashSet<>());

        Set<DTNHost> community = cd.getLocalCommunity();

        for (Map.Entry<DTNHost, List<Duration>> entry : connHistory.entrySet()) {
            DTNHost h = entry.getKey();
            if (!community.contains(h))
                continue;

            for (Duration d : entry.getValue()) {
                int timePassed = (int) (timeNow - d.end);
                if (timePassed > CENTRALITY_TIME_WINDOW * epochCount)
                    break;

                int epoch = timePassed / CENTRALITY_TIME_WINDOW;
                Set<DTNHost> nodesAlreadyCounted = nodesCountedInEpoch.get(epoch);
                if (nodesAlreadyCounted.contains(h))
                    continue;

                centralities[epoch]++;
                nodesAlreadyCounted.add(h);
            }
        }

        int sum = 0;
        for (int i = 0; i < epochCount; i++)
            sum += centralities[i];

        this.localCentrality = ((double) sum) / epochCount;
        this.lastLocalComputationTime = SimClock.getIntTime();
        return this.localCentrality;
    }

    @Override
    public int[] getGlobalArrayCentrality(Map<DTNHost, List<Duration>> connHistory) {
        int[] centralities = new int[EPOCH_COUNT];

        int timeNow = SimClock.getIntTime();
        Map<Integer, Set<DTNHost>> nodesCountedInEpoch = new HashMap<>();

        for (int i = 0; i < EPOCH_COUNT; i++)
            nodesCountedInEpoch.put(i, new HashSet<>());

        for (Map.Entry<DTNHost, List<Duration>> entry : connHistory.entrySet()) {
            DTNHost h = entry.getKey();
            for (Duration d : entry.getValue()) {
                int timePassed = (int) (timeNow - d.end);
                if (timePassed > CENTRALITY_TIME_WINDOW * EPOCH_COUNT)
                    break;

                int epoch = timePassed / CENTRALITY_TIME_WINDOW;
                Set<DTNHost> nodesAlreadyCounted = nodesCountedInEpoch.get(epoch);
                if (nodesAlreadyCounted.contains(h))
                    continue;

                centralities[epoch]++;
                nodesAlreadyCounted.add(h);
            }
        }

        this.globalCentralities = centralities;
        return globalCentralities;
    }

    @Override
    public double[] getGlobal(Map<DTNHost, List<Duration>> connHistory) {
        int[] intArray = getGlobalArrayCentrality(connHistory);
        double[] result = new double[intArray.length];
        globalPopularityList.clear();

        for (int i = 0; i < intArray.length; i++) {
            result[i] = intArray[i];
            globalPopularityList.add((double) intArray[i]);
        }

        return result;
    }

    @Override
    public List<Double> getGlobalPopularity() {
        return globalPopularityList;
    }

    @Override
    public Centrality replicate() {
        return new AverageWinCentrality1(this);
    }
}
