/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.messagingperf;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.MoreExecutors;
import org.onlab.util.BoundedThreadPool;
import org.onlab.util.KryoNamespace;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.CoreService;
import org.onosproject.store.cluster.messaging.ClusterCommunicationService;
import org.onosproject.store.cluster.messaging.MessageSubject;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.Serializer;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;

import java.util.Dictionary;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.IntStream;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.onlab.util.Tools.get;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.messagingperf.OsgiPropertyConstants.RECEIVER_THREAD_POOL_SIZE;
import static org.onosproject.messagingperf.OsgiPropertyConstants.RECEIVER_THREAD_POOL_SIZE_DEFAULT;
import static org.onosproject.messagingperf.OsgiPropertyConstants.RECEIVE_ON_IO_LOOP_THREAD;
import static org.onosproject.messagingperf.OsgiPropertyConstants.RECEIVE_ON_IO_LOOP_THREAD_DEFAULT;
import static org.onosproject.messagingperf.OsgiPropertyConstants.SENDER_THREAD_POOL_SIZE;
import static org.onosproject.messagingperf.OsgiPropertyConstants.SENDER_THREAD_POOL_SIZE_DEFAULT;
import static org.onosproject.messagingperf.OsgiPropertyConstants.SERIALIZATION_ON;
import static org.onosproject.messagingperf.OsgiPropertyConstants.SERIALIZATION_ON_DEFAULT;
import static org.osgi.service.component.annotations.ReferenceCardinality.MANDATORY;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Application for measuring cluster messaging performance.
 */
@Component(
    immediate = true,
    service = MessagingPerfApp.class,
    property = {
        SENDER_THREAD_POOL_SIZE + ":Integer=" + SENDER_THREAD_POOL_SIZE_DEFAULT,
        RECEIVER_THREAD_POOL_SIZE + ":Integer=" + RECEIVER_THREAD_POOL_SIZE_DEFAULT,
        SERIALIZATION_ON + ":Boolean=" + SERIALIZATION_ON_DEFAULT,
        RECEIVE_ON_IO_LOOP_THREAD + ":Boolean=" + RECEIVE_ON_IO_LOOP_THREAD_DEFAULT
    }
)
public class MessagingPerfApp {
    private final Logger log = getLogger(getClass());

    @Reference(cardinality = MANDATORY)
    protected ClusterService clusterService;

    @Reference(cardinality = MANDATORY)
    protected ClusterCommunicationService communicationService;

    @Reference(cardinality = MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = MANDATORY)
    protected ComponentConfigService configService;

    private static final MessageSubject TEST_UNICAST_MESSAGE_TOPIC =
            new MessageSubject("net-perf-unicast-message");

    private static final MessageSubject TEST_REQUEST_REPLY_TOPIC =
            new MessageSubject("net-perf-rr-message");

    /** Number of sender threads. */
    private int totalSenderThreads = SENDER_THREAD_POOL_SIZE_DEFAULT;

    /** Number of receiver threads. */
    private int totalReceiverThreads = RECEIVER_THREAD_POOL_SIZE_DEFAULT;

    /** Turn serialization on/off. */
    private boolean serializationOn = SERIALIZATION_ON_DEFAULT;

    /** Set this to true to handle message on IO thread. */
    private boolean receiveOnIOLoopThread = RECEIVE_ON_IO_LOOP_THREAD_DEFAULT;

    private int reportIntervalSeconds = 1;

    private Executor messageReceivingExecutor;

    private ExecutorService messageSendingExecutor =
            BoundedThreadPool.newFixedThreadPool(totalSenderThreads,
                    groupedThreads("onos/messaging-perf-test", "sender-%d"));

    private final ScheduledExecutorService reporter =
            Executors.newSingleThreadScheduledExecutor(
                    groupedThreads("onos/net-perf-test", "reporter"));

    private AtomicInteger received = new AtomicInteger(0);
    private AtomicInteger sent = new AtomicInteger(0);
    private AtomicInteger attempted = new AtomicInteger(0);
    private AtomicInteger completed = new AtomicInteger(0);

