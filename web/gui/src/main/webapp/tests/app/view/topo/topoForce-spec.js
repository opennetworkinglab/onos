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
 ONOS GUI -- Topo View -- Topo Force Service - Unit Tests
 */
describe('factory: view/topo/topoForce.js', function() {
    var $log, fs, tfs;

    beforeEach(module('ovTopo', 'onosUtil', 'onosLayer'));

    beforeEach(inject(function (_$log_, FnService, TopoForceService) {
        $log = _$log_;
        fs = FnService;
        tfs = TopoForceService;
    }));

    it('should define TopoForceService', function () {
        expect(tfs).toBeDefined();
    });

    it('should define api functions', function () {
        expect(fs.areFunctions(tfs, [
            'initForce', 'resize', 'updateDeviceColors',
            'addDevice', 'updateDevice', 'removeDevice',
            'addHost', 'updateHost', 'removeHost',
            'addLink', 'updateLink', 'removeLink'
        ])).toBeTruthy();
    });

    // TODO: more tests...
});
