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
 ONOS GUI -- Topo View -- Topo Traffic Service - Unit Tests
 */
describe('factory: view/topo/topoTraffic.js', function() {
    var $log, fs, tts;

    beforeEach(module('ovTopo', 'onosUtil', 'onosLayer', 'onosNav', 'ngRoute', 'onosApp'));

    beforeEach(inject(function (_$log_, FnService, TopoTrafficService) {
        $log = _$log_;
        fs = FnService;
        tts = TopoTrafficService;
    }));

    it('should define TopoTrafficService', function () {
        expect(tts).toBeDefined();
    });

    it('should define api functions', function () {

        expect(fs.areFunctions(tts, [
            'initTraffic',
            'destroyTraffic',
            'cancelTraffic',
            'showAllTraffic',
            'showDeviceLinkFlows',
            'showRelatedIntents',
            'showPrevIntent',
            'showNextIntent',
            'showSelectedIntentTraffic',
            'selectIntent',
            'requestTrafficForMode',
            'addHostIntent',
            'addMultiSourceIntent',
            'removeIntent',
            'resubmitIntent',
            'removeIntents',
        ])).toBeTruthy();
    });

    // TODO: more tests...
});
