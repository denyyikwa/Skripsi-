/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package test;

import core.Message;
import routing.MessageRouter;
import routing.ProphetRouter;

/**
 * Tests for PRoPHET router
 */
public class ProphetRouterTest extends AbstractRouterTest {

    private static final int SECONDS_IN_TIME_UNIT = 60;

    @Override
    public void setUp() throws Exception {
        ts.setNameSpace(null);
        ts.putSetting(MessageRouter.B_SIZE_S, String.valueOf(BUFFER_SIZE));
        ts.putSetting(
            ProphetRouter.PROPHET_NS + "." + ProphetRouter.SECONDS_IN_UNIT_S,
            String.valueOf(SECONDS_IN_TIME_UNIT)
        );
        setRouterProto(new ProphetRouter(ts));
        super.setUp();
    }

    /**
     * Tests normal PRoPHET routing behavior
     */
    public void testRouting() {
        // Create messages with various destinations
        h1.createNewMessage(new Message(h1, h2, msgId2, 1));
        h1.createNewMessage(new Message(h1, h3, msgId3, 1));
        h1.createNewMessage(new Message(h1, h4, msgId4, 1));
        h1.createNewMessage(new Message(h1, h6, "dummy", 1)); // should not be forwarded
        h1.createNewMessage(new Message(h1, h5, msgId5, 1));
        h4.createNewMessage(new Message(h4, h1, "d1", 1));

        ProphetRouter r4 = (ProphetRouter) h4.getRouter();
        ProphetRouter r5 = (ProphetRouter) h5.getRouter();

        checkCreates(6);

        h4.connect(h5);
        assertEquals(ProphetRouter.P_INIT, r4.getPredFor(h5));
        assertEquals(ProphetRouter.P_INIT, r5.getPredFor(h4));

        updateAllNodes();

        // h4 should not forward message to h5 yet
        assertFalse(mc.next());

        disconnect(h5);
        h5.connect(h3); // h3 learns about h5-h4 connection
        h1.connect(h3); // h1-h3-h5 path now exists

        updateAllNodes();

        // h1 should transfer msgId3 to h3
        assertTrue(mc.next());
        assertEquals(mc.TYPE_START, mc.getLastType());
        assertEquals(msgId3, mc.getLastMsg().getId());
        assertEquals(h1, mc.getLastFrom());
        assertFalse(mc.next());

        clock.advance(10);
        updateAllNodes();
        assertTrue(mc.next());
        assertEquals(mc.TYPE_RELAY, mc.getLastType());
        assertEquals(msgId3, mc.getLastMsg().getId());
        assertTrue(mc.getLastFirstDelivery());

        // h1 transfers msgId5 to h3
        assertTrue(mc.next());
        assertEquals(mc.TYPE_START, mc.getLastType());
        assertEquals(msgId5, mc.getLastMsg().getId());
        assertEquals(h1, mc.getLastFrom());
        assertFalse(mc.next());

        clock.advance(10);
        updateAllNodes();
        assertTrue(mc.next());
        assertEquals(mc.TYPE_RELAY, mc.getLastType());
        assertEquals(msgId5, mc.getLastMsg().getId());

        assertTrue(mc.next());
        assertEquals(mc.TYPE_START, mc.getLastType());
        assertEquals(msgId4, mc.getLastMsg().getId());
        assertEquals(h1, mc.getLastFrom());
        assertFalse(mc.next());

        // Now we do relay
        doRelay();
        assertTrue(mc.next());

        // h3 transfers msgId5 to h5
        assertEquals(mc.TYPE_START, mc.getLastType());
        assertEquals(msgId5, mc.getLastMsg().getId());
        assertEquals(h3, mc.getLastFrom());

        doRelay(); // delivers to h5
        assertTrue(mc.getLastFirstDelivery());
        assertTrue(mc.next());

        // h3 transfers msgId4 to h5
        assertEquals(mc.TYPE_START, mc.getLastType());
        assertEquals(msgId4, mc.getLastMsg().getId());
        assertEquals(h3, mc.getLastFrom());

        doRelay();

        // no more transfers expected
        assertFalse(mc.next());
    }

    /**
     * Advances time and forces relaying
     */
    private void doRelay() {
        clock.advance(10);
        updateAllNodes();
        assertTrue(mc.next());
        assertEquals(mc.TYPE_RELAY, mc.getLastType());
    }

    /**
     * Tests the aging mechanism in PRoPHET
     */
    public void testAging() {
        ProphetRouter r4 = (ProphetRouter) h4.getRouter();
        ProphetRouter r5 = (ProphetRouter) h5.getRouter();

        h4.connect(h5);
        assertEquals(ProphetRouter.P_INIT, r4.getPredFor(h5));
        assertEquals(ProphetRouter.P_INIT, r5.getPredFor(h4));

        disconnect(h5);

        clock.advance(SECONDS_IN_TIME_UNIT * 2);
        double expectedPred = ProphetRouter.P_INIT *
                              Math.pow(ProphetRouter.DEFAULT_GAMMA, 2);

        assertEquals(expectedPred, r4.getPredFor(h5));
        assertEquals(expectedPred, r5.getPredFor(h4));

        clock.advance(SECONDS_IN_TIME_UNIT / 10);
        expectedPred *= Math.pow(ProphetRouter.DEFAULT_GAMMA, 1.0 / 10);

        assertEquals(expectedPred, r4.getPredFor(h5));
        assertEquals(expectedPred, r5.getPredFor(h4));
    }
}
