/*
 * Copyright 2017-present Open Networking Laboratory
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

package org.onosproject.restconf.restconfmanager;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.glassfish.jersey.server.ChunkedOutput;
import org.onosproject.config.DynamicConfigService;
import org.onosproject.config.FailedException;
import org.onosproject.config.Filter;
import org.onosproject.restconf.api.RestconfException;
import org.onosproject.restconf.api.RestconfService;
import org.onosproject.yang.model.DataNode;
import org.onosproject.yang.model.InnerNode;
import org.onosproject.yang.model.KeyLeaf;
import org.onosproject.yang.model.ListKey;
import org.onosproject.yang.model.NodeKey;
import org.onosproject.yang.model.ResourceData;
import org.onosproject.yang.model.ResourceId;
import org.onosproject.yang.model.SchemaId;
import org.onosproject.yang.model.DefaultResourceData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;

import static java.util.concurrent.TimeUnit.SECONDS;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static org.onosproject.restconf.utils.RestconfUtils.convertDataNodeToJson;
import static org.onosproject.restconf.utils.RestconfUtils.convertJsonToDataNode;
import static org.onosproject.restconf.utils.RestconfUtils.convertUriToRid;
import static org.onosproject.yang.model.DataNode.Type.MULTI_INSTANCE_NODE;
import static org.onosproject.yang.model.DataNode.Type.SINGLE_INSTANCE_LEAF_VALUE_NODE;
import static org.onosproject.yang.model.DataNode.Type.SINGLE_INSTANCE_NODE;

/*
 * ONOS RESTCONF application. The RESTCONF Manager
 * implements the main logic of the RESTCONF application.
 *
 * The design of the RESTCONF subsystem contains 2 major bundles:
 *    This bundle module is the back-end of the server.
 *    It provides the main logic of the RESTCONF server. It interacts with
 *    the Dynamic Config Service and yang runtime service to run operations
 *    on the YANG data objects (i.e., resource id, yang data node).
 */

@Component(immediate = true)
@Service
public class RestconfManager implements RestconfService {

    private static final String RESTCONF_ROOT = "/onos/restconf";
    private static final int THREAD_TERMINATION_TIMEOUT = 10;

    // Jersey's default chunk parser uses "\r\n" as the chunk separator.
    private static final String EOL = "\r\n";

