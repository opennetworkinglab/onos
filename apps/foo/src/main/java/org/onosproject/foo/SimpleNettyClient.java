/*
 * Copyright 2014 Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onosproject.foo;

import static java.lang.Thread.sleep;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.onlab.metrics.MetricsComponent;
import org.onlab.metrics.MetricsFeature;
import org.onlab.metrics.MetricsManager;
import org.onlab.netty.Endpoint;
import org.onlab.netty.NettyMessagingService;
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
        MetricsFeature feature = new MetricsFeature("latency");
        MetricsComponent component = metrics.registerComponent("NettyMessaging");
        log.info("connecting " + host + ":" + port + " warmup:" + warmup + " iterations:" + iterations);

        for (int i = 0; i < warmup; i++) {
            messaging.sendAsync(endpoint, "simple", "Hello World".getBytes());
            Future<byte[]> responseFuture = messaging
                    .sendAndReceive(endpoint, "echo",
                            "Hello World".getBytes());
            responseFuture.get(100000, TimeUnit.MILLISECONDS);
        }

        log.info("measuring round-trip send & receive");
        Timer sendAndReceiveTimer = metrics.createTimer(component, feature, "SendAndReceive");
        int timeouts = 0;

        for (int i = 0; i < iterations; i++) {
            Future<byte[]> responseFuture;
            Timer.Context context = sendAndReceiveTimer.time();
            try {
                responseFuture = messaging
                        .sendAndReceive(endpoint, "echo",
                                "Hello World".getBytes());
                responseFuture.get(10000, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                timeouts++;
                log.info("timeout:" + timeouts + " at iteration:" + i);
            } finally {
                context.stop();
            }
            // System.out.println("Got back:" + new String(response.get(2, TimeUnit.SECONDS)));
        }

        //sleep(1000);
        log.info("measuring async sender");
        Timer sendAsyncTimer = metrics.createTimer(component, feature, "AsyncSender");

        for (int i = 0; i < iterations; i++) {
        Timer.Context context = sendAsyncTimer.time();
        messaging.sendAsync(endpoint, "simple", "Hello World".getBytes());
        context.stop();
        }
        sleep(10000);
    }

    public static void stop() {
        try {
            messaging.deactivate();
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
