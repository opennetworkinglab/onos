package org.onosproject.ipran;
import static org.onlab.util.Tools.namedThreads;
import static org.slf4j.LoggerFactory.getLogger;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.ChannelException;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.onlab.netty.Endpoint;
import org.onosproject.ipran.serializers.KryoSerializer;
import org.slf4j.Logger;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

public class IpranConnector {
    private final Logger log = getLogger(getClass());
    boolean isShutdown = true;
    private ChannelFuture ipranChannel;     // connect to ipran connections
    private ClientBootstrap clientBootstrap;
    private final AtomicLong messageIdGenerator = new AtomicLong(0);
    private static final int DEFAULT_IPRAN_PORT = 2000;
    private final Cache<Long, SettableFuture<byte[]>> responseFutures = CacheBuilder.newBuilder()
            .maximumSize(100000)
            .expireAfterWrite(10, TimeUnit.SECONDS)
            .removalListener(new RemovalListener<Long, SettableFuture<byte[]>>() {
                @Override
                public void onRemoval(RemovalNotification<Long, SettableFuture<byte[]>> entry) {
                    entry.getValue().setException(new TimeoutException("Timedout waiting for reply"));
                }
            })
            .build();
    
    protected static final KryoSerializer SERIALIZER = new KryoSerializer();
    
    
    public static enum messageType {

        /**
         * To hand shake with ipran
         */
        SAY_HELLO,

        /**
         * To tell ipran i'm leader
         */
        LEADER_ELECTED,
        
        /**
         * To tell ipran i'm leader
         */
        TOPO_CHANGED,
        
        /**
         * FlowEntryExtend downStream
         */
        FLOWENTRY_EXTEND,
    }
    
    /**
     * Starts up BGP Session Manager operation.
     *
     * @param listenPortNumber the port number to listen on. By default
     * it should be BgpConstants.BGP_PORT (179)
     */
    public void start() {
        log.debug("BGP Session Manager start.");
        isShutdown = false;

        ChannelFactory channelFactory = new NioClientSocketChannelFactory(
                Executors.newCachedThreadPool(namedThreads("BGP-SM-boss-%d")),
                Executors.newCachedThreadPool(namedThreads("BGP-SM-worker-%d")));
        ChannelPipelineFactory pipelineFactory = new ChannelPipelineFactory() {
                @Override
                public ChannelPipeline getPipeline() throws Exception {
                    // Setup the processing pipeline
                    ChannelPipeline pipeline = Channels.pipeline();
                    return pipeline;
                }
            };
        InetSocketAddress listenAddress =
            new InetSocketAddress(DEFAULT_IPRAN_PORT);

        clientBootstrap = new ClientBootstrap(channelFactory);
        clientBootstrap.setPipelineFactory(pipelineFactory);
        try {
            ipranChannel = clientBootstrap.connect(listenAddress);
            log.info("Starting IPRAN with port {}", DEFAULT_IPRAN_PORT);
        } catch (ChannelException e) {
            log.debug("Exception connect to ipRan port {}: ",
                      listenAddress.getPort(), e);
        }
    }

    /**
     * Stops the BGP Session Manager operation.
     */
    public void stop() {
        isShutdown = true;
        clientBootstrap.releaseExternalResources();
    }
    /**
     * send msg to ipran, make sure the connection has been built
     */   
    public ListenableFuture<byte[]>  sendAndRecvMsg(Endpoint host, String type, byte[] data) {
        SettableFuture<byte[]> futureResponse = SettableFuture.create();
        Long messageId = messageIdGenerator.incrementAndGet();
        responseFutures.put(messageId, futureResponse);
        IpranMessage message = new IpranMessage.Builder()
              .withId(messageId)
              .withSender(host)
              .withType(type)
              .withPayload(data)
              .build();
        if(ipranChannel.getChannel() !=null) {
            ipranChannel.getChannel().write(message);
        } else {
            this.start();
            ipranChannel.getChannel().write(message);
        }
        return futureResponse;
    }
}
