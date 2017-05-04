/*
 * Copyright 2016-present Open Networking Laboratory
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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Lists.transform;
import static java.util.stream.Stream.concat;
import static org.onosproject.net.MarkerResource.marker;
import static org.onosproject.net.behaviour.protection.ProtectedTransportEndpointDescription.buildDescription;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.packet.VlanId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultLink;
import org.onosproject.net.DefaultPath;
import org.onosproject.net.DeviceId;
import org.onosproject.net.DisjointPath;
import org.onosproject.net.FilteredConnectPoint;
import org.onosproject.net.Link;
import org.onosproject.net.Link.State;
import org.onosproject.net.NetworkResource;
import org.onosproject.net.Path;
import org.onosproject.net.behaviour.protection.TransportEndpointDescription;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentCompilationException;
import org.onosproject.net.intent.LinkCollectionIntent;
import org.onosproject.net.intent.ProtectedTransportIntent;
import org.onosproject.net.intent.ProtectionEndpointIntent;
import org.onosproject.net.resource.DiscreteResourceId;
import org.onosproject.net.resource.Resource;
import org.onosproject.net.resource.ResourceService;
import org.onosproject.net.resource.Resources;
import org.slf4j.Logger;

import com.google.common.annotations.Beta;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * IntentCompiler for {@link ProtectedTransportIntent}.
 */
