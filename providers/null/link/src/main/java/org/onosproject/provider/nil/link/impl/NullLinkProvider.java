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

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.onlab.util.Tools.namedThreads;
import static org.slf4j.LoggerFactory.getLogger;
import static org.onlab.util.Tools.toHex;
import static org.onosproject.net.MastershipRole.MASTER;

import java.util.Dictionary;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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
import org.onosproject.net.Link;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.link.DefaultLinkDescription;
import org.onosproject.net.link.LinkDescription;
import org.onosproject.net.link.LinkEvent;
import org.onosproject.net.link.LinkListener;
import org.onosproject.net.link.LinkProvider;
import org.onosproject.net.link.LinkProviderRegistry;
import org.onosproject.net.link.LinkProviderService;
import org.onosproject.net.link.LinkService;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Provider which advertises fake/nonexistent links to the core. To be used for
 * benchmarking only.
 */
@Component(immediate = true)
public class NullLinkProvider extends AbstractProvider implements LinkProvider {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MastershipService roleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService nodeService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LinkProviderRegistry providerRegistry;
    private LinkService linkService;

    private LinkProviderService providerService;

    private static final int DEFAULT_RATE = 0;
    // For now, static switch port values
    private static final PortNumber SRCPORT = PortNumber.portNumber(5);
    private static final PortNumber DSTPORT = PortNumber.portNumber(6);

    private final InternalLinkProvider linkProvider = new InternalLinkProvider();
    private final InternalLinkListener listener = new InternalLinkListener();

    // Link descriptions
    private final ConcurrentMap<ConnectPoint, LinkDescription> descriptions = Maps
            .newConcurrentMap();

    // Local Device ID's that have been seen so far
    private final List<DeviceId> devices = Lists.newArrayList();
    // tail ends of other islands
    private final List<ConnectPoint> tails = Lists.newArrayList();

    private ExecutorService linkDriver = Executors.newFixedThreadPool(1,
            namedThreads("onos-null-link-driver"));

    // For flicker = true, duration between events in msec.
    @Property(name = "eventRate", value = "0",
            label = "Duration between Link Event")
    private int eventRate = DEFAULT_RATE;
    private int checkRateDuration = 10;

    // For flicker = true, duration between events in msec.
    @Property(name = "neighbors", value = "",
            label = "Node ID of instance for neighboring island ")
    private String neighbor = "";

    public NullLinkProvider() {
        super(new ProviderId("null", "org.onosproject.provider.nil"));
    }

    @Activate
    public void activate(ComponentContext context) {
        providerService = providerRegistry.register(this);
        linkService = (LinkService) providerRegistry;
        linkService.addListener(listener);
        deviceService.addListener(linkProvider);
        modified(context);
        log.info("started");
    }

