package routing.community;

import java.util.List;
import java.util.Map;

import core.DTNHost;

/**
 * Interface untuk mendefinisikan metode perhitungan centrality dalam konteks routing komunitas.
 */
public interface Centrality {


    
    /**kom
     * Mengembalikan nilai global centrality berdasarkan riwayat koneksi antar host.
     *
     * @param connHistory Riwayat koneksi antar host
     * @return Nilai global centrality
     */
    double getGlobalCentrality(Map<DTNHost, List<Duration>> connHistory);
 
    /**
     * Mengembalikan nilai local centrality berdasarkan riwayat koneksi dan deteksi komunitas.
     *
     * @param connHistory Riwayat koneksi antar host
     * @param cd Objek deteksi komunitas
     * @return Nilai local centrality
     */
    double getLocalCentrality(Map<DTNHost, List<Duration>> connHistory, CommunityDetection cd);

    /**
     * Mengembalikan salinan baru dari objek centrality.
     *
     * @return Objek Centrality baru
     */
    Centrality replicate();

    /**
     * Mengembalikan array penuh nilai global centrality per window (bukan nilai rata-rata).
     *
     * @param connHistory Riwayat koneksi antar host
     * @return Array nilai global centrality
     */
    double[] getGlobal(Map<DTNHost, List<Duration>> connHistory);

    /**
     * Mengembalikan daftar nilai popularitas global (tidak dirata-ratakan).
     *
     * @return Daftar nilai popularitas global
     */
    List<Double> getGlobalPopularity();

    /**
     * Mengembalikan array nilai centrality global dalam bentuk integer.
     * (Ditambahkan sesuai kebutuhan khusus algoritma baru)
     *
     * @param connHistory Riwayat koneksi antar host
     * @return Array integer nilai global centrality
     */
    int[] getGlobalArrayCentrality(Map<DTNHost, List<Duration>> connHistory);
}
