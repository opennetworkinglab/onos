package org.onlab.onos.foo;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.onlab.metrics.MetricsComponent;
import org.onlab.metrics.MetricsFeature;
import org.onlab.metrics.MetricsManager;
import org.onlab.netty.Endpoint;
import org.onlab.netty.NettyMessagingService;
import org.onlab.netty.Response;

import com.codahale.metrics.Timer;

// FIXME: Should be move out to test or app
public final class SimpleNettyClient {
    private SimpleNettyClient() {
    }

    public static void main(String[] args)
            throws IOException, InterruptedException, ExecutionException,
            TimeoutException {
        try {
            startStandalone(args);
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.exit(0);
    }
    public static void startStandalone(String... args) throws Exception {
        NettyMessagingService messaging = new TestNettyMessagingService(9081);
        MetricsManager metrics = new MetricsManager();
        messaging.activate();
        metrics.activate();
        MetricsFeature feature = new MetricsFeature("latency");
        MetricsComponent component = metrics.registerComponent("NettyMessaging");

        final int warmup = 10000;
        for (int i = 0; i < warmup; i++) {
            messaging.sendAsync(new Endpoint("localhost", 8081), "simple", "Hello World".getBytes());
            Response response = messaging
                    .sendAndReceive(new Endpoint("localhost", 8081), "echo",
                            "Hello World".getBytes());
        }

        Timer sendAsyncTimer = metrics.createTimer(component, feature, "AsyncSender");
        Timer sendAndReceiveTimer = metrics.createTimer(component, feature, "SendAndReceive");

        final int iterations = 10000000;
        for (int i = 0; i < iterations; i++) {
            Timer.Context context = sendAsyncTimer.time();
            messaging.sendAsync(new Endpoint("localhost", 8081), "simple", "Hello World".getBytes());
            context.stop();
        }

        for (int i = 0; i < iterations; i++) {
            Timer.Context context = sendAndReceiveTimer.time();
            Response response = messaging
                    .sendAndReceive(new Endpoint("localhost", 8081), "echo",
                                    "Hello World".getBytes());
            // System.out.println("Got back:" + new String(response.get(2, TimeUnit.SECONDS)));
            context.stop();
        }
    }

    public static class TestNettyMessagingService extends NettyMessagingService {
        public TestNettyMessagingService(int port) throws Exception {
            super(port);
        }
    }
}
