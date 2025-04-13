import java.net.*;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

public class Main {
    private static final int CONNECTION_PORT_UDP = 50_000;
    private static final int CONNECTION_PORT_TCP = 50_001;
    private static final String LOCAL_ADDRESS = "127.0.0.52";
    private static final String BROADCAST_ADDRESS = "255.255.255.255";
    private static final ConcurrentHashMap<String, Socket> sockets = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in);
             ServerSocket serverSocket = new ServerSocket(CONNECTION_PORT_TCP, 50, InetAddress.getByName(LOCAL_ADDRESS))) {

            System.out.println("Enter your name:");
            String userName = scanner.nextLine();

            sendBroadcastUDP(userName);
            new Thread(() -> receiveAllTCP(serverSocket)).start();
            new Thread(Main::receiveUDPRequest).start();

            while (true) {
                Thread.sleep(3000);
                System.out.println(sockets);
            }
        } catch (Exception e) {
            System.err.println("Error: " + e);
        }
    }

    private static void sendBroadcastUDP(String userName) throws Exception {
        try (DatagramSocket socket = new DatagramSocket(0, InetAddress.getByName(LOCAL_ADDRESS))) {
            socket.setBroadcast(true);
            InetAddress address = InetAddress.getByName(BROADCAST_ADDRESS);
            DatagramPacket packet = new DatagramPacket(userName.getBytes(), userName.length(), address, CONNECTION_PORT_UDP);
            socket.send(packet);
        }
    }

    private static void receiveUDPRequest() {
        try (DatagramSocket socket = new DatagramSocket(CONNECTION_PORT_UDP, InetAddress.getByName(LOCAL_ADDRESS))) {
            DatagramPacket packet = new DatagramPacket(new byte[1024], 1024);
            while (true) {
                socket.receive(packet);
                String name = new String(packet.getData(), 0, packet.getLength());

                System.out.println(packet.getAddress().getHostAddress() + " " + name + " joined the chat!");
                new Thread(() -> {
                    try {
                        startTCPConnection(packet.getAddress().getHostAddress(), name);
                    } catch (Exception e) {
                        System.err.println("Error creating TCP connection: " + e);
                    }
                }).start();
            }
        } catch (Exception e) {
            System.err.println("Error: " + e);
        }
    }

    private static void startTCPConnection(String address, String userName) throws Exception {
        Socket socket = new Socket(address, CONNECTION_PORT_TCP, InetAddress.getByName(LOCAL_ADDRESS), 0);
        sockets.put(userName, socket);
        while (!socket.isClosed()) {
            Thread.sleep(1000);
        }
    }

    private static void receiveAllTCP(ServerSocket serverSocket) {
        try {
            while (true) {
                Socket newSocket = serverSocket.accept();
                System.out.println("New TCP connection: " + newSocket);
                new Thread(() -> {
                    try {
                        sockets.put(newSocket.getInetAddress().getHostAddress(), newSocket);
                        while (!newSocket.isClosed()) {
                            Thread.sleep(1000);
                        }
                    } catch (Exception e) {
                        System.err.println("Error handling TCP connection: " + e);
                    }
                }).start();
            }
        } catch (Exception e) {
            System.err.println("Error: " + e);
        }
    }
}