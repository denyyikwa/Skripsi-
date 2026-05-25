package routing.community;

import java.util.*;

import core.*;
import routing.DecisionEngineRouter;
import routing.MessageRouter;
import routing.RoutingDecisionEngine;

public class BubbleRap implements RoutingDecisionEngine, CommunityDetectionEngine {

    public static final String COMMUNITY_ALG_SETTING = "communityDetectAlg";
    public static final String CENTRALITY_ALG_SETTING = "centralityAlg";

    protected Map<DTNHost, Double> startTimestamps;
    protected Map<DTNHost, List<Duration>> connHistory;
    
    protected CommunityDetection community;
    protected Centrality centrality;

     public BubbleRap(Settings s) {
        if (s.contains(COMMUNITY_ALG_SETTING)) {
            this.community = (CommunityDetection) s.createIntializedObject(s.getSetting(COMMUNITY_ALG_SETTING));
        } else {
            this.community = new SimpleCommunityDetection(s);
        }

        if (s.contains(CENTRALITY_ALG_SETTING)) {
            this.centrality = (Centrality) s.createIntializedObject(s.getSetting(CENTRALITY_ALG_SETTING));
        } else {
            this.centrality = new AverageWinCentrality1(s);
        }
    }

    public BubbleRap(BubbleRap proto) {
        this.community = proto.community.replicate();
        this.centrality = proto.centrality.replicate();
        startTimestamps = new HashMap<DTNHost, Double>();
        connHistory = new HashMap<DTNHost, List<Duration>>();
    }

    public void connectionUp(DTNHost thisHost, DTNHost peer) {}

    @Override
    public void doExchangeForNewConnection(Connection con, DTNHost peer) {
        DTNHost myHost = con.getOtherNode(peer);
        BubbleRap de = this.getOtherDecisionEngine(peer);
        this.startTimestamps.put(peer, SimClock.getTime());
        de.startTimestamps.put(myHost, SimClock.getTime());
        this.community.newConnection(myHost, peer, de.community);
    }

    @Override
    public void connectionDown(DTNHost thisHost, DTNHost peer) {
        double time = cek(thisHost, peer);
        double etime = SimClock.getTime();

        List<Duration> history;
        if (!connHistory.containsKey(peer)) {
            history = new LinkedList<Duration>();
            connHistory.put(peer, history);
        } else {
            history = connHistory.get(peer);
        }

        if (etime - time > 0) {
            history.add(new Duration(time, etime));
        }

        CommunityDetection peerCD = this.getOtherDecisionEngine(peer).community;
        community.connectionLost(thisHost, peer, peerCD, history);
        startTimestamps.remove(peer);
    }

    public double cek(DTNHost thisHost, DTNHost peer) {
        if (startTimestamps.containsKey(thisHost)) {
            startTimestamps.get(peer);
        }
        return 0;
    }

    @Override
    public boolean newMessage(Message m) {
        return true;
    }

    @Override
    public boolean isFinalDest(Message m, DTNHost aHost) {
        return m.getTo() == aHost;
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
        DTNHost dest = m.getTo();
        BubbleRap de = getOtherDecisionEngine(otherHost);

        boolean peerInCommunity = de.commumesWithHost(dest);
        boolean meInCommunity = this.commumesWithHost(dest);

        if (peerInCommunity && !meInCommunity) {
            return true;
        } else if (!peerInCommunity && meInCommunity) {
            return false;
        } else if (peerInCommunity) {
            return de.getLocalCentrality() > this.getLocalCentrality();
        } else {
            return de.getGlobalCentralityValue() > this.getGlobalCentralityValue();
        }
    }

    @Override
    public boolean shouldDeleteSentMessage(Message m, DTNHost otherHost) {
        BubbleRap de = this.getOtherDecisionEngine(otherHost);
        return de.commumesWithHost(m.getTo()) && !this.commumesWithHost(m.getTo());
    }

    @Override
    public boolean shouldDeleteOldMessage(Message m, DTNHost hostReportingOld) {
        return true;
    }

    @Override
    public RoutingDecisionEngine replicate() {
        return new BubbleRap(this);
    }

    protected boolean commumesWithHost(DTNHost h) {
        return community.isHostInCommunity(h);
    }

    protected double getLocalCentrality() {
        return this.centrality.getLocalCentrality(connHistory, community);
    }

    protected double getGlobalCentralityValue() {
        return this.centrality.getGlobalCentrality(connHistory);
    }

    private BubbleRap getOtherDecisionEngine(DTNHost h) {
        MessageRouter otherRouter = h.getRouter();
        assert otherRouter instanceof DecisionEngineRouter : "This router only works with same type";
        return (BubbleRap) ((DecisionEngineRouter) otherRouter).getDecisionEngine();
    }

    @Override
    public Set<DTNHost> getLocalCommunity() {
        return this.community.getLocalCommunity();
    }

    @Override
    public void update(DTNHost thisHost) {}

    // ✅ Tambahan: akses ke array hasil global centrality per window
    public double[] getGlobalCentrality() {
        return this.centrality.getGlobal(connHistory);
    }

    public List<Double> getGlobalPopularity() {
        return this.centrality.getGlobalPopularity();
    }

    // Getter & Setter opsional (jika ingin expose dari luar)
    public Centrality getCentrality() {
        return centrality;
    }

    public void setCentrality(Centrality centrality) {
        this.centrality = centrality;
    }

    public Map<DTNHost, List<Duration>> getConnHistory() {
        return connHistory;
    }

    public void setConnHistory(Map<DTNHost, List<Duration>> connHistory) {
        this.connHistory = connHistory;
    }

    public Map<DTNHost, Double> getStartTimestamps() {
        return startTimestamps;
    }

    public void setStartTimestamps(Map<DTNHost, Double> startTimestamps) {
        this.startTimestamps = startTimestamps;
    }

    public CommunityDetection getCommunity() {
        return community;
    }

    public void setCommunity(CommunityDetection community) {
        this.community = community;
    }

    public static String getCommunityAlgSetting() {
        return COMMUNITY_ALG_SETTING;
    }

    public static String getCentralityAlgSetting() {
        return CENTRALITY_ALG_SETTING;
    }
}