    @Deactivate
    public void deactivate(ComponentContext context) {
        if (eventRate != 0) {
            try {
                linkDriver.awaitTermination(1000, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                log.error("LinkBuilder did not terminate");
            }
            linkDriver.shutdownNow();
        }
        deviceService.removeListener(linkProvider);
        providerRegistry.unregister(this);
        linkService.removeListener(listener);
        deviceService = null;
        linkService = null;

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
        String newNbor;
        try {
            String s = (String) properties.get("eventRate");
            newRate = isNullOrEmpty(s) ? eventRate : Integer.parseInt(s.trim());
            s = (String) properties.get("neighbors");
            newNbor = isNullOrEmpty(s) ? neighbor : getNeighbor(s.trim());
        } catch (Exception e) {
            log.warn(e.getMessage());
            newRate = eventRate;
            newNbor = neighbor;
        }

        if (newNbor != neighbor) {
            neighbor = newNbor;
        }

        if (newRate != 0 & eventRate != newRate) {
            eventRate = newRate;
            linkDriver.submit(new LinkDriver());
        }

        log.info("Using new settings: eventRate={}", eventRate);
    }

    // pick out substring from Deviceid
    private String part(String devId) {
        return devId.split(":")[1].substring(12, 16);
    }

    // pick out substring from Deviceid
    private String nIdPart(String devId) {
        return devId.split(":")[1].substring(9, 12);
    }

    // pick out the next node ID in string, return hash (i.e. what's
    // in a Device ID
    private String getNeighbor(String nbors) {
        String me = nodeService.getLocalNode().id().toString();
        String mynb = "";
        String[] nodes = nbors.split(",");
        for (int i = 0; i < nodes.length; i++) {
            if (i != 0 & nodes[i].equals(me)) {
                mynb = nodes[i - 1];
                break;
            }
        }
        // return as hash string.
        if (!mynb.isEmpty()) {
            return toHex((Objects.hash(new NodeId(mynb)))).substring(13, 16);
        }
        return "";
    }

    /**
     * Adds links as devices are found, and generates LinkEvents.
     */
    private class InternalLinkProvider implements DeviceListener {

        @Override
        public void event(DeviceEvent event) {
            Device dev = event.subject();
            switch (event.type()) {
            case DEVICE_ADDED:
                addLink(dev);
                break;
            case DEVICE_REMOVED:
                removeLink(dev);
                break;
            default:
                break;
            }
        }

        private void addLink(Device current) {
            DeviceId did = current.id();
            if (!MASTER.equals(roleService.getLocalRole(did))) {

                String part = part(did.toString());
                String npart = nIdPart(did.toString());
                if (part.equals("ffff") && npart.equals(neighbor)) {
                    // 'tail' of our neighboring island - link us <- tail
                    tails.add(new ConnectPoint(did, SRCPORT));
                }
                tryLinkTail();
                return;
            }
            devices.add(did);

            if (devices.size() == 1) {
                return;
            }

            // Normal flow - attach new device to the last-seen device
            DeviceId prev = devices.get(devices.size() - 2);
            ConnectPoint src = new ConnectPoint(prev, SRCPORT);
            ConnectPoint dst = new ConnectPoint(did, DSTPORT);

            LinkDescription fdesc = new DefaultLinkDescription(src, dst,
                    Link.Type.DIRECT);
            LinkDescription rdesc = new DefaultLinkDescription(dst, src,
                    Link.Type.DIRECT);
            descriptions.put(src, fdesc);
            descriptions.put(dst, rdesc);

            providerService.linkDetected(fdesc);
            providerService.linkDetected(rdesc);
        }

        // try to link to a tail to first element
        private void tryLinkTail() {
            if (tails.isEmpty() || devices.isEmpty()) {
                return;
            }
            ConnectPoint first = new ConnectPoint(devices.get(0), DSTPORT);
            boolean added = false;
            for (ConnectPoint cp : tails) {
                if (!linkService.getLinks(cp).isEmpty()) {
                    continue;
                }
                LinkDescription ld = new DefaultLinkDescription(cp, first,
                        Link.Type.DIRECT);
                descriptions.put(cp, ld);
                providerService.linkDetected(ld);
                added = true;
                break;
            }
            if (added) {
                tails.clear();
            }
        }

        private void removeLink(Device device) {
            if (!MASTER.equals(roleService.getLocalRole(device.id()))) {
                return;
            }
            providerService.linksVanished(device.id());
            devices.remove(device.id());
        }

    }

    private class InternalLinkListener implements LinkListener {

        @Override
        public void event(LinkEvent event) {
            switch (event.type()) {
            case LINK_ADDED:
                // If a link from another island, cast one back.
                DeviceId sdid = event.subject().src().deviceId();
                PortNumber pn = event.subject().src().port();

                if (roleService.getLocalRole(sdid).equals(MASTER)) {
                    String part = part(sdid.toString());
                    if (part.equals("ffff") && SRCPORT.equals(pn)) {
                        LinkDescription ld = new DefaultLinkDescription(event
                                .subject().dst(), event.subject().src(),
                                Link.Type.DIRECT);
                        descriptions.put(event.subject().dst(), ld);
                        providerService.linkDetected(ld);
                    }
                    return;
                }
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

        @Override
        public void run() {
            long startTime = System.currentTimeMillis();
            long countEvent = 0;
            float effLoad = 0;

            while (!linkDriver.isShutdown()) {

                //Assuming eventRate is in microsecond unit
                if (countEvent <= checkRateDuration * 1000000 / eventRate) {
                    for (LinkDescription desc : descriptions.values()) {
                        providerService.linkVanished(desc);
                        countEvent++;
                        try {
                            TimeUnit.MICROSECONDS.sleep(eventRate);
                        } catch (InterruptedException e) {
                            log.warn(String.valueOf(e));
                        }
                        providerService.linkDetected(desc);
                        countEvent++;
                        try {
                            TimeUnit.MICROSECONDS.sleep(eventRate);
                        } catch (InterruptedException e) {
                            log.warn(String.valueOf(e));
                        }
                    }
                } else {
                    // log in WARN the effective load generation rate in events/sec, every 10 seconds
                    effLoad = (float) (countEvent * 1000 / (System.currentTimeMillis() - startTime));
                    log.warn("Effective Loading is {} events/second", String.valueOf(effLoad));
                    countEvent = 0;
                    startTime = System.currentTimeMillis();
                }
            }
        }
    }
}
