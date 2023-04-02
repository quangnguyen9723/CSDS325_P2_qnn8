import util.Utility;

import javax.xml.crypto.Data;
import java.io.IOException;
import java.lang.reflect.Array;
import java.net.*;
import java.util.*;

import static util.Utility.*;

public class RDTSocket {

    private static final int TIME_OUT = 500;
    // common attributes
    private final UnreliableSocket unreliableSocket;

    private final int windowSize;

    // SENDER related - this is SENDER
    private InetSocketAddress receiverAddr;

    private int ISN;

    private int expectedACK;
    // RECEIVER related
    private InetSocketAddress senderAddr;

    private int receiverBase;

    private int expectedSeqNum;

    // For connection to Receiver
    // invoked by Sender
    public RDTSocket(int windowSize) throws SocketException {
        unreliableSocket = new UnreliableSocket();
        this.windowSize = windowSize;
    }

    // For binding to local port
    // invoked by Receiver
    public RDTSocket(int windowSize, int port) throws SocketException {
        unreliableSocket = new UnreliableSocket(port);
        this.windowSize = windowSize;
    }
    // called by receiver

    private boolean isRegisteredSender(DatagramPacket p) {
        return p.getAddress().equals(senderAddr.getAddress()) && p.getPort() == senderAddr.getPort();
    }

    // called by sender
    private boolean isRegisteredReceiver(DatagramPacket p) {
        return p.getAddress().equals(receiverAddr.getAddress()) && p.getPort() == receiverAddr.getPort();
    }


    // Similar to the accept function defined in TCP protocol
    // invoked by the receiver
    public InetSocketAddress accept() throws IOException, InterruptedException {
        /*
        Steps to include:
        1. Wait for initial START packet
        2. Send back a packet with
         */
        while (true) {
            // wait for START
            DatagramPacket recvSTART = new DatagramPacket(new byte[MSS], MSS);
            unreliableSocket.recvfrom(recvSTART);
            PacketHeader headerSTART = Utility.extractHeader(recvSTART);

            int type = headerSTART.getType();

            // checks if not START or END
            if (type != 0 && type != 1) continue;

            if (type == 1) {
                InetSocketAddress endAddr = new InetSocketAddress(recvSTART.getAddress(), recvSTART.getPort());
                PacketHeader headerEND = new PacketHeader(1, 0, 0, 0);
                byte[] segmentEND = Utility.encodeHeader(headerEND);
                DatagramPacket sendEND = new DatagramPacket(segmentEND, segmentEND.length, endAddr);
                unreliableSocket.sendto(sendEND);

                continue;
            }

            // register sender
            senderAddr = new InetSocketAddress(recvSTART.getAddress(), recvSTART.getPort());
            // expectedSeqNum = ISN + 1
            expectedSeqNum = headerSTART.getSeq_num() + 1;

            // sends ACK to the sender
            PacketHeader headerACK = new PacketHeader(3, headerSTART.getSeq_num(), 0, 0);
            byte[] segmentACK = Utility.encodeHeader(headerACK);
            DatagramPacket sendACK = new DatagramPacket(segmentACK, segmentACK.length, senderAddr);

            unreliableSocket.sendto(sendACK);

            return senderAddr;
        }
    }

