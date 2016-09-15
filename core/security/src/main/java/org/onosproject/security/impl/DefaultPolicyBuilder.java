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


import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.onosproject.cluster.ClusterAdminService;
import org.onosproject.cluster.ClusterMetadataService;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.ClusterMetadataAdminService;
import org.onosproject.cluster.LeadershipService;
import org.onosproject.cluster.LeadershipAdminService;
import org.onosproject.codec.CodecService;
import org.onosproject.event.EventDeliveryService;
import org.onosproject.mastership.MastershipTermService;
import org.onosproject.net.config.BasicNetworkConfigService;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.edge.EdgePortService;
import org.onosproject.net.key.DeviceKeyAdminService;
import org.onosproject.net.key.DeviceKeyService;
import org.onosproject.net.resource.ResourceAdminService;
import org.onosproject.net.resource.ResourceService;
import org.onosproject.net.region.RegionAdminService;
import org.onosproject.net.region.RegionService;
import org.onosproject.net.statistic.FlowStatisticService;
import org.onosproject.persistence.PersistenceService;
import org.onosproject.security.AppPermission;
import org.onosproject.app.ApplicationAdminService;
import org.onosproject.app.ApplicationService;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.core.CoreService;
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
import org.onosproject.net.intent.WorkPartitionService;
import org.onosproject.net.link.LinkAdminService;
import org.onosproject.net.link.LinkService;
import org.onosproject.net.packet.PacketService;
import org.onosproject.net.proxyarp.ProxyArpService;
import org.onosproject.net.statistic.StatisticService;
import org.onosproject.net.topology.PathService;
import org.onosproject.net.topology.TopologyService;
import org.onosproject.security.SecurityAdminService;
import org.onosproject.store.cluster.messaging.ClusterCommunicationService;
import org.onosproject.store.cluster.messaging.MessagingService;
import org.onosproject.store.primitives.PartitionAdminService;
import org.onosproject.store.primitives.PartitionService;
import org.onosproject.store.service.LogicalClockService;
import org.onosproject.store.service.StorageAdminService;
import org.onosproject.store.service.StorageService;
import org.onosproject.ui.UiExtensionService;
import org.osgi.framework.ServicePermission;
import org.osgi.framework.AdminPermission;
import org.osgi.framework.AdaptPermission;
import org.osgi.framework.CapabilityPermission;
import org.osgi.framework.BundlePermission;
import org.osgi.framework.PackagePermission;
import org.osgi.service.cm.ConfigurationPermission;

import javax.net.ssl.SSLPermission;
import javax.security.auth.AuthPermission;
import javax.security.auth.PrivateCredentialPermission;
import javax.security.auth.kerberos.DelegationPermission;
import javax.sound.sampled.AudioPermission;
import java.io.FilePermission;
import java.io.SerializablePermission;
import java.lang.reflect.ReflectPermission;
import java.net.NetPermission;
import java.net.SocketPermission;
import java.security.Permissions;
import java.sql.SQLPermission;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.PropertyPermission;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.security.Permission;
import java.util.logging.LoggingPermission;

import static org.onosproject.security.AppPermission.Type.*;

public final class DefaultPolicyBuilder {

    protected static ConcurrentHashMap<AppPermission.Type,
            Set<String>> serviceDirectory = getServiceDirectory();

    protected static List<Permission> defaultPermissions = getDefaultPerms();
    protected static List<Permission> adminServicePermissions = getAdminDefaultPerms();

    private DefaultPolicyBuilder(){
    }

    public static List<Permission> getUserApplicationPermissions(Set<org.onosproject.security.Permission> permissions) {
        List<Permission> perms = Lists.newArrayList();
        perms.addAll(defaultPermissions);
        perms.addAll(convertToJavaPermissions(permissions));
        return optimizePermissions(perms);
    }

