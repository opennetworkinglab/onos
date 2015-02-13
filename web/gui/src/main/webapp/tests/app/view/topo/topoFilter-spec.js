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
 ONOS GUI -- Topo View -- Topo Filter Service - Unit Tests
 */
describe('factory: view/topo/topoFilter.js', function() {
    var $log, fs, filter;

    beforeEach(module('ovTopo', 'onosUtil', 'onosLayer'));

    beforeEach(inject(function (_$log_, FnService, TopoFilterService) {
        $log = _$log_;
        fs = FnService;
        filter = TopoFilterService;
    }));

    it('should define TopoFilterService', function () {
        expect(filter).toBeDefined();
    });

    it('should define api functions', function () {
        expect(fs.areFunctions(filter, [
            'initFilter', 'destroyFilter'
        ])).toBeTruthy();
    });

    // TODO: more tests...
});
