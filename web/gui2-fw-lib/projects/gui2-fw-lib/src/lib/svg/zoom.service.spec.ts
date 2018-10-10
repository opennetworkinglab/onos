/*
 * Copyright 2018-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
*
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { TestBed, inject } from '@angular/core/testing';
import { ActivatedRoute, Params } from '@angular/router';
import { of } from 'rxjs';
import * as d3 from 'd3';

import { LogService } from '../log.service';
import { FnService } from '../util/fn.service';

import { ZoomService, CZ, D3S, ZoomOpts, Zoomer } from './zoom.service';

class MockActivatedRoute extends ActivatedRoute {
    constructor(params: Params) {
        super();
        this.queryParams = of(params);
    }
}


/**
 * ONOS GUI -- SVG -- Zoom Service - Unit Tests
 */
describe('ZoomService', () => {
    let zs: ZoomService;
    let ar: ActivatedRoute;
    let fs: FnService;
    let mockWindow: Window;
    let logServiceSpy: jasmine.SpyObj<LogService>;

    const svg = d3.select('body').append('svg').attr('id', 'mySvg');
    const zoomLayer = svg.append('g').attr('id', 'myZoomlayer');

    beforeEach(() => {
        const logSpy = jasmine.createSpyObj('LogService', ['debug', 'warn', 'error']);
        ar = new MockActivatedRoute({'debug': 'TestService'});
        mockWindow = <any>{
            innerWidth: 400,
            innerHeight: 200,
            navigator: {
                userAgent: 'defaultUA'
            }
        };
        fs = new FnService(ar, logSpy, mockWindow);

        TestBed.configureTestingModule({
            providers: [ ZoomService,
                { provide: FnService, useValue: fs },
                { provide: LogService, useValue: logSpy },
                { provide: ActivatedRoute, useValue: ar },
                { provide: 'Window', useFactory: (() => mockWindow ) }
            ]
        });

        zs = TestBed.get(ZoomService);
        logServiceSpy = TestBed.get(LogService);
    });

    it('should be created', () => {
        expect(zs).toBeTruthy();
    });

    it('should define ZoomService', function () {
        expect(zs).toBeDefined();
    });

    it('should define api functions', function () {
        expect(fs.areFunctions(zs, [
            'createZoomer',
            'zoomed',
            'adjustZoomLayer'
        ])).toBeTruthy();
    });

    function verifyZoomerApi() {
        expect(fs.areFunctions(zs.zoomer, [
            'panZoom', 'reset', 'translate', 'scale', 'scaleExtent'
        ])).toBeTruthy();
    }

    it('should fail gracefully with no option object', function () {
        expect(() => zs.createZoomer(<ZoomOpts>{}))
            .toThrow(new Error(CZ + 'No "svg" (svg tag)' + D3S));
        expect(logServiceSpy.error)
            .toHaveBeenCalledWith(CZ + 'No "svg" (svg tag)' + D3S);
    });

    it('should complain if we miss required options', function () {
        expect(() => zs.createZoomer(<ZoomOpts>{svg: svg}))
            .toThrow(new Error(CZ + 'No "zoomLayer" (g tag)' + D3S));
        expect(logServiceSpy.error).toHaveBeenCalledWith(CZ + 'No "zoomLayer" (g tag)' + D3S);
    });

    it('should work with minimal parameters', function () {
        const zoomer = zs.createZoomer(<ZoomOpts>{
            svg: svg,
            zoomLayer: zoomLayer
        });
        expect(logServiceSpy.error).not.toHaveBeenCalled();
        verifyZoomerApi();
    });

    it('should start at scale 1 and translate 0,0', function () {
        const zoomer = zs.createZoomer(<ZoomOpts>{
            svg: svg,
            zoomLayer: zoomLayer
        });
        verifyZoomerApi();
        expect(zoomer.translate()).toEqual([0, 0]);
        expect(zoomer.scale()).toEqual(1);
    });

    it('should allow programmatic pan/zoom', function () {
        const zoomer: Zoomer = zs.createZoomer(<ZoomOpts>{
            svg: svg,
            zoomLayer: zoomLayer
        });
        verifyZoomerApi();

        expect(zoomer.translate()).toEqual([0, 0]);
        expect(zoomer.scale()).toEqual(1);

        zoomer.panZoom([20, 30], 1);
        expect(zoomer.translate()).toEqual([20, 30]);
        expect(zoomer.scale()).toEqual(1);

        zoomer.reset();
        expect(zoomer.translate()).toEqual([0, 0]);
        expect(zoomer.scale()).toEqual(1);


    });

    it('should provide default scale extent', function () {
        const zoomer = zs.createZoomer(<ZoomOpts>{
            svg: svg,
            zoomLayer: zoomLayer
        });
        expect(zoomer.scaleExtent()).toEqual([0.05, 50]);
    });

    it('should allow us to override the minimum zoom', function () {
        const zoomer = zs.createZoomer(<ZoomOpts>{
            svg: svg,
            zoomLayer: zoomLayer,
            zoomMin: 1.23
        });
        expect(zoomer.scaleExtent()).toEqual([1.23, 50]);
    });

    it('should allow us to override the maximum zoom', function () {
        const zoomer = zs.createZoomer(<ZoomOpts>{
            svg: svg,
            zoomLayer: zoomLayer,
            zoomMax: 13
        });
        expect(zoomer.scaleExtent()).toEqual([0.05, 13]);
    });

    // TODO: test zoomed() where we fake out the d3.event.sourceEvent etc...
    //  need to check default enabled (true) and custom enabled predicate
    //  need to check that the callback is invoked also

    it('should invoke the callback on programmatic pan/zoom', function () {
        const foo = { cb() { return; } };
        spyOn(foo, 'cb');

        const zoomer = zs.createZoomer(<ZoomOpts>{
            svg: svg,
            zoomMin: 0.25,
            zoomMax: 10,
            zoomLayer: zoomLayer,
            zoomEnabled: (ev) => true,
            zoomCallback: foo.cb,
        });

        zoomer.panZoom([0, 0], 2);
        expect(foo.cb).toHaveBeenCalled();
    });
});