    private static final Serializer SERIALIZER = Serializer
            .using(
                KryoNamespace.newBuilder()
                    .register(KryoNamespaces.BASIC)
                    .register(KryoNamespaces.MISC)
                    .register(Data.class)
                    .build("MessagingPerfApp"));

    private final Data data = new Data().withStringField("test")
                                .withListField(Lists.newArrayList("1", "2", "3"))
                                .withSetField(Sets.newHashSet("1", "2", "3"));
    private final byte[] dataBytes = SERIALIZER.encode(new Data().withStringField("test")
            .withListField(Lists.newArrayList("1", "2", "3"))
            .withSetField(Sets.newHashSet("1", "2", "3")));

    private Function<Data, byte[]> encoder;
    private Function<byte[], Data> decoder;

    @Activate
    public void activate(ComponentContext context) {
        configService.registerProperties(getClass());
        setupCodecs();
        messageReceivingExecutor = receiveOnIOLoopThread
                ? MoreExecutors.directExecutor()
                : Executors.newFixedThreadPool(
                        totalReceiverThreads,
                        groupedThreads("onos/net-perf-test", "receiver-%d"));
        registerMessageHandlers();
        startTest();
        reporter.scheduleWithFixedDelay(this::reportPerformance,
                reportIntervalSeconds,
                reportIntervalSeconds,
                TimeUnit.SECONDS);
        logConfig("Started");
    }

    @Deactivate
    public void deactivate(ComponentContext context) {
        configService.unregisterProperties(getClass(), false);
        stopTest();
        reporter.shutdown();
        unregisterMessageHandlers();
        log.info("Stopped.");
    }

    @Modified
    public void modified(ComponentContext context) {
        if (context == null) {
            totalSenderThreads = SENDER_THREAD_POOL_SIZE_DEFAULT;
            totalReceiverThreads = RECEIVER_THREAD_POOL_SIZE_DEFAULT;
            serializationOn = true;
            receiveOnIOLoopThread = false;
            return;
        }

        Dictionary properties = context.getProperties();

        int newTotalSenderThreads = totalSenderThreads;
        int newTotalReceiverThreads = totalReceiverThreads;
        boolean newSerializationOn = serializationOn;
        boolean newReceiveOnIOLoopThread = receiveOnIOLoopThread;
        try {
            String s = get(properties, SENDER_THREAD_POOL_SIZE);
            newTotalSenderThreads = isNullOrEmpty(s)
                    ? totalSenderThreads : Integer.parseInt(s.trim());

            s = get(properties, RECEIVER_THREAD_POOL_SIZE);
            newTotalReceiverThreads = isNullOrEmpty(s)
                    ? totalReceiverThreads : Integer.parseInt(s.trim());

            s = get(properties, SERIALIZATION_ON);
            newSerializationOn = isNullOrEmpty(s)
                    ? serializationOn : Boolean.parseBoolean(s.trim());

            s = get(properties, RECEIVE_ON_IO_LOOP_THREAD);
            newReceiveOnIOLoopThread = isNullOrEmpty(s)
                    ? receiveOnIOLoopThread : Boolean.parseBoolean(s.trim());

        } catch (NumberFormatException | ClassCastException e) {
            return;
        }

        boolean modified = newTotalSenderThreads != totalSenderThreads ||
                newTotalReceiverThreads != totalReceiverThreads ||
                newSerializationOn != serializationOn ||
                newReceiveOnIOLoopThread != receiveOnIOLoopThread;

        // If nothing has changed, simply return.
        if (!modified) {
            return;
        }

        totalSenderThreads = newTotalSenderThreads;
        totalReceiverThreads = newTotalReceiverThreads;
        serializationOn = newSerializationOn;
        if (!receiveOnIOLoopThread && newReceiveOnIOLoopThread != receiveOnIOLoopThread) {
            ((ExecutorService) messageReceivingExecutor).shutdown();
        }
        receiveOnIOLoopThread = newReceiveOnIOLoopThread;

        // restart test.

        stopTest();
        unregisterMessageHandlers();
        setupCodecs();
        messageSendingExecutor =
                BoundedThreadPool.newFixedThreadPool(
                        totalSenderThreads,
                        groupedThreads("onos/net-perf-test", "sender-%d"));
        messageReceivingExecutor = receiveOnIOLoopThread
                    ? MoreExecutors.directExecutor()
                    : Executors.newFixedThreadPool(
                            totalReceiverThreads,
                            groupedThreads("onos/net-perf-test", "receiver-%d"));

        registerMessageHandlers();
        startTest();

        logConfig("Reconfigured");
    }


