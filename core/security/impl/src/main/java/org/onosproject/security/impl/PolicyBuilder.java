package org.onosproject.security.impl;


import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.onosproject.core.Permission;
import org.onosproject.security.AppPermission;
import org.osgi.service.permissionadmin.PermissionInfo;

import org.onosproject.app.ApplicationAdminService;
import org.onosproject.app.ApplicationService;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.cluster.ClusterAdminService;
import org.onosproject.cluster.ClusterService;
import org.onosproject.core.CoreService;
import org.onosproject.cluster.LeadershipService;
import org.onosproject.mastership.MastershipAdminService;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.device.DeviceAdminService;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.device.DeviceClockService;
import org.onosproject.net.driver.DriverAdminService;
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.group.GroupService;
import org.onosproject.net.host.HostAdminService;
import org.onosproject.net.host.HostService;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.IntentExtensionService;
import org.onosproject.net.intent.IntentClockService;
import org.onosproject.net.intent.PartitionService;
import org.onosproject.net.link.LinkAdminService;
import org.onosproject.net.link.LinkService;
import org.onosproject.net.packet.PacketService;
import org.onosproject.net.proxyarp.ProxyArpService;
import org.onosproject.net.resource.link.LinkResourceService;
import org.onosproject.net.statistic.StatisticService;
import org.onosproject.net.topology.PathService;
import org.onosproject.net.topology.TopologyService;
import org.onosproject.store.service.StorageAdminService;
import org.onosproject.store.service.StorageService;
import org.osgi.framework.ServicePermission;
import org.osgi.framework.PackagePermission;
import org.osgi.framework.AdaptPermission;


