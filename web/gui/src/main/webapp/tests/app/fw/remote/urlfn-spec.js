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
 ONOS GUI -- Remote -- General Functions - Unit Tests

 @author Bri Prebilic Cole
 @author Simon Hunt
 */
describe('factory: fw/remote/urlfn.js', function () {
    var $log, $loc, ufs, fs;

    beforeEach(module('onosRemote'));

    beforeEach(module(function($provide) {
       $provide.factory('$location', function (){
        return {
            protocol: function () { return 'http'; },
            host: function () { return 'foo'; },
            port: function () { return '80'; }
        };
       })
    }));

    beforeEach(inject(function (_$log_, $location, UrlFnService, FnService) {
        $log = _$log_;
        $loc = $location;
        ufs = UrlFnService;
        fs = FnService;
    }));

    it('should define UrlFnService', function () {
        expect(ufs).toBeDefined();
    });

    it('should define api functions', function () {
        expect(fs.areFunctions(ufs, [
            'urlPrefix'
        ])).toBeTruthy();
    });

    it('should build the url prefix', function () {
       expect(ufs.urlPrefix()).toEqual('http://foo:80');
    });
});
