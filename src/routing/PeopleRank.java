package routing;

import java.util.*;

import core.*;
import routing.community.Duration;
import routing.community.RankingNodeValue;

public class PeopleRank implements RoutingDecisionEngine, RankingNodeValue {
    public static final String DAMPING_FACTOR_SETTING = "dampingFactor";
    public static final String THRESHOLD_SETTING = "threshold";

    protected Map<DTNHost, TupleDe<Double, Integer>> per;
    protected Map<DTNHost, List<Duration>> connHistory;
    protected Map<DTNHost, Double> startTimestamps;
    protected Set<DTNHost> thisHostSet;

    protected double dampingFactor;
    protected double threshold;

    public PeopleRank(Settings s) {
        if (s.contains(DAMPING_FACTOR_SETTING)) {
            dampingFactor = s.getDouble(DAMPING_FACTOR_SETTING);
        } else {
            this.dampingFactor = 0.85;
        }

        if (s.contains(THRESHOLD_SETTING)) {
            threshold = s.getDouble(THRESHOLD_SETTING);
        } else {
            this.threshold = 700;
        }

        connHistory = new HashMap<>();
        per = new HashMap<>();
        thisHostSet = new HashSet<>();
        startTimestamps = new HashMap<>();
    }

    public PeopleRank(PeopleRank r) {
        this.dampingFactor = r.dampingFactor;
        this.threshold = r.threshold;
        this.connHistory = new HashMap<>();
        this.per = new HashMap<>();
        this.thisHostSet = new HashSet<>();
        this.startTimestamps = new HashMap<>();
    }

    @Override
    public void connectionUp(DTNHost thisHost, DTNHost peer) {}

    @Override
    public void doExchangeForNewConnection(Connection con, DTNHost peer) {
        DTNHost myHost = con.getOtherNode(peer);
        PeopleRank de = getOtherDecisionEngine(peer);

        this.startTimestamps.put(peer, SimClock.getTime());
        de.startTimestamps.put(myHost, SimClock.getTime());
    }

    @Override
    public void connectionDown(DTNHost thisHost, DTNHost peer) {
        double time = getPreviousConnectionStartTime(thisHost, peer);
        double etime = SimClock.getTime();

        List<Duration> history = connHistory.computeIfAbsent(peer, k -> new LinkedList<>());

        if (etime - time >= threshold) {
            history.add(new Duration(time, etime));
            thisHostSet.add(peer);
        }

        for (Map.Entry<DTNHost, List<Duration>> entry : connHistory.entrySet()) {
            DTNHost host = entry.getKey();
            double friendRank = calculatePer(host);
            Set<DTNHost> Fj = new HashSet<>(connHistory.keySet());
            Fj.add(peer);
            int totalFriends = Fj.size();
            per.put(host, new TupleDe<>(friendRank, totalFriends));
        }
    }

    public double getPreviousConnectionStartTime(DTNHost thisHost, DTNHost peer) {
        return startTimestamps.getOrDefault(peer, 0.0);
    }

    @Override
    public boolean isFinalDest(Message m, DTNHost aHost) {
        return m.getTo() == aHost;
    }

    @Override
    public boolean newMessage(Message m) {
        return true;
    }

    @Override
    public RoutingDecisionEngine replicate() {
        return new PeopleRank(this);
    }

    @Override
    public boolean shouldDeleteOldMessage(Message m, DTNHost hostReportingOld) {
        return true;
    }

    @Override
    public boolean shouldDeleteSentMessage(Message m, DTNHost otherHost) {
        return false;
    }

    @Override
    public boolean shouldSaveReceivedMessage(Message m, DTNHost thisHost) {
        return m.getTo() != thisHost;
    }

    @Override
    public boolean shouldSendMessageToHost(Message m, DTNHost otherHost, DTNHost thisHost) {
        if (m.getTo() == otherHost) {
            return true;
        }

        double perThisHost = calculatePer(thisHost);
        double perOtherHost = calculatePer(otherHost);

        if (connHistory.containsKey(otherHost) || thisHostSet.contains(otherHost)) {
            if (thisHostSet.contains(otherHost)) {
                return true;
            }

            if (perOtherHost >= perThisHost || otherHost.equals(m.getTo())) {
                return true;
            }
        }

        return false;
    }

    private double calculatePer(DTNHost host) {
        double sum = 0.0;

        for (Map.Entry<DTNHost, TupleDe<Double, Integer>> entry : per.entrySet()) {
            if (!entry.getKey().equals(host)) {
                double friendRanking = entry.getValue().getFirst();
                int friendsOfOtherHost = entry.getValue().getSecond();
                if (friendsOfOtherHost > 0) {
                    sum += friendRanking / friendsOfOtherHost;
                }
            }
        }

        return (1 - dampingFactor) + dampingFactor * sum;
    }

    private PeopleRank getOtherDecisionEngine(DTNHost h) {
        MessageRouter otherRouter = h.getRouter();
        assert otherRouter instanceof DecisionEngineRouter : "Router type mismatch";
        return (PeopleRank) ((DecisionEngineRouter) otherRouter).getDecisionEngine();
    }

    @Override
    public void update(DTNHost thisHost) {}

    public Map<DTNHost, Double> getAllRankings() {
        Map<DTNHost, Double> rankings = new HashMap<>();
        for (Map.Entry<DTNHost, TupleDe<Double, Integer>> entry : per.entrySet()) {
            rankings.put(entry.getKey(), entry.getValue().getFirst());
        }
        return rankings;
    }

    @Override
    public int getTotalTeman(DTNHost host) {
        DecisionEngineRouter d = (DecisionEngineRouter) host.getRouter();
        PeopleRank othRouter = (PeopleRank) d.getDecisionEngine();
        return othRouter.per.size();
    }
}