    public static List<Permission> getAdminApplicationPermissions(
            Set<org.onosproject.security.Permission> permissions) {
        List<Permission> perms = Lists.newArrayList();
        perms.addAll(defaultPermissions);
        perms.addAll(adminServicePermissions);
        for (AppPermission.Type perm : serviceDirectory.keySet()) {
            perms.add(new AppPermission(perm));
        }
        perms.addAll(convertToJavaPermissions(permissions));
        return optimizePermissions(perms);
    }

    public static List<Permission> convertToJavaPermissions(Set<org.onosproject.security.Permission> permissions) {
        List<Permission> result = Lists.newArrayList();
        for (org.onosproject.security.Permission perm : permissions) {
            Permission javaPerm = getPermission(perm);
            if (javaPerm != null) {
                if (javaPerm instanceof AppPermission) {
                    if (((AppPermission) javaPerm).getType() != null) {
                        AppPermission ap = (AppPermission) javaPerm;
                        result.add(ap);
                        if (serviceDirectory.containsKey(ap.getType())) {
                            for (String service : serviceDirectory.get(ap.getType())) {
                                result.add(new ServicePermission(service, ServicePermission.GET));
                            }
                        }
                    }
                } else if (javaPerm instanceof ServicePermission) {
                    if (!javaPerm.getName().contains(SecurityAdminService.class.getName())) {
                        result.add(javaPerm);
                    }
                } else {
                    result.add(javaPerm);
                }

            }
        }
        return result;
    }

    public static Set<org.onosproject.security.Permission> convertToOnosPermissions(List<Permission> permissions) {
        Set<org.onosproject.security.Permission> result = Sets.newHashSet();
        for (Permission perm : permissions) {
            org.onosproject.security.Permission onosPerm = getOnosPermission(perm);
            if (onosPerm != null) {
                result.add(onosPerm);
            }
        }
        return result;
    }

    public static List<Permission> getDefaultPerms() {
        List<Permission> permSet = Lists.newArrayList();
        permSet.add(new PackagePermission("*", PackagePermission.EXPORTONLY));
        permSet.add(new PackagePermission("*", PackagePermission.IMPORT));
        permSet.add(new AdaptPermission("*", AdaptPermission.ADAPT));
        permSet.add(new ConfigurationPermission("*", ConfigurationPermission.CONFIGURE));
        permSet.add(new AdminPermission("*", AdminPermission.METADATA));
        return permSet;
    }

