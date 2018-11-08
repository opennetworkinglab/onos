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
import org.apache.commons.lang3.tuple.Pair;
import org.onlab.graph.ScalarWeight;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultPath;
import org.onosproject.net.DeviceId;
import org.onosproject.net.DisjointPath;
import org.onosproject.net.EdgeLink;
import org.onosproject.net.Link;
import org.onosproject.net.Path;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.group.DefaultGroupBucket;
import org.onosproject.net.group.DefaultGroupDescription;
import org.onosproject.net.group.DefaultGroupKey;
import org.onosproject.net.group.Group;
import org.onosproject.net.group.GroupBucket;
import org.onosproject.net.group.GroupBuckets;
import org.onosproject.net.group.GroupDescription;
import org.onosproject.net.group.GroupKey;
import org.onosproject.net.group.GroupListener;
import org.onosproject.net.group.GroupService;
import org.onosproject.net.intent.FlowRuleIntent;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentCompilationException;
import org.onosproject.net.intent.IntentId;
import org.onosproject.net.intent.LinkCollectionIntent;
import org.onosproject.net.intent.PathIntent;
import org.onosproject.net.intent.PointToPointIntent;
import org.onosproject.net.intent.constraint.ProtectionConstraint;
import org.onosproject.net.intent.impl.PathNotFoundException;
import org.onosproject.net.link.LinkService;
import org.onosproject.net.provider.ProviderId;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static org.onosproject.net.DefaultEdgeLink.createEdgeLink;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * An intent compiler for {@link org.onosproject.net.intent.PointToPointIntent}.
 */
