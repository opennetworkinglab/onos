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

package org.onosproject.security.store;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.apache.karaf.features.BundleInfo;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesService;
import org.onlab.util.KryoNamespace;
import org.onosproject.app.ApplicationAdminService;
import org.onosproject.core.Application;
import org.onosproject.core.ApplicationId;
import org.onosproject.security.Permission;
import org.onosproject.store.AbstractStore;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.EventuallyConsistentMap;
import org.onosproject.store.service.LogicalClockService;
import org.onosproject.store.service.MapEvent;
import org.onosproject.store.service.MapEventListener;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.security.store.SecurityModeState.*;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Manages application permissions granted/requested to applications.
 * Uses both gossip-based and RAFT-based distributed data store.
 */
@Component(immediate = true)
@Service
public class DistributedSecurityModeStore
        extends AbstractStore<SecurityModeEvent, SecurityModeStoreDelegate>
        implements SecurityModeStore {

    private final Logger log = getLogger(getClass());

    private ConsistentMap<ApplicationId, SecurityInfo> states;
    private EventuallyConsistentMap<ApplicationId, Set<Permission>> violations;

    private ConcurrentHashMap<String, Set<ApplicationId>> localBundleAppDirectory;
    private ConcurrentHashMap<ApplicationId, Set<String>> localAppBundleDirectory;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LogicalClockService clockService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ApplicationAdminService applicationAdminService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FeaturesService featuresService;

    private ExecutorService eventHandler;
    private final SecurityStateListener statesListener = new SecurityStateListener();

    private static final Serializer STATE_SERIALIZER = Serializer.using(new KryoNamespace.Builder()
            .register(KryoNamespaces.API)
            .register(SecurityModeState.class)
            .register(SecurityInfo.class)
            .register(Permission.class)
            .build());

    private static final KryoNamespace.Builder VIOLATION_SERIALIZER = KryoNamespace.newBuilder()
            .register(KryoNamespaces.API)
            .register(Permission.class);

    @Activate
    public void activate() {
        eventHandler = newSingleThreadExecutor(groupedThreads("onos/security/store", "event-handler", log));
        states = storageService.<ApplicationId, SecurityInfo>consistentMapBuilder()
                .withName("smonos-sdata")
                .withSerializer(STATE_SERIALIZER)
                .build();

        states.addListener(statesListener, eventHandler);

        violations = storageService.<ApplicationId, Set<Permission>>eventuallyConsistentMapBuilder()
                .withName("smonos-rperms")
                .withSerializer(VIOLATION_SERIALIZER)
                .withTimestampProvider((k, v) -> clockService.getTimestamp())
                .build();

        localBundleAppDirectory = new ConcurrentHashMap<>();
        localAppBundleDirectory = new ConcurrentHashMap<>();

        log.info("Started");

    }

    @Deactivate
    public void deactivate() {
        states.removeListener(statesListener);
        eventHandler.shutdown();
        violations.destroy();
        log.info("Stopped");
    }


    @Override
    public Set<String> getBundleLocations(ApplicationId appId) {
        Set<String> locations = localAppBundleDirectory.get(appId);
        return locations != null ? locations : Sets.newHashSet();
    }

    @Override
    public Set<ApplicationId> getApplicationIds(String location) {
        Set<ApplicationId> appIds = localBundleAppDirectory.get(location);
        return appIds != null ? appIds : Sets.newHashSet();
    }

    @Override
    public Set<Permission> getRequestedPermissions(ApplicationId appId) {
        Set<Permission> permissions = violations.get(appId);
        return permissions != null ? permissions : ImmutableSet.of();
    }

    @Override
    public Set<Permission> getGrantedPermissions(ApplicationId appId) {
        return states.asJavaMap().getOrDefault(appId, new SecurityInfo(ImmutableSet.of(), null)).getPermissions();
    }

    @Override
    public void requestPermission(ApplicationId appId, Permission permission) {

        states.computeIf(appId, securityInfo -> (securityInfo == null || securityInfo.getState() != POLICY_VIOLATED),
                (id, securityInfo) -> new SecurityInfo(securityInfo.getPermissions(), POLICY_VIOLATED));
        violations.compute(appId, (k, v) -> v == null ? Sets.newHashSet(permission) : addAndGet(v, permission));
    }

    private Set<Permission> addAndGet(Set<Permission> oldSet, Permission newPerm) {
        oldSet.add(newPerm);
        return oldSet;
    }

    @Override
    public boolean isSecured(ApplicationId appId) {
        SecurityInfo info = states.get(appId).value();
        return info == null ? false : info.getState().equals(SECURED);
    }

    @Override
    public void reviewPolicy(ApplicationId appId) {
        Application app = applicationAdminService.getApplication(appId);
        if (app == null) {
            log.warn("Unknown Application");
            return;
        }
        states.computeIfPresent(appId, (applicationId, securityInfo) -> {
            if (securityInfo.getState().equals(INSTALLED)) {
                return new SecurityInfo(ImmutableSet.of(), REVIEWED);
            }
            return securityInfo;
        });
    }

    @Override
    public void acceptPolicy(ApplicationId appId, Set<Permission> permissionSet) {

        Application app = applicationAdminService.getApplication(appId);
        if (app == null) {
            log.warn("Unknown Application");
            return;
        }

        states.computeIf(appId,
                Objects::nonNull,
                (id, securityInfo) -> {
                    switch (securityInfo.getState()) {
                        case POLICY_VIOLATED:
                            System.out.println(
                                    "This application has violated the security policy. Please uninstall.");
                            return securityInfo;
                        case SECURED:
                            System.out.println(
                                    "The policy has been accepted already. To review policy, review [app.name]");
                            return securityInfo;
                        case INSTALLED:
                            System.out.println("Please review the security policy prior to accept them");
                            log.warn("Application has not been reviewed");
                            return securityInfo;
                        case REVIEWED:
                            return new SecurityInfo(permissionSet, SECURED);
                        default:
                            return securityInfo;
                    }
                });
    }

    private final class SecurityStateListener
            implements MapEventListener<ApplicationId, SecurityInfo> {

        @Override
        public void event(MapEvent<ApplicationId, SecurityInfo> event) {

            if (delegate == null) {
                return;
            }
            ApplicationId appId = event.key();
            SecurityInfo info = event.value().value();

            if (event.type() == MapEvent.Type.INSERT || event.type() == MapEvent.Type.UPDATE) {
                switch (info.getState()) {
                    case POLICY_VIOLATED:
                        notifyDelegate(new SecurityModeEvent(SecurityModeEvent.Type.POLICY_VIOLATED, appId));
                        break;
                    case SECURED:
                        notifyDelegate(new SecurityModeEvent(SecurityModeEvent.Type.POLICY_ACCEPTED, appId));
                        break;
                    default:
                        break;
                }
            } else if (event.type() == MapEvent.Type.REMOVE) {
                removeAppFromDirectories(appId);
            }
        }
    }

    private void removeAppFromDirectories(ApplicationId appId) {
        for (String location : localAppBundleDirectory.get(appId)) {
            localBundleAppDirectory.get(location).remove(appId);
        }
        violations.remove(appId);
        states.remove(appId);
        localAppBundleDirectory.remove(appId);
    }

    @Override
    public boolean registerApplication(ApplicationId appId) {
        Application app = applicationAdminService.getApplication(appId);
        if (app == null) {
            log.warn("Unknown application.");
            return false;
        }
        localAppBundleDirectory.put(appId, getBundleLocations(app));
        for (String location : localAppBundleDirectory.get(appId)) {
            if (!localBundleAppDirectory.containsKey(location)) {
                localBundleAppDirectory.put(location, new HashSet<>());
            }
            if (!localBundleAppDirectory.get(location).contains(appId)) {
                localBundleAppDirectory.get(location).add(appId);
            }
        }
        states.put(appId, new SecurityInfo(Sets.newHashSet(), INSTALLED));
        return true;
    }

    @Override
    public void unregisterApplication(ApplicationId appId) {
        if (localAppBundleDirectory.containsKey(appId)) {
            for (String location : localAppBundleDirectory.get(appId)) {
                if (localBundleAppDirectory.get(location).size() == 1) {
                    localBundleAppDirectory.remove(location);
                } else {
                    localBundleAppDirectory.get(location).remove(appId);
                }
            }
            localAppBundleDirectory.remove(appId);
        }
    }

    @Override
    public SecurityModeState getState(ApplicationId appId) {
        return states.asJavaMap().getOrDefault(appId, new SecurityInfo(null, null)).getState();
    }

    private Set<String> getBundleLocations(Application app) {
        Set<String> locations = new HashSet<>();
        for (String name : app.features()) {
            try {
                Feature feature = featuresService.getFeature(name);
                locations.addAll(
                        feature.getBundles().stream().map(BundleInfo::getLocation).collect(Collectors.toList()));
            } catch (Exception e) {
                return locations;
            }
        }
        return locations;
    }
}