    private static List<Permission> getAdminDefaultPerms() {
        List<Permission> permSet = Lists.newArrayList();
        permSet.add(new ServicePermission(ApplicationAdminService.class.getName(), ServicePermission.GET));
        permSet.add(new ServicePermission(ClusterAdminService.class.getName(), ServicePermission.GET));
        permSet.add(new ServicePermission(LeadershipAdminService.class.getName(), ServicePermission.GET));
        permSet.add(new ServicePermission(ClusterMetadataAdminService.class.getName(), ServicePermission.GET));
        permSet.add(new ServicePermission(MastershipAdminService.class.getName(), ServicePermission.GET));
        permSet.add(new ServicePermission(DeviceAdminService.class.getName(), ServicePermission.GET));
        permSet.add(new ServicePermission(DriverAdminService.class.getName(), ServicePermission.GET));
        permSet.add(new ServicePermission(HostAdminService.class.getName(), ServicePermission.GET));
        permSet.add(new ServicePermission(DeviceKeyAdminService.class.getName(), ServicePermission.GET));
        permSet.add(new ServicePermission(LinkAdminService.class.getName(), ServicePermission.GET));
        permSet.add(new ServicePermission(ResourceAdminService.class.getName(), ServicePermission.GET));
        permSet.add(new ServicePermission(RegionAdminService.class.getName(), ServicePermission.GET));
        permSet.add(new ServicePermission(PartitionAdminService.class.getName(), ServicePermission.GET));
        permSet.add(new ServicePermission(StorageAdminService.class.getName(), ServicePermission.GET));

        permSet.add(new ServicePermission(ApplicationService.class.getName(), ServicePermission.GET));
        permSet.add(new ServicePermission(ComponentConfigService.class.getName(), ServicePermission.GET));
        permSet.add(new ServicePermission(ClusterMetadataService.class.getName(), ServicePermission.GET));
        permSet.add(new ServicePermission(ClusterService.class.getName(), ServicePermission.GET));
        permSet.add(new ServicePermission(LeadershipService.class.getName(), ServicePermission.GET));
        permSet.add(new ServicePermission(CodecService.class.getName(), ServicePermission.GET));
        permSet.add(new ServicePermission(CoreService.class.getName(), ServicePermission.GET));
        permSet.add(new ServicePermission(EventDeliveryService.class.getName(), ServicePermission.GET));
        permSet.add(new ServicePermission(MastershipService.class.getName(), ServicePermission.GET));
        permSet.add(new ServicePermission(MastershipTermService.class.getName(), ServicePermission.GET));
        permSet.add(new ServicePermission(BasicNetworkConfigService.class.getName(), ServicePermission.GET));
        permSet.add(new ServicePermission(NetworkConfigService.class.getName(), ServicePermission.GET));
        permSet.add(new ServicePermission(DeviceService.class.getName(), ServicePermission.GET));
        permSet.add(new ServicePermission(DeviceClockService.class.getName(), ServicePermission.GET));
        permSet.add(new ServicePermission(DriverService.class.getName(), ServicePermission.GET));
        permSet.add(new ServicePermission(EdgePortService.class.getName(), ServicePermission.GET));
        permSet.add(new ServicePermission(FlowRuleService.class.getName(), ServicePermission.GET));
        permSet.add(new ServicePermission(FlowObjectiveService.class.getName(), ServicePermission.GET));
        permSet.add(new ServicePermission(GroupService.class.getName(), ServicePermission.GET));
        permSet.add(new ServicePermission(HostService.class.getName(), ServicePermission.GET));
        permSet.add(new ServicePermission(IntentService.class.getName(), ServicePermission.GET));
        permSet.add(new ServicePermission(IntentClockService.class.getName(), ServicePermission.GET));
        permSet.add(new ServicePermission(IntentExtensionService.class.getName(), ServicePermission.GET));
        permSet.add(new ServicePermission(WorkPartitionService.class.getName(), ServicePermission.GET));
        permSet.add(new ServicePermission(DeviceKeyService.class.getName(), ServicePermission.GET));
        permSet.add(new ServicePermission(LinkService.class.getName(), ServicePermission.GET));
//        permSet.add(new ServicePermission(MulticastRouteService.class.getName(), ServicePermission.GET));
//        permSet.add(new ServicePermission(MeterService.class.getName(), ServicePermission.GET));
        permSet.add(new ServicePermission(ResourceService.class.getName(), ServicePermission.GET));
        permSet.add(new ServicePermission(PacketService.class.getName(), ServicePermission.GET));
        permSet.add(new ServicePermission(ProxyArpService.class.getName(), ServicePermission.GET));
        permSet.add(new ServicePermission(RegionService.class.getName(), ServicePermission.GET));
//      permSet.add(new ServicePermission(LinkResourceService.class.getName(), ServicePermission.GET));
        permSet.add(new ServicePermission(FlowStatisticService.class.getName(), ServicePermission.GET));
        permSet.add(new ServicePermission(StatisticService.class.getName(), ServicePermission.GET));
        permSet.add(new ServicePermission(PathService.class.getName(), ServicePermission.GET));
        permSet.add(new ServicePermission(TopologyService.class.getName(), ServicePermission.GET));
        permSet.add(new ServicePermission(PersistenceService.class.getName(), ServicePermission.GET));
//        permSet.add(new ServicePermission(ApiDocService.class.getName(), ServicePermission.GET));
        permSet.add(new ServicePermission(ClusterCommunicationService.class.getName(), ServicePermission.GET));
        permSet.add(new ServicePermission(MessagingService.class.getName(), ServicePermission.GET));
        permSet.add(new ServicePermission(PartitionService.class.getName(), ServicePermission.GET));
        permSet.add(new ServicePermission(LogicalClockService.class.getName(), ServicePermission.GET));
        permSet.add(new ServicePermission(StorageService.class.getName(), ServicePermission.GET));
        permSet.add(new ServicePermission(UiExtensionService.class.getName(), ServicePermission.GET));

        return permSet;
    }

