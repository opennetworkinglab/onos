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

import com.google.common.base.Charsets;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.Files;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.cfg.ComponentConfigService;
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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Dictionary;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.onlab.util.Tools.groupedThreads;
import static org.onlab.util.Tools.get;
import static org.onlab.util.Tools.toHex;
import static org.onosproject.net.Link.Type.DIRECT;
import static org.slf4j.LoggerFactory.getLogger;
import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * Provider which advertises fake/nonexistent links to the core. To be used for
 * benchmarking only.
 *
 * This provider takes a topology graph file with a DOT-like syntax.
 */
@Component(immediate = true)
public class NullLinkProvider extends AbstractProvider implements LinkProvider {

    private final Logger log = getLogger(getClass());

    // default topology file location and name.
    private static final String CFG_PATH = "/opt/onos/apache-karaf-3.0.2/etc/linkGraph.cfg";
    // default number of workers. Eventually make this tunable
    private static final int THREADS = (int) Math.max(1, Runtime.getRuntime().availableProcessors() * 0.8);

    private static final int CHECK_DURATION = 10;   // sec
    private static final int DEFAULT_RATE = 0;      // usec
    private static final int REFRESH_RATE = 3;      // sec
    // Fake device used for non-flickering thread in deviceMap
    private static final DeviceId DEFAULT = DeviceId.deviceId("null:ffffffffffffffff");

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MastershipService roleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService nodeService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LinkProviderRegistry providerRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ComponentConfigService cfgService;

    private LinkProviderService providerService;

    private final InternalLinkProvider linkProvider = new InternalLinkProvider();

    // Mapping between device and drivers that advertise links from device
    private final ConcurrentMap<DeviceId, Set<LinkDriver>> driverMap = Maps
            .newConcurrentMap();

    // Link descriptions
    private final List<LinkDescription> linkDescrs = Lists.newArrayList();

    // Thread to description map for dividing links amongst threads in flicker mode
    private final List<List<LinkDescription>> linkTasks = Lists.newArrayList();

    private ScheduledExecutorService linkDriver =
            Executors.newScheduledThreadPool(THREADS, groupedThreads("onos/null", "link-driver-%d"));

    // For flicker = true, duration between events in msec.
    @Property(name = "eventRate", intValue = DEFAULT_RATE, label = "Duration between Link Event")
    private int eventRate = DEFAULT_RATE;

    // topology configuration file
    @Property(name = "cfgFile", value = CFG_PATH, label = "Topology file location")
    private String cfgFile = CFG_PATH;

    // flag checked to create a LinkDriver, if rate is non-zero.
    private volatile boolean flicker = false;

    public NullLinkProvider() {
        super(new ProviderId("null", "org.onosproject.provider.nil"));
    }

    @Activate
    public void activate(ComponentContext context) {
        cfgService.registerProperties(getClass());
        providerService = providerRegistry.register(this);
        modified(context);
        log.info("started");
    }

