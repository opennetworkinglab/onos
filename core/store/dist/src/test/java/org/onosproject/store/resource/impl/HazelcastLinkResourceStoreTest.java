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
package org.onosproject.store.resource.impl;

import java.util.HashSet;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.Annotations;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DefaultLink;
import org.onosproject.net.Link;
import org.onosproject.net.intent.IntentId;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.net.resource.Bandwidth;
import org.onosproject.net.resource.BandwidthResourceAllocation;
import org.onosproject.net.resource.DefaultLinkResourceAllocations;
import org.onosproject.net.resource.DefaultLinkResourceRequest;
import org.onosproject.net.resource.Lambda;
import org.onosproject.net.resource.LambdaResourceAllocation;
import org.onosproject.net.resource.LinkResourceAllocations;
import org.onosproject.net.resource.LinkResourceRequest;
import org.onosproject.net.resource.LinkResourceStore;
import org.onosproject.net.resource.ResourceAllocation;
import org.onosproject.net.resource.ResourceAllocationException;
import org.onosproject.net.resource.ResourceType;
import org.onosproject.store.hz.StoreService;
import org.onosproject.store.hz.TestStoreManager;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.onosproject.net.DeviceId.deviceId;
import static org.onosproject.net.Link.Type.DIRECT;
import static org.onosproject.net.PortNumber.portNumber;

/**
 * Test of the simple LinkResourceStore implementation.
 */
public class HazelcastLinkResourceStoreTest {

    private LinkResourceStore store;
    private HazelcastLinkResourceStore storeImpl;
    private Link link1;
    private Link link2;
    private Link link3;
    private TestStoreManager storeMgr;

    /**
     * Returns {@link Link} object.
     *
     * @param dev1 source device
     * @param port1 source port
     * @param dev2 destination device
     * @param port2 destination port
     * @return created {@link Link} object
     */
    private Link newLink(String dev1, int port1, String dev2, int port2) {
        Annotations annotations = DefaultAnnotations.builder()
                .set(AnnotationKeys.OPTICAL_WAVES, "80")
                .set(AnnotationKeys.BANDWIDTH, "1000000")
                .build();
        return new DefaultLink(
                new ProviderId("of", "foo"),
                new ConnectPoint(deviceId(dev1), portNumber(port1)),
                new ConnectPoint(deviceId(dev2), portNumber(port2)),
                DIRECT, annotations);
    }

    @Before
    public void setUp() throws Exception {

        Config config = TestStoreManager.getTestConfig();

        storeMgr = new TestStoreManager(Hazelcast.newHazelcastInstance(config));
        storeMgr.activate();


        storeImpl = new TestHazelcastLinkResourceStore(storeMgr);
        storeImpl.activate();
        store = storeImpl;

        link1 = newLink("of:1", 1, "of:2", 2);
        link2 = newLink("of:2", 1, "of:3", 2);
        link3 = newLink("of:3", 1, "of:4", 2);
    }

    @After
    public void tearDown() throws Exception {
        storeImpl.deactivate();

        storeMgr.deactivate();
    }

    /**
     * Tests constructor and activate method.
     */
    @Test
    public void testConstructorAndActivate() {
        final Iterable<LinkResourceAllocations> allAllocations = store.getAllocations();
        assertNotNull(allAllocations);
        assertFalse(allAllocations.iterator().hasNext());

        final Iterable<LinkResourceAllocations> linkAllocations =
                store.getAllocations(link1);
        assertNotNull(linkAllocations);
        assertFalse(linkAllocations.iterator().hasNext());

        final Set<ResourceAllocation> res = store.getFreeResources(link2);
        assertNotNull(res);
    }

    /**
     * Picks up and returns one of bandwidth allocations from a given set.
     *
     * @param resources the set of {@link ResourceAllocation}s
     * @return {@link BandwidthResourceAllocation} object if found, null
     *         otherwise
     */
    private BandwidthResourceAllocation getBandwidthObj(Set<ResourceAllocation> resources) {
        for (ResourceAllocation res : resources) {
            if (res.type() == ResourceType.BANDWIDTH) {
                return ((BandwidthResourceAllocation) res);
            }
        }
        return null;
    }

    /**
     * Returns all lambda allocations from a given set.
     *
     * @param resources the set of {@link ResourceAllocation}s
     * @return a set of {@link LambdaResourceAllocation} objects
     */
    private Set<LambdaResourceAllocation> getLambdaObjs(Set<ResourceAllocation> resources) {
        Set<LambdaResourceAllocation> lambdaResources = new HashSet<>();
        for (ResourceAllocation res : resources) {
            if (res.type() == ResourceType.LAMBDA) {
                lambdaResources.add((LambdaResourceAllocation) res);
            }
        }
        return lambdaResources;
    }