    public static Set<String> getNBServiceList() {
        Set<String> permString = new HashSet<>();
        for (Permission perm : getAdminDefaultPerms()) {
            permString.add(perm.getName());
        }
        return permString;
    }

    private static ConcurrentHashMap<AppPermission.Type, Set<String>> getServiceDirectory() {

        ConcurrentHashMap<AppPermission.Type, Set<String>> serviceDirectory = new ConcurrentHashMap<>();

        serviceDirectory.put(APP_READ, ImmutableSet.of(
                ApplicationService.class.getName(), CoreService.class.getName()));
        serviceDirectory.put(APP_EVENT, ImmutableSet.of(
                ApplicationService.class.getName(), CoreService.class.getName()));
        serviceDirectory.put(APP_WRITE, ImmutableSet.of(
                CoreService.class.getName()));
        serviceDirectory.put(CONFIG_READ, ImmutableSet.of(
                ComponentConfigService.class.getName(), NetworkConfigService.class.getName()));
        serviceDirectory.put(CONFIG_WRITE, ImmutableSet.of(
                ComponentConfigService.class.getName(), NetworkConfigService.class.getName()));
        serviceDirectory.put(CONFIG_EVENT, ImmutableSet.of(
                NetworkConfigService.class.getName()));
        serviceDirectory.put(CLUSTER_READ, ImmutableSet.of(
                ClusterService.class.getName(), LeadershipService.class.getName(),
                MastershipService.class.getName(), ClusterMetadataService.class.getName(),
                MastershipTermService.class.getName()));
        serviceDirectory.put(CLUSTER_WRITE, ImmutableSet.of(
                LeadershipService.class.getName(), MastershipService.class.getName(),
                ClusterCommunicationService.class.getName(), MessagingService.class.getName()));
        serviceDirectory.put(CLUSTER_EVENT, ImmutableSet.of(
                ClusterService.class.getName(), LeadershipService.class.getName(),
                MastershipService.class.getName()));
        serviceDirectory.put(DEVICE_READ, ImmutableSet.of(
                DeviceService.class.getName(), DeviceClockService.class.getName()));
        serviceDirectory.put(DEVICE_EVENT, ImmutableSet.of(
                DeviceService.class.getName()));
        serviceDirectory.put(DRIVER_READ, ImmutableSet.of(
                DriverService.class.getName()));
        serviceDirectory.put(DRIVER_WRITE, ImmutableSet.of(
                DriverService.class.getName()));
        serviceDirectory.put(FLOWRULE_READ, ImmutableSet.of(
                FlowRuleService.class.getName()));
        serviceDirectory.put(FLOWRULE_WRITE, ImmutableSet.of(
                FlowRuleService.class.getName(), FlowObjectiveService.class.getName()));
        serviceDirectory.put(FLOWRULE_EVENT, ImmutableSet.of(
                FlowRuleService.class.getName()));
        serviceDirectory.put(GROUP_READ, ImmutableSet.of(
                GroupService.class.getName()));
        serviceDirectory.put(GROUP_WRITE, ImmutableSet.of(
                GroupService.class.getName()));
        serviceDirectory.put(GROUP_EVENT, ImmutableSet.of(
                GroupService.class.getName()));
        serviceDirectory.put(HOST_READ, ImmutableSet.of(
                HostService.class.getName()));
        serviceDirectory.put(HOST_WRITE, ImmutableSet.of(
                HostService.class.getName()));
        serviceDirectory.put(HOST_EVENT, ImmutableSet.of(
                HostService.class.getName()));
        serviceDirectory.put(INTENT_READ, ImmutableSet.of(
                IntentService.class.getName(), WorkPartitionService.class.getName(),
                IntentClockService.class.getName(), IntentExtensionService.class.getName()));
        serviceDirectory.put(INTENT_WRITE, ImmutableSet.of(
                IntentService.class.getName(), IntentExtensionService.class.getName()));
        serviceDirectory.put(INTENT_EVENT, ImmutableSet.of(
                IntentService.class.getName(), WorkPartitionService.class.getName()));
//        serviceDirectory.put(LINK_READ, ImmutableSet.of(
//                LinkService.class.getName(), LinkResourceService.class.getName(),
//                LabelResourceService.class.getName()));
//        serviceDirectory.put(LINK_WRITE, ImmutableSet.of(
//                LinkResourceService.class.getName(), LabelResourceService.class.getName()));
//        serviceDirectory.put(LINK_EVENT, ImmutableSet.of(
//                LinkService.class.getName(), LinkResourceService.class.getName(),
//                LabelResourceService.class.getName()));
        serviceDirectory.put(PACKET_READ, ImmutableSet.of(
                PacketService.class.getName(), ProxyArpService.class.getName()));
        serviceDirectory.put(PACKET_WRITE, ImmutableSet.of(
                PacketService.class.getName(), ProxyArpService.class.getName(),
                EdgePortService.class.getName()));
        serviceDirectory.put(PACKET_EVENT, ImmutableSet.of(
                PacketService.class.getName()));
        serviceDirectory.put(STATISTIC_READ, ImmutableSet.of(
                StatisticService.class.getName(), FlowStatisticService.class.getName()));
        serviceDirectory.put(TOPOLOGY_READ, ImmutableSet.of(
                TopologyService.class.getName(), PathService.class.getName(),
                EdgePortService.class.getName()));
        serviceDirectory.put(TOPOLOGY_EVENT, ImmutableSet.of(
                TopologyService.class.getName()));
//        serviceDirectory.put(TUNNEL_READ, ImmutableSet.of(
//                TunnelService.class.getName()));
//        serviceDirectory.put(TUNNEL_WRITE, ImmutableSet.of(
//                TunnelService.class.getName()));
//        serviceDirectory.put(TUNNEL_EVENT, ImmutableSet.of(
//                TunnelService.class.getName()));
        serviceDirectory.put(STORAGE_WRITE, ImmutableSet.of(
                StorageService.class.getName()));
        serviceDirectory.put(CODEC_READ, ImmutableSet.of(
                CodecService.class.getName()));
        serviceDirectory.put(CODEC_WRITE, ImmutableSet.of(
                CodecService.class.getName()));
        serviceDirectory.put(EVENT_READ, ImmutableSet.of(
                EventDeliveryService.class.getName()));
        serviceDirectory.put(EVENT_WRITE, ImmutableSet.of(
                EventDeliveryService.class.getName()));
        serviceDirectory.put(RESOURCE_READ, ImmutableSet.of(
                ResourceService.class.getName()));
        serviceDirectory.put(RESOURCE_WRITE, ImmutableSet.of(
                ResourceService.class.getName()));
        serviceDirectory.put(RESOURCE_EVENT, ImmutableSet.of(
                ResourceService.class.getName()));
        serviceDirectory.put(REGION_READ, ImmutableSet.of(
                RegionService.class.getName()));
        serviceDirectory.put(PERSISTENCE_WRITE, ImmutableSet.of(
                PersistenceService.class.getName()));
        serviceDirectory.put(PARTITION_READ, ImmutableSet.of(
                PartitionService.class.getName()));
        serviceDirectory.put(PARTITION_EVENT, ImmutableSet.of(
                PartitionService.class.getName()));
        serviceDirectory.put(CLOCK_WRITE, ImmutableSet.of(
                LogicalClockService.class.getName()));

        return serviceDirectory;
    }


