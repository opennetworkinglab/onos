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
import {
    LayerType,
    Location,
    NodeType,
    RegionProps
} from './regions';

/**
 * Toggle state for how labels should be displayed
 */
export enum LabelToggle {
    NONE,
    ID,
    NAME
}

export namespace LabelToggle {
    export function next(current: LabelToggle) {
        if (current === LabelToggle.NONE) {
            return LabelToggle.ID;
        } else if (current === LabelToggle.ID) {
            return LabelToggle.NAME;
        } else if (current === LabelToggle.NAME) {
            return LabelToggle.NONE;
        }
    }
}

/**
 * model of the topo2CurrentRegion device props from Device below
 */
export interface DeviceProps {
    latitude: number;
    longitude: number;
    name: string;
    locType: LocationType;
}

/**
 * model of the topo2CurrentRegion Loc part of the MetaUi below
 */
export interface LocMeta {
    lng: number;
    lat: number;
}

/**
 * model of the topo2CurrentRegion MetaUi from Device below
 */
export interface MetaUi {
    equivLoc: LocMeta;
    x: number;
    y: number;
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
export abstract class Node implements d3.SimulationNodeDatum {
    // Optional - defining optional implementation properties - required for relevant typing assistance
    index?: number;
    x: number;
    y: number;
    vx?: number;
    vy?: number;
    fx?: number | null;
    fy?: number | null;

    id: string;

    protected constructor(id) {
        this.id = id;
        this.x = 0;
        this.y = 0;
    }
}

/**
 * model of the topo2CurrentRegion device from Region below
 */
export class Device extends Node {
    id: string;
    layer: LayerType;
    location: LocationType;
    metaUi: MetaUi;
    master: string;
    nodeType: NodeType;
    online: boolean;
    props: DeviceProps;
    type: string;

    constructor(id: string) {
        super(id);
    }
}

export class Host extends Node {
    configured: boolean;
    id: string;
    ips: string[];
    layer: LayerType;
    nodeType: NodeType;
    props: HostProps;

    constructor(id: string) {
        super(id);
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
    nodeType: NodeType;
    props: RegionProps;

    constructor(id: string) {
        super(id);
    }
}
