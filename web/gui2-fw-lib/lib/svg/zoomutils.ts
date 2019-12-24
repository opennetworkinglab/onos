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


import {LogService} from '../log.service';

const LONGITUDE_EXTENT = 180;
const LATITUDE_EXTENT = 75;
const GRID_EXTENT_X = 2000;
const GRID_EXTENT_Y = 1000;
const GRID_DIAGONAL = 2236; // 2236 is the length of the diagonal of the 2000x1000 box
const GRID_CENTRE_X = 500;
const GRID_CENTRE_Y = 500;


/**
 * A model of the map bounds bottom left to top right in lat and long
 */
export interface MapBounds {
    lngMin: number;
    latMin: number;
    lngMax: number;
    latMax: number;
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

/**
 * Model of the Zoom preferences
 */
export interface TopoZoomPrefs {
    tx: number;
    ty: number;
    sc: number;
}

/**
 * Utility class with static functions for scaling maps
 *
 * This is left as a class, so that the functions are loaded only as needed
 */
export class ZoomUtils {
    static convertGeoToCanvas(location: LocMeta ): MetaUi {
        const calcX = (LONGITUDE_EXTENT + location.lng) / (LONGITUDE_EXTENT * 2) * GRID_EXTENT_X - GRID_CENTRE_X;
        const calcY = (LATITUDE_EXTENT - location.lat) / (LATITUDE_EXTENT * 2) * GRID_EXTENT_Y;
        return <MetaUi>{
            x: calcX,
            y: calcY,
            equivLoc: {
                lat: location.lat,
                lng: location.lng
            }
        };
    }

    static convertXYtoGeo(x: number, y: number): MetaUi {
        const calcLong: number = (x + GRID_CENTRE_X) * 2 * LONGITUDE_EXTENT / GRID_EXTENT_X - LONGITUDE_EXTENT;
        const calcLat: number = -(y * 2 * LATITUDE_EXTENT / GRID_EXTENT_Y - LATITUDE_EXTENT);
        return <MetaUi>{
            x: x,
            y: y,
            equivLoc: <LocMeta>{
                lat: (calcLat === -0) ? 0 : calcLat,
                lng: calcLong
            }
        };
    }

    /**
     * This converts the bounds of a map loaded from a TopoGson file that has been
     * converted in to a GEOJson format by d3
     *
     * The bounds are in latitude and longitude from bottom left (min) to top right (max)
     *
     * First they are converted in to SVG viewbox coordinates 0,0 top left 1000x1000
     *
     * The the zoom level is calculated by scaling to the grid diagonal
     *
     * Finally the translation is calculated by applying the zoom first, and then
     * translating on the zoomed coordinate system
     * @param mapBounds - the bounding box of the chosen map in lat and long
     * @param log The LogService
     */
    static convertBoundsToZoomLevel(mapBounds: MapBounds, log?: LogService): TopoZoomPrefs {

        const min: MetaUi = this.convertGeoToCanvas(<LocMeta>{
            lng: mapBounds.lngMin,
            lat: mapBounds.latMin
        });

        const max: MetaUi = this.convertGeoToCanvas(<LocMeta>{
            lng: mapBounds.lngMax,
            lat: mapBounds.latMax
        });

        const diagonal = Math.sqrt(Math.pow(max.x - min.x, 2) + Math.pow(max.y - min.y, 2));
        const centreX = (max.x - min.x) / 2 + min.x;
        const centreY = (max.y - min.y) / 2 + min.y;
        // Zoom works from the top left of the 1000x1000 viewbox
        // The scale is applied first and then the translate is on the scaled coordinates
        const zoomscale = 0.5 * GRID_DIAGONAL / ((diagonal < 100) ? 100 : diagonal); // Don't divide by zero
        const zoomx = -centreX * zoomscale + GRID_CENTRE_X;
        const zoomy = -centreY * zoomscale + GRID_CENTRE_Y;

        // log.debug('MapBounds', mapBounds, 'XYMin', min, 'XYMax', max, 'Diag', diagonal,
        //     'Centre', centreX, centreY, 'translate', zoomx, zoomy, 'Scale', zoomscale);

        return <TopoZoomPrefs>{tx: zoomx, ty: zoomy, sc: zoomscale};
    }

    /**
     * Calculate Zoom settings to fit the 1000x1000 grid in to the available window height
     * less the banner height
     *
     * Scaling always happens from the top left 0,0
     * If the height is greater than the width then no scaling is required - grid will
     * need to fill the SVG canvas
     * @param bannerHeight - the top band of the screen for the mast
     * @param innerWidth - the actual width of the screen
     * @param innerHeight - the actual height of the screen
     * @return Zoom settings - scale and translate
     */
    static zoomToWindowSize(bannerHeight: number, innerWidth: number, innerHeight: number): TopoZoomPrefs {
        const newHeight = innerHeight - bannerHeight;
        if (newHeight > innerWidth) {
            return <TopoZoomPrefs>{
                sc: 1.0,
                tx: 0,
                ty: 0
            };
        } else {
            const scale = newHeight / innerWidth;
            return <TopoZoomPrefs>{
                sc: scale,
                tx: (500 / scale - 500) * scale,
                ty: 0
            };
        }
    }
}
