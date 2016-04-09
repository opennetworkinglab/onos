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
package org.onosproject.security.impl;

import com.google.common.collect.Lists;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;

import org.onosproject.app.ApplicationAdminService;
import org.onosproject.app.ApplicationState;
import org.onosproject.core.Application;
import org.onosproject.core.ApplicationId;

import org.onosproject.event.EventDeliveryService;
import org.onosproject.event.ListenerRegistry;
import org.onosproject.security.AppPermission;
import org.onosproject.security.SecurityAdminService;
import org.onosproject.security.store.SecurityModeEvent;
import org.onosproject.security.store.SecurityModeListener;
import org.onosproject.security.store.SecurityModeStore;
import org.onosproject.security.store.SecurityModeStoreDelegate;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServicePermission;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogListener;
import org.osgi.service.log.LogReaderService;
import org.osgi.service.permissionadmin.PermissionInfo;

import java.security.AccessControlException;
import java.security.Permission;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.service.permissionadmin.PermissionAdmin;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Security-Mode ONOS management implementation.
 *
 * Note: Activating Security-Mode ONOS has significant performance implications in Drake.
 *       See the wiki for instructions on how to activate it.
 */

@Component(immediate = true)
@Service
public class SecurityModeManager implements SecurityAdminService {

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected SecurityModeStore store;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ApplicationAdminService appAdminService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LogReaderService logReaderService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected EventDeliveryService eventDispatcher;

    private final Logger log = getLogger(getClass());

    protected final ListenerRegistry<SecurityModeEvent, SecurityModeListener>
            listenerRegistry = new ListenerRegistry<>();

    private final SecurityModeStoreDelegate delegate = new InternalStoreDelegate();

    private SecurityLogListener securityLogListener = new SecurityLogListener();

    private PermissionAdmin permissionAdmin = getPermissionAdmin();

    @Activate
    public void activate() {

        eventDispatcher.addSink(SecurityModeEvent.class, listenerRegistry);
        logReaderService.addLogListener(securityLogListener);

        if (System.getSecurityManager() == null) {
            log.warn("J2EE security manager is disabled.");
            deactivate();
            return;
        }
        if (permissionAdmin == null) {
            log.warn("Permission Admin not found.");
            deactivate();
            return;
        }
        store.setDelegate(delegate);

        log.info("Security-Mode Started");
    }

    @Deactivate
    public void deactivate() {
        eventDispatcher.removeSink(SecurityModeEvent.class);
        logReaderService.removeLogListener(securityLogListener);
        store.unsetDelegate(delegate);
        log.info("Stopped");

    }

    @Override
    public boolean isSecured(ApplicationId appId) {
        if (store.getState(appId) == null) {
            store.registerApplication(appId);
        }
        return store.isSecured(appId);
    }


    @Override
    public void review(ApplicationId appId) {
        if (store.getState(appId) == null) {
            store.registerApplication(appId);
        }
        store.reviewPolicy(appId);
    }

    @Override
    public void acceptPolicy(ApplicationId appId) {
        if (store.getState(appId) == null) {
            store.registerApplication(appId);
        }
        store.acceptPolicy(appId, DefaultPolicyBuilder.convertToOnosPermissions(getMaximumPermissions(appId)));
    }

    @Override
    public void register(ApplicationId appId) {
        store.registerApplication(appId);
    }

    @Override
    public Map<Integer, List<Permission>> getPrintableSpecifiedPermissions(ApplicationId appId) {
        return getPrintablePermissionMap(getMaximumPermissions(appId));
    }

    @Override
    public Map<Integer, List<Permission>> getPrintableGrantedPermissions(ApplicationId appId) {
        return getPrintablePermissionMap(
                DefaultPolicyBuilder.convertToJavaPermissions(store.getGrantedPermissions(appId)));
    }

    @Override
    public Map<Integer, List<Permission>> getPrintableRequestedPermissions(ApplicationId appId) {
        return getPrintablePermissionMap(
                DefaultPolicyBuilder.convertToJavaPermissions(store.getRequestedPermissions(appId)));
    }

