/*
 * Copyright 2015-present Open Networking Foundation
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

package org.onosproject.ui.topo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
import org.onosproject.net.Element;
import org.onosproject.net.Host;
import org.onosproject.net.Link;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.host.HostService;
import org.onosproject.net.link.LinkService;
import org.onosproject.ui.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.onosproject.net.ConnectPoint.fromString;
import static org.onosproject.net.DeviceId.deviceId;
import static org.onosproject.net.HostId.hostId;

/**
 * Encapsulates a selection of devices, hosts and links from the topology view.
 */
public class NodeSelection {

    private static final Logger log =
            LoggerFactory.getLogger(NodeSelection.class);

    private static final String IDS = "ids";
    private static final String HOVER = "hover";
    private static final String LINK_ID_DELIM = "-";

    private final DeviceService deviceService;
    private final HostService hostService;
    private final LinkService linkService;

    private final Set<String> ids;
    private final String hover;

    private final Set<Device> devices = new HashSet<>();
    private final Set<Host> hosts = new HashSet<>();
    private final Set<Link> links = new HashSet<>();
    private Element hovered;

    /**
     * Creates a node selection entity, from the given payload, using the
     * supplied link, device and host services. Note that if a link, device
     * or host was hovered over by the mouse, it is available
     * via {@link #hovered()}.
     *
     * @param payload message payload
     * @param deviceService device service
     * @param hostService host service
     * @param linkService link service
     */
    public NodeSelection(ObjectNode payload,
                         DeviceService deviceService,
                         HostService hostService,
                         LinkService linkService) {
        this.deviceService = deviceService;
        this.hostService = hostService;
        this.linkService = linkService;

        ids = extractIds(payload);
        hover = extractHover(payload);

        // start by extracting the hovered element if any
        if (isNullOrEmpty(hover)) {
            hovered = null;
        } else {
            setHoveredElement();
        }

        // now go find the links, devices and hosts that are in the selection list
        Set<String> unmatched = findLinks(ids);
        unmatched = findDevices(unmatched);
        unmatched = findHosts(unmatched);
        if (!unmatched.isEmpty()) {
            log.debug("Skipping unmatched IDs {}", unmatched);
        }

    }

    /**
     * Returns a view of the selected devices (hover not included).
     *
     * @return selected devices
     */
    public Set<Device> devices() {
        return Collections.unmodifiableSet(devices);
    }

    /**
     * Returns a view of the selected links (hover not included).
     *
     * @return selected links
     */
    public Set<Link> links() {
        return Collections.unmodifiableSet(links);
    }

    /**
     * Returns a view of the selected devices, including the hovered device
     * if there was one.
     *
     * @return selected (plus hovered) devices
     */
    public Set<Device> devicesWithHover() {
        Set<Device> withHover;
        if (hovered != null && hovered instanceof Device) {
            withHover = new HashSet<>(devices);
            withHover.add((Device) hovered);
        } else {
            withHover = devices;
        }
        return Collections.unmodifiableSet(withHover);
    }

    /**
     * Returns a view of the selected hosts (hover not included).
     *
     * @return selected hosts
     */
    public Set<Host> hosts() {
        return Collections.unmodifiableSet(hosts);
    }

    /**
     * Returns a view of the selected hosts, including the hovered host
     * if thee was one.
     *
     * @return selected (plus hovered) hosts
     */
    public Set<Host> hostsWithHover() {
        Set<Host> withHover;
        if (hovered != null && hovered instanceof Host) {
            withHover = new HashSet<>(hosts);
            withHover.add((Host) hovered);
        } else {
            withHover = hosts;
        }
        return Collections.unmodifiableSet(withHover);
    }

    /**
     * Returns a view of the selected links, including the hovered link
     * if thee was one.
     *
     * @return selected (plus hovered) links
     */
    public Set<Link> linksWithHover() {
        Set<Link> withHover;
        if (hovered != null && hovered instanceof Link) {
            withHover = new HashSet<>(links);
            withHover.add((Link) hovered);
        } else {
            withHover = links;
        }
        return Collections.unmodifiableSet(withHover);
    }

    /**
     * Returns the element (link, host or device) over which the mouse was hovering,
     * or null.
     *
     * @return element hovered over
     */
    public Element hovered() {
        return hovered;
    }

    /**
     * Returns true if nothing is selected.
     *
     * @return true if nothing selected
     */
    public boolean none() {
        return devices().isEmpty() && hosts().isEmpty() && links().isEmpty();
    }

    @Override
    public String toString() {
        return "NodeSelection{" +
                "ids=" + ids +
                ", hover='" + hover + '\'' +
                ", #devices=" + devices.size() +
                ", #hosts=" + hosts.size() +
                ", #links=" + links.size() +
                '}';
    }

    // == helper methods

    private Set<String> extractIds(ObjectNode payload) {
        ArrayNode array = (ArrayNode) payload.path(IDS);
        if (array == null || array.size() == 0) {
            return Collections.emptySet();
        }

        Set<String> ids = new HashSet<>();
        for (JsonNode node : array) {
            ids.add(node.asText());
        }
        return ids;
    }

    private String extractHover(ObjectNode payload) {
        return JsonUtils.string(payload, HOVER);
    }

    private void setHoveredElement() {
        Set<String> unmatched;
        unmatched = new HashSet<>();
        unmatched.add(hover);
        unmatched = findDevices(unmatched);
        if (devices.size() == 1) {
            hovered = devices.iterator().next();
            devices.clear();
        } else {
            unmatched = findHosts(unmatched);
            if (hosts.size() == 1) {
                hovered = hosts.iterator().next();
                hosts.clear();
            } else {
                hovered = null;
                log.debug("Skipping unmatched HOVER {}", unmatched);
            }
        }
    }

    private Set<String> findDevices(Set<String> ids) {
        Set<String> unmatched = new HashSet<>();
        Device device;

        for (String id : ids) {
            try {
                device = deviceService.getDevice(deviceId(id));
                if (device != null) {
                    devices.add(device);
                } else {
                    unmatched.add(id);
                }
            } catch (Exception e) {
                unmatched.add(id);
            }
        }
        return unmatched;
    }

    private Set<String> findHosts(Set<String> ids) {
        Set<String> unmatched = new HashSet<>();
        Host host;

        for (String id : ids) {
            try {
                host = hostService.getHost(hostId(id));
                if (host != null) {
                    hosts.add(host);
                } else {
                    unmatched.add(id);
                }
            } catch (Exception e) {
                unmatched.add(id);
            }
        }
        return unmatched;
    }

    private Set<String> findLinks(Set<String> ids) {
        Set<String> unmatched = new HashSet<>();
        ConnectPoint cpSrc, cpDst;
        Link link;

        for (String id : ids) {
            try {
                String[] connectPoints = id.split(LINK_ID_DELIM);
                if (connectPoints.length != 2) {
                    unmatched.add(id);
                    continue;
                }

                cpSrc = fromString(connectPoints[0]);
                cpDst = fromString(connectPoints[1]);
                link = linkService.getLink(cpSrc, cpDst);

                if (link != null) {
                    links.add(link);
                } else {
                    unmatched.add(id);
                }

            } catch (Exception e) {
                unmatched.add(id);
            }
        }
        return unmatched;
    }
}