import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class PolicyBuilder {

    private PolicyBuilder(){
    }

    public static PermissionInfo[] getApplicationPermissions(Map<Permission, Set<String>> serviceDirectory,
                                                             Set<Permission> permissions) {
        Set<PermissionInfo> permSet = Sets.newHashSet();
        Collections.addAll(permSet, getDefaultPerms());
        for (Permission perm : permissions) {
            permSet.add(new PermissionInfo(AppPermission.class.getName(), perm.name(), ""));
            permSet.addAll(serviceDirectory.get(perm).stream().map(service -> new PermissionInfo(
                    ServicePermission.class.getName(), service, ServicePermission.GET)).collect(Collectors.toList()));
        }
        PermissionInfo[] permissionInfos = new PermissionInfo[permSet.size()];
        return permSet.toArray(permissionInfos);
    }

    public static PermissionInfo[] getAdminApplicationPermissions(Map<Permission, Set<String>> serviceDirectory) {
        Set<PermissionInfo> permSet = Sets.newHashSet();
        Collections.addAll(permSet, getDefaultPerms());
        Collections.addAll(permSet, getAdminDefaultPerms());
        permSet.addAll(serviceDirectory.keySet().stream().map(perm ->
                new PermissionInfo(AppPermission.class.getName(), perm.name(), "")).collect(Collectors.toList()));
        PermissionInfo[] permissionInfos = new PermissionInfo[permSet.size()];
        return permSet.toArray(permissionInfos);
    }

    public static PermissionInfo[] getDefaultPerms() {
        return new PermissionInfo[]{
                new PermissionInfo(PackagePermission.class.getName(), "*", PackagePermission.EXPORTONLY),
                new PermissionInfo(PackagePermission.class.getName(), "*", PackagePermission.IMPORT),
                new PermissionInfo(AdaptPermission.class.getName(), "*", AdaptPermission.ADAPT),
        };
    }
    public static PermissionInfo[] getAdminDefaultPerms() {
        return new PermissionInfo[]{
                new PermissionInfo(ServicePermission.class.getName(),
                        ApplicationAdminService.class.getName(), ServicePermission.GET),
                new PermissionInfo(ServicePermission.class.getName(),
                        ClusterAdminService.class.getName(), ServicePermission.GET),
                new PermissionInfo(ServicePermission.class.getName(),
                        MastershipAdminService.class.getName(), ServicePermission.GET),
                new PermissionInfo(ServicePermission.class.getName(),
                        DeviceAdminService.class.getName(), ServicePermission.GET),
                new PermissionInfo(ServicePermission.class.getName(),
                        HostAdminService.class.getName(), ServicePermission.GET),
                new PermissionInfo(ServicePermission.class.getName(),
                        LinkAdminService.class.getName(), ServicePermission.GET),
                new PermissionInfo(ServicePermission.class.getName(),
                        DriverAdminService.class.getName(), ServicePermission.GET),
                new PermissionInfo(ServicePermission.class.getName(),
                        StorageAdminService.class.getName(), ServicePermission.GET),
//                new PermissionInfo(ServicePermission.class.getName(),
//                        LabelResourceAdminService.class.getName(), ServicePermission.GET),
//                new PermissionInfo(ServicePermission.class.getName(),
//                        TunnelAdminService.class.getName(), ServicePermission.GET),
                new PermissionInfo(ServicePermission.class.getName(),
                        ApplicationService.class.getName(), ServicePermission.GET),
                new PermissionInfo(ServicePermission.class.getName(),
                        ComponentConfigService.class.getName(), ServicePermission.GET),
                new PermissionInfo(ServicePermission.class.getName(),
                        CoreService.class.getName(), ServicePermission.GET),
                new PermissionInfo(ServicePermission.class.getName(),
                        ClusterService.class.getName(), ServicePermission.GET),
                new PermissionInfo(ServicePermission.class.getName(),
                        LeadershipService.class.getName(), ServicePermission.GET),
                new PermissionInfo(ServicePermission.class.getName(),
                        MastershipService.class.getName(), ServicePermission.GET),
                new PermissionInfo(ServicePermission.class.getName(),
                        DeviceService.class.getName(), ServicePermission.GET),
                new PermissionInfo(ServicePermission.class.getName(),
                        DeviceClockService.class.getName(), ServicePermission.GET),
                new PermissionInfo(ServicePermission.class.getName(),
                        DriverService.class.getName(), ServicePermission.GET),
                new PermissionInfo(ServicePermission.class.getName(),
                        FlowRuleService.class.getName(), ServicePermission.GET),
                new PermissionInfo(ServicePermission.class.getName(),
                        FlowObjectiveService.class.getName(), ServicePermission.GET),
                new PermissionInfo(ServicePermission.class.getName(),
                        GroupService.class.getName(), ServicePermission.GET),
                new PermissionInfo(ServicePermission.class.getName(),
                        HostService.class.getName(), ServicePermission.GET),
                new PermissionInfo(ServicePermission.class.getName(),
                        IntentService.class.getName(), ServicePermission.GET),
                new PermissionInfo(ServicePermission.class.getName(),
                        IntentClockService.class.getName(), ServicePermission.GET),
                new PermissionInfo(ServicePermission.class.getName(),
                        IntentExtensionService.class.getName(), ServicePermission.GET),
                new PermissionInfo(ServicePermission.class.getName(),
                        PartitionService.class.getName(), ServicePermission.GET),
                new PermissionInfo(ServicePermission.class.getName(),
                        LinkService.class.getName(), ServicePermission.GET),
                new PermissionInfo(ServicePermission.class.getName(),
                        LinkResourceService.class.getName(), ServicePermission.GET),
//                new PermissionInfo(ServicePermission.class.getName(),
//                        LabelResourceService.class.getName(), ServicePermission.GET),
                new PermissionInfo(ServicePermission.class.getName(),
                        PacketService.class.getName(), ServicePermission.GET),
                new PermissionInfo(ServicePermission.class.getName(),
                        ProxyArpService.class.getName(), ServicePermission.GET),
                new PermissionInfo(ServicePermission.class.getName(),
                        StatisticService.class.getName(), ServicePermission.GET),
                new PermissionInfo(ServicePermission.class.getName(),
                        PathService.class.getName(), ServicePermission.GET),
                new PermissionInfo(ServicePermission.class.getName(),
                        TopologyService.class.getName(), ServicePermission.GET),
//                new PermissionInfo(ServicePermission.class.getName(),
//                        TunnelService.class.getName(), ServicePermission.GET),
                new PermissionInfo(ServicePermission.class.getName(),
                        StorageService.class.getName(), ServicePermission.GET),
        };
    }


    public static Map<Permission, Set<String>> getServiceDirectory() {

        Map<Permission, Set<String>> serviceDirectory = new ConcurrentHashMap<>();

        serviceDirectory.put(Permission.APP_READ, ImmutableSet.of(
                ApplicationService.class.getName(), CoreService.class.getName()));
        serviceDirectory.put(Permission.APP_EVENT, ImmutableSet.of(
                ApplicationService.class.getName(), CoreService.class.getName()));
        serviceDirectory.put(Permission.CONFIG_READ, ImmutableSet.of(
                ComponentConfigService.class.getName()));
        serviceDirectory.put(Permission.CONFIG_WRITE, ImmutableSet.of(
                ComponentConfigService.class.getName()));
        serviceDirectory.put(Permission.CLUSTER_READ, ImmutableSet.of(
                ClusterService.class.getName(), LeadershipService.class.getName(),
                MastershipService.class.getName()));
        serviceDirectory.put(Permission.CLUSTER_WRITE, ImmutableSet.of(
                LeadershipService.class.getName(), MastershipService.class.getName()));
        serviceDirectory.put(Permission.CLUSTER_EVENT, ImmutableSet.of(
                ClusterService.class.getName(), LeadershipService.class.getName(),
                MastershipService.class.getName()));
        serviceDirectory.put(Permission.DEVICE_READ, ImmutableSet.of(
                DeviceService.class.getName(), DeviceClockService.class.getName()));
        serviceDirectory.put(Permission.DEVICE_EVENT, ImmutableSet.of(
                DeviceService.class.getName()));
        serviceDirectory.put(Permission.DRIVER_READ, ImmutableSet.of(
                DriverService.class.getName()));
        serviceDirectory.put(Permission.DRIVER_WRITE, ImmutableSet.of(
                DriverService.class.getName()));
        serviceDirectory.put(Permission.FLOWRULE_READ, ImmutableSet.of(
                FlowRuleService.class.getName()));
        serviceDirectory.put(Permission.FLOWRULE_WRITE, ImmutableSet.of(
                FlowRuleService.class.getName(), FlowObjectiveService.class.getName()));
        serviceDirectory.put(Permission.FLOWRULE_EVENT, ImmutableSet.of(
                FlowRuleService.class.getName()));
        serviceDirectory.put(Permission.GROUP_READ, ImmutableSet.of(
                GroupService.class.getName()));
        serviceDirectory.put(Permission.GROUP_WRITE, ImmutableSet.of(
                GroupService.class.getName()));
        serviceDirectory.put(Permission.GROUP_EVENT, ImmutableSet.of(
                GroupService.class.getName()));
        serviceDirectory.put(Permission.HOST_WRITE, ImmutableSet.of(
                HostService.class.getName()));
        serviceDirectory.put(Permission.HOST_EVENT, ImmutableSet.of(
                HostService.class.getName()));
        serviceDirectory.put(Permission.INTENT_READ, ImmutableSet.of(
                IntentService.class.getName(), PartitionService.class.getName(),
                IntentClockService.class.getName()));
        serviceDirectory.put(Permission.INTENT_WRITE, ImmutableSet.of(
                IntentService.class.getName()));
        serviceDirectory.put(Permission.INTENT_EVENT, ImmutableSet.of(
                IntentService.class.getName()));
//        serviceDirectory.put(Permission.LINK_READ, ImmutableSet.of(
//                LinkService.class.getName(), LinkResourceService.class.getName(),
//                LabelResourceService.class.getName()));
//        serviceDirectory.put(Permission.LINK_WRITE, ImmutableSet.of(
//                LinkResourceService.class.getName(), LabelResourceService.class.getName()));
//        serviceDirectory.put(Permission.LINK_EVENT, ImmutableSet.of(
//                LinkService.class.getName(), LinkResourceService.class.getName(),
//                LabelResourceService.class.getName()));
        serviceDirectory.put(Permission.PACKET_READ, ImmutableSet.of(
                PacketService.class.getName(), ProxyArpService.class.getName()));
        serviceDirectory.put(Permission.PACKET_WRITE, ImmutableSet.of(
                PacketService.class.getName(), ProxyArpService.class.getName()));
        serviceDirectory.put(Permission.PACKET_EVENT, ImmutableSet.of(
                PacketService.class.getName()));
        serviceDirectory.put(Permission.STATISTIC_READ, ImmutableSet.of(
                StatisticService.class.getName()));
        serviceDirectory.put(Permission.TOPOLOGY_READ, ImmutableSet.of(
                TopologyService.class.getName(), PathService.class.getName()));
        serviceDirectory.put(Permission.TOPOLOGY_EVENT, ImmutableSet.of(
                TopologyService.class.getName()));
//        serviceDirectory.put(Permission.TUNNEL_READ, ImmutableSet.of(
//                TunnelService.class.getName()));
//        serviceDirectory.put(Permission.TUNNEL_WRITE, ImmutableSet.of(
//                TunnelService.class.getName()));
//        serviceDirectory.put(Permission.TUNNEL_EVENT, ImmutableSet.of(
//                TunnelService.class.getName()));
        serviceDirectory.put(Permission.STORAGE_WRITE, ImmutableSet.of(
                StorageService.class.getName()));

        return serviceDirectory;
    }
}


