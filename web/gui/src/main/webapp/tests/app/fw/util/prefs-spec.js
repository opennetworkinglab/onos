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
 ONOS GUI -- Util -- User Preference Service - Unit Tests
 */
describe('factory: fw/util/prefs.js', function() {
    var $cookies, ps, fs;

    beforeEach(module('onosUtil', 'onosSvg', 'onosRemote'));

    var mockCookies = {
        foo: 'bar'
    };

    beforeEach(function () {
        module(function ($provide) {
            $provide.value('$cookies', mockCookies);
        });
    });

    beforeEach(inject(function (PrefsService, FnService, _$cookies_) {
        ps = PrefsService;
        fs = FnService;
        $cookies = _$cookies_;
    }));

    it('should define PrefsService', function () {
        expect(ps).toBeDefined();
    });

    it('should define api functions', function () {
        expect(fs.areFunctions(ps, [
            'getPrefs', 'asNumbers', 'setPrefs', 'mergePrefs',
            'addListener', 'removeListener'
        ])).toBe(true);
    });

    // === Tests for getPrefs()
    // TODO unit tests for getPrefs()

    // === Tests for asNumbers()
    // TODO unit tests for asNumbers()

    // === Tests for setPrefs()
    // TODO unit tests for setPrefs()

    // === Tests for mergePrefs()
    // TODO unit tests for mergePrefs()

});
