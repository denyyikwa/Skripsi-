package report;

import core.DTNHost;
import core.Message;
import core.MessageListener;

import java.util.*;

/**
 * Global Popularity Report - menunjukkan jumlah pesan yang diterima oleh tiap node
 * dan membuat ranking berdasarkan popularitas global.
 */
public class GlobalPopularityReport extends Report implements MessageListener {

    public static final String HEADER = "# Global Popularity Ranking: Node - Total Messages Received";
    private Map<DTNHost, Integer> popularityMap;

    public GlobalPopularityReport() {
        init();
    }

    @Override
    public void init() {
        super.init();
        popularityMap = new HashMap<>();
        write(HEADER);
    }

    @Override
    public void messageTransferred(Message m, DTNHost from, DTNHost to, boolean firstDelivery) {
        if (to.getAddress() == m.getTo().getAddress()) {
            popularityMap.put(to, popularityMap.getOrDefault(to, 0) + 1);
        }
    }

    @Override
    public void done() {
        write("");
        write("Node\tMessages_Received");

        // Sorting descending by number of received messages
        List<Map.Entry<DTNHost, Integer>> sortedList = new ArrayList<>(popularityMap.entrySet());
        sortedList.sort((a, b) -> b.getValue() - a.getValue());

        for (Map.Entry<DTNHost, Integer> entry : sortedList) {
            write(entry.getKey().toString() + "\t" + entry.getValue());
        }

        super.done();
    }

    // Unused but required overrides
    public void newMessage(Message m) {}
    public void messageDeleted(Message m, DTNHost where, boolean dropped) {}
    public void messageTransferStarted(Message m, DTNHost from, DTNHost to) {}
    public void messageTransferAborted(Message m, DTNHost from, DTNHost to) {}

}
