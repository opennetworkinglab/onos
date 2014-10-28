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
package org.onlab.onos.net.resource.impl;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.onlab.onos.net.Link;
import org.onlab.onos.net.intent.IntentId;
import org.onlab.onos.net.resource.BandwidthResourceAllocation;
import org.onlab.onos.net.resource.BandwidthResourceRequest;
import org.onlab.onos.net.resource.Lambda;
import org.onlab.onos.net.resource.LambdaResourceAllocation;
import org.onlab.onos.net.resource.LinkResourceAllocations;
import org.onlab.onos.net.resource.LinkResourceRequest;
import org.onlab.onos.net.resource.LinkResourceService;
import org.onlab.onos.net.resource.ResourceAllocation;
import org.onlab.onos.net.resource.ResourceRequest;
import org.slf4j.Logger;

import com.google.common.collect.Sets;

/**
 * Provides basic implementation of link resources allocation.
 */
@Component(immediate = true)
@Service
public class LinkResourceManager implements LinkResourceService {

    private final Logger log = getLogger(getClass());

    @Activate
    public void activate() {
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }

    private Iterable<Lambda> getAvailableLambdas(Iterable<Link> links) {
        return Sets.newHashSet(Lambda.valueOf(7));
    }

    @Override
    public LinkResourceAllocations requestResources(LinkResourceRequest req) {
        // TODO implement it using a resource data store.

        ResourceAllocation alloc = null;
        for (ResourceRequest r : req.resources()) {
            switch (r.type()) {
            case BANDWIDTH:
                log.info("requestResources() always returns requested bandwidth");
                BandwidthResourceRequest br = (BandwidthResourceRequest) r;
                alloc = new BandwidthResourceAllocation(br.bandwidth());
                break;
            case LAMBDA:
                log.info("requestResources() always returns lambda 7");
                Iterator<Lambda> lambdaIterator = getAvailableLambdas(req.links()).iterator();
                if (lambdaIterator.hasNext()) {
                    alloc = new LambdaResourceAllocation(lambdaIterator.next());
                }
                break;
            default:
                break;
            }
        }

        Map<Link, Set<ResourceAllocation>> allocations = new HashMap<>();
        for (Link link : req.links()) {
            allocations.put(link, Sets.newHashSet(alloc));
        }
        return new DefaultLinkResourceAllocations(req, allocations);
    }

    @Override
    public void releaseResources(LinkResourceAllocations allocations) {
        // TODO Auto-generated method stub

    }

    @Override
    public LinkResourceAllocations updateResources(LinkResourceRequest req,
                                                   LinkResourceAllocations oldAllocations) {
        return null;
    }

    @Override
    public Iterable<LinkResourceAllocations> getAllocations() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Iterable<LinkResourceAllocations> getAllocations(Link link) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public LinkResourceAllocations getAllocations(IntentId intentId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Iterable<ResourceRequest> getAvailableResources(Link link) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResourceRequest getAvailableResources(Link link,
                                                 LinkResourceAllocations allocations) {
        return null;
    }

}