    public static org.onosproject.security.Permission getOnosPermission(Permission permission) {
        if (permission instanceof AppPermission) {
            return new org.onosproject.security.Permission(AppPermission.class.getName(), permission.getName(), "");
        } else if (permission instanceof FilePermission) {
            return new org.onosproject.security.Permission(
                    FilePermission.class.getName(), permission.getName(), permission.getActions());
        } else if (permission instanceof SerializablePermission) {
            return new org.onosproject.security.Permission(
                    SerializablePermission.class.getName(), permission.getName(), permission.getActions());
        } else if (permission instanceof NetPermission) {
            return new org.onosproject.security.Permission(
                    NetPermission.class.getName(), permission.getName(), permission.getActions());
        } else if (permission instanceof RuntimePermission) {
            return new org.onosproject.security.Permission(
                    RuntimePermission.class.getName(), permission.getName(), permission.getActions());
        } else if (permission instanceof SocketPermission) {
            return new org.onosproject.security.Permission(
                    SocketPermission.class.getName(), permission.getName(), permission.getActions());
        } else if (permission instanceof SQLPermission) {
            return new org.onosproject.security.Permission(
                    SQLPermission.class.getName(), permission.getName(), permission.getActions());
        } else if (permission instanceof PropertyPermission) {
            return new org.onosproject.security.Permission(
                    PropertyPermission.class.getName(), permission.getName(), permission.getActions());
        } else if (permission instanceof LoggingPermission) {
            return new org.onosproject.security.Permission(
                    LoggingPermission.class.getName(), permission.getName(), permission.getActions());
        } else if (permission instanceof SSLPermission) {
            return new org.onosproject.security.Permission(
                    SSLPermission.class.getName(), permission.getName(), permission.getActions());
        } else if (permission instanceof AuthPermission) {
            return new org.onosproject.security.Permission(
                    AuthPermission.class.getName(), permission.getName(), permission.getActions());
        } else if (permission instanceof PrivateCredentialPermission) {
            return new org.onosproject.security.Permission(
                    PrivateCredentialPermission.class.getName(), permission.getName(), permission.getActions());
        } else if (permission instanceof DelegationPermission) {
            return new org.onosproject.security.Permission(
                    DelegationPermission.class.getName(), permission.getName(), permission.getActions());
        } else if (permission instanceof javax.security.auth.kerberos.ServicePermission) {
            return new org.onosproject.security.Permission(
                    javax.security.auth.kerberos.ServicePermission.class.getName(), permission.getName(),
                    permission.getActions());
        } else if (permission instanceof AudioPermission) {
            return new org.onosproject.security.Permission(
                    AudioPermission.class.getName(), permission.getName(), permission.getActions());
        } else if (permission instanceof AdaptPermission) {
            return new org.onosproject.security.Permission(
                    AdaptPermission.class.getName(), permission.getName(), permission.getActions());
        } else if (permission instanceof BundlePermission) {
            return new org.onosproject.security.Permission(
                    BundlePermission.class.getName(), permission.getName(), permission.getActions());
        } else if (permission instanceof CapabilityPermission) {
            return new org.onosproject.security.Permission(
                    CapabilityPermission.class.getName(), permission.getName(), permission.getActions());
        } else if (permission instanceof PackagePermission) {
            return new org.onosproject.security.Permission(
                    PackagePermission.class.getName(), permission.getName(), permission.getActions());
        } else if (permission instanceof ServicePermission) {
            return new org.onosproject.security.Permission(
                    ServicePermission.class.getName(), permission.getName(), permission.getActions());
        } else if (permission instanceof AdminPermission) {
            return new org.onosproject.security.Permission(
                    AdminPermission.class.getName(), permission.getName(), permission.getActions());
        } else if (permission instanceof ConfigurationPermission) {
            return new org.onosproject.security.Permission(
                    ConfigurationPermission.class.getName(), permission.getName(), permission.getActions());
        }
        return null;
    }