    private void logConfig(String prefix) {
        log.info("{} with senderThreadPoolSize = {}; receivingThreadPoolSize = {}"
                + " serializationOn = {}, receiveOnIOLoopThread = {}",
                 prefix,
                 totalSenderThreads,
                 totalReceiverThreads,
                 serializationOn,
                 receiveOnIOLoopThread);
    }

    private void setupCodecs() {
        encoder = serializationOn ? SERIALIZER::encode : d -> dataBytes;
        decoder = serializationOn ? SERIALIZER::decode : b -> data;
    }

    private void registerMessageHandlers() {
        communicationService.<Data>addSubscriber(
                TEST_UNICAST_MESSAGE_TOPIC,
                decoder,
                d -> {
                    received.incrementAndGet();
                },
                messageReceivingExecutor);

        communicationService.<Data, Data>addSubscriber(
                TEST_REQUEST_REPLY_TOPIC,
                decoder,
                Function.identity(),
                encoder,
                messageReceivingExecutor);
    }

    private void unregisterMessageHandlers() {
        communicationService.removeSubscriber(TEST_UNICAST_MESSAGE_TOPIC);
        communicationService.removeSubscriber(TEST_REQUEST_REPLY_TOPIC);
    }

    private void startTest() {
        IntStream.range(0, totalSenderThreads).forEach(i -> requestReply());
    }

    private void stopTest() {
        messageSendingExecutor.shutdown();
    }

    private void requestReply() {
        try {
            attempted.incrementAndGet();
            CompletableFuture<Data> response =
                    communicationService.<Data, Data>sendAndReceive(
                            data,
                            TEST_REQUEST_REPLY_TOPIC,
                            encoder,
                            decoder,
                            randomPeer());
            response.whenComplete((result, error) -> {
                if (Objects.equals(data, result)) {
                    completed.incrementAndGet();
                }
                messageSendingExecutor.submit(this::requestReply);
            });
        } catch (Exception e) {
            log.info("requestReply()", e);
        }
    }

    private void unicast() {
        try {
            sent.incrementAndGet();
            communicationService.<Data>unicast(
                    data,
                    TEST_UNICAST_MESSAGE_TOPIC,
                    encoder,
                    randomPeer());
        } catch (Exception e) {
            log.info("unicast()", e);
        }
        messageSendingExecutor.submit(this::unicast);
    }

    private void broadcast() {
        try {
            sent.incrementAndGet();
            communicationService.<Data>broadcast(
                    data,
                    TEST_UNICAST_MESSAGE_TOPIC,
                    encoder);
        } catch (Exception e) {
            log.info("broadcast()", e);
        }
        messageSendingExecutor.submit(this::broadcast);
    }

    private NodeId randomPeer() {
        return clusterService.getNodes()
                    .stream()
                    .filter(node -> clusterService.getLocalNode().equals(node))
                    .findAny()
                    .get()
                    .id();
    }

    private void reportPerformance() {
        log.info("Attempted: {} Completed: {}", attempted.getAndSet(0), completed.getAndSet(0));
    }

    private static class Data {
        private String stringField;
        private List<String> listField;
        private Set<String> setField;

        public Data withStringField(String value) {
            stringField = value;
            return this;
        }

        public Data withListField(List<String> value) {
            listField = ImmutableList.copyOf(value);
            return this;
        }

        public Data withSetField(Set<String> value) {
            setField = ImmutableSet.copyOf(value);
            return this;
        }

        @Override
        public int hashCode() {
            return Objects.hash(stringField, listField, setField);
        }

        @Override
        public boolean equals(Object other) {
            if (other instanceof Data) {
                Data that = (Data) other;
                return Objects.equals(this.stringField, that.stringField) &&
                Objects.equals(this.listField, that.listField) &&
                Objects.equals(this.setField, that.setField);
            }
            return false;
        }
    }
}
