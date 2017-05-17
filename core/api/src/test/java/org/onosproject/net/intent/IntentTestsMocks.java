/*
 * Copyright 2014-present Open Networking Laboratory
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
package org.onosproject.net.intent;

import com.google.common.base.MoreObjects;
import org.onlab.graph.Weight;
import org.onosproject.core.GroupId;
import org.onosproject.net.DefaultPath;
import org.onosproject.net.DeviceId;
import org.onosproject.net.ElementId;
import org.onosproject.net.HostId;
import org.onosproject.net.Link;
import org.onosproject.net.NetTestTools;
import org.onosproject.net.NetworkResource;
import org.onosproject.net.Path;
import org.onosproject.net.device.DeviceServiceAdapter;
import org.onosproject.net.flow.FlowId;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleExtPayLoad;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.Criterion.Type;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.flow.instructions.Instructions.MetadataInstruction;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.net.topology.DefaultTopologyEdge;
import org.onosproject.net.topology.DefaultTopologyVertex;
import org.onosproject.net.topology.LinkWeigher;
import org.onosproject.net.topology.PathServiceAdapter;
import org.onosproject.net.topology.TopologyVertex;
import org.onosproject.store.Timestamp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static org.onosproject.net.NetTestTools.*;

/**
 * Common mocks used by the intent framework tests.
 */
public class IntentTestsMocks {
    /**
     * Mock traffic selector class used for satisfying API requirements.
     */
    public static class MockSelector implements TrafficSelector {
        @Override
        public Set<Criterion> criteria() {
            return new HashSet<>();
        }

        @Override
        public Criterion getCriterion(Type type) {
            return null;
        }
    }

    /**
     * Mock traffic treatment class used for satisfying API requirements.
     */
    public static class MockTreatment implements TrafficTreatment {
        @Override
        public List<Instruction> deferred() {
            return Collections.emptyList();
        }

        @Override
        public List<Instruction> immediate() {
            return Collections.emptyList();
        }

        @Override
        public List<Instruction> allInstructions() {
            return Collections.emptyList();
        }

        @Override
        public Instructions.TableTypeTransition tableTransition() {
            return null;
        }

        @Override
        public boolean clearedDeferred() {
            return false;
        }

        @Override
        public MetadataInstruction writeMetadata() {
            return null;
        }

        @Override
        public Instructions.MeterInstruction metered() {
            return null;
        }
    }

    /**
     * Mock path service for creating paths within the test.
     */
    public static class MockPathService extends PathServiceAdapter {

        final String[] pathHops;
        final String[] reversePathHops;

        /**
         * Constructor that provides a set of hops to mock.
         *
         * @param pathHops path hops to mock
         */
        public MockPathService(String[] pathHops) {
            this.pathHops = pathHops;
            String[] reversed = pathHops.clone();
            Collections.reverse(Arrays.asList(reversed));
            reversePathHops = reversed;
        }

        @Override
        public Set<Path> getPaths(ElementId src, ElementId dst) {
            Set<Path> result = new HashSet<>();

            String[] allHops = new String[pathHops.length];

            if (src.toString().endsWith(pathHops[0])) {
                System.arraycopy(pathHops, 0, allHops, 0, pathHops.length);
            } else {
                System.arraycopy(reversePathHops, 0, allHops, 0, pathHops.length);
            }

            result.add(createPath(src instanceof HostId, dst instanceof HostId, allHops));
            return result;
        }

        @Override
        public Set<Path> getPaths(ElementId src, ElementId dst, LinkWeigher weigher) {
            Set<Path> paths = getPaths(src, dst);

            for (Path path : paths) {
                DeviceId srcDevice = path.src().elementId() instanceof DeviceId ? path.src().deviceId() : null;
                DeviceId dstDevice = path.dst().elementId() instanceof DeviceId ? path.dst().deviceId() : null;
                if (srcDevice != null && dstDevice != null) {
                    TopologyVertex srcVertex = new DefaultTopologyVertex(srcDevice);
                    TopologyVertex dstVertex = new DefaultTopologyVertex(dstDevice);
                    Link link = link(src.toString(), 1, dst.toString(), 1);

                    Weight weightValue = weigher.weight(new DefaultTopologyEdge(srcVertex, dstVertex, link));
                    if (weightValue.isNegative()) {
                        return new HashSet<>();
                    }
                }
            }
            return paths;
        }
    }

