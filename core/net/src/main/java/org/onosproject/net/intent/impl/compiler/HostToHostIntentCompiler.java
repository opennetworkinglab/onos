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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultLink;
import org.onosproject.net.DefaultPath;
import org.onosproject.net.DeviceId;
import org.onosproject.net.FilteredConnectPoint;
import org.onosproject.net.Host;
import org.onosproject.net.Link;
import org.onosproject.net.Path;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.host.HostService;
import org.onosproject.net.intent.HostToHostIntent;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentCompilationException;
import org.onosproject.net.intent.LinkCollectionIntent;
import org.onosproject.net.intent.constraint.AsymmetricPathConstraint;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.onosproject.net.Link.Type.EDGE;
import static org.onosproject.net.flow.DefaultTrafficSelector.builder;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * A intent compiler for {@link HostToHostIntent}.
 */
@Component(immediate = true)
public class HostToHostIntentCompiler
        extends ConnectivityIntentCompiler<HostToHostIntent> {

    private final Logger log = getLogger(getClass());

    private static final String DEVICE_ID_NOT_FOUND = "Didn't find device id in the link";

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected HostService hostService;

    @Activate
    public void activate() {
        intentManager.registerCompiler(HostToHostIntent.class, this);
    }

    @Deactivate
    public void deactivate() {
        intentManager.unregisterCompiler(HostToHostIntent.class);
    }

    @Override
    public List<Intent> compile(HostToHostIntent intent, List<Intent> installable) {
        // If source and destination are the same, there are never any installables.
        if (Objects.equals(intent.one(), intent.two())) {
            return ImmutableList.of();
        }

        boolean isAsymmetric = intent.constraints().contains(new AsymmetricPathConstraint());
        Path pathOne = getPathOrException(intent, intent.one(), intent.two());
        Path pathTwo = isAsymmetric ?
                getPathOrException(intent, intent.two(), intent.one()) : invertPath(pathOne);

        Host one = hostService.getHost(intent.one());
        Host two = hostService.getHost(intent.two());

        return Arrays.asList(createLinkCollectionIntent(pathOne, one, two, intent),
                             createLinkCollectionIntent(pathTwo, two, one, intent));
    }

    // Inverts the specified path. This makes an assumption that each link in
    // the path has a reverse link available. Under most circumstances, this
    // assumption will hold.
    private Path invertPath(Path path) {
        List<Link> reverseLinks = new ArrayList<>(path.links().size());
        for (Link link : path.links()) {
            reverseLinks.add(0, reverseLink(link));
        }
        return new DefaultPath(path.providerId(), reverseLinks, path.weight());
    }

    // Produces a reverse variant of the specified link.
    private Link reverseLink(Link link) {
        return DefaultLink.builder().providerId(link.providerId())
                .src(link.dst())
                .dst(link.src())
                .type(link.type())
                .state(link.state())
                .isExpected(link.isExpected())
                .build();
    }

    private FilteredConnectPoint getFilteredPointFromLink(Link link) {
        FilteredConnectPoint filteredConnectPoint;
        if (link.src().elementId() instanceof DeviceId) {
            filteredConnectPoint = new FilteredConnectPoint(link.src());
        } else if (link.dst().elementId() instanceof DeviceId) {
            filteredConnectPoint = new FilteredConnectPoint(link.dst());
        } else {
            throw new IntentCompilationException(DEVICE_ID_NOT_FOUND);
        }
        return filteredConnectPoint;
    }

    private Intent createLinkCollectionIntent(Path path,
                                             Host src,
                                             Host dst,
                                             HostToHostIntent intent) {
        // Try to allocate bandwidth
        List<ConnectPoint> pathCPs =
                path.links().stream()
                            .flatMap(l -> Stream.of(l.src(), l.dst()))
                            .collect(Collectors.toList());

        allocateBandwidth(intent, pathCPs);

        Link ingressLink = path.links().get(0);
        Link egressLink = path.links().get(path.links().size() - 1);

        FilteredConnectPoint ingressPoint = getFilteredPointFromLink(ingressLink);
        FilteredConnectPoint egressPoint = getFilteredPointFromLink(egressLink);

        TrafficSelector selector = builder(intent.selector())
                .matchEthSrc(src.mac())
                .matchEthDst(dst.mac())
                .build();

        /*
         * The path contains also the edge links, these are not necessary
         * for the LinkCollectionIntent.
         */
        Set<Link> coreLinks = path.links()
                .stream()
                .filter(link -> !link.type().equals(EDGE))
                .collect(Collectors.toSet());

        return LinkCollectionIntent.builder()
                .key(intent.key())
                .appId(intent.appId())
                .selector(selector)
                .treatment(intent.treatment())
                .links(coreLinks)
                .filteredIngressPoints(ImmutableSet.of(
                        ingressPoint
                ))
                .filteredEgressPoints(ImmutableSet.of(
                        egressPoint
                ))
                .applyTreatmentOnEgress(true)
                .constraints(intent.constraints())
                .priority(intent.priority())
                .resourceGroup(intent.resourceGroup())
                .build();
    }

}
