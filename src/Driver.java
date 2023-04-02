import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Random;
import static util.Utility.*;

public class Driver {
    public static void main(String[] args) throws IOException {

//        ServerSocket ss = new ServerSocket();
//        ss.accept();
//        PacketHeader test = new PacketHeader();
//        System.out.println(PacketHeader.compute_checksum(s.getBytes()));
//        System.out.println(new String(s.getBytes(), 0, 4));
//        System.out.println(PacketHeader.verify_packet(s.getBytes(), 1377512468));
//        System.out.println((new Random()).nextDouble());

//        var v = (new BigInteger("3342")).toByteArray();
//
//        System.out.println(Arrays.toString(v));
//        Socket so;
//        ServerSocket ss;
//        DatagramSocket datagramSocket;
//        System.out.println((char) 0);

//        String data = Files.readString(Path.of("alice.txt"));

//        System.out.println(data);
//        BufferedWriter out = new BufferedWriter(new FileWriter("download.txt", true));
//        out.append(data);
//        out.close();

//        byte[] data = Files.readAllBytes(Paths.get("alice.txt"));
//        System.out.println(data.length);

//        String line = "123456789";
//        byte[] data = line.getBytes();
////        Random r = new Random();
////        data[r.nextInt(0, data.length)] = (byte) (r.nextInt(128));
//        data[1] = -1;
//        System.out.println(Arrays.toString(data));
//        System.out.println(new String(data));

//        int num = 12345;
//        byte[] encode = ByteBuffer.allocate(4).putInt(num).array();
//        int decode = ByteBuffer.wrap(encode).getInt();
//        System.out.println(decode);

//        System.out.println("thread started");
//
//        Thread t = new Thread(() -> {
//            try {
//                System.out.println("started sleep");
//                Thread.sleep(1000);
//                System.out.println("end sleep");
//            } catch (InterruptedException e) {
//                System.out.println("interrupted");
//            }
//            System.out.println();
//        });
//
//        t.start();
//        t.interrupt();
//
//        System.out.println("done");
//        Thread.sleep(10000);

//        System.out.println(compute_checksum(new byte[100]));
        PacketHeader h = new PacketHeader(3, 5, 5, 100);

        System.out.println(Arrays.toString(encodeHeader(h)));
        System.out.println(Arrays.toString(Arrays.copyOfRange(encodeHeader(h), 3, 8)));
    }
}
