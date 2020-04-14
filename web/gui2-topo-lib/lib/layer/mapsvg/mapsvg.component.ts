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
import {
    Component, EventEmitter,
    Input,
    OnChanges, Output,
    SimpleChanges
} from '@angular/core';
import { MapObject } from '../maputils';
import {LogService, MapBounds} from 'org_onosproject_onos/web/gui2-fw-lib/public_api';
import {HttpClient} from '@angular/common/http';
import * as d3 from 'd3';
import * as topojson from 'topojson-client';

const BUNDLED_URL_PREFIX = 'data/map/';

/**
 * Model of the transform attribute of a topojson file
 */
interface TopoDataTransform {
    scale: number[];
    translate: number[];
}

/**
 * Model of the Feature returned prom topojson library
 */
interface Feature {
    geometry: Object;
    id: string;
    properties: Object;
    type: string;
}

/**
 * Model of the Features Collection returned by the topojson.features function
 */
interface FeatureCollection {
    type: string;
    features: Feature[];
}

/**
 * Model of the topojson file
 */
export interface TopoData {
    type: string; // Usually "Topology"
    objects: Object; // Can be a list of countries or individual countries
    arcs: number[][][]; // Coordinates
    bbox: number[]; // Bounding box
    transform: TopoDataTransform; // scale and translate
}

@Component({
    selector: '[onos-mapsvg]',
    templateUrl: './mapsvg.component.html',
    styleUrls: ['./mapsvg.component.css']
})
export class MapSvgComponent implements  OnChanges {
    @Input() map: MapObject = <MapObject>{id: 'none'};
    @Output() mapBounds = new EventEmitter<MapBounds>();

    geodata: FeatureCollection;
    pathgen: any; // (Feature) => string; have to leave it general, as there is the bounds method used below
    // testPath: string;
    // testFeature = <Feature>{
    //     id: 'test',
    //     type: 'Feature',
    //     geometry: {
    //         coordinates: [
    //             [[-15, 60], [45, 60], [45, 45], [-15, 45], [-15, 60]],
    //             [[-10, 55], [45, 55], [45, 50], [-10, 50], [-10, 55]],
    //         ],
    //         type: 'Polygon'
    //     },
    //     properties: { name: 'Test'}
    // };

    constructor(
        private log: LogService,
        private httpClient: HttpClient,
    ) {
        // Scale everything to 360 degrees wide and 150 high
        // See background.component.html for more details
        this.pathgen = d3.geoPath().projection(d3.geoIdentity().reflectY(true)
            // MapSvgComponent.scale()
        );

        // this.log.debug('Feature Test',this.testFeature);
        // this.testPath = this.pathgen(this.testFeature);
        // this.log.debug('Feature Path', this.testPath);
        this.log.debug('MapSvgComponent constructed');
    }

    static getUrl(id: string): string {
        if (id && id[0] === '*') {
            return BUNDLED_URL_PREFIX + id.slice(1) + '.topojson';
        }
        return id + '.topojson';
    }

    /**
     * Wrapper for the path generator function
     * @param feature The county or state within the map
     */
    pathGenerator(feature: Feature): string {
        return this.pathgen(feature);
    }

    ngOnChanges(changes: SimpleChanges): void {
        this.log.debug('Change detected', changes);
        if (changes['map']) {
            const map: MapObject = <MapObject>(changes['map'].currentValue);
            if (map.id) {
                this.httpClient
                    .get(MapSvgComponent.getUrl(map.filePath))
                    .subscribe((topoData: TopoData) => {
                        // this.mapPathGenerator =
                        this.handleTopoJson(map, topoData);
                        this.log.debug('Path Generated for', map.id,
                            'from', MapSvgComponent.getUrl(map.filePath));
                    });
            }
        }
    }

    /**
     * Handle the topojson file stream as it arrives back from the server
     *
     * The topojson library converts the topojson file in to a FeatureCollection
     * d3.geo then further converts this in to a Path
     *
     * @param map The Map chosen in the GUI
     * @param topoData The data in the TopoJson file
     */
    handleTopoJson(map: MapObject, topoData: TopoData): void {

        let topoObject = topoData.objects[map.id];
        if (!topoObject) {
            topoObject = topoData.objects['states'];
        }
        this.log.debug('Topo obj', topoObject, 'topodata', topoData);
        this.geodata = <FeatureCollection>topojson.feature(topoData, topoObject);
        const bounds = this.pathgen.bounds(this.geodata);
        this.mapBounds.emit(<MapBounds>{
            lngMin: bounds[0][0],
            latMin: -bounds[0][1], // Y was inverted in the transform
            lngMax: bounds[1][0],
            latMax: -bounds[1][1] // Y was inverted in the transform
        });
        this.log.debug('Map retrieved', topoData, this.geodata);

    }
}