    // Receive data
    // invoked by receiver
    public byte[] recv() throws IOException, InterruptedException {
        /*
        Steps to include:
        1. receive ACK, check sender
        2. handle logic for each type:
        - ACK: skip (not going to happen)
        - START: resend START
        - END: handle logic and end the loop
        - DATA: handle using rdt logic
         */
        // maps seqNum to its payload for reordering
        HashMap<Integer, byte[]> receivedPackets = new HashMap<>();

        while (true) {
            DatagramPacket recvPacket = new DatagramPacket(new byte[MSS], MSS);
            unreliableSocket.recvfrom(recvPacket);
            PacketHeader recvHeader = extractHeader(recvPacket);

            // check correct sender and corruption
            if (!(isRegisteredSender(recvPacket) && verify_packet(recvPacket))) continue;

            int recvType = recvHeader.getType();

            // handle ACK or unknown type -> discard
            if (recvType > 2 || recvType < 0) continue;

            // handle START
            if (recvType == 0) {
                PacketHeader headerAckStart = new PacketHeader(3, receiverBase, 0, 0);
                byte[] segmentAckStart = encodeHeader(headerAckStart);
                DatagramPacket sendAckStart = new DatagramPacket(segmentAckStart, segmentAckStart.length, senderAddr);
                unreliableSocket.sendto(sendAckStart);
                continue;
            }
            // handle END
            if (recvType == 1) {
                PacketHeader header = new PacketHeader(3, 0, 0, 0);
                byte[] toSend = encodeHeader(header);
                DatagramPacket sendPacket = new DatagramPacket(toSend, toSend.length, senderAddr);
                unreliableSocket.sendto(sendPacket);
                break;
            }

            // the only condition left is DATA (2)

            // check corruption
            if (!verify_packet(recvPacket)) continue;
            int recvSeqNum = recvHeader.getSeq_num();

            // store the packet if it is new and in the window range
            boolean isNewPacket = !receivedPackets.containsKey(recvSeqNum);
            boolean isInWindowRange = recvSeqNum < expectedSeqNum + windowSize && recvSeqNum >= expectedSeqNum;
            if (isInWindowRange && isNewPacket) {
                receivedPackets.put(recvSeqNum, extractPayload(recvPacket));
            }

            // sends ACK back conditionally
            if (recvSeqNum != expectedSeqNum) {
                // not the expected lowest chunk in window -> resend ack with seqNum = expectedSeqNum
                PacketHeader header = new PacketHeader(3, expectedSeqNum, 0, 0);
                byte[] toSend = encodeHeader(header);
                DatagramPacket sendPacket = new DatagramPacket(toSend, toSend.length, senderAddr);
                unreliableSocket.sendto(sendPacket);
            } else {
                // expected lowest chunk in window -> forward the window with the new expectedSeqNum being the packet has not received
                while (receivedPackets.containsKey(expectedSeqNum)) {
                    expectedSeqNum++;
                }
            }
        }

        // reassemble data
        List<byte[]> chunkList = receivedPackets
                .entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .map(Map.Entry::getValue)
                .toList();
        int numChunks = 0;
        for (byte[] chunk : chunkList) numChunks += chunk.length;

        byte[] completeData = new byte[numChunks];
        int offset = 0;
        for (byte[] chunk : chunkList) {
            System.arraycopy(chunk, 0, completeData, offset, chunk.length);
            offset += chunk.length;
        }

        return completeData;
    }

    //___________________________________________________________________________________________________________________________
    // Similar to the connect function defined in TCP
    // invoked by the sender
    public void connect(InetAddress receiverIP, int receiverPort) throws IOException, InterruptedException {
        /*
        Steps to include:
        1. Sends START with ISN
        2. Waits for ACK with seqNum = ISN
        3. If timeout, resends START with ISN
        4. repeat 2 and 3 until receive ACK
         */
        receiverAddr = new InetSocketAddress(receiverIP, receiverPort);
        int type = 0;
//        ISN = (new Random()).nextInt(10000);
        ISN = 0;

        final byte[] segmentSTART = encodeHeader(new PacketHeader(type, ISN, 0, 0));
        Timer timer = new Timer();
        // send START and begin timer
        TimerTask resendSTART = new TimerTask() {
            @Override
            public void run() {
                try {
                    DatagramPacket sendSTART = new DatagramPacket(segmentSTART, segmentSTART.length, receiverAddr);
                    unreliableSocket.sendto(sendSTART);
                } catch (IOException e) {
                    System.out.println("error in threadSTART");
                    e.printStackTrace();
                }
            }
        };
        timer.schedule(resendSTART, 0, TIME_OUT);

        // wait for ACK and verify connection
        while (true) {
            DatagramPacket recvSTART = new DatagramPacket(new byte[MSS], MSS);
            unreliableSocket.recvfrom(recvSTART);

            PacketHeader header = extractHeader(recvSTART);
            // check correct receiver, type ACK, and correct ACK seqnum
            if (header.getType() == 3 && isRegisteredReceiver(recvSTART) && header.getSeq_num() == ISN) {
                expectedACK = ISN + 2;
                break;
            }
        }
        // close timer for START
        timer.cancel();
    }

