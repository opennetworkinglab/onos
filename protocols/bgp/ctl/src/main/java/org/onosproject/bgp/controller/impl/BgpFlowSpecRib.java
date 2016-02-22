/*
 * Copyright 2016 Open Networking Laboratory
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

package org.onosproject.bgp.controller.impl;

import java.util.Map;
import java.util.TreeMap;

import org.onosproject.bgpio.types.RouteDistinguisher;
import org.onosproject.bgpio.protocol.flowspec.BgpFlowSpecDetails;
import org.onosproject.bgpio.protocol.flowspec.BgpFlowSpecPrefix;

import com.google.common.base.MoreObjects;

/**
 * Implementation of BGP flow specification RIB.
 */
public class BgpFlowSpecRib {
    private Map<BgpFlowSpecPrefix, BgpFlowSpecDetails> flowSpecTree = new TreeMap<>();
    private Map<RouteDistinguisher, Map<BgpFlowSpecPrefix, BgpFlowSpecDetails>> vpnFlowSpecTree = new TreeMap<>();

    /**
     * Returns the BGP flow spec info.
     *
     * @return BGP flow spec tree
     */
    public Map<BgpFlowSpecPrefix, BgpFlowSpecDetails> flowSpecTree() {
        return flowSpecTree;
    }

    /**
     * Gets VPN flowspec tree.
     *
     * @return vpn flow spec tree
     */
    public Map<RouteDistinguisher, Map<BgpFlowSpecPrefix, BgpFlowSpecDetails>> vpnFlowSpecTree() {
        return vpnFlowSpecTree;
    }


    /**
     * Update BGP flow spec details.
     *
     * @param prefix prefix Info
     * @param flowSpec BGP flow specifications details
     */
    public void add(BgpFlowSpecPrefix prefix, BgpFlowSpecDetails flowSpec) {
        if (flowSpecTree.containsKey(prefix)) {
            flowSpecTree.replace(prefix, flowSpec);
        } else {
            flowSpecTree.put(prefix, flowSpec);
        }
    }

    /**
     * Removes flow spec.
     *
     * @param flowSpec BGP flow specification
     */
    public void delete(BgpFlowSpecPrefix flowSpec) {
        if (flowSpecTree.containsKey(flowSpec)) {
            flowSpecTree.remove(flowSpec);
        }
    }

    /**
     * Update BGP flow spec details with routedistinguisher.
     *
     * @param routeDistinguisher route distinguisher
     * @param prefix prefix Info
     * @param flowSpec BGP flow specifications details
     */
    public void add(RouteDistinguisher routeDistinguisher, BgpFlowSpecPrefix prefix, BgpFlowSpecDetails flowSpec) {
        Map<BgpFlowSpecPrefix, BgpFlowSpecDetails> fsTree;
        if (!vpnFlowSpecTree.containsKey(routeDistinguisher)) {

            fsTree = new TreeMap<>();

            vpnFlowSpecTree.put(routeDistinguisher, fsTree);
        } else {
            fsTree = vpnFlowSpecTree().get(routeDistinguisher);
        }
        if (fsTree.containsKey(prefix)) {
            fsTree.replace(prefix, flowSpec);
        } else {
            fsTree.put(prefix, flowSpec);
        }
    }

    /**
     * Removes flow spec.
     *
     * @param routeDistinguisher route distinguisher
     * @param flowSpecPrefix BGP flow specification prefix
     */
    public void delete(RouteDistinguisher routeDistinguisher, BgpFlowSpecPrefix flowSpecPrefix) {
        if (vpnFlowSpecTree.containsKey(routeDistinguisher)) {
            Map<BgpFlowSpecPrefix, BgpFlowSpecDetails> fsTree = vpnFlowSpecTree().get(routeDistinguisher);
            fsTree.remove(flowSpecPrefix);
            if (fsTree.size() == 0) {
                vpnFlowSpecTree.remove(routeDistinguisher);
            }
        }
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .omitNullValues()
                .add("flowSpecTree", flowSpecTree)
                .add("vpnFlowSpecTree", vpnFlowSpecTree)
                .toString();
    }
}
