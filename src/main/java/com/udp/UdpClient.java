package com.udp;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

public class UdpClient {

    private final String serverAddress;
    private final int serverPort;

    public UdpClient(String serverAddress, int serverPort) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
    }

    public void sendMessage(String message) {
        try (DatagramSocket socket = new DatagramSocket()) {
            byte[] sendData = message.getBytes(StandardCharsets.UTF_8);

            InetAddress address = InetAddress.getByName(serverAddress);
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, address, serverPort);

            socket.send(sendPacket);
            System.out.println("Sent message: " + message);

            byte[] receiveBuffer = new byte[1024];
            DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
            socket.receive(receivePacket);

            String response = new String(receivePacket.getData(), 0, receivePacket.getLength(), StandardCharsets.UTF_8);
            System.out.println("Received response: " + response);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        UdpClient client = new UdpClient("localhost", 8080);
        String orderMessage = "BUY;AAPL;10;150";
        client.sendMessage(orderMessage);
    }
}