@Component(immediate = true)
public class PointToPointIntentCompiler
        extends ConnectivityIntentCompiler<PointToPointIntent> {

    // TODO: use off-the-shell core provider ID
    private static final ProviderId PID =
            new ProviderId("core", "org.onosproject.core", true);
    // TODO: consider whether the default cost is appropriate or not
    public static final int DEFAULT_COST = 1;

    protected static final int PRIORITY = Intent.DEFAULT_INTENT_PRIORITY;

    private static final int GROUP_TIMEOUT = 5;

    private final Logger log = getLogger(getClass());

    protected boolean erasePrimary = false;
    protected boolean eraseBackup = false;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected GroupService groupService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected LinkService linkService;

    @Activate
    public void activate() {
        intentManager.registerCompiler(PointToPointIntent.class, this);
    }

    @Deactivate
    public void deactivate() {
        intentManager.unregisterCompiler(PointToPointIntent.class);
    }

    @Override
    public List<Intent> compile(PointToPointIntent intent, List<Intent> installable) {
        log.trace("compiling {} {}", intent, installable);
        ConnectPoint ingressPoint = intent.filteredIngressPoint().connectPoint();
        ConnectPoint egressPoint = intent.filteredEgressPoint().connectPoint();

        //TODO: handle protected path case with suggested path!!
        //Idea: use suggested path as primary and another path from path service as protection
        if (intent.suggestedPath() != null && intent.suggestedPath().size() > 0) {
            Path path = new DefaultPath(PID, intent.suggestedPath(), new ScalarWeight(1));
            //Check intent constraints against suggested path and suggested path availability
            if (checkPath(path, intent.constraints()) && pathAvailable(intent)) {
                allocateIntentBandwidth(intent, path);
                return asList(createLinkCollectionIntent(ImmutableSet.copyOf(intent.suggestedPath()),
                                                         DEFAULT_COST, intent));
            }
        }

        if (ingressPoint.deviceId().equals(egressPoint.deviceId())) {
            return createZeroHopLinkCollectionIntent(intent);
        }

        // proceed with no protected paths
        if (!ProtectionConstraint.requireProtectedPath(intent)) {
            return createUnprotectedLinkCollectionIntent(intent);
        }

        try {
            // attempt to compute and implement backup path
            return createProtectedIntent(ingressPoint, egressPoint, intent, installable);
        } catch (PathNotFoundException e) {
            log.warn("Could not find disjoint Path for {}", intent);
            // no disjoint path extant -- maximum one path exists between devices
            return createSinglePathIntent(ingressPoint, egressPoint, intent, installable);
        }
    }

    private void allocateIntentBandwidth(PointToPointIntent intent, Path path) {
        ConnectPoint ingressCP = intent.filteredIngressPoint().connectPoint();
        ConnectPoint egressCP = intent.filteredEgressPoint().connectPoint();

        List<ConnectPoint> pathCPs =
                path.links().stream()
                        .flatMap(l -> Stream.of(l.src(), l.dst()))
                        .collect(Collectors.toList());

        pathCPs.add(ingressCP);
        pathCPs.add(egressCP);

        allocateBandwidth(intent, pathCPs);
    }

    private List<Intent> createZeroHopIntent(ConnectPoint ingressPoint,
                                             ConnectPoint egressPoint,
                                             PointToPointIntent intent) {
        List<Link> links = asList(createEdgeLink(ingressPoint, true), createEdgeLink(egressPoint, false));
        return asList(createPathIntent(new DefaultPath(PID, links, ScalarWeight.toWeight(DEFAULT_COST)),
                                       intent, PathIntent.ProtectionType.PRIMARY));
    }

    private List<Intent> createZeroHopLinkCollectionIntent(PointToPointIntent intent) {
        return asList(createLinkCollectionIntent(ImmutableSet.of(), DEFAULT_COST,
                                       intent));
    }

    private List<Intent> createUnprotectedLinkCollectionIntent(PointToPointIntent intent) {
        Path path = getPathOrException(intent, intent.filteredIngressPoint().connectPoint().deviceId(),
                                       intent.filteredEgressPoint().connectPoint().deviceId());

        // Allocate bandwidth if a bandwidth constraint is set
        allocateIntentBandwidth(intent, path);

        return asList(createLinkCollectionIntent(ImmutableSet.copyOf(path.links()),
                                                 path.cost(),
                                                 intent));
    }

    //FIXME: Compatibility with EncapsulationConstraint
    private List<Intent> createProtectedIntent(ConnectPoint ingressPoint,
                                               ConnectPoint egressPoint,
                                               PointToPointIntent intent,
                                               List<Intent> installable) {
        log.trace("createProtectedIntent");
        DisjointPath path = getDisjointPath(intent, ingressPoint.deviceId(),
                                            egressPoint.deviceId());

        List<Intent> reusableIntents = null;
        if (installable != null) {
            reusableIntents = filterInvalidSubIntents(installable, intent);
            if (reusableIntents.size() == installable.size()) {
                // all old paths are still viable
                return installable;
            }
        }

        List<Intent> intentList = new ArrayList<>();

        // primary path intent
        List<Link> links = new ArrayList<>();
        links.addAll(path.links());
        links.add(createEdgeLink(egressPoint, false));

        // backup path intent
        List<Link> backupLinks = new ArrayList<>();
        backupLinks.addAll(path.backup().links());
        backupLinks.add(createEdgeLink(egressPoint, false));

        /*
         * One of the old paths is still entirely intact. This old path has
         * already been made primary, so we must add a backup path intent
         * and modify the failover group treatment accordingly.
         */
        if (reusableIntents != null && reusableIntents.size() > 1) {
            /*
             * Ensures that the egress port on source device is different than
             * that of existing path so that failover group will be useful
             * (would not be useful if both output ports in group bucket were
             * the same). Does not necessarily ensure that the new backup path
             * is entirely disjoint from the old path.
             */
            PortNumber primaryPort = getPrimaryPort(intent);
            if (primaryPort != null && !links.get(0).src().port().equals(primaryPort)) {
                reusableIntents.add(createPathIntent(new DefaultPath(PID, links,
                                                     path.weight(), path.annotations()),
                                                     intent, PathIntent.ProtectionType.BACKUP));
                updateFailoverGroup(intent, links);
                return reusableIntents;

            } else {
                reusableIntents.add(createPathIntent(new DefaultPath(PID, backupLinks,
                        path.backup().weight(),
                        path.backup().annotations()), intent, PathIntent.ProtectionType.BACKUP));
                updateFailoverGroup(intent, backupLinks);
                return reusableIntents;
            }
        }

        intentList.add(createPathIntent(new DefaultPath(PID, links, path.weight(),
                                                        path.annotations()),
                                        intent, PathIntent.ProtectionType.PRIMARY));
        intentList.add(createPathIntent(new DefaultPath(PID, backupLinks, path.backup().weight(),
                                                        path.backup().annotations()),
                                        intent, PathIntent.ProtectionType.BACKUP));

        // Create fast failover flow rule intent or, if it already exists,
        // add contents appropriately.
        if (groupService.getGroup(ingressPoint.deviceId(),
                                  makeGroupKey(intent.id())) == null) {
            // manufactured fast failover flow rule intent
            createFailoverTreatmentGroup(path.links(), path.backup().links(), intent);

            FlowRuleIntent frIntent = new FlowRuleIntent(intent.appId(),
                                                         intent.key(),
                                                         createFailoverFlowRules(intent),
                                                         asList(ingressPoint.deviceId()),
                                                         PathIntent.ProtectionType.FAILOVER,
                                                         intent.resourceGroup());
            intentList.add(frIntent);
        } else {
            updateFailoverGroup(intent, links);
            updateFailoverGroup(intent, backupLinks);
        }

        return intentList;
    }

    private List<Intent> createSinglePathIntent(ConnectPoint ingressPoint,
                                                ConnectPoint egressPoint,
                                                PointToPointIntent intent,
                                                List<Intent> installable) {
        List<Link> links = new ArrayList<>();
        Path onlyPath = getPathOrException(intent, ingressPoint.deviceId(),
                                           egressPoint.deviceId());

        List<Intent> reusableIntents = null;
        if (installable != null) {
            reusableIntents = filterInvalidSubIntents(installable, intent);
            if (reusableIntents.size() == installable.size()) {
                // all old paths are still viable
                return installable;
            }
        }

        // If there exists a full path from old installable intents,
        // return the intents that comprise it.
        if (reusableIntents != null && reusableIntents.size() > 1) {
            return reusableIntents;
        } else {
            // Allocate bandwidth if a bandwidth constraint is set
            allocateIntentBandwidth(intent, onlyPath);

            links.add(createEdgeLink(ingressPoint, true));
            links.addAll(onlyPath.links());
            links.add(createEdgeLink(egressPoint, false));
            return asList(createPathIntent(new DefaultPath(PID, links, onlyPath.weight(),
                                                           onlyPath.annotations()),
                                           intent, PathIntent.ProtectionType.PRIMARY));
        }
    }

    /**
     * Creates a path intent from the specified path and original
     * connectivity intent.
     *
     * @param path   path to create an intent for
     * @param intent original intent
     * @param type   primary or backup
     */
    private Intent createPathIntent(Path path,
                                    PointToPointIntent intent,
                                    PathIntent.ProtectionType type) {
        return PathIntent.builder()
                .appId(intent.appId())
                .key(intent.key())
                .selector(intent.selector())
                .treatment(intent.treatment())
                .path(path)
                .constraints(intent.constraints())
                .priority(intent.priority())
                .setType(type)
                .resourceGroup(intent.resourceGroup())
                .build();
    }


    /**
     * Creates a link collection intent from the specified path and original
     * point to point intent.
     *
     * @param links the links of the packets
     * @param cost the cost associated to the links
     * @param intent the point to point intent we are compiling
     * @return the link collection intent
     */
    private Intent createLinkCollectionIntent(Set<Link> links,
                                              double cost,
                                              PointToPointIntent intent) {

        return LinkCollectionIntent.builder()
                .key(intent.key())
                .appId(intent.appId())
                .selector(intent.selector())
                .treatment(intent.treatment())
                .links(ImmutableSet.copyOf(links))
                .filteredIngressPoints(ImmutableSet.of(
                        intent.filteredIngressPoint()
                ))
                .filteredEgressPoints(ImmutableSet.of(
                        intent.filteredEgressPoint()
                ))
                .applyTreatmentOnEgress(true)
                .constraints(intent.constraints())
                .priority(intent.priority())
                .cost(cost)
                .resourceGroup(intent.resourceGroup())
                .build();
    }

    /**
     * Gets primary port number through failover group associated
     * with this intent.
     */
    private PortNumber getPrimaryPort(PointToPointIntent intent) {
        Group group = groupService.getGroup(intent.filteredIngressPoint().connectPoint().deviceId(),
                                            makeGroupKey(intent.id()));
        PortNumber primaryPort = null;
        if (group != null) {
            List<GroupBucket> buckets = group.buckets().buckets();
            Iterator<GroupBucket> iterator = buckets.iterator();
            while (primaryPort == null && iterator.hasNext()) {
                GroupBucket bucket = iterator.next();
                Instruction individualInstruction = bucket.treatment().allInstructions().get(0);
                if (individualInstruction instanceof Instructions.OutputInstruction) {
                    Instructions.OutputInstruction outInstruction =
                            (Instructions.OutputInstruction) individualInstruction;
                    PortNumber tempPortNum = outInstruction.port();
                    Port port = deviceService.getPort(intent.filteredIngressPoint().connectPoint().deviceId(),
                                                      tempPortNum);
                    if (port != null && port.isEnabled()) {
                        primaryPort = tempPortNum;
                    }
                }
            }
        }
        return primaryPort;
    }

    /**
     * Creates group key unique to each intent.
     *
     * @param intentId identifier of intent to get a key for
     * @return unique group key for the intent identifier
     */
    public static GroupKey makeGroupKey(IntentId intentId) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.putLong(intentId.fingerprint());
        return new DefaultGroupKey(buffer.array());
    }

    /**
     * Creates a new failover group with the initial ports of the links
     * from the primary and backup path.
     *
     * @param links         links from the primary path
     * @param backupLinks   links from the backup path
     * @param intent        intent from which this call originates
     */
    private void createFailoverTreatmentGroup(List<Link> links,
                                              List<Link> backupLinks,
                                              PointToPointIntent intent) {

        List<GroupBucket> buckets = new ArrayList<>();

        TrafficTreatment.Builder tBuilderIn = DefaultTrafficTreatment.builder();
        ConnectPoint src = links.get(0).src();
        tBuilderIn.setOutput(src.port());

        TrafficTreatment.Builder tBuilderIn2 = DefaultTrafficTreatment.builder();
        ConnectPoint src2 = backupLinks.get(0).src();
        tBuilderIn2.setOutput(src2.port());

        buckets.add(DefaultGroupBucket.createFailoverGroupBucket(tBuilderIn.build(), src.port(), null));
        buckets.add(DefaultGroupBucket.createFailoverGroupBucket(tBuilderIn2.build(), src2.port(), null));

        GroupBuckets groupBuckets = new GroupBuckets(buckets);

        GroupDescription groupDesc = new DefaultGroupDescription(src.deviceId(), Group.Type.FAILOVER,
                                         groupBuckets, makeGroupKey(intent.id()), null, intent.appId());
        log.trace("adding failover group {}", groupDesc);
        groupService.addGroup(groupDesc);
    }

    /**
     * Manufactures flow rule with treatment that is defined by failover
     * group and traffic selector determined by ingress port of the intent.
     *
     * @param intent intent which is being compiled (for appId)
     * @return       a list of a singular flow rule with fast failover
     *               outport traffic treatment
     */
    private List<FlowRule> createFailoverFlowRules(PointToPointIntent intent) {
        List<FlowRule> flowRules = new ArrayList<>();

        ConnectPoint ingress = intent.filteredIngressPoint().connectPoint();
        DeviceId deviceId = ingress.deviceId();

        // flow rule with failover traffic treatment
        TrafficSelector trafficSelector = DefaultTrafficSelector.builder(intent.selector())
                                                      .matchInPort(ingress.port()).build();

        FlowRule.Builder flowRuleBuilder = DefaultFlowRule.builder();
        flowRules.add(flowRuleBuilder.withSelector(trafficSelector)
                              .withTreatment(buildFailoverTreatment(deviceId, makeGroupKey(intent.id())))
                              .fromApp(intent.appId())
                              .makePermanent()
                              .forDevice(deviceId)
                              .withPriority(PRIORITY)
                              .build());

        return flowRules;
    }


    /**
     * Waits for specified group to appear maximum of {@value #GROUP_TIMEOUT} seconds.
     *
     * @param deviceId {@link DeviceId}
     * @param groupKey {@link GroupKey} to wait for.
     * @return {@link Group}
     * @throws IntentCompilationException on any error.
     */
    private Group waitForGroup(DeviceId deviceId, GroupKey groupKey) {
        return waitForGroup(deviceId, groupKey, GROUP_TIMEOUT, TimeUnit.SECONDS);
    }

    /**
     * Waits for specified group to appear until timeout.
     *
     * @param deviceId {@link DeviceId}
     * @param groupKey {@link GroupKey} to wait for.
     * @param timeout timeout
     * @param unit unit of timeout
     * @return {@link Group}
     * @throws IntentCompilationException on any error.
     */
    private Group waitForGroup(DeviceId deviceId, GroupKey groupKey, long timeout, TimeUnit unit) {
        Group group = groupService.getGroup(deviceId, groupKey);
        if (group != null) {
            return group;
        }

        final CompletableFuture<Group> future = new CompletableFuture<>();
        final GroupListener listener = event -> {
            if (event.subject().deviceId() == deviceId &&
                event.subject().appCookie().equals(groupKey)) {
                future.complete(event.subject());
                return;
            }
        };

        groupService.addListener(listener);
        try {
            group = groupService.getGroup(deviceId, groupKey);
            if (group != null) {
                return group;
            }
            return future.get(timeout, unit);
        } catch (InterruptedException e) {
            log.debug("Interrupted", e);
            Thread.currentThread().interrupt();
            throw new IntentCompilationException("Interrupted", e);
        } catch (ExecutionException e) {
            log.debug("ExecutionException", e);
            throw new IntentCompilationException("ExecutionException caught", e);
        } catch (TimeoutException e) {
            // one last try
            group = groupService.getGroup(deviceId, groupKey);
            if (group != null) {
                return group;
            } else {
                log.debug("Timeout", e);
                throw new IntentCompilationException("Timeout", e);
            }
        } finally {
            groupService.removeListener(listener);
        }
    }

    private TrafficTreatment buildFailoverTreatment(DeviceId srcDevice,
                                                    GroupKey groupKey) {
        Group group = waitForGroup(srcDevice, groupKey);
        TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder();
        TrafficTreatment trafficTreatment = tBuilder.group(group.id()).build();
        return trafficTreatment;
    }

    /**
     * Deletes intents from the given list if the ports or links the intent
     * relies on are no longer viable. The failover flow rule intent is never
     * deleted -- only its contents are updated.
     *
     * @param oldInstallables  list of intents to examine
     * @return                 list of reusable installable intents
     */
    private List<Intent> filterInvalidSubIntents(List<Intent> oldInstallables,
                                                 PointToPointIntent pointIntent) {
        List<Intent> intentList = new ArrayList<>();
        intentList.addAll(oldInstallables);

        Iterator<Intent> iterator = intentList.iterator();
        while (iterator.hasNext()) {
            Intent intent = iterator.next();
            intent.resources().forEach(resource -> {
                if (resource instanceof Link) {
                    Link link = (Link) resource;
                    if (link.state() == Link.State.INACTIVE) {
                        setPathsToRemove(intent);
                    } else if (link instanceof EdgeLink) {
                        ConnectPoint connectPoint = (link.src().elementId() instanceof DeviceId)
                                ? link.src() : link.dst();
                        Port port = deviceService.getPort(connectPoint.deviceId(), connectPoint.port());
                        if (port == null || !port.isEnabled()) {
                            setPathsToRemove(intent);
                        }
                    } else {
                        Port port1 = deviceService.getPort(link.src().deviceId(), link.src().port());
                        Port port2 = deviceService.getPort(link.dst().deviceId(), link.dst().port());
                        if (port1 == null || !port1.isEnabled() || port2 == null || !port2.isEnabled()) {
                            setPathsToRemove(intent);
                        }
                    }
                }
            });
        }
        removeAndUpdateIntents(intentList, pointIntent);

        return intentList;
    }

    /**
     * Sets instance variables erasePrimary and eraseBackup. If erasePrimary,
     * the primary path is no longer viable and related intents will be deleted.
     * If eraseBackup, the backup path is no longer viable and related intents
     * will be deleted.
     *
     * @param intent  intent whose resources are found to be disabled/inactive:
     *                if intent is part of primary path, primary path set for removal;
     *                if intent is part of backup path, backup path set for removal;
     *                if bad intent is of type failover, the ingress point is down,
     *                and both paths are rendered inactive.
     * @return        true if both primary and backup paths are to be removed
     */
    private boolean setPathsToRemove(Intent intent) {
        if (intent instanceof FlowRuleIntent) {
            FlowRuleIntent frIntent = (FlowRuleIntent) intent;
            PathIntent.ProtectionType type = frIntent.type();
            if (type == PathIntent.ProtectionType.PRIMARY || type == PathIntent.ProtectionType.FAILOVER) {
                erasePrimary = true;
            }
            if (type == PathIntent.ProtectionType.BACKUP || type == PathIntent.ProtectionType.FAILOVER) {
                eraseBackup = true;
            }
        }
        return erasePrimary && eraseBackup;
    }

    /**
     * Removes intents from installables list, depending on the values
     * of instance variables erasePrimary and eraseBackup. Flow rule intents
     * that contain the manufactured fast failover flow rules are never deleted.
     * The contents are simply modified as necessary. If cleanUpIntents size
     * is greater than 1 (failover intent), then one whole path from previous
     * installables must be still viable.
     *
     * @param cleanUpIntents   list of installable intents
     */
    private void removeAndUpdateIntents(List<Intent> cleanUpIntents,
                                        PointToPointIntent pointIntent) {
        ListIterator<Intent> iterator = cleanUpIntents.listIterator();
        while (iterator.hasNext()) {
            Intent cIntent = iterator.next();
            if (cIntent instanceof FlowRuleIntent) {
                FlowRuleIntent fIntent = (FlowRuleIntent) cIntent;
                if (fIntent.type() == PathIntent.ProtectionType.PRIMARY && erasePrimary) {
                    // remove primary path's flow rule intents
                    iterator.remove();
                } else if (fIntent.type() == PathIntent.ProtectionType.BACKUP && eraseBackup) {
                    //remove backup path's flow rule intents
                    iterator.remove();
                } else if (fIntent.type() == PathIntent.ProtectionType.BACKUP && erasePrimary) {
                    // promote backup path's flow rule intents to primary
                    iterator.set(new FlowRuleIntent(fIntent, PathIntent.ProtectionType.PRIMARY));
                }
            }
        }
        // remove buckets whose watchports are disabled if the failover group exists
        Group group = groupService.getGroup(pointIntent.filteredIngressPoint().connectPoint().deviceId(),
                                            makeGroupKey(pointIntent.id()));
        if (group != null) {
            updateFailoverGroup(pointIntent);
        }
    }

    // Removes buckets whose treatments rely on disabled ports from the
    // failover group.
    private void updateFailoverGroup(PointToPointIntent pointIntent) {
        DeviceId deviceId = pointIntent.filteredIngressPoint().connectPoint().deviceId();
        GroupKey groupKey = makeGroupKey(pointIntent.id());
        Group group = waitForGroup(deviceId, groupKey);
        Iterator<GroupBucket> groupIterator = group.buckets().buckets().iterator();
        while (groupIterator.hasNext()) {
            GroupBucket bucket = groupIterator.next();
            Instruction individualInstruction = bucket.treatment().allInstructions().get(0);
            if (individualInstruction instanceof Instructions.OutputInstruction) {
                Instructions.OutputInstruction outInstruction =
                        (Instructions.OutputInstruction) individualInstruction;
                Port port = deviceService.getPort(deviceId, outInstruction.port());
                if (port == null || !port.isEnabled()) {
                    GroupBuckets removeBuckets = new GroupBuckets(Collections.singletonList(bucket));
                    groupService.removeBucketsFromGroup(deviceId, groupKey,
                                                        removeBuckets, groupKey,
                                                        pointIntent.appId());
                }
            }
        }
    }

    // Adds failover group bucket with treatment outport determined by the
    // ingress point of the links.
    private void updateFailoverGroup(PointToPointIntent intent, List<Link> links) {
        GroupKey groupKey = makeGroupKey(intent.id());

        TrafficTreatment.Builder tBuilderIn = DefaultTrafficTreatment.builder();
        ConnectPoint src = links.get(0).src();
        tBuilderIn.setOutput(src.port());
        GroupBucket bucket = DefaultGroupBucket.createFailoverGroupBucket(tBuilderIn.build(), src.port(), null);
        GroupBuckets addBuckets = new GroupBuckets(Collections.singletonList(bucket));

        groupService.addBucketsToGroup(src.deviceId(), groupKey, addBuckets, groupKey, intent.appId());
    }

    /**
     * Checks suggested path availability.
     * It checks:
     * - single links availability;
     * - that first and last device of the path are coherent with ingress and egress devices;
     * - links contiguity.
     *
     * @param intent    Intent with suggested path to check
     * @return true if the suggested path is available
     */
    private boolean pathAvailable(PointToPointIntent intent) {
        // Check links availability
        List<Link> suggestedPath = intent.suggestedPath();
        for (Link link : suggestedPath) {
            if (!(link instanceof EdgeLink) && !linkService.getLinks(link.src()).contains(link)) {
                return false;
            }
        }

        //Check that first and last device of the path are intent ingress and egress devices
        if (!suggestedPath.get(0).src()
                .deviceId().equals(intent.filteredIngressPoint().connectPoint().deviceId())) {
            return false;
        }
        if (!suggestedPath.get(suggestedPath.size() - 1).dst()
                .deviceId().equals(intent.filteredEgressPoint().connectPoint().deviceId())) {
            return false;
        }

        // Check contiguity
        List<Pair<Link, Link>> linkPairs = IntStream.
            range(0, suggestedPath.size() - 1)
            .mapToObj(i -> Pair.of(suggestedPath.get(i), suggestedPath.get(i + 1)))
            .collect(Collectors.toList());

        for (Pair<Link, Link> linkPair : linkPairs) {
            if (!linkPair.getKey().dst().deviceId().equals(linkPair.getValue().src().deviceId())) {
                return false;
            }
        }
        return true;
    }
}
