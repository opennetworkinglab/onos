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
 ONOS GUI -- Remote -- REST Service - Unit Tests
 */
describe('factory: fw/remote/rest.js', function() {
    var $log, $httpBackend, fs, rs;

    beforeEach(module('onosUtil', 'onosRemote'));

    beforeEach(module(function($provide) {
        $provide.factory('$location', function () {
            return {
                protocol: function () { return 'http'; },
                host: function () { return 'foo'; },
                port: function () { return '80'; },
                search: function() {
                    return {debug: 'true'};
                },
                absUrl: function () {
                    return 'http://foo:123/onos/ui/rs/path';
                }
            };
        });
    }));

    beforeEach(inject(function (_$log_, _$httpBackend_, FnService, RestService) {
        $log = _$log_;
        $httpBackend = _$httpBackend_;
        fs = FnService;
        rs = RestService;
    }));

    it('should define RestService', function () {
        expect(rs).toBeDefined();
    });

    it('should define api functions', function () {
        expect(fs.areFunctions(rs, [
            'get',
            'post'
        ])).toBeTruthy();
    });

    var mockData = {
        id: 1,
        prop: 'abc'
    };

    it('should fetch remote data', function () {
        var called = 0,
            capture = null;
        $httpBackend.whenGET(/.*/).respond(mockData);
        spyOn($log, 'warn');

        rs.get('bar', function (data) {
            called++;
            capture = data;
        });

        expect(called).toEqual(0);
        expect(capture).toBeNull();
        $httpBackend.flush();
        expect(called).toEqual(1);
        expect(capture).toEqual(mockData);
        expect($log.warn).not.toHaveBeenCalled();
    });

    it('should fail to fetch remote data', function () {
        var called = 0,
            capture = null;
        $httpBackend.whenGET(/.*/).respond(404, 'Not Found');
        spyOn($log, 'warn');

        rs.get('bar', function (data) {
            called++;
            capture = data;
        });

        expect(called).toEqual(0);
        expect(capture).toBeNull();
        $httpBackend.flush();
        expect(called).toEqual(0);
        expect(capture).toBeNull();
        expect($log.warn).toHaveBeenCalledWith(
            'Failed to retrieve JSON data: http://foo:80/onos/ui/rs/bar',
            404, 'Not Found');
    });

});
