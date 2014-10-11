package org.onlab.onos.foo;

import static java.lang.Thread.sleep;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
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

/**
 * The Simple netty client test.
 */
// FIXME: Should be move out to test or app
public final class SimpleNettyClient {

private static Logger log = LoggerFactory.getLogger(SimpleNettyClient.class);

    static NettyMessagingService messaging;
    static MetricsManager metrics;

        private SimpleNettyClient() {
    }

    /**
     * The entry point of application.
     *
     * @param args the input arguments
     * @throws IOException the iO exception
     * @throws InterruptedException the interrupted exception
     * @throws ExecutionException the execution exception
     * @throws TimeoutException the timeout exception
     */
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

    /**
     * Start standalone.
     *
     * @param args the args
     * @throws Exception the exception
     */
    public static void startStandalone(String[] args) throws Exception {
        String host = args.length > 0 ? args[0] : "localhost";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 8081;
        int warmup = args.length > 2 ? Integer.parseInt(args[2]) : 1000;
        int iterations = args.length > 3 ? Integer.parseInt(args[3]) : 50 * 100000;
        messaging = new TestNettyMessagingService(9081);
        metrics = new MetricsManager();
        Endpoint endpoint = new Endpoint(host, port);
        messaging.activate();
        metrics.activate();
        MetricsFeature feature = new MetricsFeature("latency");
        MetricsComponent component = metrics.registerComponent("NettyMessaging");
        log.info("connecting " + host + ":" + port + " warmup:" + warmup + " iterations:" + iterations);

        for (int i = 0; i < warmup; i++) {
            messaging.sendAsync(endpoint, "simple", "Hello World".getBytes());
            Response response = messaging
                    .sendAndReceive(endpoint, "echo",
                            "Hello World".getBytes());
            response.get(100000, TimeUnit.MILLISECONDS);
        }

        log.info("measuring round-trip send & receive");
        Timer sendAndReceiveTimer = metrics.createTimer(component, feature, "SendAndReceive");
        int timeouts = 0;

        for (int i = 0; i < iterations; i++) {
            Response response;
            Timer.Context context = sendAndReceiveTimer.time();
            try {
                response = messaging
                        .sendAndReceive(endpoint, "echo",
                                "Hello World".getBytes());
                response.get(10000, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                timeouts++;
                log.info("timeout:" + timeouts + " at iteration:" + i);
            } finally {
                context.stop();
            }
            // System.out.println("Got back:" + new String(response.get(2, TimeUnit.SECONDS)));
        }

        sleep(1000);
        log.info("measuring async sender");
        Timer sendAsyncTimer = metrics.createTimer(component, feature, "AsyncSender");

        for (int i = 0; i < iterations; i++) {
        Timer.Context context = sendAsyncTimer.time();
        messaging.sendAsync(endpoint, "simple", "Hello World".getBytes());
        context.stop();
        }
        sleep(1000);
    }

    public static void stop() {
        try {
            messaging.deactivate();
            metrics.deactivate();
        } catch (Exception e) {
            log.info("Unable to stop client %s", e);
        }
    }

    /**
     * The type Test netty messaging service.
     */
    public static class TestNettyMessagingService extends NettyMessagingService {
        /**
         * Instantiates a new Test netty messaging service.
         *
         * @param port the port
         * @throws Exception the exception
         */
        public TestNettyMessagingService(int port) throws Exception {
            super(port);
        }
    }
}