    @Deactivate
    public void deactivate(ComponentContext context) {
        cfgService.unregisterProperties(getClass(), false);
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
            String s = get(properties, "eventRate");
            newRate = isNullOrEmpty(s) ? DEFAULT_RATE : Integer.parseInt(s.trim());
            s = (String) properties.get("cfgFile");
            newPath = isNullOrEmpty(s) ? CFG_PATH : s.trim();
        } catch (NumberFormatException e) {
            log.warn(e.getMessage());
            newRate = eventRate;
            newPath = cfgFile;
        }
        // find/read topology file.
        if (!newPath.equals(cfgFile)) {
            cfgFile = newPath;
        }
        readGraph(cfgFile, nodeService.getLocalNode().id());
        // check for new eventRate settings.
        if (newRate != eventRate) {
            if (eventRate < 0) {
                log.warn("Invalid rate, ignoring and using default");
                eventRate = DEFAULT_RATE;
            } else {
                eventRate = newRate;
            }
        }
        configureWorkers();
        log.info("Using settings: eventRate={}, topofile={}", eventRate, cfgFile);
    }

    // Configures and schedules worker threads based on settings.
    private void configureWorkers() {
        if (eventRate > 0) {
            // now set to 'flicker', previously not flickering
            if (!flicker) {
                flicker = true;
                allocateLinks();
                // kill off refresh worker for symmetry
                if (driverMap.containsKey(DEFAULT)) {
                    driverMap.get(DEFAULT).forEach(d -> d.setTasks(Lists.newArrayList()));
                    driverMap.remove(DEFAULT);
                }
                for (int i = 0; i < linkTasks.size(); i++) {
                    List<LinkDescription> links = linkTasks.get(i);
                    LinkDriver driver = new LinkDriver(links);
                    links.forEach(v -> {
                        DeviceId sd = v.src().deviceId();
                        DeviceId dd = v.src().deviceId();
                        driverMap.computeIfAbsent(sd, k -> Sets.newConcurrentHashSet()).add(driver);
                        driverMap.computeIfAbsent(dd, k -> Sets.newConcurrentHashSet()).add(driver);
                    });
                    linkDriver.schedule(driver, eventRate, TimeUnit.MICROSECONDS);
                }
            }
            // no need for was flicker since eventRate will be read by workers
        } else {
            // now set to 'refresh' was 'flicker' before
            if (flicker) {
                driverMap.forEach((dev, lds) -> lds.forEach(l -> l.deviceRemoved(dev)));
                driverMap.clear();
                linkTasks.clear();
                flicker = false;
                LinkDriver driver = new LinkDriver(linkDescrs);
                driverMap.computeIfAbsent(DEFAULT, k -> Sets.newConcurrentHashSet()).add(driver);
                linkDriver.schedule(driver, DEFAULT_RATE, TimeUnit.SECONDS);
                // was 'refresh' - something changed or we're just starting.
            } else {
                if (driverMap.containsKey(DEFAULT)) {
                    driverMap.forEach((dev, ld) -> ld.forEach(d -> d.setTasks(linkDescrs)));
                    return;
                }
                LinkDriver driver = new LinkDriver(linkDescrs);
                driverMap.computeIfAbsent(DEFAULT, k -> Sets.newConcurrentHashSet()).add(driver);
                linkDriver.schedule(driver, DEFAULT_RATE, TimeUnit.SECONDS);
            }
        }
    }

    // parse simplified dot-like topology graph
    private void readGraph(String path, NodeId me) {
        log.info("path: {}, local: {}", path, me);
        Set<LinkDescription> read = Sets.newHashSet();
        BufferedReader br = null;
        try {
            br = Files.newReader(new File(path), Charsets.US_ASCII);
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
                            readLink(cur.trim().split(" "), me, read);
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
        synchronized (linkDescrs) {
            if (!read.isEmpty()) {
                linkDescrs.clear();
                linkDescrs.addAll(read);
            }
        }
    }

    // parses a link descriptor to make a LinkDescription
    private void readLink(String[] linkArr, NodeId me, Set<LinkDescription> links) {
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
        // both link types have incoming half-link
        LinkDescription in = new DefaultLinkDescription(dst, src, DIRECT);
        links.add(in);
        if (op.equals("--")) {
            // bidirectional - within our node's island, make outbound link
            LinkDescription out = new DefaultLinkDescription(src, dst, DIRECT);
            links.add(out);
            log.info("Created bidirectional link: {}, {}", out, in);
        } else if (op.equals("->")) {
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
        int dev = Integer.parseInt(base);
        try {
            return DeviceId.deviceId(new URI("null", toHex(hash | dev), null));
        } catch (URISyntaxException e) {
            log.warn("could not create a DeviceID for descriptor {}", dev);
            return DeviceId.NONE;
        }
    }

    // adds a LinkDescription to a worker's to-be queue, for flickering
    private void allocateLinks() {
        int index, lcount = 0;
        linkTasks.clear();
        for (LinkDescription ld : linkDescrs) {
            index = (lcount % THREADS);
            log.info("allocation: total={}, index={}", linkDescrs.size(), lcount, index);
            if (linkTasks.size() <= index) {
                linkTasks.add(index, Lists.newArrayList(ld));
            } else {
                linkTasks.get(index).add(ld);
            }
            lcount++;
        }
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
                // TODO: wait for all devices to stop core from balking
                break;
            case DEVICE_REMOVED:
                for (LinkDriver d : driverMap.get(dev.id())) {
                    d.deviceRemoved(dev.id());
                }
                providerService.linksVanished(dev.id());
                break;
            default:
                break;
            }
        }
    }

    /**
     * Generates link events using fake links.
     * TODO: stats collection should be its own thing.
     */
    private class LinkDriver implements Runnable {
        // List to actually work off of
        List<LinkDescription> tasks = Lists.newCopyOnWriteArrayList();
        float effLoad = 0;
        Long counter = 0L;
        int next = 0;
        boolean up = true;

        long startTime;

        LinkDriver(List<LinkDescription> links) {
            setTasks(links);
            startTime = System.currentTimeMillis(); // yes, this will start off inaccurate
        }

        @Override
        public void run() {
            if (flicker) {
                flicker();
            } else {
                refresh();
            }
        }

        private void flicker() {
            if ((!linkDriver.isShutdown() || !tasks.isEmpty())) {
                log.trace("next: {}, count: {}", next, counter);
                if (counter <= CHECK_DURATION * 1_000_000 / eventRate) {
                    if (up) {
                        providerService.linkDetected(tasks.get(next++));
                    } else {
                        providerService.linkVanished(tasks.get(next++));
                    }
                    if (next >= tasks.size()) {
                        next = 0;
                        up = !up;
                    }
                    counter++;
                } else {
                    // log in WARN the effective load generation rate in events/sec, every 10 seconds
                    effLoad = (float) (counter * 1000.0 / (System
                            .currentTimeMillis() - startTime));
                    log.warn("Effective Loading for thread is {} events/second",
                            String.valueOf(effLoad));
                    counter = 0L;
                    startTime = System.currentTimeMillis();
                }
                linkDriver.schedule(this, eventRate, TimeUnit.MICROSECONDS);
            }
        }

        private void refresh() {
            if (!linkDriver.isShutdown() || !tasks.isEmpty()) {
                log.trace("iter {} refresh_links", counter);

                for (LinkDescription desc : tasks) {
                    providerService.linkDetected(desc);
                    log.info("iteration {}, {}", counter, desc);
                }
                counter++;
                linkDriver.schedule(this, REFRESH_RATE, TimeUnit.SECONDS);
            }
        }

        public void deviceRemoved(DeviceId did) {
            List<LinkDescription> rm = Lists.newArrayList();
            for (LinkDescription ld : tasks) {
                if (did.equals(ld.dst().deviceId())
                        || (did.equals(ld.src().deviceId()))) {
                    rm.add(ld);
                }
            }
            tasks.removeAll(rm);
        }

        public void setTasks(List<LinkDescription> links) {
            HashMultimap<ConnectPoint, ConnectPoint> nm = HashMultimap.create();
            List<LinkDescription> rm = Lists.newArrayList();
            links.forEach(v -> nm.put(v.src(), v.dst()));
            // remove and send linkVanished for stale links.
            for (LinkDescription l : tasks) {
                if (!nm.containsEntry(l.src(), l.dst())) {
                    rm.add(l);
                }
            }
            tasks.clear();
            tasks.addAll(links);
            rm.forEach(l -> providerService.linkVanished(l));
        }
    }

}
