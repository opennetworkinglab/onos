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
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Link;
import org.onosproject.net.Path;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentException;
import org.onosproject.net.intent.LinkCollectionIntent;
import org.onosproject.net.intent.SinglePointToMultiPointIntent;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.onosproject.net.intent.constraint.PartialFailureConstraint.intentAllowsPartialFailure;

@Component(immediate = true)
public class SinglePointToMultiPointIntentCompiler
        extends ConnectivityIntentCompiler<SinglePointToMultiPointIntent> {

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Activate
    public void activate() {
        intentManager.registerCompiler(SinglePointToMultiPointIntent.class, this);
    }

    @Deactivate
    public void deactivate() {
        intentManager.unregisterCompiler(SinglePointToMultiPointIntent.class);
    }

    @Override
    public List<Intent> compile(SinglePointToMultiPointIntent intent,
                                List<Intent> installable) {
        Set<Link> links = new HashSet<>();

        final boolean allowMissingPaths = intentAllowsPartialFailure(intent);
        boolean hasPaths = false;
        boolean missingSomePaths = false;

        for (ConnectPoint egressPoint : intent.egressPoints()) {
            if (egressPoint.deviceId().equals(intent.ingressPoint().deviceId())) {
                // Do not need to look for paths, since ingress and egress
                // devices are the same.
                if (deviceService.isAvailable(egressPoint.deviceId())) {
                    hasPaths = true;
                } else {
                    missingSomePaths = true;
                }
                continue;
            }

            Path path = getPath(intent, intent.ingressPoint().deviceId(), egressPoint.deviceId());

            if (path != null) {
                hasPaths = true;
                links.addAll(path.links());
            } else {
                missingSomePaths = true;
            }
        }

        // Allocate bandwidth if a bandwidth constraint is set
        ConnectPoint ingressCP = intent.filteredIngressPoint().connectPoint();
        List<ConnectPoint> egressCPs =
                intent.filteredEgressPoints().stream()
                        .map(fcp -> fcp.connectPoint())
                        .collect(Collectors.toList());

        List<ConnectPoint> pathCPs =
                links.stream()
                     .flatMap(l -> Stream.of(l.src(), l.dst()))
                     .collect(Collectors.toList());

        pathCPs.add(ingressCP);
        pathCPs.addAll(egressCPs);

        allocateBandwidth(intent, pathCPs);

        if (!hasPaths) {
            throw new IntentException("Cannot find any path between ingress and egress points.");
        } else if (!allowMissingPaths && missingSomePaths) {
            throw new IntentException("Missing some paths between ingress and egress points.");
        }

        Intent result = LinkCollectionIntent.builder()
                .appId(intent.appId())
                .key(intent.key())
                .selector(intent.selector())
                .treatment(intent.treatment())
                .links(links)
                .filteredIngressPoints(ImmutableSet.of(intent.filteredIngressPoint()))
                .filteredEgressPoints(intent.filteredEgressPoints())
                .priority(intent.priority())
                .applyTreatmentOnEgress(true)
                .constraints(intent.constraints())
                .resourceGroup(intent.resourceGroup())
                .build();

        return Collections.singletonList(result);
    }
}