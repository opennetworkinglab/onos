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
    var $log, fs, ms, d3Elem, geomap;

    var urlPrefix = 'data/map/';

    beforeEach(module('onosUtil', 'onosSvg'));

    beforeEach(inject(function (_$log_, FnService, MapService) {
        $log = _$log_;
        fs = FnService;
        ms = MapService;
        ms.clearCache();
        // TODO: d3Elem = d3.select('body').append('...').attr('id', 'myFoo');
    }));

    afterEach(function () {
        // TODO d3.select('#myFoo').remove();
    });

    it('should define MapService', function () {
        expect(ms).toBeDefined();
    });

    it('should define api functions', function () {
        expect(fs.areFunctions(ms, [
            'clearCache', 'fetchGeoMap'
        ])).toBeTruthy();
    });

    it('should return null when no parameters given', function () {
        geomap = ms.fetchGeoMap();
        expect(geomap).toBeNull();
    });

    it('should augment the id of a bundled map', function () {
        var id = '*foo';
        geomap = ms.fetchGeoMap(id);
        expect(geomap).toBeDefined();
        expect(geomap.id).toBe(id);
        expect(geomap.url).toBe('data/map/foo.json');
    });

    it('should treat an external id as the url itself', function () {
        var id = 'some/path/to/foo';
        geomap = ms.fetchGeoMap(id);
        expect(geomap).toBeDefined();
        expect(geomap.id).toBe(id);
        expect(geomap.url).toBe(id + '.json');
    });

    it('should cache the returned objects', function () {
        var id = 'foo';
        geomap = ms.fetchGeoMap(id);
        expect(geomap).toBeDefined();
        expect(geomap.wasCached).toBeFalsy();
        expect(geomap.tagged).toBeUndefined();

        geomap.tagged = 'I woz here';

        geomap = ms.fetchGeoMap(id);
        expect(geomap).toBeDefined();
        expect(geomap.wasCached).toBeTruthy();
        expect(geomap.tagged).toEqual('I woz here');
    });

    it('should clear the cache when asked', function () {
        var id = 'foo';
        geomap = ms.fetchGeoMap(id);
        expect(geomap.wasCached).toBeFalsy();

        geomap = ms.fetchGeoMap(id);
        expect(geomap.wasCached).toBeTruthy();

        ms.clearCache();
        geomap = ms.fetchGeoMap(id);
        expect(geomap.wasCached).toBeFalsy();
    });
});
