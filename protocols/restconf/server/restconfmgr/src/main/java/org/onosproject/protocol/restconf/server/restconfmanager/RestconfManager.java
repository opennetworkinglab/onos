/*
 * Copyright 2016-present Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onosproject.protocol.restconf.server.restconfmanager;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.glassfish.jersey.server.ChunkedOutput;
import org.onosproject.event.ListenerTracker;
import org.onosproject.protocol.restconf.server.api.RestconfException;
import org.onosproject.protocol.restconf.server.api.RestconfService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/*
 * Skeletal ONOS RESTCONF Server application. The RESTCONF Manager
 * implements the main logic of the RESTCONF Server.
 *
 * The design of the RESTCONF subsystem contains 2 major bundles:
 *
 * 1. RESTCONF Protocol Proxy (RPP). This bundle is implemented as a JAX-RS application.
 * It acts as the frond-end of the the RESTCONF server. It handles
 * HTTP requests that are sent to the RESTCONF Root Path. It then calls the RESTCONF Manager
 * to process the requests.
 *
 * 2. RESTCONF Manager. This is the back-end. It provides the main logic of the RESTCONF server.
 * It calls the YMS (YANG Management System) to operate on the YANG data objects.
 */

/**
 * Implementation of the RestconfService interface. The class is designed
 * as a Apache Flex component. Note that to avoid unnecessary
 * activation, the @Component annotation's immediate parameter is set to false.
 * So the component is not activated until a RESTCONF request is received by
 * the RESTCONF Protocol Proxy (RPP) module, which consumes the service.
 */
@Component(immediate = false)
@Service
public class RestconfManager implements RestconfService {

    private static final String RESTCONF_ROOT = "/onos/restconf";
    private static final int THREAD_TERMINATION_TIMEOUT = 10;
    private static final String EOL = String.format("%n");

    private final int maxNumOfWorkerThreads = 5;

    private final Logger log = LoggerFactory.getLogger(getClass());

    //TODO: YMS service
    //@Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    //protected YmsService ymsService;

    private ListenerTracker listeners;

    private ConcurrentMap<String, BlockingQueue<ObjectNode>> eventQueueList =
            new ConcurrentHashMap<String, BlockingQueue<ObjectNode>>();

    private ExecutorService workerThreadPool;

    @Activate
    protected void activate() {
        workerThreadPool = Executors.newFixedThreadPool(maxNumOfWorkerThreads,
                                                        new ThreadFactoryBuilder()
                                                                .setNameFormat("restconf-worker")
                                                                .build());
        listeners = new ListenerTracker();
        //TODO: YMS notification
        //listeners.addListener(ymsService, new InternalYangNotificationListener());
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        listeners.removeListeners();
        shutdownAndAwaitTermination(workerThreadPool);
        log.info("Stopped");
    }

    @Override
    public ObjectNode runGetOperationOnDataResource(String uri) throws RestconfException {
        //TODO: YMS integration
        return null;
    }

    @Override
    public void runPostOperationOnDataResource(String uri, ObjectNode rootNode) throws RestconfException {
        //TODO: YMS integration
    }

    @Override
    public void runPutOperationOnDataResource(String uri, ObjectNode rootNode) throws RestconfException {
        //TODO: YMS integration
    }

    /**
     * Process the delete operation on a data resource.
     *
     * @param uri URI of the data resource to be deleted.
     */
    @Override
    public void runDeleteOperationOnDataResource(String uri) throws RestconfException {
        //TODO: YMS integration
    }

    @Override
    public String getRestconfRootPath() {
        return this.RESTCONF_ROOT;
    }

