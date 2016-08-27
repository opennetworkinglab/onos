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
package org.onosproject.net.intent.impl.compiler;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.Path;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentCompiler;
import org.onosproject.net.intent.IntentException;
import org.onosproject.net.intent.IntentExtensionService;
import org.onosproject.net.intent.LinkCollectionIntent;
import org.onosproject.net.intent.MultiPointToSinglePointIntent;
import org.onosproject.net.intent.PointToPointIntent;
import org.onosproject.net.topology.PathService;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.onosproject.net.intent.constraint.PartialFailureConstraint.intentAllowsPartialFailure;


/**
 * An intent compiler for
 * {@link org.onosproject.net.intent.MultiPointToSinglePointIntent}.
 */
@Component(immediate = true)
public class MultiPointToSinglePointIntentCompiler
        implements IntentCompiler<MultiPointToSinglePointIntent> {

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected IntentExtensionService intentManager;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PathService pathService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Activate
    public void activate() {
        intentManager.registerCompiler(MultiPointToSinglePointIntent.class, this);
    }

    @Deactivate
    public void deactivate() {
        intentManager.unregisterCompiler(PointToPointIntent.class);
    }

    @Override
    public List<Intent> compile(MultiPointToSinglePointIntent intent, List<Intent> installable) {
        Map<DeviceId, Link> links = new HashMap<>();
        ConnectPoint egressPoint = intent.egressPoint();

        final boolean allowMissingPaths = intentAllowsPartialFailure(intent);
        boolean partialTree = false;
        boolean anyMissingPaths = false;
        for (ConnectPoint ingressPoint : intent.ingressPoints()) {
            if (ingressPoint.deviceId().equals(egressPoint.deviceId())) {
                if (deviceService.isAvailable(ingressPoint.deviceId())) {
                    partialTree = true;
                } else {
                    anyMissingPaths = true;
                }

                continue;
            }

            Path path = getPath(ingressPoint, intent.egressPoint());
            if (path != null) {
                partialTree = true;

                for (Link link : path.links()) {
                    if (links.containsKey(link.dst().deviceId())) {
                        // We've already reached the existing tree with the first
                        // part of this path. Add the merging point with different
                        // incoming port, but don't add the remainder of the path
                        // in case it differs from the path we already have.
                        links.put(link.src().deviceId(), link);
                        break;
                    }
                    links.put(link.src().deviceId(), link);
                }
            } else {
                anyMissingPaths = true;
            }
        }

        if (!partialTree) {
            throw new IntentException("Could not find any paths between ingress and egress points.");
        } else if (!allowMissingPaths && anyMissingPaths) {
            throw new IntentException("Missing some paths between ingress and egress ports.");
        }

        Intent result = LinkCollectionIntent.builder()
                .appId(intent.appId())
                .selector(intent.selector())
                .treatment(intent.treatment())
                .links(Sets.newHashSet(links.values()))
                .ingressPoints(intent.ingressPoints())
                .egressPoints(ImmutableSet.of(intent.egressPoint()))
                .ingressSelectors(intent.ingressSelectors())
                .priority(intent.priority())
                .constraints(intent.constraints())
                .build();

        return Collections.singletonList(result);
    }

    /**
     * Computes a path between two ConnectPoints.
     *
     * @param one start of the path
     * @param two end of the path
     * @return Path between the two
     */
    private Path getPath(ConnectPoint one, ConnectPoint two) {
        Set<Path> paths = pathService.getPaths(one.deviceId(), two.deviceId());
        if (paths.isEmpty()) {
            return null;
        }
        // TODO: let's be more intelligent about this eventually
        return paths.iterator().next();
    }
}
