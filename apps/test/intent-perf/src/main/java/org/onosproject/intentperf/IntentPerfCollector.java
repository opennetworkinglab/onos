/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.intentperf;

import com.google.common.collect.ImmutableList;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.cluster.NodeId;
import org.onosproject.store.cluster.messaging.ClusterCommunicationService;
import org.onosproject.store.cluster.messaging.ClusterMessage;
import org.onosproject.store.cluster.messaging.ClusterMessageHandler;
import org.onosproject.store.cluster.messaging.MessageSubject;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.onlab.util.SharedExecutors.getPoolThreadExecutor;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Collects and distributes performance samples.
 */
@Component(immediate = true)
@Service(value = IntentPerfCollector.class)
public class IntentPerfCollector {

    private static final long SAMPLE_TIME_WINDOW_MS = 5_000;
    private final Logger log = getLogger(getClass());

    private static final int MAX_SAMPLES = 1_000;

    private final List<Sample> samples = new LinkedList<>();

    private static final MessageSubject SAMPLE = new MessageSubject("intent-perf-sample");

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterCommunicationService communicationService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected IntentPerfUi ui;

    // Auxiliary structures used to accrue data for normalized time interval
    // across all nodes.
    private long newestTime;
    private Sample overall;
    private Sample current;

    private ControllerNode[] nodes;
    private Map<NodeId, Integer> nodeToIndex;

    private NodeId nodeId;

    @Activate
    public void activate() {
        nodeId = clusterService.getLocalNode().id();

        communicationService.addSubscriber(SAMPLE, new InternalSampleCollector(),
                                           getPoolThreadExecutor());

        nodes = clusterService.getNodes().toArray(new ControllerNode[]{});
        Arrays.sort(nodes, (a, b) -> a.id().toString().compareTo(b.id().toString()));

        nodeToIndex = new HashMap<>();
        for (int i = 0; i < nodes.length; i++) {
            nodeToIndex.put(nodes[i].id(), i);
        }

        clearSamples();
        ui.setCollector(this);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        communicationService.removeSubscriber(SAMPLE);
        log.info("Stopped");
    }

    /**
     * Clears all previously accumulated data.
     */
    public synchronized void clearSamples() {
        newestTime = 0;
        overall = new Sample(0, nodes.length);
        current = new Sample(0, nodes.length);
        samples.clear();
    }


    /**
     * Records a sample point of data about intent operation rate.
     *
     * @param overallRate overall rate
     * @param currentRate current rate
     */
    public void recordSample(double overallRate, double currentRate) {
        long now = System.currentTimeMillis();
        addSample(now, nodeId, overallRate, currentRate);
        broadcastSample(now, nodeId, overallRate, currentRate);
    }

    /**
     * Returns set of node ids as headers.
     *
     * @return node id headers
     */
    public List<String> getSampleHeaders() {
        List<String> headers = new ArrayList<>();
        for (ControllerNode node : nodes) {
            headers.add(node.id().toString());
        }
        return headers;
    }

    /**
     * Returns set of all accumulated samples normalized to the local set of
     * samples.
     *
     * @return accumulated samples
     */
    public synchronized List<Sample> getSamples() {
        return ImmutableList.copyOf(samples);
    }

    /**
     * Returns overall throughput performance for each of the cluster nodes.
     *
     * @return overall intent throughput
     */
    public synchronized Sample getOverall() {
        return overall;
    }

    // Records a new sample to our collection of samples
    private synchronized void addSample(long time, NodeId nodeId,
                                        double overallRate, double currentRate) {
        Sample fullSample = createCurrentSampleIfNeeded(time);
        setSampleData(current, nodeId, currentRate);
        setSampleData(overall, nodeId, overallRate);
        pruneSamplesIfNeeded();

        if (fullSample != null && ui != null) {
            ui.reportSample(fullSample);
        }
    }

    private Sample createCurrentSampleIfNeeded(long time) {
        Sample oldSample = time - newestTime > SAMPLE_TIME_WINDOW_MS || current.isComplete() ? current : null;
        if (oldSample != null) {
            newestTime = time;
            current = new Sample(time, nodes.length);
            if (oldSample.time > 0) {
                samples.add(oldSample);
            }
        }
        return oldSample;
    }

    private void setSampleData(Sample sample, NodeId nodeId, double data) {
        Integer index = nodeToIndex.get(nodeId);
        if (index != null) {
            sample.data[index] = data;
        }
    }

    private void pruneSamplesIfNeeded() {
        if (samples.size() > MAX_SAMPLES) {
            samples.remove(0);
        }
    }

    // Performance data sample.
    static class Sample {
        final long time;
        final double[] data;

        public Sample(long time, int nodeCount) {
            this.time = time;
            this.data = new double[nodeCount];
            Arrays.fill(data, -1);
        }

        public boolean isComplete() {
            for (int i = 0; i < data.length; i++) {
                if (data[i] < 0) {
                    return false;
                }
            }
            return true;
        }
    }

    private void broadcastSample(long time, NodeId nodeId, double overallRate, double currentRate) {
        String data = String.format("%d|%f|%f", time, overallRate, currentRate);
        communicationService.broadcast(data, SAMPLE, str -> str.getBytes());
    }

    private class InternalSampleCollector implements ClusterMessageHandler {
        @Override
        public void handle(ClusterMessage message) {
            String[] fields = new String(message.payload()).split("\\|");
            log.debug("Received sample from {}: {}", message.sender(), fields);
            addSample(Long.parseLong(fields[0]), message.sender(),
                      Double.parseDouble(fields[1]), Double.parseDouble(fields[2]));
        }
    }
}
