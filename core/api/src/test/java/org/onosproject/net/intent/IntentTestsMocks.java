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
import org.onlab.util.Bandwidth;
import org.onosproject.core.DefaultGroupId;
import org.onosproject.core.GroupId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.ElementId;
import org.onosproject.net.HostId;
import org.onosproject.net.Link;
import org.onosproject.net.NetTestTools;
import org.onosproject.net.NetworkResource;
import org.onosproject.net.Path;
import org.onosproject.net.flow.FlowRule.FlowRemoveReason;
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
import org.onosproject.net.resource.DiscreteResourceId;
import org.onosproject.net.resource.Resource;
import org.onosproject.net.resource.ResourceAllocation;
import org.onosproject.net.resource.ResourceConsumer;
import org.onosproject.net.resource.ResourceId;
import org.onosproject.net.resource.ResourceListener;
import org.onosproject.net.resource.ResourceService;
import org.onosproject.net.topology.DefaultTopologyEdge;
import org.onosproject.net.topology.DefaultTopologyVertex;
import org.onosproject.net.topology.LinkWeight;
import org.onosproject.net.topology.PathServiceAdapter;
import org.onosproject.net.topology.TopologyVertex;
import org.onosproject.store.Timestamp;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
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
        public Set<Path> getPaths(ElementId src, ElementId dst, LinkWeight weight) {
            final Set<Path> paths = getPaths(src, dst);

            for (Path path : paths) {
                final DeviceId srcDevice = path.src().elementId() instanceof DeviceId ? path.src().deviceId() : null;
                final DeviceId dstDevice = path.dst().elementId() instanceof DeviceId ? path.dst().deviceId() : null;
                if (srcDevice != null && dstDevice != null) {
                    final TopologyVertex srcVertex = new DefaultTopologyVertex(srcDevice);
                    final TopologyVertex dstVertex = new DefaultTopologyVertex(dstDevice);
                    final Link link = link(src.toString(), 1, dst.toString(), 1);

                    final double weightValue = weight.weight(new DefaultTopologyEdge(srcVertex, dstVertex, link));
                    if (weightValue < 0) {
                        return new HashSet<>();
                    }
                }
            }
            return paths;
        }
    }

    public static final class MockResourceService implements ResourceService {

        private final double bandwidth;

        public static ResourceService makeBandwidthResourceService(double bandwidth) {
            return new MockResourceService(bandwidth);
        }

        private MockResourceService(double bandwidth) {
            this.bandwidth = bandwidth;
        }

        @Override
        public List<ResourceAllocation> allocate(ResourceConsumer consumer, List<? extends Resource> resources) {
            return null;
        }

        @Override
        public boolean release(List<ResourceAllocation> allocations) {
            return false;
        }

        @Override
        public boolean release(ResourceConsumer consumer) {
            return false;
        }

        @Override
        public List<ResourceAllocation> getResourceAllocations(ResourceId id) {
            return null;
        }

        @Override
        public <T> Collection<ResourceAllocation> getResourceAllocations(DiscreteResourceId parent, Class<T> cls) {
            return null;
        }

        @Override
        public Collection<ResourceAllocation> getResourceAllocations(ResourceConsumer consumer) {
            return null;
        }

        @Override
        public Set<Resource> getAvailableResources(DiscreteResourceId parent) {
            return null;
        }

        @Override
        public <T> Set<Resource> getAvailableResources(DiscreteResourceId parent, Class<T> cls) {
            return null;
        }

        @Override
        public <T> Set<T> getAvailableResourceValues(DiscreteResourceId parent, Class<T> cls) {
            return null;
        }

        @Override
        public Set<Resource> getRegisteredResources(DiscreteResourceId parent) {
            return null;
        }

        @Override
        public boolean isAvailable(Resource resource) {
            if (!resource.isTypeOf(Bandwidth.class)) {
                return false;
            }

            Optional<Double> value = resource.valueAs(Double.class);
            return value.filter(requested -> requested <= bandwidth).isPresent();
        }

        @Override
        public void addListener(ResourceListener listener) {
        }

        @Override
        public void removeListener(ResourceListener listener) {
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
            return new DefaultGroupId(0);
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
    }

}
