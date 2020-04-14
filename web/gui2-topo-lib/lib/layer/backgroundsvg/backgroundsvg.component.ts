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
import {Component, EventEmitter, Input, Output} from '@angular/core';
import {MapObject} from '../maputils';
import {MapBounds, TopoZoomPrefs, LogService, ZoomUtils} from 'org_onosproject_onos/web/gui2-fw-lib/public_api';

/**
 * model of the topo2CurrentLayout attrs from BgZoom below
 */
export interface BgZoomAttrs {
    offsetX: number;
    offsetY: number;
    scale: number;
}

/**
 * model of the topo2CurrentLayout background zoom attrs from Layout below
 */
export interface BgZoom {
    cfg: BgZoomAttrs;
    usr?: BgZoomAttrs;
}

/**
 * model of the topo2CurrentLayout breadcrumb from Layout below
 */
export interface LayoutCrumb {
    id: string;
    name: string;
}

/**
 * Enum of the topo2CurrentRegion location type from Location below
 */
export enum LocationType {
    NONE = 'none',
    GEO = 'geo',
    GRID = 'grid'
}

/**
 * model of the topo2CurrentLayout WebSocket response
 */
export interface Layout {
    id: string;
    bgDefaultScale: number;
    bgDesc: string;
    bgFilePath: string;
    bgId: string;
    bgType: LocationType;
    bgWarn: string;
    bgZoom: BgZoom;
    crumbs: LayoutCrumb[];
    parent: string;
    region: string;
    regionName: string;
}

/**
 * ONOS GUI -- Topology Background Layer View.
 *
 * TODO: consider that this layer has only one component the MapSvg and hence
 * might be able to be eliminated
 */
@Component({
    selector: '[onos-backgroundsvg]',
    templateUrl: './backgroundsvg.component.html',
    styleUrls: ['./backgroundsvg.component.css']
})
export class BackgroundSvgComponent {
    @Input() map: MapObject;
    @Output() zoomlevel = new EventEmitter<TopoZoomPrefs>();

    layoutData: Layout = <Layout>{};

    constructor(
        private log: LogService
    ) {
        this.log.debug('BackgroundSvg constructed');
    }

    /**
     * Called when ever the mapBounds event is raised by the MapSvgComponent
     *
     * @param bounds - the bounds of the newly loaded map in terms of Lat and Long
     */
    updatedBounds(bounds: MapBounds): void {
        const zoomPrefs: TopoZoomPrefs =
            ZoomUtils.convertBoundsToZoomLevel(bounds, this.log);

        this.zoomlevel.emit(zoomPrefs);
    }

}
