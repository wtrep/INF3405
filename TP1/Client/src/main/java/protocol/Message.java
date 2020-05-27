package protocol;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import exceptions.MessageSizeException;

import java.time.Instant;

final public class Message {
    private final String senderName;
    private final String senderIp;
    private final Integer senderPort;
    private final String time;
    private final String message;

    private static final int MAXIMUM_SIZE = 200;

    public Message(String senderName, String senderIp, Integer senderPort, Instant time, String message)
            throws MessageSizeException {
        if (message.length() > MAXIMUM_SIZE) {
            throw new MessageSizeException("La taille maximale du message est de 200 caractères.");
        }

        this.senderName = senderName;
        this.senderIp = senderIp;
        this.senderPort = senderPort;
        this.time = time.toString();
        this.message = message;
    }

    public String getSenderName() {
        return senderName;
    }

    public String getSenderIp() {
        return senderIp;
    }

    public Integer getSenderPort() {
        return senderPort;
    }

    public String getTime() {
        return time;
    }

    public String getMessage() {
        return message;
    }

    public String encodeMessage() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    public static Message decodeMessage(String string) {
        Gson gson = new Gson();
        try {
            return gson.fromJson(string, Message.class);
        } catch (JsonSyntaxException e) {
            return null;
        }
    }

    public String toConsole() {
        return "["+senderName+"]: " + message;
    }

}