    /**
     * Mock path service for creating paths within the test.
     *
     */
    public static class Mp2MpMockPathService extends PathServiceAdapter {

        final String[] pathHops;
        final String[] reversePathHops;

        /**
         * Constructor that provides a set of hops to mock.
         *
         * @param pathHops path hops to mock
         */
        public Mp2MpMockPathService(String[] pathHops) {
            this.pathHops = pathHops;
            String[] reversed = pathHops.clone();
            Collections.reverse(Arrays.asList(reversed));
            reversePathHops = reversed;
        }

        @Override
        public Set<Path> getPaths(ElementId src, ElementId dst) {
            Set<Path> result = new HashSet<>();

            String[] allHops = new String[pathHops.length + 2];
            allHops[0] = src.toString();
            allHops[allHops.length - 1] = dst.toString();

            if (pathHops.length != 0) {
                System.arraycopy(pathHops, 0, allHops, 1, pathHops.length);
            }

            result.add(createPath(allHops));

            return result;
        }

        @Override
        public Set<Path> getPaths(ElementId src, ElementId dst, LinkWeigher weigher) {
            final Set<Path> paths = getPaths(src, dst);

            for (Path path : paths) {
                final DeviceId srcDevice = path.src().elementId() instanceof DeviceId ? path.src().deviceId() : null;
                final DeviceId dstDevice = path.dst().elementId() instanceof DeviceId ? path.dst().deviceId() : null;
                if (srcDevice != null && dstDevice != null) {
                    final TopologyVertex srcVertex = new DefaultTopologyVertex(srcDevice);
                    final TopologyVertex dstVertex = new DefaultTopologyVertex(dstDevice);
                    final Link link = link(src.toString(), 1, dst.toString(), 1);

                    final Weight weightValue = weigher.weight(new DefaultTopologyEdge(srcVertex, dstVertex, link));
                    if (weightValue.isNegative()) {
                        return new HashSet<>();
                    }
                }
            }
            return paths;
        }
    }

    /**
     * Mock path service for creating paths for MP2SP intent tests, returning
     * pre-determined paths.
     */
    public static class FixedMP2MPMockPathService extends PathServiceAdapter {

        final String[] pathHops;

        public static final String DPID_1 = "of:s1";
        public static final String DPID_2 = "of:s2";
        public static final String DPID_3 = "of:s3";
        public static final String DPID_4 = "of:s4";

        /**
         * Constructor that provides a set of hops to mock.
         *
         * @param pathHops path hops to mock
         */
        public FixedMP2MPMockPathService(String[] pathHops) {
            this.pathHops = pathHops;
        }

        @Override
        public Set<Path> getPaths(ElementId src, ElementId dst) {
            List<Link> links = new ArrayList<>();
            Set<Path> result = new HashSet<>();
            ProviderId providerId = new ProviderId("of", "foo");
            DefaultPath path;
            if (src.toString().equals(DPID_1) && dst.toString().equals(DPID_4)) {
                links.add(NetTestTools.linkNoPrefixes(src.toString(), 2, pathHops[0], 1));
                links.add(NetTestTools.linkNoPrefixes(pathHops[0], 2, dst.toString(), 1));
            } else if (src.toString().equals(DPID_2) && dst.toString().equals(DPID_4)) {
                links.add(NetTestTools.linkNoPrefixes(src.toString(), 2, pathHops[0], 3));
                links.add(NetTestTools.linkNoPrefixes(pathHops[0], 2, dst.toString(), 1));
            } else if (src.toString().equals(DPID_4) && dst.toString().equals(DPID_1)) {
                links.add(NetTestTools.linkNoPrefixes(src.toString(), 2, pathHops[0], 1));
                links.add(NetTestTools.linkNoPrefixes(pathHops[0], 2, dst.toString(), 1));
            } else if (src.toString().equals(DPID_4) && dst.toString().equals(DPID_2)) {
                links.add(NetTestTools.linkNoPrefixes(src.toString(), 2, pathHops[0], 1));
                links.add(NetTestTools.linkNoPrefixes(pathHops[0], 3, dst.toString(), 1));
            } else {
                return result;
            }
            path = new DefaultPath(providerId, links, 3);
            result.add(path);

            return result;
        }

