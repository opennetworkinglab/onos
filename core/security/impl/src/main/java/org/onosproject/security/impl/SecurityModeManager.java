package org.onosproject.security.impl;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.karaf.features.BundleInfo;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesService;

import org.onosproject.app.ApplicationAdminService;
import org.onosproject.app.ApplicationEvent;
import org.onosproject.app.ApplicationListener;
import org.onosproject.app.ApplicationState;
import org.onosproject.core.Application;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.Permission;
import org.onosproject.security.AppPermission;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.PackagePermission;
import org.osgi.framework.ServicePermission;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogListener;
import org.osgi.service.log.LogReaderService;
import org.osgi.service.permissionadmin.PermissionInfo;

import java.security.AccessControlException;
import java.security.AllPermission;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.osgi.service.permissionadmin.PermissionAdmin;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Security-Mode ONOS management implementation.
 */

//TODO : implement a dedicated distributed store for SM-ONOS

@Component(immediate = true)
public class SecurityModeManager {

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ApplicationAdminService appAdminService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FeaturesService featuresService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LogReaderService logReaderService;

    private final Logger log = getLogger(getClass());

    private SecurityBundleListener securityBundleListener = new SecurityBundleListener();

    private SecurityApplicationListener securityApplicationListener = new SecurityApplicationListener();

    private SecurityLogListener securityLogListener = new SecurityLogListener();

    private Bundle bundle = null;

    private BundleContext bundleContext = null;

    private PermissionAdmin permissionAdmin = null;

    private Map<String, ApplicationId> appTracker = null;

    private Map<Permission, Set<String>> serviceDirectory = null;


    @Activate
    public void activate() {
        if (System.getSecurityManager() == null) {
            log.warn("J2EE security manager is disabled.");
            deactivate();
            return;
        }
        bundle = FrameworkUtil.getBundle(this.getClass());
        bundleContext = bundle.getBundleContext();

        bundleContext.addBundleListener(securityBundleListener);
        appAdminService.addListener(securityApplicationListener);
        logReaderService.addLogListener(securityLogListener);
        appTracker = new ConcurrentHashMap<>();

        permissionAdmin = getPermissionAdmin(bundleContext);
        if (permissionAdmin == null) {
            log.warn("Permission Admin not found.");
            this.deactivate();
            return;
        }

        serviceDirectory = PolicyBuilder.getServiceDirectory();

        PermissionInfo[] allPerm = {
                new PermissionInfo(AllPermission.class.getName(), "", ""), };

        permissionAdmin.setPermissions(bundle.getLocation(), allPerm);
        log.warn("Security-Mode Started");
    }


    @Deactivate
    public void deactivate() {
        bundleContext.removeBundleListener(securityBundleListener);
        appAdminService.removeListener(securityApplicationListener);
        logReaderService.removeLogListener(securityLogListener);
        log.info("Stopped");

    }

    private class SecurityApplicationListener implements ApplicationListener {

        @Override
        public void event(ApplicationEvent event) {
            //App needs to be restarted
            if (event.type() == ApplicationEvent.Type.APP_PERMISSIONS_CHANGED) {
                if (appAdminService.getState(event.subject().id()) == ApplicationState.ACTIVE) {
                    appAdminService.deactivate(event.subject().id());
                    print("Permissions updated (%s). Deactivating...",
                            event.subject().id().name());
                }
            }
        }
    }

    private class SecurityBundleListener implements BundleListener {

        @Override
        public void bundleChanged(BundleEvent event) {
            switch (event.getType()) {
                case BundleEvent.INSTALLED:
                    setPermissions(event);
                    break;
                case BundleEvent.UNINSTALLED:
                    clearPermissions(event);
                    break;
                default:
                    break;
            }
        }
    }

    private void clearPermissions(BundleEvent bundleEvent) {
        if (appTracker.containsKey(bundleEvent.getBundle().getLocation())) {
            permissionAdmin.setPermissions(bundleEvent.getBundle().getLocation(), new PermissionInfo[]{});
            appTracker.remove(bundleEvent.getBundle().getLocation());
        }
    }

    // find the location of the installed bundle and enforce policy
    private void setPermissions(BundleEvent bundleEvent) {
        for (Application app : appAdminService.getApplications()) {
            if (getBundleLocations(app).contains(bundleEvent.getBundle().getLocation())) {
                String location = bundleEvent.getBundle().getLocation();

                Set<org.onosproject.core.Permission> permissions =
                        appAdminService.getPermissions(app.id());

                //Permissions granted by user overrides the permissions specified in App.Xml file
                if (permissions == null) {
                    permissions = app.permissions();
                }

                if (permissions.isEmpty()) {
                    print("Application %s has not been granted any permission.", app.id().name());
                }

                PermissionInfo[] perms = null;

                switch (app.role()) {
                    case ADMIN:
                        perms = PolicyBuilder.getAdminApplicationPermissions(serviceDirectory);
                        break;
                    case REGULAR:
                        perms = PolicyBuilder.getApplicationPermissions(serviceDirectory, permissions);
                        break;
                    case UNSPECIFIED:
                    default:
                        //no role has been assigned.
                        perms = PolicyBuilder.getDefaultPerms();
                        log.warn("Application %s has no role assigned.", app.id().name());
                        break;
                }
                permissionAdmin.setPermissions(location, perms);
                appTracker.put(location, app.id());
                break;
            }
        }
    }

    //TODO: dispatch security policy violation event via distributed store
    //immediately notify and deactivate the application upon policy violation
    private class SecurityLogListener implements LogListener {
        @Override
        public void logged(LogEntry entry) {
            if (entry != null) {
                if (entry.getException() != null) {
                    ApplicationId applicationId = appTracker.get(entry.getBundle().getLocation());
                    if (applicationId != null) {
                        if (appAdminService.getState(applicationId).equals(ApplicationState.ACTIVE)) {
                            if (entry.getException() instanceof AccessControlException) {
                                java.security.Permission permission =
                                        ((AccessControlException) entry.getException()).getPermission();
                                handleException(applicationId.name(), permission);
                                appAdminService.deactivate(applicationId);
                            }
                        }
                    }
                }
            }
        }
    }

    private void handleException(String name, java.security.Permission perm) {
        if (perm instanceof ServicePermission || perm instanceof PackagePermission) {
            print("%s has attempted to %s %s.", name, perm.getActions(), perm.getName());
        } else if (perm instanceof AppPermission) {
            print("%s has attempted to call an NB API that requires %s permission.",
                    name, perm.getName().toUpperCase());
        } else {
            print("%s has attempted to perform an action that requires %s", name, perm.toString());
        }
        print("POLICY VIOLATION: Deactivating %s.", name);

    }
    private void print(String format, Object... args) {
        System.out.println(String.format("SM-ONOS: " + format, args));
        log.warn(String.format(format, args));
    }

    private List<String> getBundleLocations(Application app) {
        List<String> locations = new ArrayList();
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

    private PermissionAdmin getPermissionAdmin(BundleContext context) {
        return (PermissionAdmin) context.getService(context.getServiceReference(PermissionAdmin.class.getName()));
    }

}
