package org.onlab.onos.store.cluster.impl;

import org.onlab.nio.IOLoop;
import org.onlab.nio.MessageStream;
import org.onlab.onos.cluster.DefaultControllerNode;
import org.onlab.onos.cluster.NodeId;
import org.onlab.onos.store.cluster.messaging.ClusterMessage;
import org.onlab.onos.store.cluster.messaging.ClusterMessageStream;
import org.onlab.onos.store.cluster.messaging.HelloMessage;
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

    private final ClusterCommunicationManager manager;
    private final SerializationService serializationService;
    private final ClusterMessage helloMessage;

    /**
     * Creates a new cluster IO worker.
     *
     * @param manager              parent comms manager
     * @param serializationService serialization service for encode/decode
     * @param helloMessage         hello message for greeting peers
     * @throws IOException if errors occur during IO loop ignition
     */
    ClusterIOWorker(ClusterCommunicationManager manager,
                    SerializationService serializationService,
                    ClusterMessage helloMessage) throws IOException {
        super(SELECT_TIMEOUT);
        this.manager = manager;
        this.serializationService = serializationService;
        this.helloMessage = helloMessage;
    }

    @Override
    protected ClusterMessageStream createStream(ByteChannel byteChannel) {
        return new ClusterMessageStream(serializationService, this, byteChannel);
    }

    @Override
    protected void processMessages(List<ClusterMessage> messages, MessageStream<ClusterMessage> stream) {
        NodeId nodeId = getNodeId(messages, (ClusterMessageStream) stream);
        for (ClusterMessage message : messages) {
            manager.dispatch(message, nodeId);
        }
    }

    // Retrieves the node from the stream. If one is not bound, it attempts
    // to bind it using the knowledge that the first message must be a hello.
    private NodeId getNodeId(List<ClusterMessage> messages, ClusterMessageStream stream) {
        DefaultControllerNode node = stream.node();
        if (node == null && !messages.isEmpty()) {
            ClusterMessage firstMessage = messages.get(0);
            if (firstMessage instanceof HelloMessage) {
                HelloMessage hello = (HelloMessage) firstMessage;
                node = manager.addNodeStream(hello.nodeId(), hello.ipAddress(),
                                             hello.tcpPort(), stream);
            }
        }
        return node != null ? node.id() : null;
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
            manager.removeNodeStream(node);
        }
        super.removeStream(stream);
    }

}
