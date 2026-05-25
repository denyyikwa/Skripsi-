/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package routing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import util.Tuple;

import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;
import core.SimClock;

/**
 * Implementation of PRoPHET router as described in
 * <I>Probabilistic routing in intermittently connected networks</I> by
 * Anders Lindgren et al.
 */
public class ProphetRouter extends ActiveRouter {
	public static final double P_INIT = 0.75;
	public static final double DEFAULT_BETA = 0.25;
	public static final double DEFAULT_GAMMA = 0.98;

	public static final String PROPHET_NS = "ProphetRouter";
	public static final String SECONDS_IN_UNIT_S = "secondsInTimeUnit";
	public static final String BETA_S = "beta";
	public static final String GAMMA_S = "gamma";

	private int secondsInTimeUnit;
	private double beta;
	private double gamma;

	private Map<DTNHost, Double> preds;
	private double lastAgeUpdate;

	public ProphetRouter(Settings s) {
		super(s);
		Settings prophetSettings = new Settings(PROPHET_NS);
		secondsInTimeUnit = prophetSettings.getInt(SECONDS_IN_UNIT_S);
		beta = prophetSettings.contains(BETA_S) ? 
		       prophetSettings.getDouble(BETA_S) : DEFAULT_BETA;
		gamma = prophetSettings.contains(GAMMA_S) ? 
		        prophetSettings.getDouble(GAMMA_S) : DEFAULT_GAMMA;
		initPreds();
	}

	protected ProphetRouter(ProphetRouter r) {
		super(r);
		this.secondsInTimeUnit = r.secondsInTimeUnit;
		this.beta = r.beta;
		this.gamma = r.gamma;
		initPreds();
	}

	private void initPreds() {
		this.preds = new HashMap<DTNHost, Double>();
	}

	@Override
	public void changedConnection(Connection con) {
		super.changedConnection(con);

		if (con.isUp()) {
			DTNHost otherHost = con.getOtherNode(getHost());
			updateDeliveryPredFor(otherHost);
			updateTransitivePreds(otherHost);
		}
	}

	private void updateDeliveryPredFor(DTNHost host) {
		double oldValue = getPredFor(host);
		double newValue = oldValue + (1 - oldValue) * P_INIT;
		preds.put(host, newValue);
	}

	public double getPredFor(DTNHost host) {
		ageDeliveryPreds();
		return preds.getOrDefault(host, 0.0);
	}

	private void updateTransitivePreds(DTNHost host) {
		MessageRouter otherRouter = host.getRouter();
		assert otherRouter instanceof ProphetRouter;

		double pForHost = getPredFor(host);
		Map<DTNHost, Double> othersPreds = ((ProphetRouter) otherRouter).getDeliveryPreds();

		for (Map.Entry<DTNHost, Double> e : othersPreds.entrySet()) {
			if (e.getKey() == getHost()) {
				continue;
			}
			double pOld = getPredFor(e.getKey());
			double pNew = pOld + (1 - pOld) * pForHost * e.getValue() * beta;
			preds.put(e.getKey(), pNew);
		}
	}

	private void ageDeliveryPreds() {
		double timeDiff = (SimClock.getTime() - this.lastAgeUpdate) / secondsInTimeUnit;

		if (timeDiff == 0) {
			return;
		}

		double mult = Math.pow(gamma, timeDiff);
		for (Map.Entry<DTNHost, Double> e : preds.entrySet()) {
			e.setValue(e.getValue() * mult);
		}

		this.lastAgeUpdate = SimClock.getTime();
	}

	private Map<DTNHost, Double> getDeliveryPreds() {
		ageDeliveryPreds();
		return this.preds;
	}

	@Override
	public void update() {
		super.update();
		if (!canStartTransfer() || isTransferring()) {
			return;
		}

		if (exchangeDeliverableMessages() != null) {
			return;
		}

		tryOtherMessages();
	}

	private core.Tuple<Message, Connection> tryOtherMessages() {
		List<TupleDe<Message, Connection>> messages = new ArrayList<>();

		Collection<Message> msgCollection = getMessageCollection();

		for (Connection con : getConnections()) {
			DTNHost other = con.getOtherNode(getHost());
			ProphetRouter othRouter = (ProphetRouter) other.getRouter();

			if (othRouter.isTransferring()) {
				continue;
			}

			for (Message m : msgCollection) {
				if (othRouter.hasMessage(m.getId())) {
					continue;
				}
				if (othRouter.getPredFor(m.getTo()) > getPredFor(m.getTo())) {
					messages.add(new TupleDe<>(m, con));
				}
			}
		}

		if (messages.isEmpty()) {
			return null;
		}

		Collections.sort(messages, new TupleComparator());
		return tryMessagesForConnected(null);
	}

	private class TupleComparator implements Comparator<TupleDe<Message, Connection>> {
		public int compare(Tuple<Message, Connection> t1, Tuple<Message, Connection> t2) {
			double p1 = ((ProphetRouter) t1.getValue().getOtherNode(getHost()).getRouter())
					.getPredFor(t1.getKey().getTo());
			double p2 = ((ProphetRouter) t2.getValue().getOtherNode(getHost()).getRouter())
					.getPredFor(t2.getKey().getTo());

			if (p2 - p1 == 0) {
				return compareByQueueMode(t1.getKey(), t2.getKey());
			} else {
				return Double.compare(p2, p1); // descending order
			}
		}

        @Override
        public int compare(TupleDe<Message, Connection> o1, TupleDe<Message, Connection> o2) {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'compare'");
        }
	}

	@Override
	public routing.RoutingInfo getRoutingInfo() {
		ageDeliveryPreds();
		routing.RoutingInfo top = super.getRoutingInfo();
		RoutingInfo ri = new RoutingInfo(preds.size() + " delivery prediction(s)");

		for (Map.Entry<DTNHost, Double> e : preds.entrySet()) {
			DTNHost host = e.getKey();
			Double value = e.getValue();
			ri.addMoreInfo(new RoutingInfo(String.format("%s : %.6f", host, value)));
		}

		top.addMoreInfo(ri);
		return top;
	}

	@Override
	public MessageRouter replicate() {
		return new ProphetRouter(this);
	}
}
