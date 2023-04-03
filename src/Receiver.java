import java.io.*;

public class Receiver {
    public static final String fileWrite = "download.txt";

    public static void main(String[] args) throws IOException, InterruptedException {
        if (args.length != 2) {
            System.err.println("Usage: java Receiver [Receiver Port] [Window Size]");
            System.exit(1);
        }

        int receiverPort = Integer.parseInt(args[0]);
        int windowSize = Integer.parseInt(args[1]);

        if (windowSize <= 0) {
            System.err.println("Window Size should be positive");
            System.exit(1);
        }

        // accepting connection
        RDTSocket receiverSocket = new RDTSocket(windowSize, receiverPort);
        System.out.println("Started Receiver...");
        // receiving data and writing data
        while (receiverSocket.accept() != null) {
            System.out.println("connected and waiting for data");
            byte[] data = receiverSocket.recv();
            System.out.println("done received data at sender");
            BufferedWriter writer = new BufferedWriter(new FileWriter(fileWrite));
            writer.write(new String(data));
            System.out.println("wrote data to download.txt");
            writer.close();
        }
    }
}
