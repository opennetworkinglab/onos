/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.provider.nil.link.impl;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import org.apache.commons.lang3.concurrent.ConcurrentUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.NodeId;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.link.DefaultLinkDescription;
import org.onosproject.net.link.LinkDescription;
import org.onosproject.net.link.LinkProvider;
import org.onosproject.net.link.LinkProviderRegistry;
import org.onosproject.net.link.LinkProviderService;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;

import java.util.Dictionary;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.onlab.util.Tools.groupedThreads;
import static org.onlab.util.Tools.toHex;
import static org.onosproject.net.MastershipRole.MASTER;
import static org.onosproject.net.Link.Type.DIRECT;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Provider which advertises fake/nonexistent links to the core. To be used for
 * benchmarking only.
 *
 * This provider takes a topology graph file with a DOT-like syntax.
 */
@Component(immediate = true)
public class NullLinkProvider extends AbstractProvider implements LinkProvider {

    private final Logger log = getLogger(getClass());

    private static final String CFG_PATH = "/opt/onos/apache-karaf-3.0.2/etc/linkGraph.cfg";

    private static final int CHECK_DURATION = 10;
    private static final int DEFAULT_RATE = 0;
    private static final int REFRESH_RATE = 3000000; // in us

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MastershipService roleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService nodeService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LinkProviderRegistry providerRegistry;

    private LinkProviderService providerService;

    private final InternalLinkProvider linkProvider = new InternalLinkProvider();

    // True for device with Driver, false otherwise.
    private final ConcurrentMap<DeviceId, Boolean> driverMap = Maps
            .newConcurrentMap();

    // Link descriptions
    private final ConcurrentMap<DeviceId, Set<LinkDescription>> linkDescrs = Maps
            .newConcurrentMap();

    private ExecutorService linkDriver =
            Executors.newCachedThreadPool(groupedThreads("onos/null", "link-driver-%d"));

    // For flicker = true, duration between events in msec.
    @Property(name = "eventRate", value = "0", label = "Duration between Link Event")
    private int eventRate = DEFAULT_RATE;

    // topology configuration file
    @Property(name = "cfgFile",
            value = "/opt/onos/apache-karaf-3.0.2/etc/linkGraph.cfg",
            label = "Topology file location")
    private String cfgFile = CFG_PATH;

    // flag checked to create a LinkDriver, if rate is non-zero.
    private boolean flicker = false;

    public NullLinkProvider() {
        super(new ProviderId("null", "org.onosproject.provider.nil"));
    }

    @Activate
    public void activate(ComponentContext context) {
        providerService = providerRegistry.register(this);
        modified(context);
        deviceService.addListener(linkProvider);

        log.info("started");
    }

