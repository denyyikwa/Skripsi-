package routing;

import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;
import java.util.List;

// NARA: extend dari EnergyAwareRouter karena sudah ada mekanisme untuk baterai (energy)
public class ThresholdBatteryBufferRouter extends EnergyAwareRouter {

    private double batteryThreshold;
    private double bufferThreshold;

    public ThresholdBatteryBufferRouter(Settings s) {
        super(s);
        batteryThreshold = s.getDouble("ThresholdBatteryBufferRouter.batteryThreshold");
        bufferThreshold = s.getDouble("ThresholdBatteryBufferRouter.bufferThreshold");
    }

    protected ThresholdBatteryBufferRouter(ThresholdBatteryBufferRouter r) {
        super(r);
        this.batteryThreshold = r.batteryThreshold;
        this.bufferThreshold = r.bufferThreshold;
    }

    @Override
    public void update() {
        super.update();

        // Jika sedang transfer atau tidak bisa mulai
        if (isTransferring())
            return;
        if (!canStartTransfer())
            return;

        // CEK BATTERY

        // NARA: baca dari EnergyAwareRouter, default 100.0 jika tidak ada
        // double battery = getHost().getComBus().getDouble("Energy.value", 100.0);

        // if (battery < batteryThreshold) {
        // return; // stop kirim jika baterai rendah
        // }

        // // CEK BUFFER

        // double usedBuffer = getBufferSize() - getFreeBufferSize();
        // double bufferUsage = usedBuffer / getBufferSize();

        // if (bufferUsage > bufferThreshold) {
        // return; // stop kirim jika buffer penuh
        // }

        // // FORWARD MESSAGE

        tryForwardMessages();
    }

    private void tryForwardMessages() {

        List<Connection> connections = getConnections();

        if (connections.size() == 0)
            return;

        for (Connection con : connections) {

            DTNHost other = con.getOtherNode(getHost());

            if (isTransferring()) {
                return; // stop jika sudah mulai transfer
            }

            for (Message m : getMessageCollection()) {
                // NARA: jangan kirim jika buffer sudah melewati atau di threshold
                // if (getBufferSize() / 1000.0 >= bufferThreshold) { //step 1: cek threshold
                // buffer
                // return;
                // }
                double usedBuffer = getBufferSize() - getFreeBufferSize();
                double bufferUsage = usedBuffer / getBufferSize();

                if (bufferUsage > bufferThreshold) {
                    return; // stop kirim jika buffer penuh
                }

                double battery = getHost().getComBus().getDouble("Energy.value", 1000.0);

                if (battery < batteryThreshold) { // step 2: cek threshold baterai
                    return; // stop kirim jika baterai rendah
                }
                // Kirim jika tujuan langsung
                if (m.getTo() == other) {
                    startTransfer(m, con);
                    return;
                }

                // Kirim jika node lain belum punya pesan
                if (!other.getRouter().hasMessage(m.getId())) {
                    startTransfer(m, con);
                    return;
                }
            }
        }
    }

    @Override
    public ThresholdBatteryBufferRouter replicate() {
        return new ThresholdBatteryBufferRouter(this);
    }
}