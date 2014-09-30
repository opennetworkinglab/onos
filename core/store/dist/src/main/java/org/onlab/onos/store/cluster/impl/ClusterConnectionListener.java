package org.onlab.onos.store.cluster.impl;

import org.onlab.nio.AcceptorLoop;
import org.onlab.packet.IpPrefix;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import static java.net.InetAddress.getByAddress;

/**
 * Listens to inbound connection requests and accepts them.
 */
public class ClusterConnectionListener extends AcceptorLoop {

    private static final long SELECT_TIMEOUT = 50;
    private static final int COMM_BUFFER_SIZE = 32 * 1024;

    private static final boolean SO_NO_DELAY = false;
    private static final int SO_SEND_BUFFER_SIZE = COMM_BUFFER_SIZE;
    private static final int SO_RCV_BUFFER_SIZE = COMM_BUFFER_SIZE;

    private final ClusterCommunicationManager manager;

    ClusterConnectionListener(ClusterCommunicationManager manager,
                              IpPrefix ip, int tcpPort) throws IOException {
        super(SELECT_TIMEOUT, new InetSocketAddress(getByAddress(ip.toOctets()), tcpPort));
        this.manager = manager;
    }

    @Override
    protected void acceptConnection(ServerSocketChannel channel) throws IOException {
        SocketChannel sc = channel.accept();
        sc.configureBlocking(false);

        Socket so = sc.socket();
        so.setTcpNoDelay(SO_NO_DELAY);
        so.setReceiveBufferSize(SO_RCV_BUFFER_SIZE);
        so.setSendBufferSize(SO_SEND_BUFFER_SIZE);

        manager.findWorker().acceptStream(sc);
    }

}
