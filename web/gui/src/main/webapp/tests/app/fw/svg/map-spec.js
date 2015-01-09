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
    var $log, fs, ms, d3Elem, promise;

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

    it('should load USA into cache', function () {
        var id = '*continental_us';
        promise = ms.fetchGeoMap(id);
        expect(promise).toBeDefined();
        expect(promise.meta.id).toBe(id);
        expect(promise.meta.url).toBe('../data/map/continental_us.json');
        // TODO: WIP -- after a pause, the data should be there!!!

    });
});
