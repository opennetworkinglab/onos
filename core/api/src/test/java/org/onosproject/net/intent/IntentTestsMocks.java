/*
 * Copyright 2014-2015 Open Networking Laboratory
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
import com.google.common.collect.ImmutableSet;
import org.onlab.util.Bandwidth;
import org.onosproject.core.DefaultGroupId;
import org.onosproject.core.GroupId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.ElementId;
import org.onosproject.net.Link;
import org.onosproject.net.NetTestTools;
import org.onosproject.net.NetworkResource;
import org.onosproject.net.Path;
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
import org.onosproject.net.resource.ResourceAllocation;
import org.onosproject.net.resource.ResourceRequest;
import org.onosproject.net.resource.ResourceType;
import org.onosproject.net.resource.link.BandwidthResource;
import org.onosproject.net.resource.link.BandwidthResourceRequest;
import org.onosproject.net.resource.link.LambdaResource;
import org.onosproject.net.resource.link.LambdaResourceAllocation;
import org.onosproject.net.resource.link.LambdaResourceRequest;
import org.onosproject.net.resource.link.LinkResourceAllocations;
import org.onosproject.net.resource.link.LinkResourceListener;
import org.onosproject.net.resource.link.LinkResourceRequest;
import org.onosproject.net.resource.link.LinkResourceService;
import org.onosproject.net.resource.link.MplsLabel;
import org.onosproject.net.resource.link.MplsLabelResourceAllocation;
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
import java.util.LinkedList;
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

            result.add(createPath(allHops));
            return result;
        }

        @Override
        public Set<Path> getPaths(ElementId src, ElementId dst, LinkWeight weight) {
            final Set<Path> paths = getPaths(src, dst);

            for (Path path : paths) {
                final DeviceId srcDevice = path.src().deviceId();
                final DeviceId dstDevice = path.dst().deviceId();
                final TopologyVertex srcVertex = new DefaultTopologyVertex(srcDevice);
                final TopologyVertex dstVertex = new DefaultTopologyVertex(dstDevice);
                final Link link = link(src.toString(), 1, dst.toString(), 1);

                final double weightValue = weight.weight(new DefaultTopologyEdge(srcVertex, dstVertex, link));
                if (weightValue < 0) {
                    return new HashSet<>();
                }
            }
            return paths;
        }
    }

    public static class MockLinkResourceAllocations implements LinkResourceAllocations {
        @Override
        public Set<ResourceAllocation> getResourceAllocation(Link link) {
            return ImmutableSet.of(
                    new LambdaResourceAllocation(LambdaResource.valueOf(77)),
                    new MplsLabelResourceAllocation(MplsLabel.valueOf(10)));
        }

        @Override
        public IntentId intentId() {
            return null;
        }

        @Override
        public Collection<Link> links() {
            return null;
        }

        @Override
        public Set<ResourceRequest> resources() {
            return null;
        }

        @Override
        public ResourceType type() {
            return null;
        }
    }

    public static class MockedAllocationFailure extends RuntimeException { }

    public static class MockResourceService implements LinkResourceService {

        double availableBandwidth = -1.0;
        int availableLambda = -1;

        /**
         * Allocates a resource service that will allow bandwidth allocations
         * up to a limit.
         *
         * @param bandwidth available bandwidth limit
         * @return resource manager for bandwidth requests
         */
        public static MockResourceService makeBandwidthResourceService(double bandwidth) {
            final MockResourceService result = new MockResourceService();
            result.availableBandwidth = bandwidth;
            return result;
        }

        /**
         * Allocates a resource service that will allow lambda allocations.
         *
         * @param lambda Lambda to return for allocation requests. Currently unused
         * @return resource manager for lambda requests
         */
        public static MockResourceService makeLambdaResourceService(int lambda) {
            final MockResourceService result = new MockResourceService();
            result.availableLambda = lambda;
            return result;
        }

        public void setAvailableBandwidth(double availableBandwidth) {
            this.availableBandwidth = availableBandwidth;
        }

        public void setAvailableLambda(int availableLambda) {
            this.availableLambda = availableLambda;
        }


        @Override
        public LinkResourceAllocations requestResources(LinkResourceRequest req) {
            int lambda = -1;
            double bandwidth = -1.0;

            for (ResourceRequest resourceRequest : req.resources()) {
                if (resourceRequest.type() == ResourceType.BANDWIDTH) {
                    final BandwidthResourceRequest brr = (BandwidthResourceRequest) resourceRequest;
                    bandwidth = brr.bandwidth().toDouble();
                } else if (resourceRequest.type() == ResourceType.LAMBDA) {
                    lambda = 1;
                }
            }

            if (availableBandwidth < bandwidth) {
                throw new MockedAllocationFailure();
            }
            if (lambda > 0 && availableLambda == 0) {
                throw new MockedAllocationFailure();
            }

            return new IntentTestsMocks.MockLinkResourceAllocations();
        }

        @Override
        public void releaseResources(LinkResourceAllocations allocations) {
            // Mock
        }

        @Override
        public LinkResourceAllocations updateResources(LinkResourceRequest req,
                                                       LinkResourceAllocations oldAllocations) {
            return null;
        }

        @Override
        public Iterable<LinkResourceAllocations> getAllocations() {
            return ImmutableSet.of(
                    new IntentTestsMocks.MockLinkResourceAllocations());
        }

        @Override
        public Iterable<LinkResourceAllocations> getAllocations(Link link) {
            return ImmutableSet.of(
                    new IntentTestsMocks.MockLinkResourceAllocations());
        }

        @Override
        public LinkResourceAllocations getAllocations(IntentId intentId) {
            return new IntentTestsMocks.MockLinkResourceAllocations();
        }

        @Override
        public Iterable<ResourceRequest> getAvailableResources(Link link) {
            final List<ResourceRequest> result = new LinkedList<>();
            if (availableBandwidth > 0.0) {
                result.add(new BandwidthResourceRequest(
                        new BandwidthResource(Bandwidth.bps(availableBandwidth))));
            }
            if (availableLambda > 0) {
                result.add(new LambdaResourceRequest());
            }
            return result;
        }

        @Override
        public Iterable<ResourceRequest> getAvailableResources(Link link, LinkResourceAllocations allocations) {
            return null;
        }

        @Override
        public void addListener(LinkResourceListener listener) {

        }

        @Override
        public void removeListener(LinkResourceListener listener) {

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
