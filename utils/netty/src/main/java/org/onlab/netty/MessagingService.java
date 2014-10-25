package org.onlab.netty;

import java.io.IOException;

import com.google.common.util.concurrent.ListenableFuture;

/**
 * Interface for low level messaging primitives.
 */
public interface MessagingService {
    /**
     * Sends a message asynchronously to the specified communication end point.
     * The message is specified using the type and payload.
     * @param ep end point to send the message to.
     * @param type type of message.
     * @param payload message payload bytes.
     * @throws IOException
     */
    public void sendAsync(Endpoint ep, String type, byte[] payload) throws IOException;

    /**
     * Sends a message synchronously and waits for a response.
     * @param ep end point to send the message to.
     * @param type type of message.
     * @param payload message payload.
     * @return a response future
     * @throws IOException
     */
    public ListenableFuture<byte[]> sendAndReceive(Endpoint ep, String type, byte[] payload) throws IOException;

    /**
     * Registers a new message handler for message type.
     * @param type message type.
     * @param handler message handler
     */
    public void registerHandler(String type, MessageHandler handler);

    /**
     * Unregister current handler, if one exists for message type.
     * @param type message type
     */
    public void unregisterHandler(String type);
}