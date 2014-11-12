package org.onlab.onos.store.service.impl;

import static com.google.common.base.Verify.verifyNotNull;
import static org.slf4j.LoggerFactory.getLogger;
import static org.onlab.onos.store.service.impl.ClusterMessagingProtocol.SERIALIZER;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import net.kuujo.copycat.cluster.TcpMember;
import net.kuujo.copycat.protocol.PingRequest;
import net.kuujo.copycat.protocol.PingResponse;
import net.kuujo.copycat.protocol.PollRequest;
import net.kuujo.copycat.protocol.PollResponse;
import net.kuujo.copycat.protocol.SubmitRequest;
import net.kuujo.copycat.protocol.SubmitResponse;
import net.kuujo.copycat.protocol.SyncRequest;
import net.kuujo.copycat.protocol.SyncResponse;
import net.kuujo.copycat.spi.protocol.ProtocolClient;

import org.onlab.onos.cluster.ClusterEvent;
import org.onlab.onos.cluster.ClusterEventListener;
import org.onlab.onos.cluster.ClusterService;
import org.onlab.onos.cluster.ControllerNode;
import org.onlab.onos.store.cluster.messaging.ClusterCommunicationService;
import org.onlab.onos.store.cluster.messaging.ClusterMessage;
import org.onlab.onos.store.cluster.messaging.MessageSubject;
import org.slf4j.Logger;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

/**
 * ONOS Cluster messaging based Copycat protocol client.
 */
public class ClusterMessagingProtocolClient implements ProtocolClient {

    private final Logger log = getLogger(getClass());

    private static final ThreadFactory THREAD_FACTORY =
            new ThreadFactoryBuilder().setNameFormat("copycat-netty-messaging-%d").build();

    public static final long RETRY_INTERVAL_MILLIS = 2000;

    private final ClusterService clusterService;
    private final ClusterCommunicationService clusterCommunicator;
    private final ControllerNode localNode;
    private final TcpMember remoteMember;
    private ControllerNode remoteNode;

    // FIXME: Thread pool sizing.
    private static final ScheduledExecutorService THREAD_POOL =
            new ScheduledThreadPoolExecutor(10, THREAD_FACTORY);

    private volatile CompletableFuture<Void> appeared;

    private volatile InternalClusterEventListener listener;

    public ClusterMessagingProtocolClient(
            ClusterService clusterService,
            ClusterCommunicationService clusterCommunicator,
            ControllerNode localNode,
            TcpMember remoteMember) {

        this.clusterService = clusterService;
        this.clusterCommunicator = clusterCommunicator;
        this.localNode = localNode;
        this.remoteMember = remoteMember;
    }

    @Override
    public CompletableFuture<PingResponse> ping(PingRequest request) {
        return requestReply(request);
    }

    @Override
    public CompletableFuture<SyncResponse> sync(SyncRequest request) {
        return requestReply(request);
    }

    @Override
    public CompletableFuture<PollResponse> poll(PollRequest request) {
        return requestReply(request);
    }

    @Override
    public CompletableFuture<SubmitResponse> submit(SubmitRequest request) {
        return requestReply(request);
    }

    @Override
    public synchronized CompletableFuture<Void> connect() {
        if (remoteNode != null) {
            // done
            return CompletableFuture.completedFuture(null);
        }

        remoteNode = getControllerNode(remoteMember);

        if (remoteNode != null) {
            // done
            return CompletableFuture.completedFuture(null);
        }

        if (appeared != null) {
            // already waiting for member to appear
            return appeared;
        }

        appeared = new CompletableFuture<>();
        listener = new InternalClusterEventListener();
        clusterService.addListener(listener);

        // wait for specified controller node to come up
        return null;
    }

    @Override
    public synchronized CompletableFuture<Void> close() {
        if (listener != null) {
            clusterService.removeListener(listener);
            listener = null;
        }
        if (appeared != null) {
            appeared.cancel(true);
            appeared = null;
        }
        return CompletableFuture.completedFuture(null);
    }

    private synchronized void checkIfMemberAppeared() {
        final ControllerNode controllerNode = getControllerNode(remoteMember);
        if (controllerNode == null) {
            // still not there: no-op
            return;
        }

        // found
        remoteNode = controllerNode;
        if (appeared != null) {
            appeared.complete(null);
        }

        if (listener != null) {
            clusterService.removeListener(listener);
            listener = null;
        }
    }

    private <I> MessageSubject messageType(I input) {
        Class<?> clazz = input.getClass();
        if (clazz.equals(PollRequest.class)) {
            return ClusterMessagingProtocol.COPYCAT_POLL;
        } else if (clazz.equals(SyncRequest.class)) {
            return ClusterMessagingProtocol.COPYCAT_SYNC;
        } else if (clazz.equals(SubmitRequest.class)) {
            return ClusterMessagingProtocol.COPYCAT_SUBMIT;
        } else if (clazz.equals(PingRequest.class)) {
            return ClusterMessagingProtocol.COPYCAT_PING;
        } else {
            throw new IllegalArgumentException("Unknown class " + clazz.getName());
        }

    }

    private <I, O> CompletableFuture<O> requestReply(I request) {
        CompletableFuture<O> future = new CompletableFuture<>();
        THREAD_POOL.schedule(new RPCTask<I, O>(request, future), 0, TimeUnit.MILLISECONDS);
        return future;
    }

    private ControllerNode getControllerNode(TcpMember remoteMember) {
        final String host = remoteMember.host();
        final int port = remoteMember.port();
        for (ControllerNode node : clusterService.getNodes()) {
            if (node.ip().toString().equals(host) && node.tcpPort() == port) {
                return node;
            }
        }
        return null;
    }

    private final class InternalClusterEventListener
            implements ClusterEventListener {

        public InternalClusterEventListener() {
        }

        @Override
        public void event(ClusterEvent event) {
            checkIfMemberAppeared();
        }

    }

    private class RPCTask<I, O> implements Runnable {

        private final I request;
        private final ClusterMessage message;
        private final CompletableFuture<O> future;

        public RPCTask(I request, CompletableFuture<O> future) {
            this.request = request;
            this.message =
                    new ClusterMessage(
                            localNode.id(),
                            messageType(request),
                            verifyNotNull(SERIALIZER.encode(request)));
            this.future = future;
        }

        @Override
        public void run() {
            try {
                byte[] response = clusterCommunicator
                    .sendAndReceive(message, remoteNode.id())
                    .get(RETRY_INTERVAL_MILLIS, TimeUnit.MILLISECONDS);
                future.complete(verifyNotNull(SERIALIZER.decode(response)));

            } catch (IOException | InterruptedException | ExecutionException | TimeoutException e) {
                log.warn("RPCTask for {} failed.", request, e);
                future.completeExceptionally(e);
            } catch (Exception e) {
                log.warn("RPCTask for {} terribly failed.", request, e);
                future.completeExceptionally(e);
            }
        }
    }
}
