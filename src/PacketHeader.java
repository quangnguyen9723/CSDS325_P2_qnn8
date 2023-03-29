package utility;

public class PacketHeader {
    // 0: START; 1: END; 2: DATA; 3: ACK
    int type;
    // Described below
    int seq_num;
    // Length of data; 0 for ACK, START and END packets
    int length;
    // 32-bit CRC
    int checksum;


    public void compute_checksum() {

    }

    public void verify_packet() {

    }
}