    /**
     * Creates a worker thread to listen to events and write to chunkedOutput.
     * The worker thread blocks if no events arrive.
     *
     * @param streamId ID of the RESTCONF stream to subscribe
     * @param output   A string data stream
     * @throws RestconfException if the worker thread fails to create
     */
    @Override
    public void subscribeEventStream(String streamId, ChunkedOutput<String> output) throws RestconfException {
        BlockingQueue<ObjectNode> eventQueue = new LinkedBlockingQueue<ObjectNode>();
        if (workerThreadPool instanceof ThreadPoolExecutor) {
            if (((ThreadPoolExecutor) workerThreadPool).getActiveCount() >= maxNumOfWorkerThreads) {
                throw new RestconfException("no more work threads left to handle event subscription",
                                            Response.Status.INTERNAL_SERVER_ERROR);
            }
        } else {
            throw new RestconfException("Server ERROR: workerThreadPool NOT instanceof ThreadPoolExecutor",
                                        Response.Status.INTERNAL_SERVER_ERROR);

        }

        workerThreadPool.submit(new EventConsumer(output, eventQueue));
    }


    /**
     * Shutdown a pool cleanly if possible.
     *
     * @param pool an executorService
     */
    private void shutdownAndAwaitTermination(ExecutorService pool) {
        pool.shutdown(); // Disable new tasks from being submitted
        try {
            // Wait a while for existing tasks to terminate
            if (!pool.awaitTermination(THREAD_TERMINATION_TIMEOUT, TimeUnit.SECONDS)) {
                pool.shutdownNow(); // Cancel currently executing tasks
                // Wait a while for tasks to respond to being cancelled
                if (!pool.awaitTermination(THREAD_TERMINATION_TIMEOUT, TimeUnit.SECONDS)) {
                    log.error("Pool did not terminate");
                }
            }
        } catch (Exception ie) {
            // (Re-)Cancel if current thread also interrupted
            pool.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }
    }

    private class EventConsumer implements Runnable {

        private final String queueId;
        private final ChunkedOutput<String> output;
        private final BlockingQueue<ObjectNode> bqueue;

        public EventConsumer(ChunkedOutput<String> output, BlockingQueue<ObjectNode> q) {
            this.queueId = Thread.currentThread().getName();
            this.output = output;
            this.bqueue = q;
            eventQueueList.put(queueId, bqueue);
        }

        @Override
        public void run() {
            try {
                ObjectNode chunk;
                while ((chunk = bqueue.take()) != null) {
                    output.write(chunk.toString().concat(EOL));
                }
            } catch (IOException e) {
                log.debug("chunkedOuput is closed: {}", this.bqueue.toString());
                /*
                 * Remove queue from the queue list, so that the event producer
                 * (i.e., listener) would stop working.
                 */
                eventQueueList.remove(this.queueId);
            } catch (InterruptedException e) {
                log.error("ERROR: EventConsumer: bqueue.take() has been interrupted.");
                log.debug("EventConsumer Exception:", e);
            } finally {
                try {
                    output.close();
                    log.debug("EventConsumer thread terminated: {}", queueId);
                } catch (IOException e) {
                    log.error("ERROR: EventConsumer: ", e);
                }
            }
        }

    }

    /**
     * The listener class acts as the event producer for the event queues. The
     * queues are created by the event consumer threads and are removed when the
     * threads terminate.
     */
    //TODO: YMS notification
    /*private class InternalYangNotificationListener implements YangNotificationListener {

        @Override
        public void event(YangNotificationEvent event) {
            if (event.type() != YangNotificationEvent.Type.YANG_NOTIFICATION) {
                // For now, we only handle YANG notification events.
                return;
            }

            if (eventQueueList.isEmpty()) {
                *//*
                 * There is no consumer waiting to consume, so don't have to
                 * produce this event.
                 *//*
                return;
            }

            try {
                *//*
                 * Put the event to every queue out there. Each queue is
                 * corresponding to an event stream session. The queue is
                 * removed when the session terminates.
                 *//*
                for (Entry<String, BlockingQueue<ObjectNode>> entry : eventQueueList
                        .entrySet()) {
                    entry.getValue().put(event.subject().getData());
                }
            } catch (InterruptedException e) {
                Log.error("ERROR", e);
                throw new RestconfException("queue", Status.INTERNAL_SERVER_ERROR);
            }
        }

    }*/
}
