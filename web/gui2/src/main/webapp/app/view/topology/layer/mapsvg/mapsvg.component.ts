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
    Component,
    Input,
    OnChanges,
    SimpleChanges
} from '@angular/core';
import { MapObject } from '../maputils';
import {LogService} from 'gui2-fw-lib';
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
 * Model of the Generator setting for D3 GEO
 */
interface GeneratorSettings {
    objectTag: string;
    projection: Object;
    logicalSize: number;
    mapFillScale: number;
}

/**
 * Model of the Path Generator
 */
interface PathGenerator {
    geodata: FeatureCollection;
    pathgen: (Feature) => string;
    settings: GeneratorSettings;
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
interface TopoData {
    type: string; // Usually "Topology"
    objects: Object; // Can be a list of countries or individual countries
    arcs: number[][][]; // Coordinates
    bbox: number[]; // Bounding box
    transform: TopoDataTransform; // scale and translate
}

/**
 * Default settings for the path generator for TopoJson
 */
const DEFAULT_GEN_SETTINGS: GeneratorSettings = <GeneratorSettings>{
    objectTag: 'states',
    projection: d3.geoMercator(),
    logicalSize: 1000,
    mapFillScale: .95,
};

@Component({
    selector: '[onos-mapsvg]',
    templateUrl: './mapsvg.component.html',
    styleUrls: ['./mapsvg.component.css']
})
export class MapSvgComponent implements  OnChanges {
    @Input() map: MapObject = <MapObject>{id: 'none'};

    cache = new Map<string, TopoData>();
    topodata: TopoData;
    mapPathGenerator: PathGenerator;

    constructor(
        private log: LogService,
        private httpClient: HttpClient,
    ) {
        this.log.debug('MapSvgComponent constructed');
    }

    static getUrl(id: string): string {
        if (id && id[0] === '*') {
            return BUNDLED_URL_PREFIX + id.slice(1) + '.topojson';
        }
        return id + '.topojson';
    }

    ngOnChanges(changes: SimpleChanges): void {
        this.log.debug('Change detected', changes);
        if (changes['map']) {
            const map: MapObject = <MapObject>(changes['map'].currentValue);
            if (map.id) {
                if (this.cache.get(map.id)) {
                    this.topodata = this.cache.get(map.id);
                } else {
                    this.httpClient
                        .get(MapSvgComponent.getUrl(map.filePath))
                        .subscribe((topoData: TopoData) => {
                            this.mapPathGenerator = this.handleTopoJson(map, topoData);
                            this.log.debug('Path Generated for', map.id,
                                'from', MapSvgComponent.getUrl(map.filePath));
                        });
                }
            }
        }
    }

    /**
     * Wrapper for the path generator function
     * @param feature The county or state within the map
     */
    pathGenerator(feature: Feature): string {
        return this.mapPathGenerator.pathgen(feature);
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
    handleTopoJson(map: MapObject, topoData: TopoData): PathGenerator {
        this.topodata = topoData;
        this.cache.set(map.id, topoData);
        this.log.debug('Map retrieved', topoData);

        const topoObject = topoData.objects[map.id];
        const geoData: FeatureCollection = <FeatureCollection>topojson.feature(topoData, topoObject);
        this.log.debug('Map retrieved', topoData, geoData);

        const settings: GeneratorSettings = Object.assign({}, DEFAULT_GEN_SETTINGS);
        const path = d3.geoPath().projection(settings.projection);
        this.rescaleProjection(
            settings.projection,
            settings.mapFillScale,
            settings.logicalSize,
            path,
            geoData);
        this.log.debug('Scale adjusted');

        return <PathGenerator>{
            geodata: geoData,
            pathgen: path,
            settings: settings
        };
    }

    /**
     * Adjust projection scale and translation to fill the view
     * with the map
     * @param proj
     * @param mfs
     * @param dim
     * @param path
     * @param geoData
     * @param adjustScale
     */
    rescaleProjection(proj: any, mfs: number, dim: number, path: any,
                      geoData: FeatureCollection, adjustScale: number = 1.0) {
        // start with unit scale, no translation..
        proj.scale(1).translate([0, 0]);

        // figure out dimensions of map data..
        const b = path.bounds(geoData);
        const x1 = b[0][0];
        const y1 = b[0][1];
        const x2 = b[1][0];
        const y2 = b[1][1];
        const dx = x2 - x1;
        const dy = y2 - y1;
        const x = (x1 + x2) / 2;
        const y = (y1 + y2) / 2;

        // size map to 95% of minimum dimension to fill space..
        const s = (mfs / Math.min(dx / dim, dy / dim)) * adjustScale;
        const t = [dim / 2 - s * x, dim / 2 - s * y];

        // set new scale, translation on the projection..
        proj.scale(s).translate(t);
    }
}
