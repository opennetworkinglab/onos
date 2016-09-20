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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.glassfish.jersey.server.ChunkedOutput;
import org.onosproject.event.ListenerTracker;
import org.onosproject.protocol.restconf.server.api.RestconfException;
import org.onosproject.protocol.restconf.server.api.RestconfService;
import org.onosproject.yms.ydt.YdtBuilder;
import org.onosproject.yms.ydt.YdtContext;
import org.onosproject.yms.ydt.YdtContextOperationType;
import org.onosproject.yms.ydt.YdtResponse;
import org.onosproject.yms.ydt.YmsOperationExecutionStatus;
import org.onosproject.yms.ydt.YmsOperationType;
import org.onosproject.yms.ymsm.YmsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;


import static org.onosproject.yms.ydt.YmsOperationType.QUERY_REQUEST;
import static org.onosproject.yms.ydt.YmsOperationType.EDIT_CONFIG_REQUEST;
import static org.onosproject.yms.ydt.YdtContextOperationType.NONE;
import static org.onosproject.yms.ydt.YdtContextOperationType.CREATE;
import static org.onosproject.yms.ydt.YdtContextOperationType.DELETE;
import static org.onosproject.yms.ydt.YdtContextOperationType.REPLACE;
import static org.onosproject.yms.ydt.YdtContextOperationType.MERGE;
import static org.onosproject.yms.ydt.YdtType.SINGLE_INSTANCE_LEAF_VALUE_NODE;
import static org.onosproject.yms.ydt.YmsOperationExecutionStatus.EXECUTION_SUCCESS;
import static org.onosproject.protocol.restconf.server.utils.parser.json.ParserUtils.convertYdtToJson;
import static org.onosproject.protocol.restconf.server.utils.parser.json.ParserUtils.convertUriToYdt;
import static org.onosproject.protocol.restconf.server.utils.parser.json.ParserUtils.convertJsonToYdt;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static java.util.concurrent.TimeUnit.SECONDS;
/*
 * Skeletal ONOS RESTCONF Server application. The RESTCONF Manager
 * implements the main logic of the RESTCONF Server.
 *
 * The design of the RESTCONF subsystem contains 2 major bundles:
 *
 * 1. RESTCONF Protocol Proxy (RPP). This bundle is implemented as a
 *    JAX-RS application. It acts as the frond-end of the RESTCONF server.
 *    It intercepts/handles HTTP requests that are sent to the RESTCONF
 *    Root Path. It then calls the RESTCONF Manager to process the requests.
 *
 * 2. RESTCONF Manager. This bundle module is the back-end of the server.
 *    It provides the main logic of the RESTCONF server. It interacts with
 *    the YMS (YANG Management System) to run operations on the YANG data
 *    objects (i.e., data resources).
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

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected YmsService ymsService;

    private ListenerTracker listeners;

    private ConcurrentMap<String, BlockingQueue<ObjectNode>> eventQueueList =
            new ConcurrentHashMap<>();

    private ExecutorService workerThreadPool;

    @Activate
    protected void activate() {
        workerThreadPool = Executors
                .newFixedThreadPool(maxNumOfWorkerThreads,
                                    new ThreadFactoryBuilder()
                                            .setNameFormat("restconf-worker")
                                            .build());
        listeners = new ListenerTracker();
        //TODO: YMS notification
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        listeners.removeListeners();
        shutdownAndAwaitTermination(workerThreadPool);
        log.info("Stopped");
    }

    @Override
    public ObjectNode runGetOperationOnDataResource(String uri)
            throws RestconfException {
        YdtBuilder ydtBuilder = getYdtBuilder(QUERY_REQUEST);
        //Convert the URI to ydtBuilder
        convertUriToYdt(uri, ydtBuilder, NONE);
        YdtResponse ydtResponse = ymsService.executeOperation(ydtBuilder);
        YmsOperationExecutionStatus status = ydtResponse
                .getYmsOperationResult();
        if (status != EXECUTION_SUCCESS) {
            throw new RestconfException("YMS GET operation failed",
                                        INTERNAL_SERVER_ERROR);
        }

        YdtContext rootNode = ydtResponse.getRootNode();
        YdtContext curNode = ydtBuilder.getCurNode();

        ObjectNode result = convertYdtToJson(curNode.getName(), rootNode,
                                             ymsService.getYdtWalker());
        //if the query URI contain a key, something like list=key
        //here should only get get child with the specific key
        YdtContext child = curNode.getFirstChild();
        if (child != null &&
                child.getYdtType() == SINGLE_INSTANCE_LEAF_VALUE_NODE) {

            ArrayNode jsonNode = (ArrayNode) result.get(curNode.getName());
            for (JsonNode next : jsonNode) {
                if (next.findValue(child.getName())
                        .asText().equals(child.getValue())) {
                    return (ObjectNode) next;
                }
            }
            throw new RestconfException(String.format("No content for %s = %s",
                                                      child.getName(),
                                                      child.getValue()),
                                        INTERNAL_SERVER_ERROR);
        }
        return result;
    }

    private YmsOperationExecutionStatus
    invokeYmsOp(String uri, ObjectNode rootNode,
                YdtContextOperationType opType) {
        YdtBuilder ydtBuilder = getYdtBuilder(EDIT_CONFIG_REQUEST);
        //Convert the URI to ydtBuilder
        convertUriToYdt(uri, ydtBuilder, opType);

        //set default operation type for the payload node
        ydtBuilder.setDefaultEditOperationType(opType);
        //convert the payload json body to ydt
        convertJsonToYdt(rootNode, ydtBuilder);

        return ymsService
                .executeOperation(ydtBuilder)
                .getYmsOperationResult();
    }

    @Override
    public void runPostOperationOnDataResource(String uri, ObjectNode rootNode)
            throws RestconfException {
        YmsOperationExecutionStatus status =
                invokeYmsOp(uri, rootNode, CREATE);

        if (status != EXECUTION_SUCCESS) {
            throw new RestconfException("YMS post operation failed.",
                                        INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public void runPutOperationOnDataResource(String uri, ObjectNode rootNode)
            throws RestconfException {
        YmsOperationExecutionStatus status =
                invokeYmsOp(uri, rootNode, REPLACE);

        if (status != EXECUTION_SUCCESS) {
            throw new RestconfException("YMS put operation failed.",
                                        INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public void runDeleteOperationOnDataResource(String uri)
            throws RestconfException {
        //Get a root ydtBuilder
        YdtBuilder ydtBuilder = getYdtBuilder(EDIT_CONFIG_REQUEST);
        //Convert the URI to ydtBuilder
        convertUriToYdt(uri, ydtBuilder, DELETE);
        //Execute the delete operation
        YmsOperationExecutionStatus status = ymsService
                .executeOperation(ydtBuilder)
                .getYmsOperationResult();
        if (status != EXECUTION_SUCCESS) {
            throw new RestconfException("YMS delete operation failed.",
                                        INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public void runPatchOperationOnDataResource(String uri, ObjectNode rootNode)
            throws RestconfException {
        YmsOperationExecutionStatus status = invokeYmsOp(uri, rootNode, MERGE);

        if (status != EXECUTION_SUCCESS) {
            throw new RestconfException("YMS patch operation failed.",
                                        INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public String getRestconfRootPath() {
        return RESTCONF_ROOT;
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
    public void subscribeEventStream(String streamId,
                                     ChunkedOutput<String> output)
            throws RestconfException {
        BlockingQueue<ObjectNode> eventQueue = new LinkedBlockingQueue<>();
        if (workerThreadPool instanceof ThreadPoolExecutor) {
            if (((ThreadPoolExecutor) workerThreadPool).getActiveCount() >=
                    maxNumOfWorkerThreads) {
                throw new RestconfException("no more work threads left to " +
                                                    "handle event subscription",
                                            INTERNAL_SERVER_ERROR);
            }
        } else {
            throw new RestconfException("Server ERROR: workerThreadPool NOT " +
                                                "instanceof ThreadPoolExecutor",
                                        INTERNAL_SERVER_ERROR);

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
            if (!pool.awaitTermination(THREAD_TERMINATION_TIMEOUT, SECONDS)) {
                pool.shutdownNow(); // Cancel currently executing tasks
                // Wait a while for tasks to respond to being cancelled
                if (!pool.awaitTermination(THREAD_TERMINATION_TIMEOUT,
                                           SECONDS)) {
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

        public EventConsumer(ChunkedOutput<String> output,
                             BlockingQueue<ObjectNode> q) {
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
                log.error("ERROR: EventConsumer: bqueue.take() " +
                                  "has been interrupted.");
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

    private YdtBuilder getYdtBuilder(YmsOperationType ymsOperationType) {
        return ymsService.getYdtBuilder(RESTCONF_ROOT, null, ymsOperationType);
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