    @Deactivate
    public void deactivate(ComponentContext context) {
        linkDriver.shutdown();
        try {
            linkDriver.awaitTermination(1000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            log.error("LinkBuilder did not terminate");
            linkDriver.shutdownNow();
        }
        deviceService.removeListener(linkProvider);
        providerRegistry.unregister(this);
        deviceService = null;

        log.info("stopped");
    }

    @Modified
    public void modified(ComponentContext context) {
        if (context == null) {
            log.info("No configs, using defaults: eventRate={}", DEFAULT_RATE);
            return;
        }
        Dictionary<?, ?> properties = context.getProperties();
        int newRate;
        String newPath;
        try {
            String s = (String) properties.get("eventRate");
            newRate = isNullOrEmpty(s) ? eventRate : Integer.parseInt(s.trim());
            s = (String) properties.get("cfgFile");
            newPath = s.trim();
        } catch (NumberFormatException | ClassCastException e) {
            log.warn(e.getMessage());
            newRate = eventRate;
            newPath = cfgFile;
        }

        // topology file configuration
        if (!newPath.equals(cfgFile)) {
            cfgFile = newPath;
        }
        readGraph(cfgFile, nodeService.getLocalNode().id());

        // test mode configuration
        if (eventRate != newRate && newRate > 0) {
            driverMap.replaceAll((k, v) -> false);
            eventRate = newRate;
            flicker = true;
        } else if (newRate == 0) {
            driverMap.replaceAll((k, v) -> false);
            flicker = false;
        }

        log.info("Using settings: eventRate={}, topofile={}", eventRate, cfgFile);
        for (Device dev : deviceService.getDevices()) {
            DeviceId did = dev.id();
            synchronized (this) {
                if (driverMap.get(did) == null || !driverMap.get(did)) {
                    driverMap.put(dev.id(), true);
                    linkDriver.submit(new LinkDriver(dev));
                }
            }
        }
    }

    // parse simplified dot-like topology graph
    private void readGraph(String path, NodeId me) {
        log.info("path: {}, local: {}", path, me);
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(path));
            String cur = br.readLine();
            while (cur != null) {
                if (cur.startsWith("#")) {
                    cur = br.readLine();
                    continue;
                }
                String[] parts = cur.trim().split(" ");
                if (parts.length < 1) {
                    continue;
                }
                if (parts[0].equals("graph")) {
                    String node = parts[1].trim();
                    if (node.equals(me.toString())) {
                        cur = br.readLine(); // move to next line, start of links list
                        while (cur != null) {
                            if (cur.trim().contains("}")) {
                                break;
                            }
                            readLink(cur.trim().split(" "), me);
                            cur = br.readLine();
                        }
                    } else {
                        while (cur != null) {
                            if (cur.trim().equals("}")) {
                                break;
                            }
                            cur = br.readLine();
                        }
                    }
                }
                cur = br.readLine();
            }
        } catch (IOException e) {
            log.warn("Could not find topology file: {}", e);
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
            } catch (IOException e) {
                log.warn("Could not close topology file: {}", e);
            }
        }
    }

    // parses a link descriptor to make a LinkDescription
    private void readLink(String[] linkArr, NodeId me) {
        if (linkArr[0].startsWith("#")) {
            return;
        }
        if (linkArr.length != 3) {
            log.warn("Malformed link descriptor:"
                    + " link should be of format src:port [--|->] dst:port,"
                    + " skipping");
            return;
        }

        String op = linkArr[1];
        String[] cp1 = linkArr[0].split(":");
        String[] cp2 = linkArr[2].split(":");

        log.debug("cp1:{} cp2:{}", cp1, cp2);
        if (cp1.length != 2 && (cp2.length != 2 || cp2.length != 3)) {
            log.warn("Malformed endpoint descriptor(s):"
                    + "endpoint format should be DeviceId:port or DeviceId:port:NodeId,"
                    + "skipping");
            return;
        }
        // read in hints about topology.
        NodeId adj = null;
        if (cp2.length == 3) {
            adj = new NodeId(cp2[2]);
            log.debug("found an island: {}", adj);
        }

        // reconstruct deviceIDs. Convention is based on NullDeviceProvider.
        DeviceId sdev = recover(cp1[0], me);
        DeviceId ddev = (adj == null) ? recover(cp2[0], me) : recover(cp2[0], adj);
        ConnectPoint src = new ConnectPoint(sdev, PortNumber.portNumber(cp1[1]));
        ConnectPoint dst = new ConnectPoint(ddev, PortNumber.portNumber(cp2[1]));

        if (op.equals("--")) {
            // bidirectional - within our node's island
            LinkDescription out = new DefaultLinkDescription(src, dst, DIRECT);
            LinkDescription in = new DefaultLinkDescription(dst, src, DIRECT);
            addLdesc(sdev, out);
            addLdesc(ddev, in);
            log.info("Created bidirectional link: {}", out);
        } else if (op.equals("->")) {
            // unidirectional - likely from another island
            LinkDescription in = new DefaultLinkDescription(dst, src, DIRECT);
            addLdesc(ddev, in);
            log.info("Created unidirectional link: {}", in);
        } else {
            log.warn("Unknown link descriptor operand:"
                    + " operand must be '--' or '->', skipping");
            return;
        }
    }

    // recover DeviceId from configs and NodeID
    private DeviceId recover(String base, NodeId node) {
        long hash = node.hashCode() << 16;
        int dev = Integer.valueOf(base);
        log.debug("hash: {}, dev: {}, {}", hash, dev, toHex(hash | dev));
        try {
            return DeviceId.deviceId(new URI("null", toHex(hash | dev), null));
        } catch (URISyntaxException e) {
            log.warn("could not create a DeviceID for descriptor {}", dev);
            return DeviceId.NONE;
        }
    }

    // add LinkDescriptions to map
    private boolean addLdesc(DeviceId did, LinkDescription ldesc) {
        Set<LinkDescription> ldescs = ConcurrentUtils.putIfAbsent(
                linkDescrs, did, Sets.newConcurrentHashSet());
        return ldescs.add(ldesc);
    }

    /**
     * Generate LinkEvents using configurations when devices are found.
     */
    private class InternalLinkProvider implements DeviceListener {

        @Override
        public void event(DeviceEvent event) {
            Device dev = event.subject();
            switch (event.type()) {
            case DEVICE_ADDED:
                synchronized (this) {
                    if (!driverMap.getOrDefault(dev.id(), false)) {
                        driverMap.put(dev.id(), true);
                        linkDriver.submit(new LinkDriver(dev));
                    }
                }
                break;
            case DEVICE_REMOVED:
                driverMap.put(dev.id(), false);
                if (!MASTER.equals(roleService.getLocalRole(dev.id()))) {
                    return;
                }
                // no need to remove static links, just stop advertising them
                providerService.linksVanished(dev.id());
                break;
            default:
                break;
            }
        }
    }

    /**
     * Generates link events using fake links.
     */
    private class LinkDriver implements Runnable {
        Device myDev;
        LinkDriver(Device dev) {
            myDev = dev;
        }

        @Override
        public void run() {
            log.info("Thread started for dev {}", myDev.id());
            if (flicker) {
                flicker();
            } else {
                refresh();
            }
        }

        private void flicker() {
            long startTime = System.currentTimeMillis();
            long countEvent = 0;
            float effLoad = 0;

            while (!linkDriver.isShutdown() && driverMap.get(myDev.id())) {
                if (!flicker) {
                    break;
                }
                //Assuming eventRate is in microsecond unit
                if (countEvent <= CHECK_DURATION * 1000000 / eventRate) {
                    for (LinkDescription desc : linkDescrs.get(myDev.id())) {
                        providerService.linkVanished(desc);
                        countEvent++;
                        sleepFor(eventRate);
                        providerService.linkDetected(desc);
                        countEvent++;
                        sleepFor(eventRate);
                    }
                } else {
                    // log in WARN the effective load generation rate in events/sec, every 10 seconds
                    effLoad = (float) (countEvent * 1000.0 /
                            (System.currentTimeMillis() - startTime));
                    log.warn("Effective Loading for thread is {} events/second",
                            String.valueOf(effLoad));
                    countEvent = 0;
                    startTime = System.currentTimeMillis();
                }
            }
        }

        private void refresh() {
            while (!linkDriver.isShutdown() && driverMap.get(myDev.id())) {
                if (flicker) {
                    break;
                }
                for (LinkDescription desc : linkDescrs.get(myDev.id())) {
                    providerService.linkDetected(desc);
                    sleepFor(REFRESH_RATE);
                }
            }
        }

        private void sleepFor(int time) {
            try {
                TimeUnit.MICROSECONDS.sleep(time);
            } catch (InterruptedException e) {
                log.warn(String.valueOf(e));
            }
        }
    }

}
