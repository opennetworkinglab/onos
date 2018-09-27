/*
 * Copyright 2015-present Open Networking Foundation
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
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.Path;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentException;
import org.onosproject.net.intent.LinkCollectionIntent;
import org.onosproject.net.intent.MultiPointToSinglePointIntent;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.onosproject.net.intent.constraint.PartialFailureConstraint.intentAllowsPartialFailure;


/**
 * An intent compiler for
 * {@link org.onosproject.net.intent.MultiPointToSinglePointIntent}.
 */
@Component(immediate = true)
public class MultiPointToSinglePointIntentCompiler
        extends ConnectivityIntentCompiler<MultiPointToSinglePointIntent> {

    @Activate
    public void activate() {
        intentManager.registerCompiler(MultiPointToSinglePointIntent.class, this);
    }

    @Deactivate
    public void deactivate() {
        intentManager.unregisterCompiler(MultiPointToSinglePointIntent.class);
    }

    @Override
    public List<Intent> compile(MultiPointToSinglePointIntent intent, List<Intent> installable) {
        Map<DeviceId, Link> links = new HashMap<>();
        ConnectPoint egressPoint = intent.egressPoint();

        final boolean allowMissingPaths = intentAllowsPartialFailure(intent);
        boolean hasPaths = false;
        boolean missingSomePaths = false;

        for (ConnectPoint ingressPoint : intent.ingressPoints()) {
            if (ingressPoint.deviceId().equals(egressPoint.deviceId())) {
                if (deviceService.isAvailable(ingressPoint.deviceId())) {
                    hasPaths = true;
                } else {
                    missingSomePaths = true;
                }
                continue;
            }

            Path path = getPath(intent, ingressPoint.deviceId(), egressPoint.deviceId());

            if (path != null) {
                hasPaths = true;

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
                missingSomePaths = true;
            }
        }

        // Allocate bandwidth on existing paths if a bandwidth constraint is set
        List<ConnectPoint> ingressCPs =
                intent.filteredIngressPoints().stream()
                                              .map(fcp -> fcp.connectPoint())
                                              .collect(Collectors.toList());
        ConnectPoint egressCP = intent.filteredEgressPoint().connectPoint();

        List<ConnectPoint> pathCPs =
                links.values().stream()
                              .flatMap(l -> Stream.of(l.src(), l.dst()))
                              .collect(Collectors.toList());

        pathCPs.addAll(ingressCPs);
        pathCPs.add(egressCP);

        allocateBandwidth(intent, pathCPs);

        if (!hasPaths) {
            throw new IntentException("Cannot find any path between ingress and egress points.");
        } else if (!allowMissingPaths && missingSomePaths) {
            throw new IntentException("Missing some paths between ingress and egress points.");
        }

        Intent result = LinkCollectionIntent.builder()
                .appId(intent.appId())
                .key(intent.key())
                .treatment(intent.treatment())
                .selector(intent.selector())
                .links(Sets.newHashSet(links.values()))
                .filteredIngressPoints(intent.filteredIngressPoints())
                .filteredEgressPoints(ImmutableSet.of(intent.filteredEgressPoint()))
                .priority(intent.priority())
                .constraints(intent.constraints())
                .resourceGroup(intent.resourceGroup())
                .build();

        return Collections.singletonList(result);
    }
}
