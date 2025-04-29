package com.server;

import java.io.*;
import java.net.Socket;

public class ClientConnection {
    private final Socket socket;
    private final BufferedWriter out;
    private final BufferedReader in;


    public ClientConnection(Socket socket) throws IOException {
        this.socket = socket;
        this.out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    public Socket getSocket() {
        return socket;
    }

    public BufferedWriter getOut() {
        return out;
    }

    public BufferedReader getIn() {
        return in;
    }
}
