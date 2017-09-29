package at.htl.server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class Broadcaster implements Runnable {
    public static int PORT = 4445;

    private DatagramSocket socket;

    public void run() {
        while(!Thread.currentThread().isInterrupted()) {
            try {
                broadcast(String.valueOf(Server.PORT), InetAddress.getByName("255.255.255.255"));
                Thread.sleep(1000);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                break;
            }
        }

        if (socket != null) {
            socket.close();
        }
    }

    private void broadcast(String broadcastMessage, InetAddress address) throws IOException {
        socket = new DatagramSocket();
        socket.setBroadcast(true);

        byte[] buffer = broadcastMessage.getBytes();

        DatagramPacket packet
                = new DatagramPacket(buffer, buffer.length, address, PORT);
        socket.send(packet);
        socket.close();
    }
}