    private final int maxNumOfWorkerThreads = 5;

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DynamicConfigService dynamicConfigService;

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
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        shutdownAndAwaitTermination(workerThreadPool);
        log.info("Stopped");
    }

    @Override
    public ObjectNode runGetOperationOnDataResource(String uri)
            throws RestconfException {
        ResourceId rid = convertUriToRid(uri);
        // TODO: define Filter (if there is any requirement).
        Filter filter = new Filter();
        DataNode dataNode;
        try {
            dataNode = dynamicConfigService.readNode(rid, filter);
        } catch (FailedException e) {
            log.error("ERROR: DynamicConfigService: ", e);
            throw new RestconfException("ERROR: DynamicConfigService",
                                        INTERNAL_SERVER_ERROR);
        }
        ObjectNode rootNode = convertDataNodeToJson(rid, dataNode);
        return rootNode;
    }

    @Override
    public void runPostOperationOnDataResource(String uri, ObjectNode rootNode)
            throws RestconfException {
        ResourceData receivedData = convertJsonToDataNode(uri, rootNode);
        ResourceData resourceData = getDataForStore(receivedData);
        ResourceId rid = resourceData.resourceId();
        List<DataNode> dataNodeList = resourceData.dataNodes();
        // TODO: Error message needs to be fixed
        if (dataNodeList.size() > 1) {
            log.warn("ERROR: There are more than one Data Node can be proceed");
        }
        DataNode dataNode = dataNodeList.get(0);
        try {
            dynamicConfigService.createNodeRecursive(rid, dataNode);
        } catch (FailedException e) {
            log.error("ERROR: DynamicConfigService: ", e);
            throw new RestconfException("ERROR: DynamicConfigService",
                                        INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public void runPutOperationOnDataResource(String uri, ObjectNode rootNode)
            throws RestconfException {
        runPostOperationOnDataResource(uri, rootNode);
    }

    @Override
    public void runDeleteOperationOnDataResource(String uri)
            throws RestconfException {
        ResourceId rid = convertUriToRid(uri);
        try {
            dynamicConfigService.deleteNodeRecursive(rid);
        } catch (FailedException e) {
            log.error("ERROR: DynamicConfigService: ", e);
            throw new RestconfException("ERROR: DynamicConfigService",
                                        INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public void runPatchOperationOnDataResource(String uri, ObjectNode rootNode)
            throws RestconfException {
    }

    @Override
    public String getRestconfRootPath() {
        return RESTCONF_ROOT;
    }

    /**
     * Creates a worker thread to listen to events and write to chunkedOutput.
     * The worker thread blocks if no events arrive.
     *
     * @param streamId the RESTCONF stream id to which the client subscribes
     * @param output   the string data stream
     * @throws RestconfException if the worker thread fails to create
     */
    @Override
    public void subscribeEventStream(String streamId,
                                     ChunkedOutput<String> output)
            throws RestconfException {
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

        BlockingQueue<ObjectNode> eventQueue = new LinkedBlockingQueue<>();
        workerThreadPool.submit(new EventConsumer(output, eventQueue));
    }

    private ResourceData getDataForStore(ResourceData resourceData) {
        List<DataNode> nodes = resourceData.dataNodes();
        ResourceId rid = resourceData.resourceId();
        DataNode.Builder dbr = null;
        ResourceId parentId = null;
        try {
            NodeKey lastKey = rid.nodeKeys().get(rid.nodeKeys().size() - 1);
            SchemaId sid = lastKey.schemaId();
            if (lastKey instanceof ListKey) {
                dbr = InnerNode.builder(
                        sid.name(), sid.namespace()).type(MULTI_INSTANCE_NODE);
                for (KeyLeaf keyLeaf : ((ListKey) lastKey).keyLeafs()) {
                    Object val = keyLeaf.leafValue();
                    dbr = dbr.addKeyLeaf(keyLeaf.leafSchema().name(),
                                         sid.namespace(), val);
                    dbr = dbr.createChildBuilder(keyLeaf.leafSchema().name(),
                                                 sid.namespace(), val)
                            .type(SINGLE_INSTANCE_LEAF_VALUE_NODE);
                    //Exit for key leaf node
                    dbr = dbr.exitNode();
                }
            } else {
                dbr = InnerNode.builder(
                        sid.name(), sid.namespace()).type(SINGLE_INSTANCE_NODE);
            }
            if (nodes != null && !nodes.isEmpty()) {
                // adding the parent node for given list of nodes
                for (DataNode node : nodes) {
                    dbr = ((InnerNode.Builder) dbr).addNode(node);
                }
            }
            parentId = rid.copyBuilder().removeLastKey().build();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        ResourceData.Builder resData = DefaultResourceData.builder();
        resData.addDataNode(dbr.build());
        resData.resourceId(parentId);
        return resData.build();
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

    /**
     * Implementation of a worker thread which reads data from a
     * blocking queue and writes the data to a given chunk output stream.
     * The thread is blocked when no data arrive to the queue and is
     * terminated when the chunk output stream is closed (i.e., the
     * HTTP-keep-alive session is closed).
     */
    private class EventConsumer implements Runnable {

        private String queueId;
        private final ChunkedOutput<String> output;
        private final BlockingQueue<ObjectNode> bqueue;

        public EventConsumer(ChunkedOutput<String> output,
                             BlockingQueue<ObjectNode> q) {
            this.output = output;
            this.bqueue = q;
        }

        @Override
        public void run() {
            try {
                queueId = String.valueOf(Thread.currentThread().getId());
                eventQueueList.put(queueId, bqueue);
                log.debug("EventConsumer thread created: {}", queueId);

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
}
