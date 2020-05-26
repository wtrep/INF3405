package ca.polymtl.inf3405.server;

import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;

public class ConnectedUser extends User {
    private final String token;
    private volatile Instant lastConnection;
    private final InetAddress userAddress;
    private final int userPort;

    public ConnectedUser(User user, InetAddress addr, int port) {
        super(user);
        token = generateToken();
        lastConnection = Instant.now();
        userAddress = addr;
        userPort = port;
    }

    public String getToken() {
        return token;
    }

    public Instant getLastConnection() {
        return lastConnection;
    }

    public InetAddress getUserAddress() {
        return userAddress;
    }

    public int getUserPort() {
        return userPort;
    }

    public synchronized void updateLastConnection() {
        lastConnection = Instant.now();
    }

    private String generateToken() {
        SecureRandom s = new SecureRandom();
        byte[] token = new byte[256];
        s.nextBytes(token);
        return new String(token, StandardCharsets.UTF_8);
    }
}
