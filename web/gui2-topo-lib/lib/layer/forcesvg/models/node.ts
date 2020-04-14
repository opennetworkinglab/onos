/*
 * Copyright 2018-present Open Networking Foundation
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
import * as d3 from 'd3';
import {LocationType} from '../../backgroundsvg/backgroundsvg.component';
import {LayerType, Location, NodeType, RegionProps} from './regions';
import {LocMeta, MetaUi, ZoomUtils} from 'org_onosproject_onos/web/gui2-fw-lib/public_api';

export interface UiElement {
    index?: number;
    id: string;
}

export namespace LabelToggle {
    /**
     * Toggle state for how device labels should be displayed
     */
    export enum Enum {
        NONE,
        ID,
        NAME
    }

    /**
     * Add the method 'next()' to the LabelToggle enum above
     */
    export function next(current: Enum) {
        if (current === Enum.NONE) {
            return Enum.ID;
        } else if (current === Enum.ID) {
            return Enum.NAME;
        } else if (current === Enum.NAME) {
            return Enum.NONE;
        }
    }
}

export namespace HostLabelToggle {
    /**
     * Toggle state for how host labels should be displayed
     */
    export enum Enum {
        NONE,
        NAME,
        IP,
        MAC
    }

    /**
     * Add the method 'next()' to the HostLabelToggle enum above
     */
    export function next(current: Enum) {
        if (current === Enum.NONE) {
            return Enum.NAME;
        } else if (current === Enum.NAME) {
            return Enum.IP;
        } else if (current === Enum.IP) {
            return Enum.MAC;
        } else if (current === Enum.MAC) {
            return Enum.NONE;
        }
    }
}

export namespace GridDisplayToggle {
    /**
     * Toggle state for how the grid should be displayed
     */
    export enum Enum {
        GRIDNONE,
        GRID1000,
        GRIDGEO,
        GRIDBOTH
    }

    /**
     * Add the method 'next()' to the GridDisplayToggle enum above
     */
    export function next(current: Enum) {
        if (current === Enum.GRIDNONE) {
            return Enum.GRID1000;
        } else if (current === Enum.GRID1000) {
            return Enum.GRIDGEO;
        } else if (current === Enum.GRIDGEO) {
            return Enum.GRIDBOTH;
        } else if (current === Enum.GRIDBOTH) {
            return Enum.GRIDNONE;
        }
    }
}

/**
 * model of the topo2CurrentRegion device props from Device below
 */
export interface DeviceProps {
    latitude: string;
    longitude: string;
    gridX: number;
    gridY: number;
    name: string;
    locType: LocationType;
    uiType: string;
    channelId: string;
    managementAddress: string;
    protocol: string;
    driver: string;
}

export interface HostProps {
    gridX: number;
    gridY: number;
    latitude: number;
    longitude: number;
    locType: LocationType;
    name: string;
}

/**
 * Implementing SimulationNodeDatum interface into our custom Node class
 */
export class Node implements UiElement, d3.SimulationNodeDatum {
    // Optional - defining optional implementation properties - required for relevant typing assistance
    index?: number;
    x: number;
    y: number;
    vx?: number;
    vy?: number;
    fx?: number | null;
    fy?: number | null;
    nodeType: NodeType;
    location: Location;
    id: string;

    protected constructor(id) {
        this.id = id;
        this.x = 0;
        this.y = 0;
    }

    /**
     * Static method to reset the node's position to that specified in its
     * coordinates
     * This is overridden for host
     * @param node The node to reset
     */
    static resetNodeLocation(node: Node): void {
        let origLoc: MetaUi;

        if (!node.location || node.location.locType === LocationType.NONE) {
            // No location - nothing to do
            return;
        } else if (node.location.locType === LocationType.GEO) {
            origLoc = ZoomUtils.convertGeoToCanvas(<LocMeta>{
                lng: node.location.longOrX,
                lat: node.location.latOrY
            });
        } else if (node.location.locType === LocationType.GRID) {
            origLoc = ZoomUtils.convertXYtoGeo(
                node.location.longOrX, node.location.latOrY);
        }
        Node.moveNodeTo(node, origLoc);
    }

    protected static moveNodeTo(node: Node, origLoc: MetaUi) {
        const currentX = node.fx;
        const currentY = node.fy;
        const distX = origLoc.x - node.fx;
        const distY = origLoc.y - node.fy;
        let count = 0;
        const task = setInterval(() => {
            count++;
            if (count >= 10) {
                clearInterval(task);
            }
            node.fx = currentX + count * distX / 10;
            node.fy = currentY + count * distY / 10;
        }, 50);
    }

    static unpinOrFreezeNode(node: Node, freeze: boolean): void {
        if (!node.location || node.location.locType === LocationType.NONE) {
            if (freeze) {
                node.fx = node.x;
                node.fy = node.y;
            } else {
                node.fx = null;
                node.fy = null;
            }
        }
    }
}

export interface Badge {
    status: string;
    isGlyph: boolean;
    txt: string;
    msg: string;
}

/**
 * model of the topo2CurrentRegion device from Region below
 */
export class Device extends Node {
    id: string;
    layer: LayerType;
    metaUi: MetaUi;
    master: string;
    online: boolean;
    props: DeviceProps;
    type: string;

    constructor(id: string) {
        super(id);
    }
}

export interface DeviceHighlight {
    id: string;
    badge: Badge;
}

export interface HostHighlight {
    id: string;
    badge: Badge;
}

/**
 * Model of the ONOS Host element in the topology
 */
export class Host extends Node {
    configured: boolean;
    id: string;
    ips: string[];
    layer: LayerType;
    props: HostProps;

    constructor(id: string) {
        super(id);
    }

    static resetNodeLocation(host: Host): void {
        let origLoc: MetaUi;

        if (!host.props || host.props.locType === LocationType.NONE) {
            // No location - nothing to do
            return;
        } else if (host.props.locType === LocationType.GEO) {
            origLoc = ZoomUtils.convertGeoToCanvas(<LocMeta>{
                lng: host.props.longitude,
                lat: host.props.latitude
            });
        } else if (host.props.locType === LocationType.GRID) {
            origLoc = ZoomUtils.convertXYtoGeo(
                host.props.gridX, host.props.gridY);
        }
        Node.moveNodeTo(host, origLoc);
    }
}


/**
 * model of the topo2CurrentRegion subregion from Region below
 */
export class SubRegion extends Node {
    id: string;
    location: Location;
    nDevs: number;
    nHosts: number;
    name: string;
    props: RegionProps;

    constructor(id: string) {
        super(id);
    }
}

/**
 * Enumerated values for topology update event types
 */
export enum ModelEventType {
    HOST_ADDED_OR_UPDATED,
    LINK_ADDED_OR_UPDATED,
    DEVICE_ADDED_OR_UPDATED,
    DEVICE_REMOVED,
    HOST_REMOVED,
    LINK_REMOVED,
}

/**
 * Enumerated values for topology update event memo field
 */
export enum ModelEventMemo {
    ADDED = 'added',
    REMOVED = 'removed',
    UPDATED = 'updated'
}