    // Transmit data
    // invoked by sender
    public void send(byte[] data) throws IOException, InterruptedException {
        /*
        Steps to include:
        1. Sends the initial window, start the timer
        2. Waits for ack to come back
        - timer continuously send the window in interval of TIME_OUT
        - for each ACK received, check using the rdt logic:
            - corruption and receiver
            - if recvACK >= expectedACK
            a. cancel the current timer
            b. forward the window
            c. start new timer sending the new window
            - if recvACK < expectedACK -> don't care
         */
        // split data into chunks of MAX_PAYLOAD length each
        LinkedList<DatagramPacket> sendPackets = splitData(data);
        int numPackets = sendPackets.size();

        // init resources

        int lastExpectedACK = ISN + numPackets + 1; // after sending last seqNum = N, sender will receive ACK = N + 1

        // sliding window from initial round window
        LinkedList<DatagramPacket> slidingWindow = new LinkedList<>();
        for (int i = 0; i < windowSize && !sendPackets.isEmpty(); i++) {
            slidingWindow.add(sendPackets.removeFirst());
        }

        // send the initial round
        Timer timer = new Timer(true);
        timer.schedule(createSendWindowTask(slidingWindow), 0, TIME_OUT);

        // loop send data using RDT logic
        while (expectedACK <= lastExpectedACK) {
            // receive packet
            DatagramPacket recvPacket = new DatagramPacket(new byte[MSS], MSS);
            unreliableSocket.recvfrom(recvPacket);
            PacketHeader recvHeader = extractHeader(recvPacket);

            // check correct receiver, not corrupted, and is ACK
            if (!(isRegisteredReceiver(recvPacket) && verify_packet(recvPacket) && recvHeader.getType() == 3)) continue;

            // execute the RDT logic
            int recvACK = recvHeader.getSeq_num();
            // if received ACK smaller than the expected ACK -> drop this packet
            if (recvACK < expectedACK) continue;

            // if received ACK larger than or equal the expected ACK:
            // 1. cancel the previous timer
            // 2. forward the sender's window
            // 3. make a new timer for the new window

            // cancel the timer
            timer.cancel();

            // forward the window
            while (expectedACK <= recvACK) {
                if (!slidingWindow.isEmpty()) slidingWindow.removeFirst(); // remove the ACKed packet
                if (!sendPackets.isEmpty())
                    slidingWindow.add(sendPackets.removeFirst()); // add more to window if there is any
                expectedACK++;
            }

            // make new timer
            timer = new Timer(true);
            timer.schedule(createSendWindowTask(slidingWindow), 0, TIME_OUT);
        }
    }

    private TimerTask createSendWindowTask(List<DatagramPacket> window) {
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                try {
                    for (DatagramPacket packet : window) {
                        unreliableSocket.sendto(packet);
                    }
                } catch (IOException e) {
                    System.out.println("error at resendWindow TimerTask");
                    throw new RuntimeException(e);
                } catch (ConcurrentModificationException e) {
                    // skip
                }
            }
        };
        return task;
    }

    private LinkedList<DatagramPacket> splitData(byte[] data) {
        int numPackets = Math.ceilDiv(data.length, MAX_PAYLOAD);
        DatagramPacket[] packets = new DatagramPacket[numPackets];

        for (int i = 0; i < numPackets; i++) {
            int type = 2;
            int seqNum = ISN + 1 + i;
            int length;
            int checksum;
            byte[] payload;

            if (i == numPackets - 1) {
                payload = Arrays.copyOfRange(data, MAX_PAYLOAD * i, data.length);
            } else {
                payload = Arrays.copyOfRange(data, MAX_PAYLOAD * i, MAX_PAYLOAD * (i + 1));
            }

            length = payload.length;
            checksum = compute_checksum(payload);
            PacketHeader header = new PacketHeader(type, seqNum, length, checksum);
            byte[] segment = insertHeader(Utility.encodeHeader(header), payload);

            packets[i] = new DatagramPacket(segment, segment.length, receiverAddr);
        }

        LinkedList<DatagramPacket> list = new LinkedList<>();
        for (DatagramPacket p : packets) {
            list.add(p);
        }

        return list;
    }

    // close connection
    // invoked by sender
    public void close() throws IOException, InterruptedException {
        /*
        Steps to include:
        1. sends END, start timer
        2. wait for ACK OF END
         */
        // send END
        PacketHeader endHeader = new PacketHeader(1, 0, 0, 0);
        byte[] endChunk = encodeHeader(endHeader);

        Timer timer = new Timer();
        TimerTask resendEnd = new TimerTask() {
            @Override
            public void run() {
                try {
                    DatagramPacket endPacket = new DatagramPacket(endChunk, endChunk.length, receiverAddr);
                    unreliableSocket.sendto(endPacket);
                } catch (IOException e) {
                    System.out.println("error in resendEnd");
                    e.printStackTrace();
                }
            }
        };
        timer.schedule(resendEnd, 0, TIME_OUT);
        // wait for ACK

        while (true) {
            DatagramPacket recvPacket = new DatagramPacket(new byte[MSS], MSS);
            unreliableSocket.recvfrom(recvPacket);

            if (!(isRegisteredReceiver(recvPacket) && verify_packet(recvPacket))) continue;

            PacketHeader recvHeader = extractHeader(recvPacket);

            if (recvHeader.getType() == 3 && recvHeader.getSeq_num() == 0) break;
        }
        timer.cancel();
        Thread.sleep(TIME_OUT);
        unreliableSocket.close();
    }
}
