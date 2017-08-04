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
 ONOS GUI -- Layer -- Veil Service - Unit Tests
 */

describe('factory: fw/layer/veil.js', function () {
    var $log, $route, vs, fs, ks, gs;

    beforeEach(module('onosLayer', 'onosNav', 'onosSvg', 'ngRoute', 'onosRemote'));

    beforeEach(inject(function (_$log_, _$route_, VeilService, FnService,
                                KeyService, GlyphService) {
        $log = _$log_;
        $route = _$route_;
        vs = VeilService;
        fs = FnService;
        ks = KeyService;
        gs = GlyphService;
    }));

    it('should define VeilService', function () {
        expect(vs).toBeDefined();
    });

    it('should define api functions', function () {
        expect(fs.areFunctions(vs, [
            'init', 'show', 'hide', 'lostServer'
        ])).toBeTruthy();
    });
});