    /**
     * Tests initial free bandwidth for a link.
     */
    @Test
    public void testInitialBandwidth() {
        final Set<ResourceAllocation> freeRes = store.getFreeResources(link1);
        assertNotNull(freeRes);

        final BandwidthResourceAllocation alloc = getBandwidthObj(freeRes);
        assertNotNull(alloc);

        assertEquals(Bandwidth.valueOf(1000000.0), alloc.bandwidth());
    }

    /**
     * Tests initial free lambda for a link.
     */
    @Test
    public void testInitialLambdas() {
        final Set<ResourceAllocation> freeRes = store.getFreeResources(link3);
        assertNotNull(freeRes);

        final Set<LambdaResourceAllocation> res = getLambdaObjs(freeRes);
        assertNotNull(res);
        assertEquals(80, res.size());
    }

    public static final class TestHazelcastLinkResourceStore
            extends HazelcastLinkResourceStore {

        public TestHazelcastLinkResourceStore(StoreService storeMgr) {
            super.storeService = storeMgr;
        }

    }

    /**
     * Tests a successful bandwidth allocation.
     */
    @Test
    public void testSuccessfulBandwidthAllocation() {
        final Link link = newLink("of:1", 1, "of:2", 2);

        final LinkResourceRequest request =
                DefaultLinkResourceRequest.builder(IntentId.valueOf(1),
                        ImmutableSet.of(link))
                .build();
        final ResourceAllocation allocation =
                new BandwidthResourceAllocation(Bandwidth.valueOf(900000.0));
        final Set<ResourceAllocation> allocationSet = ImmutableSet.of(allocation);

        final LinkResourceAllocations allocations =
                new DefaultLinkResourceAllocations(request, ImmutableMap.of(link, allocationSet));

        store.allocateResources(allocations);
    }

    /**
     * Tests a unsuccessful bandwidth allocation.
     */
    @Test
    public void testUnsuccessfulBandwidthAllocation() {
        final Link link = newLink("of:1", 1, "of:2", 2);

        final LinkResourceRequest request =
                DefaultLinkResourceRequest.builder(IntentId.valueOf(1),
                        ImmutableSet.of(link))
                        .build();
        final ResourceAllocation allocation =
                new BandwidthResourceAllocation(Bandwidth.valueOf(9000000.0));
        final Set<ResourceAllocation> allocationSet = ImmutableSet.of(allocation);

        final LinkResourceAllocations allocations =
                new DefaultLinkResourceAllocations(request, ImmutableMap.of(link, allocationSet));

        boolean gotException = false;
        try {
            store.allocateResources(allocations);
        } catch (ResourceAllocationException rae) {
            assertEquals(true, rae.getMessage().contains("Unable to allocate bandwidth for link"));
            gotException = true;
        }
        assertEquals(true, gotException);
    }

    /**
     * Tests a successful bandwidth allocation.
     */
    @Test
    public void testSuccessfulLambdaAllocation() {
        final Link link = newLink("of:1", 1, "of:2", 2);

        final LinkResourceRequest request =
                DefaultLinkResourceRequest.builder(IntentId.valueOf(1),
                        ImmutableSet.of(link))
                        .build();
        final ResourceAllocation allocation =
                new BandwidthResourceAllocation(Bandwidth.valueOf(900000.0));
        final Set<ResourceAllocation> allocationSet = ImmutableSet.of(allocation);

        final LinkResourceAllocations allocations =
                new DefaultLinkResourceAllocations(request, ImmutableMap.of(link, allocationSet));

        store.allocateResources(allocations);
    }

    /**
     * Tests a unsuccessful bandwidth allocation.
     */
    @Test
    public void testUnsuccessfulLambdaAllocation() {
        final Link link = newLink("of:1", 1, "of:2", 2);

        final LinkResourceRequest request =
                DefaultLinkResourceRequest.builder(IntentId.valueOf(1),
                        ImmutableSet.of(link))
                        .build();
        final ResourceAllocation allocation =
                new LambdaResourceAllocation(Lambda.valueOf(33));
        final Set<ResourceAllocation> allocationSet = ImmutableSet.of(allocation);

        final LinkResourceAllocations allocations =
                new DefaultLinkResourceAllocations(request, ImmutableMap.of(link, allocationSet));
        store.allocateResources(allocations);

        boolean gotException = false;
        try {
            store.allocateResources(allocations);
        } catch (ResourceAllocationException rae) {
            assertEquals(true, rae.getMessage().contains("Unable to allocate lambda for link"));
            gotException = true;
        }
        assertEquals(true, gotException);
    }
}
