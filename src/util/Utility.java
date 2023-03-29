import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.Random;

public class Utility {
    public static class UnreliableSocket implements AutoCloseable{
        DatagramSocket socket;

        public UnreliableSocket() throws SocketException {
            socket = new DatagramSocket();
        }
        public void bind(int port) throws SocketException {
            socket.bind(new InetSocketAddress(port));
        }

        // TODO: simulate packet loss, delay, and corruption
        public void recvfrom(DatagramPacket p) throws IOException, InterruptedException {
            socket.receive(p); // receive a packet
            Random r = new Random();

            if (r.nextDouble() <= 0.1) { // lost packet -> simulate by dropping this packet and receive the packet at the end of the method
                System.out.println("dropped");
                socket.receive(p);
            } else if (r.nextDouble() <= 0.2) { // delayed packet -> sleep for 500ms, sure to delay
                System.out.println("delayed");
                Thread.sleep(500);
            } else if (r.nextDouble() <= 0.5) { // corrupted packet -> change bytes
                System.out.println("corrupted");
                int index = r.nextInt(p.getData().length); // TODO: exclude header after that
//            System.out.println((char) p.getData()[index]);
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

}
