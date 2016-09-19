/*
 *  Copyright 2016-present Open Networking Laboratory
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.onosproject.ui.impl.topo.util;

import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.Link;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.host.HostService;
import org.onosproject.net.intent.FlowObjectiveIntent;
import org.onosproject.net.intent.FlowRuleIntent;
import org.onosproject.net.intent.HostToHostIntent;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.LinkCollectionIntent;
import org.onosproject.net.intent.MultiPointToSinglePointIntent;
import org.onosproject.net.intent.OpticalConnectivityIntent;
import org.onosproject.net.intent.PathIntent;
import org.onosproject.net.intent.PointToPointIntent;
import org.onosproject.net.link.LinkService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static org.onosproject.net.intent.IntentState.INSTALLED;

/**
 * Auxiliary facility to query the intent service based on the specified
 * set of end-station hosts, edge points or infrastructure devices.
 */
public class TopoIntentFilter {

    private final IntentService intentService;
    private final DeviceService deviceService;
    private final HostService hostService;
    private final LinkService linkService;

    /**
     * Creates an intent filter.
     *
     * @param services service references bundle
     */
    public TopoIntentFilter(ServicesBundle services) {
        this.intentService = services.intentService();
        this.deviceService = services.deviceService();
        this.hostService = services.hostService();
        this.linkService = services.linkService();
    }

    /**
     * Finds all path (host-to-host or point-to-point) intents that pertain
     * to the given hosts and devices.
     *
     * @param hosts         set of hosts to query by
     * @param devices       set of devices to query by
     * @param links       set of links to query by
     * @return set of intents that 'match' all hosts, devices and links given
     */
    public List<Intent> findPathIntents(Set<Host> hosts,
                                        Set<Device> devices,
                                        Set<Link> links) {
        // start with all intents
        Iterable<Intent> sourceIntents = intentService.getIntents();

        // Derive from this the set of edge connect points.
        Set<ConnectPoint> edgePoints = getEdgePoints(hosts);

        // Iterate over all intents and produce a set that contains only those
        // intents that target all selected hosts or derived edge connect points.
        return getIntents(hosts, devices, links, edgePoints, sourceIntents);
    }


    // Produces a set of edge points from the specified set of hosts.
    private Set<ConnectPoint> getEdgePoints(Set<Host> hosts) {
        Set<ConnectPoint> edgePoints = new HashSet<>();
        for (Host host : hosts) {
            edgePoints.add(host.location());
        }
        return edgePoints;
    }

    // Produces a list of intents that target all selected hosts, devices, links or connect points.
    private List<Intent> getIntents(Set<Host> hosts, Set<Device> devices, Set<Link> links,
                                    Set<ConnectPoint> edgePoints,
                                    Iterable<Intent> sourceIntents) {
        List<Intent> intents = new ArrayList<>();
        if (hosts.isEmpty() && devices.isEmpty() && links.isEmpty()) {
            return intents;
        }

        Set<OpticalConnectivityIntent> opticalIntents = new HashSet<>();

        // Search through all intents and see if they are relevant to our search.
        for (Intent intent : sourceIntents) {
            if (intentService.getIntentState(intent.key()) == INSTALLED) {
                boolean isRelevant = false;
                if (intent instanceof HostToHostIntent) {
                    isRelevant = isIntentRelevantToHosts((HostToHostIntent) intent, hosts) &&
                            isIntentRelevantToDevices(intent, devices) && isIntentRelevantToLinks(intent, links);
                } else if (intent instanceof PointToPointIntent) {
                    isRelevant = isIntentRelevant((PointToPointIntent) intent, edgePoints) &&
                            isIntentRelevantToDevices(intent, devices) && isIntentRelevantToLinks(intent, links);
                } else if (intent instanceof MultiPointToSinglePointIntent) {
                    isRelevant = isIntentRelevant((MultiPointToSinglePointIntent) intent, edgePoints) &&
                            isIntentRelevantToDevices(intent, devices) && isIntentRelevantToLinks(intent, links);
                } else if (intent instanceof OpticalConnectivityIntent) {
                    opticalIntents.add((OpticalConnectivityIntent) intent);
                }
                // TODO: add other intents, e.g. SinglePointToMultiPointIntent

                if (isRelevant) {
                    intents.add(intent);
                }
            }
        }

        // As a second pass, try to link up any optical intents with the
        // packet-level ones.
        for (OpticalConnectivityIntent intent : opticalIntents) {
            if (isIntentRelevant(intent, intents) &&
                    isIntentRelevantToDevices(intent, devices)) {
                intents.add(intent);
            }
        }
        return intents;
    }

    // Indicates whether the specified intent involves all of the given hosts.
    private boolean isIntentRelevantToHosts(HostToHostIntent intent, Iterable<Host> hosts) {
        for (Host host : hosts) {
            HostId id = host.id();
            // Bail if intent does not involve this host.
            if (!id.equals(intent.one()) && !id.equals(intent.two())) {
                return false;
            }
        }
        return true;
    }

    // Indicates whether the specified intent involves all of the given devices.
    private boolean isIntentRelevantToDevices(Intent intent, Iterable<Device> devices) {
        List<Intent> installables = intentService.getInstallableIntents(intent.key());
        for (Device device : devices) {
            if (!isIntentRelevantToDevice(installables, device)) {
                return false;
            }
        }
        return true;
    }