        @Override
        public Set<Path> getPaths(ElementId src, ElementId dst, LinkWeigher weigher) {
            final Set<Path> paths = getPaths(src, dst);

            for (Path path : paths) {
                final DeviceId srcDevice = path.src().elementId() instanceof DeviceId ? path.src().deviceId() : null;
                final DeviceId dstDevice = path.dst().elementId() instanceof DeviceId ? path.dst().deviceId() : null;
                if (srcDevice != null && dstDevice != null) {
                    final TopologyVertex srcVertex = new DefaultTopologyVertex(srcDevice);
                    final TopologyVertex dstVertex = new DefaultTopologyVertex(dstDevice);
                    final Link link = link(src.toString(), 1, dst.toString(), 1);

                    final Weight weightValue = weigher.weight(new DefaultTopologyEdge(srcVertex, dstVertex, link));
                    if (weightValue.isNegative()) {
                        return new HashSet<>();
                    }
                }
            }
            return paths;
        }
    }

    private static final IntentTestsMocks.MockSelector SELECTOR =
            new IntentTestsMocks.MockSelector();
    private static final IntentTestsMocks.MockTreatment TREATMENT =
            new IntentTestsMocks.MockTreatment();

    public static class MockFlowRule implements FlowRule {
        static int nextId = 0;

        int priority;
        int tableId;
        long timestamp;
        int id;
        FlowRuleExtPayLoad payLoad;

        public MockFlowRule(int priority) {
            this.priority = priority;
            this.tableId = 0;
            this.timestamp = System.currentTimeMillis();
            this.id = nextId++;
            this.payLoad = null;
        }

        public MockFlowRule(int priority, FlowRuleExtPayLoad payLoad) {
            this.priority = priority;
            this.timestamp = System.currentTimeMillis();
            this.id = nextId++;
            this.payLoad = payLoad;
        }

        @Override
        public FlowId id() {
            return FlowId.valueOf(id);
        }

        @Override
        public short appId() {
            return 0;
        }

        @Override
        public GroupId groupId() {
            return new GroupId(0);
        }

        @Override
        public int priority() {
            return priority;
        }

        @Override
        public DeviceId deviceId() {
            return did("1");
        }

        @Override
        public TrafficSelector selector() {
            return SELECTOR;
        }

        @Override
        public TrafficTreatment treatment() {
            return TREATMENT;
        }

        @Override
        public int timeout() {
            return 0;
        }

        @Override
        public int hardTimeout() {
            return 0;
        }

        @Override
        public FlowRemoveReason reason() {
            return FlowRemoveReason.NO_REASON;
        }

        @Override
        public boolean isPermanent() {
            return false;
        }

        @Override
        public int hashCode() {
            return priority;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            final MockFlowRule other = (MockFlowRule) obj;
            return Objects.equals(this.timestamp, other.timestamp) &&
                    this.id == other.id;
        }

        @Override
        public boolean exactMatch(FlowRule rule) {
            return this.equals(rule);
        }

        @Override
        public int tableId() {
            return tableId;
        }

        @Override
        public FlowRuleExtPayLoad payLoad() {
            return payLoad;
        }
    }

    public static class MockIntent extends Intent {
        private static AtomicLong counter = new AtomicLong(0);

        private final Long number;

        public MockIntent(Long number) {
            super(NetTestTools.APP_ID, null, Collections.emptyList(),
                  Intent.DEFAULT_INTENT_PRIORITY);
            this.number = number;
        }

        public MockIntent(Long number, Collection<NetworkResource> resources) {
            super(NetTestTools.APP_ID, null, resources, Intent.DEFAULT_INTENT_PRIORITY);
            this.number = number;
        }

        public Long number() {
            return number;
        }

        public static Long nextId() {
            return counter.getAndIncrement();
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(getClass())
                    .add("id", id())
                    .add("appId", appId())
                    .toString();
        }
    }

    public static class MockTimestamp implements Timestamp {
        final int value;

        public MockTimestamp(int value) {
            this.value = value;
        }

        @Override
        public int compareTo(Timestamp o) {
            if (!(o instanceof MockTimestamp)) {
                return -1;
            }
            MockTimestamp that = (MockTimestamp) o;
            return this.value - that.value;
        }

        @Override
        public int hashCode() {
            return value;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof MockTimestamp) {
                return this.compareTo((MockTimestamp) obj) == 0;
            }
            return false;
        }
    }

    /**
     * Mocks the device service so that a device appears available in the test.
     */
    public static class MockDeviceService extends DeviceServiceAdapter {
        @Override
        public boolean isAvailable(DeviceId deviceId) {
            return true;
        }
    }
}
