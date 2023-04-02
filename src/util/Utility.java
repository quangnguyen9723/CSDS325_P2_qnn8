package util;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Random;

public class Utility {

    public static final int MSS = 1472;

    public static final int HEADER_LENGTH = 16;

    public static final int MAX_PAYLOAD = MSS - HEADER_LENGTH;

    //______________________________________________________Unreliable Socket_____________________________________________________________________
    public static class UnreliableSocket implements AutoCloseable{
        DatagramSocket socket;

        public UnreliableSocket() throws SocketException {
            socket = new DatagramSocket();
        }

        public UnreliableSocket(int port) throws SocketException {
            socket = new DatagramSocket(port);
        }

        public void bind(int port) throws SocketException {
            socket.bind(new InetSocketAddress(port));
        }

        // TODO: simulate packet loss, delay, and corruption
        public void recvfrom(DatagramPacket p) throws IOException, InterruptedException {
            socket.receive(p); // receive a packet
            Random r = new Random();

            if (r.nextDouble() <= 0.3) {
                Thread.sleep(r.nextInt(300)); // delay from 0 to 300ms -> high chance of delay
            }

            if (r.nextDouble() <= 0.1) { // lost packet -> simulate by dropping this packet and receive the packet at the end of the method
                socket.receive(p);
            }
            if (r.nextDouble() <= 0.5) { // corrupted packet -> change bytes
                PacketHeader header = extractHeader(p);
                if (header.getLength() == 0) return;

                int index = r.nextInt(HEADER_LENGTH, p.getData().length);
                p.getData()[index] = (byte) (r.nextInt(128)); // some random index is randomized
            }
        }

        public void sendto(DatagramPacket p) throws IOException {
            socket.send(p);
        }

        @Override
        public void close() {
            socket.close();
        }
    }

    //______________________________________________________Packet Header_____________________________________________________________________

    public static class PacketHeader {
        private final int type; // 0: START; 1: END; 2: DATA; 3: ACK -> 4 bytes
        private final int seq_num; // Described below -> 4 bytes TODO: remember to implement wraparounds in byte
        private final int length; // Length of data; 0 for ACK, START and END packets
        private final int checksum; // 32-bit CRC -> 4 bytes


        public PacketHeader(int type, int seq_num, int length, int checksum) {
            this.type = type;
            this.seq_num = seq_num;
            this.length = length;
            this.checksum = checksum;
        }

        public int getType() {
            return type;
        }

        public int getSeq_num() {
            return seq_num;
        }

        public int getLength() {
            return length;
        }

        public int getChecksum() {
            return checksum;
        }

        @Override
        public String toString() {
            return "type:" + getType() + " seq_num:" + getSeq_num() + " length:" + getLength() + " checksum:" + getChecksum();
        }
    }

    //______________________________________________________Utility Function_____________________________________________________________________


    public static byte[] encodeHeader(PacketHeader header) {
        byte[] encodedType = ByteBuffer.allocate(4).putInt(header.type).array();
        byte[] encodedSeq_num = ByteBuffer.allocate(4).putInt(header.seq_num).array();
        byte[] encodedLength = ByteBuffer.allocate(4).putInt(header.length).array();
        byte[] encodedChecksum = ByteBuffer.allocate(4).putInt(header.checksum).array();
        byte[] encodedHeader = new byte[16];
        System.arraycopy(encodedType, 0, encodedHeader, 0, 4);
        System.arraycopy(encodedSeq_num, 0, encodedHeader, 4, 4);
        System.arraycopy(encodedLength, 0, encodedHeader, 8, 4);
        System.arraycopy(encodedChecksum, 0, encodedHeader, 12, 4);

        return encodedHeader;
    }

    public static PacketHeader decodeHeader(byte[] header) {
        if (header.length != HEADER_LENGTH) {
            throw new IllegalArgumentException("header length should be 16");
        }

        int type = ByteBuffer.wrap(Arrays.copyOfRange(header, 0, 4)).getInt();
        int seq_num = ByteBuffer.wrap(Arrays.copyOfRange(header, 4, 8)).getInt();
        int length = ByteBuffer.wrap(Arrays.copyOfRange(header, 8, 12)).getInt();
        int checksum = ByteBuffer.wrap(Arrays.copyOfRange(header, 12, 16)).getInt();

        return new PacketHeader(type, seq_num, length, checksum);
    }

    public static PacketHeader extractHeader(DatagramPacket p) {
        return decodeHeader(Arrays.copyOf(p.getData(), HEADER_LENGTH));
    }

    public static byte[] extractPayload(DatagramPacket p) {
        PacketHeader header = extractHeader(p);
        return Arrays.copyOfRange(p.getData(), HEADER_LENGTH, HEADER_LENGTH + header.getLength());
    }

    // checksum only calculates the body of segment
    public static int compute_checksum(byte[] data) {
        int crc = 0xffffffff;
        for (byte b : data) {
            crc ^= b & 0xff;
            for (int i = 0; i < 8; i++) {
                if ((crc & 1) == 1) {
                    crc = (crc >>> 1) ^ 0xedb88320;
                } else {
                    crc >>>= 1;
                }
            }
        }
        return ~crc;
    }

    public static boolean verify_packet(DatagramPacket p) {
        PacketHeader header = extractHeader(p);
        if (header.getLength() == 0) return true;

        byte[] payload = extractPayload(p);
        return header.getChecksum() == compute_checksum(payload);
    }

    public static byte[] insertHeader(byte[] header, byte[] payload) {
        byte[] segment = new byte[header.length + payload.length];
        System.arraycopy(header, 0, segment, 0, HEADER_LENGTH);
        System.arraycopy(payload, 0, segment, HEADER_LENGTH, payload.length);
        return segment;
    }

}
