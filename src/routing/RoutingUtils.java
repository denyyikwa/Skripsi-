package routing;

import java.util.List;
import core.Connection;
import core.Message;
import core.DTNHost;
import core.Tuple;

/**
 * Utility functions for routing algorithms.
 */
public class RoutingUtils {

    /**
     * Tries to send messages for the connections that are mentioned
     * in the Tuples in the order they are in the list until one of
     * the connections starts transferring or all tuples have been tried.
     * 
     * @param router The router initiating the transfer
     * @param tuples The tuples to try
     * @return The tuple whose connection accepted the message or null if
     * none of the connections accepted the message that was meant for them.
     */
    public static Tuple<Message, Connection> tryMessagesForConnected(
            ActiveRouter router,
            List<Tuple<Message, Connection>> tuples) {

        if (tuples == null || tuples.isEmpty()) {
            return null;
        }

        for (Tuple<Message, Connection> t : tuples) {
            Message m = t.getKey();
            Connection con = t.getValue();
            if (router.startTransfer(m, con) == ActiveRouter.RCV_OK) {
                return t;
            }
        }

        return null;
    }
}
