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
 ONOS GUI -- Topo View -- Topo Instance Service - Unit Tests
 */
describe('factory: view/topo/topoInst.js', function() {
    var $log, fs, tis;

    beforeEach(module('ovTopo', 'onosUtil', 'onosLayer', 'onosNav', 'onosWidget', 'onosMast'));

    beforeEach(inject(function (_$log_, FnService, TopoInstService) {
        $log = _$log_;
        fs = FnService;
        tis = TopoInstService;
    }));

    it('should define TopoInstService', function () {
        expect(tis).toBeDefined();
    });

    it('should define api functions', function () {
        expect(fs.areFunctions(tis, [
            'initInst', 'destroyInst',
            'addInstance', 'updateInstance', 'removeInstance',
            'cancelAffinity',
            'isVisible', 'show', 'hide', 'toggle', 'showMaster'
        ])).toBeTruthy();
    });

    // TODO: more tests...
});
