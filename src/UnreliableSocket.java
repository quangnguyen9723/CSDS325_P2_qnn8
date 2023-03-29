package utility;

import java.io.IOException;
import java.net.*;

public class UnreliableSocket {
    DatagramSocket socket;

    public UnreliableSocket() throws SocketException {
        socket = new DatagramSocket();
    }

    public void bind(InetAddress laddr, int port) throws SocketException {
        socket.bind(new InetSocketAddress(laddr, port));
    }

    // TODO: simulate packet loss, delay, and corruption
    public void recvfrom(DatagramPacket p) throws IOException {
        socket.receive(p);
    }

    public void sendto(DatagramPacket p) throws IOException {
        socket.send(p);
    }

    public void close() {
        socket.close();
    }
}