    private class SecurityLogListener implements LogListener {
        @Override
        public void logged(LogEntry entry) {
            if (entry.getException() != null &&
                    entry.getException() instanceof AccessControlException) {
                String location = entry.getBundle().getLocation();
                Permission javaPerm =
                        ((AccessControlException) entry.getException()).getPermission();
                org.onosproject.security.Permission permission = DefaultPolicyBuilder.getOnosPermission(javaPerm);
                if (permission == null) {
                    log.warn("Unsupported permission requested.");
                    return;
                }
                store.getApplicationIds(location).stream().filter(
                        appId -> store.isSecured(appId) &&
                                appAdminService.getState(appId) == ApplicationState.ACTIVE).forEach(appId -> {
                    store.requestPermission(appId, permission);
                    print("[POLICY VIOLATION] APP: %s / Bundle: %s / Permission: %s ",
                            appId.name(), location, permission.toString());
                });
            }
        }
    }

    private class InternalStoreDelegate implements SecurityModeStoreDelegate {
        @Override
        public void notify(SecurityModeEvent event) {
            if (event.type() == SecurityModeEvent.Type.POLICY_ACCEPTED) {
                setLocalPermissions(event.subject());
                log.info("{} POLICY ACCEPTED and ENFORCED", event.subject().name());
            } else if (event.type() == SecurityModeEvent.Type.POLICY_VIOLATED) {
                log.info("{} POLICY VIOLATED", event.subject().name());
            } else if (event.type() == SecurityModeEvent.Type.POLICY_REVIEWED) {
                log.info("{} POLICY REVIEWED", event.subject().name());
            }
            eventDispatcher.post(event);
        }
    }

    /**
     * TYPES.
     * 0 - APP_PERM
     * 1 - ADMIN SERVICE
     * 2 - NB_SERVICE
     * 3 - ETC_SERVICE
     * 4 - ETC
     * @param perms
     */
    private Map<Integer, List<Permission>> getPrintablePermissionMap(List<Permission> perms) {
        ConcurrentHashMap<Integer, List<Permission>> sortedMap = new ConcurrentHashMap<>();
        sortedMap.put(0, new ArrayList());
        sortedMap.put(1, new ArrayList());
        sortedMap.put(2, new ArrayList());
        sortedMap.put(3, new ArrayList());
        sortedMap.put(4, new ArrayList());
        for (Permission perm : perms) {
            if (perm instanceof ServicePermission) {
                if (DefaultPolicyBuilder.getNBServiceList().contains(perm.getName())) {
                    if (perm.getName().contains("Admin")) {
                        sortedMap.get(1).add(perm);
                    } else {
                        sortedMap.get(2).add(perm);
                    }
                } else {
                    sortedMap.get(3).add(perm);
                }
            } else if (perm instanceof AppPermission) {
                sortedMap.get(0).add(perm);
            } else {
                sortedMap.get(4).add(perm);
            }
        }
        return sortedMap;
    }

    private void setLocalPermissions(ApplicationId applicationId) {
        for (String location : store.getBundleLocations(applicationId)) {
            permissionAdmin.setPermissions(location, permissionsToInfo(store.getGrantedPermissions(applicationId)));
        }
    }

    private PermissionInfo[] permissionsToInfo(Set<org.onosproject.security.Permission> permissions) {
        List<PermissionInfo> result = Lists.newArrayList();
        for (org.onosproject.security.Permission perm : permissions) {
            result.add(new PermissionInfo(perm.getClassName(), perm.getName(), perm.getActions()));
        }
        PermissionInfo[] permissionInfos = new PermissionInfo[result.size()];
        return result.toArray(permissionInfos);
    }



    private List<Permission> getMaximumPermissions(ApplicationId appId) {
        Application app = appAdminService.getApplication(appId);
        if (app == null) {
            print("Unknown application.");
            return null;
        }
        List<Permission> appPerms;
        switch (app.role()) {
            case ADMIN:
                appPerms = DefaultPolicyBuilder.getAdminApplicationPermissions(app.permissions());
                break;
            case USER:
                appPerms = DefaultPolicyBuilder.getUserApplicationPermissions(app.permissions());
                break;
            case UNSPECIFIED:
            default:
                appPerms = DefaultPolicyBuilder.getDefaultPerms();
                break;
        }

        return appPerms;
    }


    private void print(String format, Object... args) {
        System.out.println(String.format("SM-ONOS: " + format, args));
        log.warn(String.format(format, args));
    }

    private PermissionAdmin getPermissionAdmin() {
        BundleContext context = getBundleContext();
        return (PermissionAdmin) context.getService(context.getServiceReference(PermissionAdmin.class.getName()));
    }

    private BundleContext getBundleContext() {
        return FrameworkUtil.getBundle(this.getClass()).getBundleContext();

    }


}