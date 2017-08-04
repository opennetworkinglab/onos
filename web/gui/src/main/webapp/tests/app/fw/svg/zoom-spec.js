/*
 * Copyright 2015-present Open Networking Foundation
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

/*
 ONOS GUI -- SVG -- Zoom Service - Unit Tests
 */
describe('factory: fw/svg/zoom.js', function() {
    var $log, fs, zs, svg, zoomLayer, zoomer;

    var cz = 'ZoomService.createZoomer(): ',
        d3s = ' (D3 selection) property defined';

    beforeEach(module('onosUtil', 'onosSvg'));

    beforeEach(inject(function (_$log_, FnService, ZoomService) {
        $log = _$log_;
        fs = FnService;
        zs = ZoomService;
        svg = d3.select('body').append('svg').attr('id', 'mySvg');
        zoomLayer = svg.append('g').attr('id', 'myZoomlayer');
    }));

    afterEach(function () {
        d3.select('#mySvg').remove();
        // Note: since zoomLayer is a child of svg, it should be removed also
    });

    it('should define ZoomService', function () {
        expect(zs).toBeDefined();
    });

    it('should define api functions', function () {
        expect(fs.areFunctions(zs, ['createZoomer'])).toBeTruthy();
    });

    function verifyZoomerApi() {
        expect(fs.areFunctions(zoomer, [
            'panZoom', 'reset', 'translate', 'scale', 'scaleExtent'
        ])).toBeTruthy();
    }

    it('should fail gracefully with no option object', function () {
        spyOn($log, 'error');

        zoomer = zs.createZoomer();
        expect($log.error).toHaveBeenCalledWith(cz + 'No "svg" (svg tag)' + d3s);
        expect($log.error).toHaveBeenCalledWith(cz + 'No "zoomLayer" (g tag)' + d3s);
        expect(zoomer).toBeNull();
    });

    it('should complain if we miss required options', function () {
        spyOn($log, 'error');

        zoomer = zs.createZoomer({});
        expect($log.error).toHaveBeenCalledWith(cz + 'No "svg" (svg tag)' + d3s);
        expect($log.error).toHaveBeenCalledWith(cz + 'No "zoomLayer" (g tag)' + d3s);
        expect(zoomer).toBeNull();
    });

    it('should work with minimal parameters', function () {
        spyOn($log, 'error');

        zoomer = zs.createZoomer({
            svg: svg,
            zoomLayer: zoomLayer
        });
        expect($log.error).not.toHaveBeenCalled();
        verifyZoomerApi();
    });

    it('should start at scale 1 and translate 0,0', function () {
        zoomer = zs.createZoomer({
            svg: svg,
            zoomLayer: zoomLayer
        });
        verifyZoomerApi();
        expect(zoomer.translate()).toEqual([0,0]);
        expect(zoomer.scale()).toEqual(1);
    });

    it('should allow programmatic pan/zoom', function () {
        zoomer = zs.createZoomer({
            svg: svg,
            zoomLayer: zoomLayer
        });
        verifyZoomerApi();
        expect(zoomer.translate()).toEqual([0,0]);
        expect(zoomer.scale()).toEqual(1);

        zoomer.panZoom([20,30], 3);
        expect(zoomer.translate()).toEqual([20,30]);
        expect(zoomer.scale()).toEqual(3);
    });

    xit('should provide default scale extent', function () {
        zoomer = zs.createZoomer({
            svg: svg,
            zoomLayer: zoomLayer
        });
        expect(zoomer.scaleExtent()).toEqual([0.25, 10]);
    });

    xit('should allow us to override the minimum zoom', function () {
        zoomer = zs.createZoomer({
            svg: svg,
            zoomLayer: zoomLayer,
            zoomMin: 1.23
        });
        expect(zoomer.scaleExtent()).toEqual([1.23, 10]);
    });

    xit('should allow us to override the maximum zoom', function () {
        zoomer = zs.createZoomer({
            svg: svg,
            zoomLayer: zoomLayer,
            zoomMax: 13
        });
        expect(zoomer.scaleExtent()).toEqual([0.25, 13]);
    });

    // TODO: test zoomed() where we fake out the d3.event.sourceEvent etc...
    //  need to check default enabled (true) and custom enabled predicate
    //  need to check that the callback is invoked also

    it('should invoke the callback on programmatic pan/zoom', function () {
        var foo = { cb: function () {} };
        spyOn(foo, 'cb');

        zoomer = zs.createZoomer({
            svg: svg,
            zoomLayer: zoomLayer,
            zoomCallback: foo.cb
        });

        zoomer.panZoom([0,0], 2);
        expect(foo.cb).toHaveBeenCalled();
    });

});