//    public static PermissionInfo[] getNonAdminPerms() {
//        return new PermissionInfo[]{
//                new PermissionInfo(PackagePermission.class.getName(), "*", PackagePermission.EXPORTONLY),
//                new PermissionInfo(PackagePermission.class.getName(), "*", PackagePermission.IMPORT),
//                new PermissionInfo(AdaptPermission.class.getName(), "*", AdaptPermission.ADAPT),
//                new PermissionInfo(ServicePermission.class.getName(),
//                        ApplicationService.class.getName(), ServicePermission.GET),
//                new PermissionInfo(ServicePermission.class.getName(),
//                        ComponentConfigService.class.getName(), ServicePermission.GET),
//                new PermissionInfo(ServicePermission.class.getName(),
//                        CoreService.class.getName(), ServicePermission.GET),
//                new PermissionInfo(ServicePermission.class.getName(),
//                        ClusterService.class.getName(), ServicePermission.GET),
//                new PermissionInfo(ServicePermission.class.getName(),
//                        LeadershipService.class.getName(), ServicePermission.GET),
//                new PermissionInfo(ServicePermission.class.getName(),
//                        MastershipService.class.getName(), ServicePermission.GET),
//                new PermissionInfo(ServicePermission.class.getName(),
//                        DeviceService.class.getName(), ServicePermission.GET),
//                new PermissionInfo(ServicePermission.class.getName(),
//                        DeviceClockService.class.getName(), ServicePermission.GET),
//                new PermissionInfo(ServicePermission.class.getName(),
//                        DriverService.class.getName(), ServicePermission.GET),
//                new PermissionInfo(ServicePermission.class.getName(),
//                        FlowRuleService.class.getName(), ServicePermission.GET),
//                new PermissionInfo(ServicePermission.class.getName(),
//                        FlowObjectiveService.class.getName(), ServicePermission.GET),
//                new PermissionInfo(ServicePermission.class.getName(),
//                        GroupService.class.getName(), ServicePermission.GET),
//                new PermissionInfo(ServicePermission.class.getName(),
//                        HostService.class.getName(), ServicePermission.GET),
//                new PermissionInfo(ServicePermission.class.getName(),
//                        HostClockService.class.getName(), ServicePermission.GET),
//                new PermissionInfo(ServicePermission.class.getName(),
//                        IntentService.class.getName(), ServicePermission.GET),
//                new PermissionInfo(ServicePermission.class.getName(),
//                        IntentClockService.class.getName(), ServicePermission.GET),
//                new PermissionInfo(ServicePermission.class.getName(),
//                        IntentExtensionService.class.getName(), ServicePermission.GET),
//                new PermissionInfo(ServicePermission.class.getName(),
//                        PartitionService.class.getName(), ServicePermission.GET),
//                new PermissionInfo(ServicePermission.class.getName(),
//                        LinkService.class.getName(), ServicePermission.GET),
//                new PermissionInfo(ServicePermission.class.getName(),
//                        LinkResourceService.class.getName(), ServicePermission.GET),
//                new PermissionInfo(ServicePermission.class.getName(),
//                        LabelResourceService.class.getName(), ServicePermission.GET),
//                new PermissionInfo(ServicePermission.class.getName(),
//                        PacketService.class.getName(), ServicePermission.GET),
//                new PermissionInfo(ServicePermission.class.getName(),
//                        ProxyArpService.class.getName(), ServicePermission.GET),
//                new PermissionInfo(ServicePermission.class.getName(),
//                        StatisticService.class.getName(), ServicePermission.GET),
//                new PermissionInfo(ServicePermission.class.getName(),
//                        PathService.class.getName(), ServicePermission.GET),
//                new PermissionInfo(ServicePermission.class.getName(),
//                        TopologyService.class.getName(), ServicePermission.GET),
//                new PermissionInfo(ServicePermission.class.getName(),
//                        TunnelService.class.getName(), ServicePermission.GET),
//                new PermissionInfo(ServicePermission.class.getName(),
//                        StorageService.class.getName(), ServicePermission.GET),
//        };
//    }