    // Indicates whether the specified intent involves all of the given links.
    private boolean isIntentRelevantToLinks(Intent intent, Iterable<Link> links) {
        List<Intent> installables = intentService.getInstallableIntents(intent.key());
        for (Link link : links) {
            if (!isIntentRelevantToLink(installables, link)) {
                return false;
            }
        }
        return true;
    }

    // Indicates whether the specified intent involves the given device.
    private boolean isIntentRelevantToDevice(List<Intent> installables, Device device) {
        if (installables != null) {
            for (Intent installable : installables) {
                if (installable instanceof PathIntent) {
                    PathIntent pathIntent = (PathIntent) installable;
                    if (pathContainsDevice(pathIntent.path().links(), device.id())) {
                        return true;
                    }
                } else if (installable instanceof FlowRuleIntent) {
                    FlowRuleIntent flowRuleIntent = (FlowRuleIntent) installable;
                    if (rulesContainDevice(flowRuleIntent.flowRules(), device.id())) {
                        return true;
                    }
                } else if (installable instanceof FlowObjectiveIntent) {
                    FlowObjectiveIntent objectiveIntent = (FlowObjectiveIntent) installable;
                    return objectiveIntent.devices().contains(device.id());

                } else if (installable instanceof LinkCollectionIntent) {
                    LinkCollectionIntent linksIntent = (LinkCollectionIntent) installable;
                    if (pathContainsDevice(linksIntent.links(), device.id())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // Indicates whether the specified intent involves the given link.
    private boolean isIntentRelevantToLink(List<Intent> installables, Link link) {
        Link reverseLink = linkService.getLink(link.dst(), link.src());

        if (installables != null) {
            for (Intent installable : installables) {
                if (installable instanceof PathIntent) {
                    PathIntent pathIntent = (PathIntent) installable;
                    return pathIntent.path().links().contains(link) ||
                            pathIntent.path().links().contains(reverseLink);

                } else if (installable instanceof FlowRuleIntent) {
                    FlowRuleIntent flowRuleIntent = (FlowRuleIntent) installable;
                    return flowRuleIntent.resources().contains(link) ||
                            flowRuleIntent.resources().contains(reverseLink);

                } else if (installable instanceof FlowObjectiveIntent) {
                    FlowObjectiveIntent objectiveIntent = (FlowObjectiveIntent) installable;
                    return objectiveIntent.resources().contains(link) ||
                            objectiveIntent.resources().contains(reverseLink);

                } else if (installable instanceof LinkCollectionIntent) {
                    LinkCollectionIntent linksIntent = (LinkCollectionIntent) installable;
                    return linksIntent.links().contains(link) ||
                            linksIntent.links().contains(reverseLink);

                }
            }
        }
        return false;
    }

    // Indicates whether the specified links involve the given device.
    private boolean pathContainsDevice(Iterable<Link> links, DeviceId id) {
        for (Link link : links) {
            if (link.src().elementId().equals(id) || link.dst().elementId().equals(id)) {
                return true;
            }
        }
        return false;
    }

    // Indicates whether the specified flow rules involvesthe given device.
    private boolean rulesContainDevice(Collection<FlowRule> flowRules, DeviceId id) {
        for (FlowRule rule : flowRules) {
            if (rule.deviceId().equals(id)) {
                return true;
            }
        }
        return false;
    }

    private boolean isIntentRelevant(PointToPointIntent intent,
                                     Iterable<ConnectPoint> edgePoints) {
        for (ConnectPoint point : edgePoints) {
            // Bail if intent does not involve this edge point.
            if (!point.equals(intent.egressPoint()) &&
                    !point.equals(intent.ingressPoint())) {
                return false;
            }
        }
        return true;
    }

    // Indicates whether the specified intent involves all of the given edge points.
    private boolean isIntentRelevant(MultiPointToSinglePointIntent intent,
                                     Iterable<ConnectPoint> edgePoints) {
        for (ConnectPoint point : edgePoints) {
            // Bail if intent does not involve this edge point.
            if (!point.equals(intent.egressPoint()) &&
                    !intent.ingressPoints().contains(point)) {
                return false;
            }
        }
        return true;
    }

    // Indicates whether the specified intent involves all of the given edge points.
    private boolean isIntentRelevant(OpticalConnectivityIntent opticalIntent,
                                     Iterable<Intent> intents) {
        Link ccSrc = getFirstLink(opticalIntent.getSrc(), false);
        Link ccDst = getFirstLink(opticalIntent.getDst(), true);
        if (ccSrc == null || ccDst == null) {
            return false;
        }

        for (Intent intent : intents) {
            List<Intent> installables = intentService.getInstallableIntents(intent.key());
            for (Intent installable : installables) {
                if (installable instanceof PathIntent) {
                    List<Link> links = ((PathIntent) installable).path().links();
                    if (links.size() == 3) {
                        Link tunnel = links.get(1);
                        if (Objects.equals(tunnel.src(), ccSrc.src()) &&
                                Objects.equals(tunnel.dst(), ccDst.dst())) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private Link getFirstLink(ConnectPoint point, boolean ingress) {
        for (Link link : linkService.getLinks(point)) {
            if (point.equals(ingress ? link.src() : link.dst())) {
                return link;
            }
        }
        return null;
    }
}
