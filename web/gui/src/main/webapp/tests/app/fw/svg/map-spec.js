/*
 * Copyright 2015 Open Networking Laboratory
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
 ONOS GUI -- SVG -- Map Service - Unit Tests

 @author Simon Hunt
 */
describe('factory: fw/svg/map.js', function() {
    var $log, $httpBackend, fs, ms, d3Elem, promise;

    beforeEach(module('onosUtil', 'onosSvg'));

    beforeEach(inject(function (_$log_, _$httpBackend_, FnService, MapService) {
        $log = _$log_;
        $httpBackend = _$httpBackend_;
        fs = FnService;
        ms = MapService;
        //ms.clearCache();
        d3Elem = d3.select('body').append('svg').append('g').attr('id', 'mapLayer');
    }));

    afterEach(function () {
        d3.select('svg').remove();
    });

    it('should define MapService', function () {
        expect(ms).toBeDefined();
    });

    it('should define api functions', function () {
        expect(fs.areFunctions(ms, [
            'loadMapInto'
        ])).toBeTruthy();
    });

    var fakeMapId = '../tests/app/fw/svg/fake-map-data',
        fakeMapUrl = fakeMapId + '.json';

    var fakeMapData = {
        "type": "Topology",
        "objects": {
            "states": {
                "type": "GeometryCollection",
                "geometries": [
                    { "type": "Polygon", "arcs": [[0, 1]]},
                    { "type": "Polygon", "arcs": [[2, 3]]}
                ]
            }
        },
        "arcs": [
            [ [6347, 2300], [ -16, -9], [ -22, 1], [ -5, 3], [ 9, 6], [ 27, 7], [ 7, -8]],
            [ [6447, 2350], [ -4, -4], [ -19, -41], [ -66, -14], [ 4, 9], [ 14, 2]],
            [ [6290, 2347], [ -2, 83], [ -2, 76], [ -2, 75], [ -2, 76], [ -2, 76], [ -2, 75]],
            [ [6329, 4211], [ -3, 6], [ -2, 4], [ 2, 1], [ 28, -1], [ 28, 0]]
        ],
        "transform": {
            "scale": [0.005772872856602365, 0.0024829805705001468],
            "translate": [-124.70997774915153, 24.542349340056283]
        }
    };


    it('should load map into layer', function () {
        $httpBackend.expectGET(fakeMapUrl).respond(fakeMapData);

        var obj = ms.loadMapInto(d3Elem, fakeMapId);
        //$httpBackend.flush();
        // TODO: figure out how to test this function as a black box test.

        expect(obj).toBeTruthy();
        debugger;

        // todo: assert that paths are added to map layer element
    });

/*



    it('should return null when no parameters given', function () {
        promise = ms.fetchGeoMap();
        expect(promise).toBeNull();
    });

    it('should augment the id of a bundled map', function () {
        var id = '*foo';
        promise = ms.fetchGeoMap(id);
        expect(promise.meta).toBeDefined();
        expect(promise.meta.id).toBe(id);
        expect(promise.meta.url).toBe('../data/map/foo.json');
    });

    it('should treat an external id as the url itself', function () {
        var id = 'some/path/to/foo';
        promise = ms.fetchGeoMap(id);
        expect(promise.meta).toBeDefined();
        expect(promise.meta.id).toBe(id);
        expect(promise.meta.url).toBe(id + '.json');
    });

    it('should cache the returned objects', function () {
        var id = 'foo';
        promise = ms.fetchGeoMap(id);
        expect(promise).toBeDefined();
        expect(promise.meta.wasCached).toBeFalsy();
        expect(promise.tagged).toBeUndefined();

        promise.tagged = 'I woz here';

        promise = ms.fetchGeoMap(id);
        expect(promise).toBeDefined();
        expect(promise.meta.wasCached).toBeTruthy();
        expect(promise.tagged).toEqual('I woz here');
    });

    it('should clear the cache when asked', function () {
        var id = 'foo';
        promise = ms.fetchGeoMap(id);
        expect(promise.meta.wasCached).toBeFalsy();

        promise = ms.fetchGeoMap(id);
        expect(promise.meta.wasCached).toBeTruthy();

        ms.clearCache();
        promise = ms.fetchGeoMap(id);
        expect(promise.meta.wasCached).toBeFalsy();
    });


    it('should log a warning if data fails to load', function () {
        $httpBackend.expectGET(mapurl).respond(404, 'Not found');
        spyOn($log, 'warn');

        promise = ms.fetchGeoMap(mapid);
        $httpBackend.flush();
        expect(promise.mapdata).toBeUndefined();
        expect($log.warn)
            .toHaveBeenCalledWith('Failed to retrieve map data: ' + mapurl,
                                    404, 'Not found');

    });
*/
});
