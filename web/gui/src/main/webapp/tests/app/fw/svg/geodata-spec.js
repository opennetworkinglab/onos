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
 ONOS GUI -- SVG -- GeoData Service - Unit Tests
 */
describe('factory: fw/svg/geodata.js', function() {
    var $log, $httpBackend, fs, gds, promise;

    beforeEach(module('onosUtil', 'onosSvg'));

    beforeEach(inject(function (_$log_, _$httpBackend_, FnService, GeoDataService) {
        $log = _$log_;
        $httpBackend = _$httpBackend_;
        fs = FnService;
        gds = GeoDataService;
        gds.clearCache();
    }));


    it('should define GeoDataService', function () {
        expect(gds).toBeDefined();
    });

    it('should define api functions', function () {
        expect(fs.areFunctions(gds, [
            'clearCache', 'fetchTopoData', 'createPathGenerator', 'rescaleProjection'
        ])).toBeTruthy();
    });

    it('should return null when no parameters given', function () {
        promise = gds.fetchTopoData();
        expect(promise).toBeNull();
    });

    it('should augment the id of a bundled map', function () {
        var id = '*foo';
        promise = gds.fetchTopoData(id);
        expect(promise.meta).toBeDefined();
        expect(promise.meta.id).toBe(id);
        expect(promise.meta.url).toBe('data/map/foo.topojson');
    });

    it('should treat an external id as the url itself', function () {
        var id = 'some/path/to/foo';
        promise = gds.fetchTopoData(id);
        expect(promise.meta).toBeDefined();
        expect(promise.meta.id).toBe(id);
        expect(promise.meta.url).toBe(id + '.topojson');
    });

    it('should cache the returned objects', function () {
        var id = 'foo';
        promise = gds.fetchTopoData(id);
        expect(promise).toBeDefined();
        expect(promise.meta.wasCached).toBeFalsy();
        expect(promise.tagged).toBeUndefined();

        promise.tagged = 'I woz here';

        promise = gds.fetchTopoData(id);
        expect(promise).toBeDefined();
        expect(promise.meta.wasCached).toBeTruthy();
        expect(promise.tagged).toEqual('I woz here');
    });

    it('should clear the cache when asked', function () {
        var id = 'foo';
        promise = gds.fetchTopoData(id);
        expect(promise.meta.wasCached).toBeFalsy();

        promise = gds.fetchTopoData(id);
        expect(promise.meta.wasCached).toBeTruthy();

        gds.clearCache();
        promise = gds.fetchTopoData(id);
        expect(promise.meta.wasCached).toBeFalsy();
    });


    it('should log a warning if data fails to load', function () {
        var id = 'foo';
        $httpBackend.expectGET('foo.topojson').respond(404, 'Not found');
        spyOn($log, 'warn');

        promise = gds.fetchTopoData(id);
        $httpBackend.flush();
        expect(promise.topodata).toBeUndefined();
        expect($log.warn)
            .toHaveBeenCalledWith('Failed to retrieve map TopoJSON data: foo.topojson',
            404, 'Not found');
    });

    // --- path generator tests

    function simpleTopology(object) {
        return {
            type: "Topology",
            transform: {scale: [1, 1], translate: [0, 0]},
            objects: {states: object},
            arcs: [
                [[0, 0], [1, 0], [0, 1], [-1, 0], [0, -1]],
                [[0, 0], [1, 0], [0, 1]],
                [[1, 1], [-1, 0], [0, -1]],
                [[1, 1]],
                [[0, 0]]
            ]
        };
    }

    function simpleLineStringTopo() {
        return simpleTopology({type: "LineString", arcs: [1, 2]});
    }

    it('should use default settings if none are supplied', function () {
        var gen = gds.createPathGenerator(simpleLineStringTopo(), {adjustScale: true});
        expect(gen.settings.objectTag).toBe('states');
        expect(gen.settings.logicalSize).toBe(1000);
        expect(gen.settings.mapFillScale).toBe(.95);
        // best we can do for now is test that projection is a function ...
        expect(fs.isF(gen.settings.projection)).toBeTruthy();
    });

    it('should allow us to override default settings', function () {
        var gen = gds.createPathGenerator(simpleLineStringTopo(), {
            mapFillScale: .80
        });
        expect(gen.settings.objectTag).toBe('states');
        expect(gen.settings.logicalSize).toBe(1000);
        expect(gen.settings.mapFillScale).toBe(.80);
    });

    it('should create transformed geodata, and a path generator', function () {
        var gen = gds.createPathGenerator(simpleLineStringTopo(), {adjustScale: true});
        expect(fs.isO(gen.settings)).toBeTruthy();
        expect(fs.isO(gen.geodata)).toBeTruthy();
        expect(fs.isF(gen.pathgen)).toBeTruthy();
    });
    // NOTE: we probably should have more unit tests that assert stuff about
    //       the transformed data (geo data) -- though perhaps we can rely on
    //       the unit testing of TopoJSON? See...
    //  https://github.com/mbostock/topojson/blob/master/test/feature-test.js
    //       and, what about the path generator?, and the computed bounds?
    //  In summary, more work should be done here..

});
