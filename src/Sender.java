import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Scanner;

public class Sender {
    private static final String fileRead = "alice.txt";

    public static void main(String[] args) throws IOException, InterruptedException {
        if (args.length != 3) {
            System.err.println("Usage: java Sender [Receiver IP] [Receiver Port] [Window Size]");
            System.exit(1);
        }

        InetAddress receiverIP = InetAddress.getByName(args[0]);
        int receiverPort = Integer.parseInt(args[1]);
        int windowSize = Integer.parseInt(args[2]);

        if (windowSize <= 0) {
            System.err.println("Window Size should be positive");
            System.exit(1);
        }

        // establish connection
        RDTSocket senderSocket = new RDTSocket(windowSize);
        System.out.println("connecting to receiver");
        senderSocket.connect(receiverIP, receiverPort);
        System.out.println("connected");
        // send data
        byte[] data = Files.readAllBytes(Paths.get(fileRead));
        System.out.println("sending data");
        senderSocket.send(data);
        // close connection
        senderSocket.close();
        System.out.println("closed connection");
    }
}
