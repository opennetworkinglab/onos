package org.onlab.onos.store.cluster.impl;

import org.onlab.nio.IOLoop;
import org.onlab.nio.MessageStream;
import org.onlab.onos.cluster.DefaultControllerNode;
import org.onlab.onos.store.cluster.messaging.ClusterMessage;
import org.onlab.onos.store.cluster.messaging.ClusterMessageStream;
import org.onlab.onos.store.cluster.messaging.SerializationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ByteChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.Objects;

import static org.onlab.packet.IpPrefix.valueOf;

/**
 * Performs the IO operations related to a cluster-wide communications.
 */
public class ClusterIOWorker extends
        IOLoop<ClusterMessage, ClusterMessageStream> {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final long SELECT_TIMEOUT = 50;

    private final ConnectionManager connectionManager;
    private final CommunicationsDelegate commsDelegate;
    private final SerializationService serializationService;
    private final ClusterMessage helloMessage;

    /**
     * Creates a new cluster IO worker.
     *
     * @param connectionManager    parent connection manager
     * @param commsDelegate        communications delegate for dispatching
     * @param serializationService serialization service for encode/decode
     * @param helloMessage         hello message for greeting peers
     * @throws IOException if errors occur during IO loop ignition
     */
    ClusterIOWorker(ConnectionManager connectionManager,
                    CommunicationsDelegate commsDelegate,
                    SerializationService serializationService,
                    ClusterMessage helloMessage) throws IOException {
        super(SELECT_TIMEOUT);
        this.connectionManager = connectionManager;
        this.commsDelegate = commsDelegate;
        this.serializationService = serializationService;
        this.helloMessage = helloMessage;
    }

    @Override
    protected ClusterMessageStream createStream(ByteChannel byteChannel) {
        return new ClusterMessageStream(serializationService, this, byteChannel);
    }

    @Override
    protected void processMessages(List<ClusterMessage> messages, MessageStream<ClusterMessage> stream) {
        for (ClusterMessage message : messages) {
            commsDelegate.dispatch(message);
        }
    }

    @Override
    public ClusterMessageStream acceptStream(SocketChannel channel) {
        ClusterMessageStream stream = super.acceptStream(channel);
        try {
            InetSocketAddress sa = (InetSocketAddress) channel.getRemoteAddress();
            log.info("Accepted connection from node {}", valueOf(sa.getAddress().getAddress()));
            stream.write(helloMessage);

        } catch (IOException e) {
            log.warn("Unable to accept connection from an unknown end-point", e);
        }
        return stream;
    }

    @Override
    protected void connect(SelectionKey key) throws IOException {
        try {
            super.connect(key);
            ClusterMessageStream stream = (ClusterMessageStream) key.attachment();
            stream.write(helloMessage);

        } catch (IOException e) {
            if (!Objects.equals(e.getMessage(), "Connection refused")) {
                throw e;
            }
        }
    }

    @Override
    protected void removeStream(MessageStream<ClusterMessage> stream) {
        DefaultControllerNode node = ((ClusterMessageStream) stream).node();
        if (node != null) {
            log.info("Closed connection to node {}", node.id());
            connectionManager.removeNodeStream(node);
        }
        super.removeStream(stream);
    }

}
