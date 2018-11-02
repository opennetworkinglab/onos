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
import {Component, Input, OnInit} from '@angular/core';
import { LocationType } from '../backgroundsvg/backgroundsvg.component';

/**
 * Enum of the topo2CurrentRegion node type from SubRegion below
 */
export enum NodeType {
    REGION = 'region',
    DEVICE = 'device'
}

/**
 * Enum of the topo2CurrentRegion layerOrder from Region below
 */
export enum LayerOrder {
    LAYER_OPTICAL = 'opt',
    LAYER_PACKET = 'pkt',
    LAYER_DEFAULT = 'def'
}

/**
 * model of the topo2CurrentRegion location from SubRegion below
 */
export interface Location {
    locType: LocationType;
    latOrY: number;
    longOrX: number;
}

/**
 * model of the topo2CurrentRegion props from SubRegion below
 */
export interface RegionProps {
    latitude: number;
    longitude: number;
    name: string;
    peerLocations: string;
}

/**
 * model of the topo2CurrentRegion subregion from Region below
 */
export interface SubRegion {
    id: string;
    location: Location;
    nDevs: number;
    nHosts: number;
    name: string;
    nodeType: NodeType;
    props: RegionProps;
}

export enum LinkType {
    UiRegionLink,
    UiDeviceLink
}

/**
 * model of the topo2CurrentRegion region rollup from Region below
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
 * model of the topo2CurrentRegion region link from Region below
 */
export interface RegionLink {
    id: string;
    epA: string;
    epB: string;
    rollup: RegionRollup[];
    type: LinkType;
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

export interface Device {
    id: string;
    layer: LayerOrder;
    location: LocationType;
    master: string;
    nodeType: NodeType;
    online: boolean;
    props: DeviceProps;
    type: string;
}

/**
 * model of the topo2CurrentRegion WebSocket response
 */
export interface Region {
    note?: string;
    id: string;
    devices: Device[][];
    hosts: Object[];
    links: RegionLink[];
    layerOrder: LayerOrder[];
    peerLocations?: Location[];
    subregions: SubRegion[];
}

/**
 * model of the topo2PeerRegions WebSocket response
 */
export interface Peer {
    peers: SubRegion[];
}

/**
 * ONOS GUI -- Topology Forces Graph Layer View.
 */
@Component({
    selector: '[onos-forcesvg]',
    templateUrl: './forcesvg.component.html',
    styleUrls: ['./forcesvg.component.css']
})
export class ForceSvgComponent implements OnInit {
    @Input() onosInstMastership: string = '';
    regionData: Region;

    constructor() { }

    ngOnInit() {
    }

}
