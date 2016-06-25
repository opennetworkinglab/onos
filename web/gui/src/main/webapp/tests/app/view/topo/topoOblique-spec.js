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
 ONOS GUI -- Topo View -- Topo Oblique View Service - Unit Tests
 */
describe('factory: view/topo/topoOblique.js', function() {
    var $log, fs, tos, flash;

    beforeEach(module('ovTopo', 'onosUtil', 'onosLayer', 'onosNav', 'onosWidget', 'onosMast'));

    beforeEach(inject(function (_$log_, FnService,
                                TopoObliqueService, FlashService) {
        $log = _$log_;
        fs = FnService;
        tos = TopoObliqueService;
        flash = FlashService;
    }));

    it('should define TopoTrafficService', function () {
        expect(tos).toBeDefined();
    });

    it('should define api functions', function () {
        expect(fs.areFunctions(tos, [
            'initOblique', 'destroyOblique', 'isOblique', 'toggleOblique'
        ])).toBeTruthy();
    });

    // TODO: more tests...
});

