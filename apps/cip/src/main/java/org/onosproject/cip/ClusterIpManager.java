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
package org.onosproject.cip;

import com.google.common.io.ByteStreams;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.LeadershipEvent;
import org.onosproject.cluster.LeadershipEventListener;
import org.onosproject.cluster.LeadershipService;
import org.onosproject.cluster.NodeId;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Objects;
import java.util.Properties;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.onlab.util.Tools.get;

/**
 * Manages cluster IP address alias.
 *
 * To use the application, simply install it on ONOS and then configure it
 * with the desired alias IP/mask/adapter configuration.
 *
 * If you are running it using upstart, you can also add the following
 * command to the /opt/onos/options file:
 *
 * sudo ifconfig eth0:0 down       # use the desired alias adapter
 *
 * This will make sure that if the process is killed abruptly, the IP alias
 * will be dropped upon respawn.
 */
@Component(immediate = true)
public class ClusterIpManager {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String CLUSTER_IP = "cluster/ip";

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LeadershipService leadershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ComponentConfigService cfgService;

    private final LeadershipEventListener listener = new InternalLeadershipListener();

    private NodeId localId;
    private boolean wasLeader = false;

    // By default there is no IP; this has to be configured
    @Property(name = "aliasIp", value = "", label = "Alias IP address")
    private String aliasIp = "";

    public static final String DEFAULT_MASK = "255.255.0.0";
    @Property(name = "aliasMask", value = DEFAULT_MASK, label = "Alias IP mask")
    private String aliasMask = DEFAULT_MASK;

    public static final String ETH_0 = "eth0:0";
    @Property(name = "aliasAdapter", value = ETH_0, label = "Alias IP adapter")
    private String aliasAdapter = ETH_0;

    @Activate
    protected void activate(ComponentContext context) {
        cfgService.registerProperties(getClass());

        localId = clusterService.getLocalNode().id();
        processLeaderChange(leadershipService.getLeader(CLUSTER_IP));

        leadershipService.addListener(listener);
        leadershipService.runForLeadership(CLUSTER_IP);
        log.info("Started");
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        cfgService.unregisterProperties(getClass(), false);

        removeIpAlias(aliasIp, aliasMask, aliasAdapter);

        leadershipService.removeListener(listener);
        leadershipService.withdraw(CLUSTER_IP);
        log.info("Stopped");
    }

    @Modified
    protected void modified(ComponentContext context) {
        log.info("Received configuration change...");
        Dictionary<?, ?> properties = context != null ? context.getProperties() : new Properties();
        String newIp = get(properties, "aliasIp");
        String newMask = get(properties, "aliasMask");
        String newAdapter = get(properties, "aliasAdapter");

        // Process any changes in the parameters...
        if (!Objects.equals(newIp, aliasIp) ||
                !Objects.equals(newMask, aliasMask) ||
                !Objects.equals(newAdapter, aliasAdapter)) {
            synchronized (this) {
                log.info("Reconfiguring with aliasIp={}, aliasMask={}, aliasAdapter={}, wasLeader={}",
                         newIp, newMask, newAdapter, wasLeader);
                if (wasLeader) {
                    removeIpAlias(aliasIp, aliasMask, aliasAdapter);
                    addIpAlias(newIp, newMask, newAdapter);
                }
                aliasIp = newIp;
                aliasMask = newMask;
                aliasAdapter = newAdapter;
            }
        }
    }

    private synchronized void processLeaderChange(NodeId newLeader) {
        boolean isLeader = Objects.equals(newLeader, localId);
        log.info("Processing leadership change; wasLeader={}, isLeader={}", wasLeader, isLeader);
        if (!wasLeader && isLeader) {
            // Gaining leadership, so setup the IP alias
            addIpAlias(aliasIp, aliasMask, aliasAdapter);
            wasLeader = true;
        } else if (wasLeader && !isLeader) {
            // Loosing leadership, so drop the IP alias
            removeIpAlias(aliasIp, aliasMask, aliasAdapter);
            wasLeader = false;
        }
    }

    private synchronized void addIpAlias(String ip, String mask, String adapter) {
        if (!isNullOrEmpty(ip) && !isNullOrEmpty(mask) && !isNullOrEmpty(adapter)) {
            log.info("Adding IP alias {}/{} to {}", ip, mask, adapter);
            execute("sudo ifconfig " + adapter + " " + ip + " netmask " + mask + " up", false);
            execute("sudo /usr/sbin/arping -c 1 -I " + adapter + " " + ip, true);
        }
    }

    private synchronized void removeIpAlias(String ip, String mask, String adapter) {
        if (!isNullOrEmpty(ip) && !isNullOrEmpty(mask) && !isNullOrEmpty(adapter)) {
            log.info("Removing IP alias from {}", adapter, false);
            execute("sudo ifconfig " + adapter + " down", true);
        }
    }

    private void execute(String command, boolean ignoreCode) {
        try {
            log.info("Executing [{}]", command);
            Process process = Runtime.getRuntime().exec(command);
            byte[] output = ByteStreams.toByteArray(process.getInputStream());
            byte[] error = ByteStreams.toByteArray(process.getErrorStream());
            int code = process.waitFor();
            if (code != 0 && !ignoreCode) {
                log.info("Command failed: status={}, output={}, error={}",
                         code, new String(output), new String(error));
            }
        } catch (IOException e) {
            log.error("Unable to execute command {}", command, e);
        } catch (InterruptedException e) {
            log.error("Interrupted executing command {}", command, e);
        }
    }

    // Listens for leadership changes.
    private class InternalLeadershipListener implements LeadershipEventListener {

        @Override
        public boolean isRelevant(LeadershipEvent event) {
            return CLUSTER_IP.equals(event.subject().topic());
        }

        @Override
        public void event(LeadershipEvent event) {
             processLeaderChange(event.subject().leaderNodeId());
        }
    }

}
