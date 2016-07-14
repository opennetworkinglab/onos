/*
 * Copyright 2015-present Open Networking Laboratory
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
            'loadMapRegionInto',
            'loadMapInto',
            'reshade'
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

        // todo: assert that paths are added to map layer element
    });

});
