/*
 * Copyright 2019-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the 'License');
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an 'AS IS' BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import {Node, UiElement} from './node';
import * as d3 from 'd3';

export enum LinkType {
    UiRegionLink,
    UiDeviceLink,
    UiEdgeLink
}

/**
 * model of the topo2CurrentRegion region rollup from Region below
 *
 */
export interface RegionRollup {
    id: string;
    epA: string;
    epB: string;
    portA: string;
    portB: string;
    type: LinkType;
}

/**
 * Implementing SimulationLinkDatum interface into our custom Link class
 */
export class Link implements UiElement, d3.SimulationLinkDatum<Node> {
    // Optional - defining optional implementation properties - required for relevant typing assistance
    index?: number;
    id: string; // The id of the link in the format epA/portA~epB/portB
    epA: string; // The name of the device or host at one end
    epB: string; // The name of the device or host at the other end
    portA: string; // The number of the port at one end
    portB: string; // The number of the port at the other end
    type: LinkType;
    rollup: RegionRollup[]; // Links in sub regions represented by this one link

    // Must - defining enforced implementation properties
    source: Node;
    target: Node;

    public static deviceNameFromEp(ep: string): string {
        if (ep !== undefined && ep.lastIndexOf('/') > 0) {
            return ep.substr(0, ep.lastIndexOf('/'));
        }
        return ep;
    }

    /**
     * The WSS event showHighlights is sent up with a slightly different
     * name format on the link id using the "-" separator rather than the "~"
     * @param linkId The id of the link in either format
     */
    public static linkIdFromShowHighlights(linkId: string) {
        if (linkId.includes('-')) {
            const parts: string[] = linkId.split('-');
            const part0 = Link.removeHostPortNum(parts[0]);
            const part1 = Link.removeHostPortNum(parts[1]);
            return part0 + '~' + part1;
        }
        return linkId;
    }

    private static removeHostPortNum(hostStr: string) {
        if (hostStr.includes('/None/')) {
            const subparts = hostStr.split('/');
            return subparts[0] + '/' + subparts[1];
        }
        return hostStr;
    }

    constructor(source, target) {
        this.source = source;
        this.target = target;
    }

    linkTypeStr(): string {
        return LinkType[this.type];
    }
}

/**
 * model of the topo2CurrentRegion region link from Region
 */
export class RegionLink extends Link {

    constructor(type: LinkType, nodeA: Node, nodeB: Node) {
        super(nodeA, nodeB);
        this.type = type;
    }
}

/**
 * model of the highlights that are sent back from WebSocket when traffic is shown
 */
export interface LinkHighlight {
    id: string;
    css: string;
    label: string;
    fadems: number;
}
