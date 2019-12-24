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


import {TestBed} from '@angular/core/testing';
import {LocMeta, MapBounds, ZoomUtils} from './zoomutils';

describe('ZoomUtils', () => {
    beforeEach(() => {
        TestBed.configureTestingModule({

        });
    });

    it('should be created', () => {
        const zu = new ZoomUtils();
        expect(zu).toBeTruthy();
    });

    it('should covert GEO origin to Canvas', () => {
        const canvas = ZoomUtils.convertGeoToCanvas(<LocMeta>{
            lng: 0, lat: 0
        });

        expect(canvas).not.toBeNull();
        expect(canvas.x).toEqual(500);
        expect(canvas.y).toEqual(500);
    });

    it('should covert GEO positive to Canvas', () => {
        const canvas = ZoomUtils.convertGeoToCanvas(<LocMeta>{
            lng: 30, lat: 30
        });

        expect(canvas).not.toBeNull();
        expect(Math.round(canvas.x)).toEqual(667);
        expect(canvas.y).toEqual(300);
    });

    it('should covert GEO negative to Canvas', () => {
        const canvas = ZoomUtils.convertGeoToCanvas(<LocMeta>{
            lng: -30, lat: -30
        });

        expect(canvas).not.toBeNull();
        expect(Math.round(canvas.x)).toEqual(333);
        expect(canvas.y).toEqual(700);
    });

    it('should convert XY origin to GEO', () => {
        const geo = ZoomUtils.convertXYtoGeo(0, 0);
        expect(geo.equivLoc.lng).toEqual(-90);
        expect(geo.equivLoc.lat).toEqual(75);
    });

    it('should convert XY centre to GEO', () => {
        const geo = ZoomUtils.convertXYtoGeo(500, 500);
        expect(geo.equivLoc.lng).toEqual(0);
        expect(geo.equivLoc.lat).toEqual(0);
    });

    it('should convert XY 1000 to GEO', () => {
        const geo = ZoomUtils.convertXYtoGeo(1000, 1000);
        expect(geo.equivLoc.lng).toEqual(90);
        expect(geo.equivLoc.lat).toEqual(-75);
    });

    it('should convert XY leftmost to GEO', () => {
        const geo = ZoomUtils.convertXYtoGeo(-500, 500);
        expect(geo.equivLoc.lng).toEqual(-180);
        expect(geo.equivLoc.lat).toEqual(0);
    });

    it('should convert XY rightmost to GEO', () => {
        const geo = ZoomUtils.convertXYtoGeo(1500, 500);
        expect(geo.equivLoc.lng).toEqual(+180);
        expect(geo.equivLoc.lat).toEqual(0);
    });

    it('should convert MapBounds in upper left quadrant to Zoom level', () => {
        const zoomParams = ZoomUtils.convertBoundsToZoomLevel(
            <MapBounds>{ latMin: 40, lngMin: -40, latMax: 50, lngMax: -30 });

        expect(zoomParams.sc).toEqual(11.18);
        expect(Math.round(zoomParams.tx)).toEqual(-2916);
        expect(Math.round(zoomParams.ty)).toEqual(-1736);
    });

    it('should convert MapBounds in lower right quadrant to Zoom level', () => {
        const zoomParams = ZoomUtils.convertBoundsToZoomLevel(
            <MapBounds>{ latMin: -50, lngMin: 30, latMax: -40, lngMax: 40 });

        expect(zoomParams.sc).toEqual(11.18);
        expect(Math.round(zoomParams.tx)).toEqual(-7264);
        expect(Math.round(zoomParams.ty)).toEqual(-8444);
    });

    it('should convert MapBounds around equator to Zoom level', () => {
        const zoomParams = ZoomUtils.convertBoundsToZoomLevel(
            <MapBounds>{ latMin: -10, lngMin: -10, latMax: 10, lngMax: 10 });

        expect(Math.round(zoomParams.sc * 100)).toEqual(644);
        expect(Math.round(zoomParams.tx)).toEqual(-2721);
        expect(Math.round(zoomParams.ty)).toEqual(-2721);
    });
});