    private static Permission getPermission(org.onosproject.security.Permission permission) {

        String classname = permission.getClassName();
        String name = permission.getName();
        String actions = permission.getActions();

        if (classname == null || name == null) {
            return null;
        }
        classname = classname.trim();
        name = name.trim();
        actions = actions.trim();

        if (AppPermission.class.getName().equals(classname)) {
            return new AppPermission(name);
        } else if (FilePermission.class.getName().equals(classname)) {
            return new FilePermission(name, actions);
        } else if (SerializablePermission.class.getName().equals(classname)) {
            return new SerializablePermission(name, actions);
        } else if (NetPermission.class.getName().equals(classname)) {
            return new NetPermission(name, actions);
        } else if (RuntimePermission.class.getName().equals(classname)) {
            return new RuntimePermission(name, actions);
        } else if (SocketPermission.class.getName().equals(classname)) {
            return new SocketPermission(name, actions);
        } else if (SQLPermission.class.getName().equals(classname)) {
            return new SQLPermission(name, actions);
        } else if (PropertyPermission.class.getName().equals(classname)) {
            return new PropertyPermission(name, actions);
        } else if (LoggingPermission.class.getName().equals(classname)) {
            return new LoggingPermission(name, actions);
        } else if (SSLPermission.class.getName().equals(classname)) {
            return new SSLPermission(name, actions);
        } else if (AuthPermission.class.getName().equals(classname)) {
            return new AuthPermission(name, actions);
        } else if (PrivateCredentialPermission.class.getName().equals(classname)) {
            return new PrivateCredentialPermission(name, actions);
        } else if (DelegationPermission.class.getName().equals(classname)) {
            return new DelegationPermission(name, actions);
        } else if (javax.security.auth.kerberos.ServicePermission.class.getName().equals(classname)) {
            return new javax.security.auth.kerberos.ServicePermission(name, actions);
        } else if (AudioPermission.class.getName().equals(classname)) {
            return new AudioPermission(name, actions);
        } else if (AdaptPermission.class.getName().equals(classname)) {
            return new AdaptPermission(name, actions);
        } else if (BundlePermission.class.getName().equals(classname)) {
            return new BundlePermission(name, actions);
        } else if (CapabilityPermission.class.getName().equals(classname)) {
            return new CapabilityPermission(name, actions);
        } else if (PackagePermission.class.getName().equals(classname)) {
            return new PackagePermission(name, actions);
        } else if (ServicePermission.class.getName().equals(classname)) {
            return new ServicePermission(name, actions);
        } else if (AdminPermission.class.getName().equals(classname)) {
            return new AdminPermission(name, actions);
        } else if (ConfigurationPermission.class.getName().equals(classname)) {
            return new ConfigurationPermission(name, actions);
        } else if (ReflectPermission.class.getName().equals(classname)) {
            return new ReflectPermission(name, actions);
        }

        //AllPermission, SecurityPermission, UnresolvedPermission
        //AWTPermission,  ReflectPermission not allowed
        return null;

    }
    private static List<Permission> optimizePermissions(List<Permission> perms) {
        Permissions permissions = listToPermissions(perms);
        return permissionsToList(permissions);
    }

    private static List<Permission> permissionsToList(Permissions perms) {
        List<Permission> permissions = new ArrayList<>();
        Enumeration<Permission> e = perms.elements();
        while (e.hasMoreElements()) {
            permissions.add(e.nextElement());
        }
        return permissions;
    }

    private static Permissions listToPermissions(List<Permission> perms) {
        Permissions permissions = new Permissions();
        for (Permission perm : perms) {
            permissions.add(perm);
        }
        return permissions;
    }
}
