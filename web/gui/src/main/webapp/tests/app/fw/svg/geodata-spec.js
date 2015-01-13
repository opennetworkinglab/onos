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
 ONOS GUI -- SVG -- GeoData Service - Unit Tests

 @author Simon Hunt
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
            'clearCache', 'fetchGeoMap'
        ])).toBeTruthy();
    });

    it('should return null when no parameters given', function () {
        promise = gds.fetchGeoMap();
        expect(promise).toBeNull();
    });

    it('should augment the id of a bundled map', function () {
        var id = '*foo';
        promise = gds.fetchGeoMap(id);
        expect(promise.meta).toBeDefined();
        expect(promise.meta.id).toBe(id);
        expect(promise.meta.url).toBe('../data/map/foo.json');
    });

    it('should treat an external id as the url itself', function () {
        var id = 'some/path/to/foo';
        promise = gds.fetchGeoMap(id);
        expect(promise.meta).toBeDefined();
        expect(promise.meta.id).toBe(id);
        expect(promise.meta.url).toBe(id + '.json');
    });

    it('should cache the returned objects', function () {
        var id = 'foo';
        promise = gds.fetchGeoMap(id);
        expect(promise).toBeDefined();
        expect(promise.meta.wasCached).toBeFalsy();
        expect(promise.tagged).toBeUndefined();

        promise.tagged = 'I woz here';

        promise = gds.fetchGeoMap(id);
        expect(promise).toBeDefined();
        expect(promise.meta.wasCached).toBeTruthy();
        expect(promise.tagged).toEqual('I woz here');
    });

    it('should clear the cache when asked', function () {
        var id = 'foo';
        promise = gds.fetchGeoMap(id);
        expect(promise.meta.wasCached).toBeFalsy();

        promise = gds.fetchGeoMap(id);
        expect(promise.meta.wasCached).toBeTruthy();

        gds.clearCache();
        promise = gds.fetchGeoMap(id);
        expect(promise.meta.wasCached).toBeFalsy();
    });


    it('should log a warning if data fails to load', function () {
        var id = 'foo';
        $httpBackend.expectGET('foo.json').respond(404, 'Not found');
        spyOn($log, 'warn');

        promise = gds.fetchGeoMap(id);
        $httpBackend.flush();
        expect(promise.mapdata).toBeUndefined();
        expect($log.warn)
            .toHaveBeenCalledWith('Failed to retrieve map data: foo.json',
            404, 'Not found');
    });

});
