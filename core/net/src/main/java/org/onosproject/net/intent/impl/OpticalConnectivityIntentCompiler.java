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
package org.onosproject.net.intent.impl;

import com.google.common.collect.ImmutableList;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Link;
import org.onosproject.net.Path;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentCompiler;
import org.onosproject.net.intent.IntentExtensionService;
import org.onosproject.net.intent.OpticalConnectivityIntent;
import org.onosproject.net.intent.OpticalPathIntent;
import org.onosproject.net.resource.LinkResourceAllocations;
import org.onosproject.net.topology.LinkWeight;
import org.onosproject.net.topology.Topology;
import org.onosproject.net.topology.TopologyEdge;
import org.onosproject.net.topology.TopologyService;

import java.util.List;
import java.util.Set;

/**
 * An intent compiler for {@link org.onosproject.net.intent.OpticalConnectivityIntent}.
 */
@Component(immediate = true)
public class OpticalConnectivityIntentCompiler implements IntentCompiler<OpticalConnectivityIntent> {

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected IntentExtensionService intentManager;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TopologyService topologyService;

    @Activate
    public void activate() {
        intentManager.registerCompiler(OpticalConnectivityIntent.class, this);
    }

    @Deactivate
    public void deactivate() {
        intentManager.unregisterCompiler(OpticalConnectivityIntent.class);
    }

    @Override
    public List<Intent> compile(OpticalConnectivityIntent intent,
                                List<Intent> installable,
                                Set<LinkResourceAllocations> resources) {
        // TODO: compute multiple paths using the K-shortest path algorithm
        Path path = calculateOpticalPath(intent.getSrc(), intent.getDst());
        Intent newIntent = new OpticalPathIntent(intent.appId(),
                                                 intent.getSrc(),
                                                 intent.getDst(),
                                                 path);
        return ImmutableList.of(newIntent);
    }

    private Path calculateOpticalPath(ConnectPoint start, ConnectPoint end) {
        // TODO: support user policies
        Topology topology = topologyService.currentTopology();
        LinkWeight weight = new LinkWeight() {
            @Override
            public double weight(TopologyEdge edge) {
                if (edge.link().state() == Link.State.INACTIVE) {
                    return -1;
                }
                return edge.link().type() == Link.Type.OPTICAL ? +1 : -1;
            }
        };

        Set<Path> paths = topologyService.getPaths(topology, start.deviceId(),
                                                   end.deviceId(), weight);
        if (paths.isEmpty()) {
            throw new PathNotFoundException(start.elementId(), end.elementId());
        }

        // TODO: let's be more intelligent about this eventually
        return paths.iterator().next();
    }

}