@Beta
@Component(immediate = true)
public class ProtectedTransportIntentCompiler
        extends ConnectivityIntentCompiler<ProtectedTransportIntent> {

    /**
     * Marker value for forward path.
     */
    private static final String FWD = "fwd";

    /**
     * Marker value for reverse path.
     */
    private static final String REV = "rev";


    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ResourceService resourceService;

    @Activate
    public void activate() {
        intentManager.registerCompiler(ProtectedTransportIntent.class, this);
        log.info("started");
    }

    @Deactivate
    public void deactivate() {
        intentManager.unregisterCompiler(ProtectedTransportIntent.class);
        log.info("stopped");
    }

    @Override
    public List<Intent> compile(ProtectedTransportIntent intent,
                                List<Intent> installable) {
        log.trace("compiling {} {}", intent, installable);

        // case 0 hop, same device
        final DeviceId did1 = intent.one();
        final DeviceId did2 = intent.two();
        if (Objects.equals(did1, did2)) {
            // Doesn't really make sense to create 0 hop protected path, but
            // can generate Flow for the device, just to provide connectivity.
            // future work.
            log.error("0 hop not supported yet.");
            throw new IntentCompilationException("0 hop not supported yet.");
        }

        List<Intent> reusable = Optional.ofNullable(installable).orElse(ImmutableList.of())
            .stream()
            .filter(this::isIntact)
            .collect(Collectors.toList());
        if (reusable.isEmpty() ||
            reusable.stream().allMatch(ProtectionEndpointIntent.class::isInstance)) {
            // case provisioning new protected path
            //   or
            // case re-compilation (total failure -> restoration)
            return createFreshProtectedPaths(intent, did1, did2);
        } else {
            // case re-compilation (partial failure)
            log.warn("Re-computing adding new backup path not supported yet. No-Op.");
            // TODO This part needs to be flexible to support various use case
            // - non-revertive behavior (Similar to PartialFailureConstraint)
            // - revertive behavior
            // - compute third path
            // ...
            //  Require further input what they actually need.

            // TODO handle PartialFailureConstraint

            /// case only need to update transit portion
            /// case head and/or tail needs to be updated

            // TODO do we need to prune broken
            return installable;
        }
    }

    /**
     * Test if resources used by specified Intent is intact.
     *
     * @param installed Intent to test
     * @return true if Intent is intact
     */
    private boolean isIntact(Intent installed) {
        return installed.resources().stream()
            .filter(Link.class::isInstance)
            .map(Link.class::cast)
            .allMatch(this::isLive);
    }

    /**
     * Test if specified Link is intact.
     *
     * @param link to test
     * @return true if link is intact
     */
    private boolean isLive(Link link) {
        // Only testing link state for now
        // in the long run, consider verifying OAM state on ports
        return link.state() != State.INACTIVE;
    }

    /**
     * Creates new protected paths.
     *
     * @param intent    original intention
     * @param did1      identifier of first device
     * @param did2      identifier of second device
     * @return compilation result
     * @throws IntentCompilationException when there's no satisfying path.
     */
    private List<Intent> createFreshProtectedPaths(ProtectedTransportIntent intent,
                                                   DeviceId did1,
                                                   DeviceId did2) {
        DisjointPath disjointPath = getDisjointPath(intent, did1, did2);
        if (disjointPath == null || disjointPath.backup() == null) {
            log.error("Unable to find disjoint path between {}, {}", did1, did2);
            throw new IntentCompilationException("Unable to find disjoint paths.");
        }
        Path primary = disjointPath.primary();
        Path secondary = disjointPath.backup();

        String fingerprint = intent.key().toString();

        // pick and allocate Vlan to use as S-tag
        Pair<VlanId, VlanId> vlans = allocateEach(intent, primary, secondary, VlanId.class);

        VlanId primaryVlan = vlans.getLeft();
        VlanId secondaryVlan = vlans.getRight();

        // Build edge Intents for head/tail

        // resource for head/tail
        Collection<NetworkResource> oneResources = new ArrayList<>();
        Collection<NetworkResource> twoResources = new ArrayList<>();

        List<TransportEndpointDescription> onePaths = new ArrayList<>();
        onePaths.add(TransportEndpointDescription.builder()
                         .withOutput(vlanPort(primary.src(), primaryVlan))
                         .build());
        onePaths.add(TransportEndpointDescription.builder()
                         .withOutput(vlanPort(secondary.src(), secondaryVlan))
                         .build());

        List<TransportEndpointDescription> twoPaths = new ArrayList<>();
        twoPaths.add(TransportEndpointDescription.builder()
                     .withOutput(vlanPort(primary.dst(), primaryVlan))
                     .build());
        twoPaths.add(TransportEndpointDescription.builder()
                     .withOutput(vlanPort(secondary.dst(), secondaryVlan))
                     .build());

        ProtectionEndpointIntent oneIntent = ProtectionEndpointIntent.builder()
                .key(intent.key())
                .appId(intent.appId())
                .priority(intent.priority())
                .resources(oneResources)
                .deviceId(did1)
                .description(buildDescription(onePaths, did2, fingerprint))
                .build();
        ProtectionEndpointIntent twoIntent = ProtectionEndpointIntent.builder()
                .key(intent.key())
                .appId(intent.appId())
                .resources(twoResources)
                .deviceId(did2)
                .description(buildDescription(twoPaths, did1, fingerprint))
                .build();

        // Build transit intent for primary/secondary path

        Collection<NetworkResource> resources1 = ImmutableList.of(marker("protection1"));
        Collection<NetworkResource> resources2 = ImmutableList.of(marker("protection2"));

        ImmutableList<Intent> result = ImmutableList.<Intent>builder()
                // LinkCollection for primary and backup paths
                .addAll(createTransitIntent(intent, primary, primaryVlan, resources1))
                .addAll(createTransitIntent(intent, secondary, secondaryVlan, resources2))
                .add(oneIntent)
                .add(twoIntent)
                .build();
        log.trace("createFreshProtectedPaths result: {}", result);
        return result;
    }

    /**
     * Creates required Intents required to transit bi-directionally the network.
     *
     * @param intent parent IntentId
     * @param path whole path
     * @param vid VlanId to use as tunnel labels
     * @param resources to be passed down to generated Intents
     * @return List on transit Intents, if any is required.
     */
    List<LinkCollectionIntent> createTransitIntent(Intent intent,
                                                   Path path,
                                                   VlanId vid,
                                                   Collection<NetworkResource> resources) {
        if (path.links().size() <= 1) {
            // There's no need for transit Intents
            return ImmutableList.of();
        }

        Collection<NetworkResource> fwd = ImmutableList.<NetworkResource>builder()
                                            .addAll(resources)
                                            .add(marker(FWD))
                                            .build();
        Collection<NetworkResource> rev = ImmutableList.<NetworkResource>builder()
                                            .addAll(resources)
                                            .add(marker(REV))
                                            .build();

        return ImmutableList.of(createSubTransitIntent(intent, path, vid, fwd),
                                createSubTransitIntent(intent, reverse(path), vid, rev));
    }

    /**
     * Returns a path in reverse direction.
     *
     * @param path to reverse
     * @return reversed path
     */
    Path reverse(Path path) {
        List<Link> revLinks = Lists.reverse(transform(path.links(), this::reverse));
        return new DefaultPath(path.providerId(),
                               revLinks,
                               path.cost(),
                               path.annotations());
    }

    // TODO consider adding equivalent to Link/DefaultLink.
    /**
     * Returns a link in reverse direction.
     *
     * @param link to revese
     * @return reversed link
     */
    Link reverse(Link link) {
        return DefaultLink.builder()
                .providerId(link.providerId())
                .src(link.dst())
                .dst(link.src())
                .type(link.type())
                .state(link.state())
                .isExpected(link.isExpected())
                .annotations(link.annotations())
                .build();
    }

    /**
     * Creates required Intents required to transit uni-directionally along the Path.
     *
     * @param intent parent IntentId
     * @param path whole path
     * @param vid VlanId to use as tunnel labels
     * @param resources to be passed down to generated Intents
     * @return List of transit Intents, if any is required.
     */
    LinkCollectionIntent createSubTransitIntent(Intent intent,
                                                Path path,
                                                VlanId vid,
                                                Collection<NetworkResource> resources) {
        checkArgument(path.links().size() > 1);

        // transit ingress/egress
        ConnectPoint one = path.links().get(0).dst();
        ConnectPoint two = path.links().get(path.links().size() - 1).src();

        return LinkCollectionIntent.builder()
                    // TODO there should probably be .parent(intent)
                    // which copies key, appId, priority, ...
                    .key(intent.key())
                    .appId(intent.appId())
                    .priority(intent.priority())
                    //.constraints(intent.constraints())
                    // VLAN tunnel
                    //.selector(DefaultTrafficSelector.builder().matchVlanId(vid).build())
                    //.treatment(intent.treatment())
                    .resources(resources)
                    .links(ImmutableSet.copyOf(path.links()))
                    .filteredIngressPoints(ImmutableSet.of(vlanPort(one, vid)))
                    .filteredEgressPoints(ImmutableSet.of(vlanPort(two, vid)))
                    // magic flag required for p2p type
                    .applyTreatmentOnEgress(true)
                    .cost(path.cost())
                    .build();
    }

    /**
     * Creates VLAN filtered-ConnectPoint.
     *
     * @param cp  ConnectPoint
     * @param vid VLAN ID
     * @return filtered-ConnectPoint
     */
    static FilteredConnectPoint vlanPort(ConnectPoint cp, VlanId vid) {
        return new FilteredConnectPoint(cp, DefaultTrafficSelector.builder()
                                        .matchVlanId(vid)
                                        .build());
    }

    /**
     * Creates ResourceId for a port.
     *
     * @param cp ConnectPoint
     * @return ResourceId
     */
    static DiscreteResourceId resourceId(ConnectPoint cp) {
        return Resources.discrete(cp.deviceId(), cp.port()).id();
    }

    /**
     * Allocate resource for each {@link Path}s.
     *
     * @param intent to allocate resource to
     * @param primary path
     * @param secondary path
     * @param klass label resource class
     * @return Pair of chosen resource (primary, secondary)
     * @param <T> label resource type
     * @throws IntentCompilationException when there is no resource available
     */
    <T> Pair<T, T> allocateEach(Intent intent, Path primary, Path secondary, Class<T> klass) {
        log.trace("allocateEach({}, {}, {}, {})", intent, primary, secondary, klass);
        Pair<T, T> vlans = null;
        do {
            Set<T> primaryVlans = commonLabelResource(primary, klass);
            Set<T> secondaryVlans = commonLabelResource(secondary, klass);
            Pair<T, T> candidates = pickEach(primaryVlans, secondaryVlans);
            T primaryT = candidates.getLeft();
            T secondaryT = candidates.getRight();

            // try to allocate candidates along each path
            Stream<Resource> primaryResources = primary.links().stream()
                    .flatMap(link -> Stream.of(link.src(), link.dst()))
                    .distinct()
                    .map(cp -> Resources.discrete(resourceId(cp), primaryT).resource());
            Stream<Resource> secondaryResources = secondary.links().stream()
                    .flatMap(link -> Stream.of(link.src(), link.dst()))
                    .distinct()
                    .map(cp -> Resources.discrete(resourceId(cp), secondaryT).resource());

            List<Resource> resources = concat(primaryResources, secondaryResources)
                                        .collect(Collectors.toList());
            log.trace("Calling allocate({},{})", intent.key(), resources);
            if (resourceService.allocate(intent.key(), resources).isEmpty()) {
                log.warn("Allocation failed, retrying");
                continue;
            }
            vlans = candidates;
        } while (false);
        log.trace("allocation done.");
        return vlans;
    }

    /**
     * Randomly pick one resource from candidates.
     *
     * @param set of candidates
     * @return chosen one
     * @param <T> label resource type
     */
    <T> T pickOne(Set<T> set) {
        // Note: Set returned by commonLabelResource(..) assures,
        // there is at least one element.

        // FIXME more reasonable selection logic
        return Iterables.get(set, RandomUtils.nextInt(0, set.size()));
    }

    /**
     * Select resource from available Resources.
     *
     * @param primary   Set of resource to pick from
     * @param secondary Set of resource to pick from
     * @return Pair of chosen resource (primary, secondary)
     * @param <T> label resource type
     */
    <T> Pair<T, T> pickEach(Set<T> primary, Set<T> secondary) {
        Set<T> intersection = Sets.intersection(primary, secondary);

        if (!intersection.isEmpty()) {
            // favor common
            T picked = pickOne(intersection);
            return Pair.of(picked, picked);
        }

        T pickedP = pickOne(primary);
        T pickedS = pickOne(secondary);
        return Pair.of(pickedP, pickedS);
    }

    /**
     * Finds label resource, which can be used in common along the path.
     *
     * @param path path
     * @param klass Label class
     * @return Set of common resources
     * @throws IntentCompilationException when there is no resource available
     * @param <T> label resource type
     */
    <T> Set<T> commonLabelResource(Path path, Class<T> klass) {
         Optional<Set<T>> common = path.links().stream()
            .flatMap(link -> Stream.of(link.src(), link.dst()))
            .distinct()
            .map(cp -> getAvailableResourceValues(cp, klass))
            .reduce(Sets::intersection);

         if (!common.isPresent() || common.get().isEmpty()) {
             log.error("No common label available for: {}", path);
             throw new IntentCompilationException("No common label available for: " + path);
         }
         return common.get();
    }

    <T> Set<T> getAvailableResourceValues(ConnectPoint cp, Class<T> klass) {
        return resourceService.getAvailableResourceValues(
                                 resourceId(cp),
                                 klass);
    }

}
