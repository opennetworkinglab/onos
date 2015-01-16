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
 ONOS GUI -- Device Controller - Unit Tests

 @author Simon Hunt
 */
describe('Controller: OvDeviceCtrl', function () {
    // instantiate the Device module
    beforeEach(module('ovDevice', 'onosRemote'));

    var $log, $controller, ctrl, $mockHttp;

    var fakeData = {
        "devices": [{
            "id": "of:0000000000000001",
            "available": true,
            "mfr": "Nicira, Inc.",
            "hw": "Open vSwitch",
            "sw": "2.0.1"
        },
        {
            "id": "of:0000000000000004",
            "available": true,
            "mfr": "Nicira, Inc.",
            "hw": "Open vSwitch",
            "sw": "2.0.1"
        }]
    };

    beforeEach(inject(function(_$log_, _$controller_, $httpBackend) {
        $log = _$log_;
        $controller = _$controller_;
        $mockHttp = $httpBackend;

        $mockHttp.whenGET(/\/device$/).respond(fakeData);
    }));

    it('should be an empty array', function () {
        ctrl = $controller('OvDeviceCtrl');
        expect(ctrl.deviceData).toEqual([]);
        $mockHttp.flush();
        expect(ctrl.deviceData).toEqual(fakeData.devices);
    });

});
