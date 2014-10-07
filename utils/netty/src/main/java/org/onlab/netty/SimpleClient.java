package org.onlab.netty;

import java.util.concurrent.TimeUnit;

import org.onlab.metrics.MetricsComponent;
import org.onlab.metrics.MetricsFeature;
import org.onlab.metrics.MetricsManager;

import com.codahale.metrics.Timer;

// FIXME: Should be move out to test or app
public final class SimpleClient {
    private SimpleClient() {
    }

    public static void main(String... args) throws Exception {
        NettyMessagingService messaging = new TestNettyMessagingService(9081);
        MetricsManager metrics = new MetricsManager();
        messaging.activate();
        metrics.activate();
        MetricsFeature feature = new MetricsFeature("timers");
        MetricsComponent component = metrics.registerComponent("NettyMessaging");
        Timer sendAsyncTimer = metrics.createTimer(component, feature, "AsyncSender");
        final int warmup = 100;
        for (int i = 0; i < warmup; i++) {
            Timer.Context context = sendAsyncTimer.time();
            messaging.sendAsync(new Endpoint("localhost", 8080), "simple", "Hello World");
            context.stop();
        }
        metrics.registerMetric(component, feature, "AsyncTimer", sendAsyncTimer);

        Timer sendAndReceiveTimer = metrics.createTimer(component, feature, "SendAndReceive");
        final int iterations = 1000000;
        for (int i = 0; i < iterations; i++) {
            Timer.Context context = sendAndReceiveTimer.time();
            Response<String> response = messaging
                    .sendAndReceive(new Endpoint("localhost", 8080), "echo",
                                    "Hello World");
            System.out.println("Got back:" + response.get(2, TimeUnit.SECONDS));
            context.stop();
        }
        metrics.registerMetric(component, feature, "AsyncTimer", sendAndReceiveTimer);
    }

    public static class TestNettyMessagingService extends NettyMessagingService {
        public TestNettyMessagingService(int port) throws Exception {
            super(port);
            PayloadSerializer payloadSerializer = new KryoSerializer();
            this.payloadSerializer = payloadSerializer;
        }
    }
}
