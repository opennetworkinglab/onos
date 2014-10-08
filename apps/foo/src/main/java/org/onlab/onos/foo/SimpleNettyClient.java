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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Timer;

// FIXME: Should be move out to test or app
public final class SimpleNettyClient {

private static Logger log = LoggerFactory.getLogger(SimpleNettyClient.class);

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
        String host = args.length > 0 ? args[0] : "localhost";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 8081;
        int warmup = args.length > 2 ? Integer.parseInt(args[2]) : 1000;
        int iterations = args.length > 3 ? Integer.parseInt(args[3]) : 50 * 100000;
        NettyMessagingService messaging = new TestNettyMessagingService(9081);
        MetricsManager metrics = new MetricsManager();
        Endpoint endpoint = new Endpoint(host, port);
        messaging.activate();
        metrics.activate();
        MetricsFeature feature = new MetricsFeature("latency");
        MetricsComponent component = metrics.registerComponent("NettyMessaging");
        log.info("warmup....");

        for (int i = 0; i < warmup; i++) {
            messaging.sendAsync(endpoint, "simple", "Hello World".getBytes());
            Response response = messaging
                    .sendAndReceive(endpoint, "echo",
                            "Hello World".getBytes());
        }

        log.info("measuring async sender");
        Timer sendAsyncTimer = metrics.createTimer(component, feature, "AsyncSender");

        for (int i = 0; i < iterations; i++) {
            Timer.Context context = sendAsyncTimer.time();
            messaging.sendAsync(endpoint, "simple", "Hello World".getBytes());
            context.stop();
        }

        Timer sendAndReceiveTimer = metrics.createTimer(component, feature, "SendAndReceive");
        for (int i = 0; i < iterations; i++) {
            Timer.Context context = sendAndReceiveTimer.time();
            Response response = messaging
                    .sendAndReceive(endpoint, "echo",
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
