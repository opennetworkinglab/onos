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

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultPath;
import org.onosproject.net.DeviceId;
import org.onosproject.net.DisjointPath;
import org.onosproject.net.EdgeLink;
import org.onosproject.net.Link;
import org.onosproject.net.Path;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
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
import org.onosproject.net.intent.PathIntent;
import org.onosproject.net.intent.PointToPointIntent;
import org.onosproject.net.intent.constraint.ProtectionConstraint;
import org.onosproject.net.intent.impl.PathNotFoundException;
import org.onosproject.net.link.LinkService;
import org.onosproject.net.provider.ProviderId;
import org.slf4j.Logger;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected GroupService groupService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LinkService linkService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

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
        ConnectPoint ingressPoint = intent.ingressPoint();
        ConnectPoint egressPoint = intent.egressPoint();

        if (ingressPoint.deviceId().equals(egressPoint.deviceId())) {
            return createZeroHopIntent(ingressPoint, egressPoint, intent);
        }

        // proceed with no protected paths
        if (!ProtectionConstraint.requireProtectedPath(intent)) {
            return createUnprotectedIntent(ingressPoint, egressPoint, intent);
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

    private List<Intent> createZeroHopIntent(ConnectPoint ingressPoint,
                                             ConnectPoint egressPoint,
                                             PointToPointIntent intent) {
        List<Link> links = asList(createEdgeLink(ingressPoint, true), createEdgeLink(egressPoint, false));
        return asList(createPathIntent(new DefaultPath(PID, links, DEFAULT_COST),
                                       intent, PathIntent.ProtectionType.PRIMARY));
    }

    private List<Intent> createUnprotectedIntent(ConnectPoint ingressPoint,
                                                 ConnectPoint egressPoint,
                                                 PointToPointIntent intent) {
        List<Link> links = new ArrayList<>();
        Path path = getPath(intent, ingressPoint.deviceId(),
                            egressPoint.deviceId());

        links.add(createEdgeLink(ingressPoint, true));
        links.addAll(path.links());
        links.add(createEdgeLink(egressPoint, false));

        return asList(createPathIntent(new DefaultPath(PID, links, path.cost(),
                                                       path.annotations()), intent,
                                       PathIntent.ProtectionType.PRIMARY));
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
                                                                     path.cost(), path.annotations()),
                                                     intent, PathIntent.ProtectionType.BACKUP));
                updateFailoverGroup(intent, links);
                return reusableIntents;

            } else {
                reusableIntents.add(createPathIntent(new DefaultPath(PID, backupLinks, path.backup().cost(),
                                     path.backup().annotations()), intent, PathIntent.ProtectionType.BACKUP));
                updateFailoverGroup(intent, backupLinks);
                return reusableIntents;
            }
        }

        intentList.add(createPathIntent(new DefaultPath(PID, links, path.cost(),
                                                        path.annotations()),
                                        intent, PathIntent.ProtectionType.PRIMARY));
        intentList.add(createPathIntent(new DefaultPath(PID, backupLinks, path.backup().cost(),
                                                        path.backup().annotations()),
                                        intent, PathIntent.ProtectionType.BACKUP));

        // Create fast failover flow rule intent or, if it already exists,
        // add contents appropriately.
        if (groupService.getGroup(ingressPoint.deviceId(),
                                  makeGroupKey(intent.id())) == null) {
            // manufactured fast failover flow rule intent
            createFailoverTreatmentGroup(path.links(), path.backup().links(), intent);

            FlowRuleIntent frIntent = new FlowRuleIntent(intent.appId(),
                                                         createFailoverFlowRules(intent),
                                                         asList(ingressPoint.deviceId()),
                                                         PathIntent.ProtectionType.FAILOVER);
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
        Path onlyPath = getPath(intent, ingressPoint.deviceId(),
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
            links.add(createEdgeLink(ingressPoint, true));
            links.addAll(onlyPath.links());
            links.add(createEdgeLink(egressPoint, false));

            return asList(createPathIntent(new DefaultPath(PID, links, onlyPath.cost(),
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
                .selector(intent.selector())
                .treatment(intent.treatment())
                .path(path)
                .constraints(intent.constraints())
                .priority(intent.priority())
                .setType(type)
                .build();
    }

    /**
     * Gets primary port number through failover group associated
     * with this intent.
     */
    private PortNumber getPrimaryPort(PointToPointIntent intent) {
        Group group = groupService.getGroup(intent.ingressPoint().deviceId(),
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
                    Port port = deviceService.getPort(intent.ingressPoint().deviceId(),
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

        ConnectPoint ingress = intent.ingressPoint();
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
        erasePrimary = false;
        eraseBackup = false;
        if (intentList != null) {
            Iterator<Intent> iterator = intentList.iterator();
            while (iterator.hasNext() && !(erasePrimary && eraseBackup)) {
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
        }
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
        Group group = groupService.getGroup(pointIntent.ingressPoint().deviceId(),
                                            makeGroupKey(pointIntent.id()));
        if (group != null) {
            updateFailoverGroup(pointIntent);
        }
    }

    // Removes buckets whose treatments rely on disabled ports from the
    // failover group.
    private void updateFailoverGroup(PointToPointIntent pointIntent) {
        DeviceId deviceId = pointIntent.ingressPoint().deviceId();
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
}
