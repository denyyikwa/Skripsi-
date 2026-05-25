package routing;

import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;
import java.util.List;

public class ThresholdBatteryBufferRouter extends EnergyAwareRouter {

    private double batteryThreshold;
    private double bufferThreshold;
    private double configMaxEnergy; 

    public ThresholdBatteryBufferRouter(Settings s) {
        super(s);
        // Membaca threshold dari file konfigurasi
        batteryThreshold = s.getDouble("ThresholdBatteryBufferRouter.batteryThreshold");
        bufferThreshold = s.getDouble("ThresholdBatteryBufferRouter.bufferThreshold");
        
        double[] energies = s.getCsvDoubles(INIT_ENERGY_S);
        this.configMaxEnergy = energies[energies.length - 1];
    }

    protected ThresholdBatteryBufferRouter(ThresholdBatteryBufferRouter r) {
        super(r);
        this.batteryThreshold = r.batteryThreshold;
        this.bufferThreshold = r.bufferThreshold;
        this.configMaxEnergy = r.configMaxEnergy;
    }

    @Override
    public void update() {
        reduceSendingAndScanningEnergy(); 
            
        if (isTransferring() || !canStartTransfer()) {
            return; 
        }
        
        tryForwardMessages();
    }

    private void tryForwardMessages() {
        List<Connection> connections = getConnections();

        if (connections.size() == 0)
            return;

        for (Connection con : connections) {
            DTNHost other = con.getOtherNode(getHost());

            if (isTransferring()) {
                return; 
            }

            for (Message m : getMessageCollection()) {
                
                // 1. CEK THRESHOLD BUFFER
                double usedBuffer = getBufferSize() - getFreeBufferSize();
                double bufferUsage = usedBuffer / getBufferSize();

                if (bufferUsage > bufferThreshold) {
                    break; 
                }

                // 2. CEK THRESHOLD BATERAI
                double battery = getHost().getComBus().getDouble(ENERGY_VALUE_ID, this.configMaxEnergy);
                double batteryPercentage = battery / this.configMaxEnergy;

                if (batteryPercentage < batteryThreshold) { 
                    break;
                }

                if (m.getTo() == other) {
                    if (startTransfer(m, con) >= 0) {
                        return; 
                    }
                }

                if (!other.getRouter().hasMessage(m.getId())) {
                    if (startTransfer(m, con) >= 0) {
                        return; 
                    }
                }
            }
        }
    }

    @Override
    public ThresholdBatteryBufferRouter replicate() {
        return new ThresholdBatteryBufferRouter(this);
    }
}