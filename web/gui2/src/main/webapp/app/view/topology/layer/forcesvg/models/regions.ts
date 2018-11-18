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
/**
 * Enum of the topo2CurrentRegion node type from SubRegion below
 */
import {LocationType} from '../../backgroundsvg/backgroundsvg.component';
import {Device, Host, SubRegion} from './node';
import {RegionLink} from './link';

export enum NodeType {
    REGION = 'region',
    DEVICE = 'device',
    HOST = 'host',
}

/**
 * Enum of the topo2CurrentRegion layerOrder from Region below
 */
export enum LayerType {
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
 * model of the topo2CurrentRegion WebSocket response
 *
 * The Devices are in a 2D array - 1st order is layer type, 2nd order is
 * devices in that layer
 */
export interface Region {
    note?: string;
    id: string;
    devices: Device[][];
    hosts: Host[][];
    links: RegionLink[];
    layerOrder: LayerType[];
    peerLocations?: Location[];
    subregions: SubRegion[];
}

/**
 * model of the topo2PeerRegions WebSocket response
 */
export interface Peer {
    peers: SubRegion[];
}

