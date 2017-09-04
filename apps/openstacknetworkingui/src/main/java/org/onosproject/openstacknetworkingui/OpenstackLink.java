/*
 * Copyright 2017-present Open Networking Foundation
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
package org.onosproject.openstacknetworkingui;

import org.onosproject.net.Link;
import org.onosproject.net.LinkKey;

import org.onosproject.ui.topo.BiLink;
import org.onosproject.ui.topo.LinkHighlight;
import org.onosproject.ui.topo.Mod;

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;
import static org.onosproject.ui.topo.LinkHighlight.Flavor.SECONDARY_HIGHLIGHT;

/**
 * Link for OpenStack Networking UI service.
 */
public class OpenstackLink extends BiLink {

    private static final Mod PORT_TRAFFIC_GREEN = new Mod("port-traffic-green");
    private static final Mod PORT_TRAFFIC_ORANGE = new Mod("port-traffic-orange");

    public OpenstackLink(LinkKey key, Link link) {
        super(key, link);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof OpenstackLink) {
            OpenstackLink that = (OpenstackLink) obj;
            if (Objects.equals(linkId(), that.linkId())) {
                return true;
            }
        }

        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(linkId());
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("linkId", linkId())
                .add("link src", one().src().deviceId())
                .add("link dst", one().dst().deviceId())
                .toString();
    }

    @Override
    public LinkHighlight highlight(Enum<?> type) {
        RequestType requestType = (RequestType) type;

        Mod m = null;

        switch (requestType) {
            case HOST_SELECTED:
                m = PORT_TRAFFIC_GREEN;
                break;
            case DEVICE_SELECTED:
                m = PORT_TRAFFIC_ORANGE;
                break;
            default:
                break;
        }
        LinkHighlight hlite = new LinkHighlight(linkId(), SECONDARY_HIGHLIGHT);
        if (m != null) {
            hlite.addMod(m);
        }

        return hlite;
    }

    /**
     * Designates requested type.
     */
    public enum RequestType {
        HOST_SELECTED,
        DEVICE_SELECTED,
    }
}
