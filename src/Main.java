import java.net.*;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

public class Main {
    private static final int CONNECTION_PORT_UDP = 50_000;
    private static final int CONNECTION_PORT_TCP = 50_001;
    private static final String LOCAL_ADDRESS = "127.0.0.3";
    private static final String BROADCAST_ADDRESS = "255.255.255.255";
    private static final ConcurrentHashMap<Socket, String> connectedUsersNames = new ConcurrentHashMap<>();

    private static String CURRENT_USER_NAME = "";

    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in);
             ServerSocket serverSocket = new ServerSocket(CONNECTION_PORT_TCP, 50, InetAddress.getByName(LOCAL_ADDRESS))) {

            System.out.println("Enter your name:");
            CURRENT_USER_NAME = scanner.nextLine();

            sendBroadcastUDP(CURRENT_USER_NAME);
            new Thread(() -> receiveAllTCP(serverSocket)).start();
            new Thread(Main::receiveUDPRequest).start();

            while (true) {
                byte[] input = scanner.nextLine().getBytes();
                for (Socket socket : connectedUsersNames.keySet()) {
                    socket.getOutputStream().write(input);
                }
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
        connectedUsersNames.put(socket, userName);
        socket.getOutputStream().write(CURRENT_USER_NAME.getBytes());
        receiveTCPMessages(socket);
    }

    private static void receiveAllTCP(ServerSocket serverSocket) {
        try {
            while (true) {
                Socket newSocket = serverSocket.accept();
                new Thread(() -> {
                    try {
                        byte[] bytes = new byte[1024];
                        int bytesRead = newSocket.getInputStream().read(bytes);
                        String name = new String(bytes, 0, bytesRead);
                        connectedUsersNames.put(newSocket, name);
                        receiveTCPMessages(newSocket);
                    } catch (Exception e) {
                        System.err.println("Error handling TCP connection: " + e);
                    }
                }).start();
            }
        } catch (Exception e) {
            System.err.println("Error: " + e);
        }
    }

    private static void receiveTCPMessages(Socket socket) {
        try {
            while (!socket.isClosed()) {
                byte[] bytes = new byte[1024];
                int bytesRead = socket.getInputStream().read(bytes);
                String input = new String(bytes, 0, bytesRead);
                System.out.println(socket.getInetAddress().getHostAddress() + " " + connectedUsersNames.get(socket)
                        + " : " + input);
            }
        } catch (Exception e) {
            System.err.println("Error: " + e);
        }
    }
}