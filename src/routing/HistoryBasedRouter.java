package routing;

import core.*;

import java.util.*;

/**
 * HistoryBasedRouter is an extension of the ProphetRouter that incorporates
 * the concept of previous delivery predictability (preP) as proposed in
 * the paper "An Efficient Routing Protocol Using the History of Delivery Predictability in Opportunistic Networks".
 */
public class HistoryBasedRouter extends ActiveRouter {
    private static final double P_INIT = 0.75;
    private static final double DEFAULT_BETA = 0.25;
    private static final double GAMMA = 0.98;

    private int secondsInTimeUnit;
    private double beta;

    private Map<DTNHost, Double> preds;
    private Map<DTNHost, Double> preP;
    private double lastAgeUpdate;

    public HistoryBasedRouter(Settings s) {
        super(s);
        Settings prophetSettings = new Settings("ProphetRouter");
        secondsInTimeUnit = prophetSettings.getInt("secondsInTimeUnit");

        // Perbaikan di sini:
        if (prophetSettings.contains("beta")) {
            beta = prophetSettings.getDouble("beta");
        } else {
            beta = DEFAULT_BETA;
        }

        preds = new HashMap<>();
        preP = new HashMap<>();
    }

    protected HistoryBasedRouter(HistoryBasedRouter r) {
        super(r);
        this.secondsInTimeUnit = r.secondsInTimeUnit;
        this.beta = r.beta;
        this.preds = new HashMap<>();
        this.preP = new HashMap<>();
    }

    @Override
    public void changedConnection(Connection con) {
        if (con.isUp()) {
            DTNHost other = con.getOtherNode(getHost());
            updateDeliveryPredFor(other);
            updateTransitivePreds(other);
        }
    }

    private void updateDeliveryPredFor(DTNHost host) {
        double oldP = getPredFor(host);
        double newP = oldP + (1 - oldP) * P_INIT;
        preds.put(host, newP);
    }

    private void updateTransitivePreds(DTNHost host) {
        HistoryBasedRouter r = (HistoryBasedRouter) host.getRouter();
        double pForHost = getPredFor(host);

        for (Map.Entry<DTNHost, Double> entry : r.preds.entrySet()) {
            DTNHost h = entry.getKey();
            if (h.equals(getHost())) continue;
            double pOld = getPredFor(h);
            double pNew = pOld + (1 - pOld) * pForHost * entry.getValue() * beta;
            preds.put(h, pNew);
        }
    }

    private void ageDeliveryPreds() {
        double timeDiff = (SimClock.getTime() - lastAgeUpdate) / secondsInTimeUnit;
        if (timeDiff == 0) return;
        double mult = Math.pow(GAMMA, timeDiff);

        preds.replaceAll((k, v) -> v * mult);
        lastAgeUpdate = SimClock.getTime();
    }

    public double getPredFor(DTNHost host) {
        ageDeliveryPreds();
        return preds.getOrDefault(host, 0.0);
    }

    public double getPrePFor(DTNHost host) {
        return preP.getOrDefault(host, 0.0);
    }

    @Override
    public void update() {
        super.update();
        if (!canStartTransfer() || isTransferring()) return;
        if (exchangeDeliverableMessages() != null) return;
        tryOtherMessages();
    }

    private core.Tuple<Message, Connection> tryOtherMessages() {
        List<Tuple<Message, Connection>> messages = new ArrayList<>();

        for (Connection con : getConnections()) {
            DTNHost other = con.getOtherNode(getHost());
            HistoryBasedRouter r = (HistoryBasedRouter) other.getRouter();

            if (r.isTransferring()) continue;

            for (Message m : getMessageCollection()) {
                if (r.hasMessage(m.getId())) continue;

                double pOther = r.getPredFor(m.getTo());
                double pThis = getPredFor(m.getTo());
                double prePThis = getPrePFor(m.getTo());

                if ((pOther > pThis && pOther > prePThis) || prePThis == 0.0) {
                    messages.add(new Tuple<>(m, con));
                }
            }
        }

        if (messages.isEmpty()) return null;
        messages.sort(new TupleComparator());
        return tryMessagesForConnected();
    }

    private core.Tuple<Message, Connection> tryMessagesForConnected() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'tryMessagesForConnected'");
    }

    private class TupleComparator implements Comparator<Tuple<Message, Connection>> {
        public int compare(Tuple<Message, Connection> t1, Tuple<Message, Connection> t2) {
            double p1 = ((HistoryBasedRouter)t1.getValue().getOtherNode(getHost()).getRouter()).getPredFor(t1.getKey().getTo());
            double p2 = ((HistoryBasedRouter)t2.getValue().getOtherNode(getHost()).getRouter()).getPredFor(t2.getKey().getTo());

            if (p2 == p1) {
                return compareByQueueMode(t1.getKey(), t2.getKey());
            } else {
                return Double.compare(p2, p1); // descending order
            }
        }
    }

    @Override
    public MessageRouter replicate() {
        return new HistoryBasedRouter(this);
    }

    @Override
    public RoutingInfo getRoutingInfo() {
        ageDeliveryPreds();
        RoutingInfo top = super.getRoutingInfo();
        RoutingInfo ri = new RoutingInfo(preds.size() + " delivery prediction(s)");

        for (Map.Entry<DTNHost, Double> e : preds.entrySet()) {
            ri.addMoreInfo(new RoutingInfo(String.format("%s : %.6f", e.getKey(), e.getValue())));
        }

        top.addMoreInfo(ri);
        return top;
    }
}
