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
 ONOS GUI -- Device Controller - Unit Tests
 */
describe('Controller: OvDeviceCtrl', function () {
    var $log, $scope, $controller, ctrl, $mockHttp;

    // instantiate the Device module
    beforeEach(module('ovDevice', 'onosRemote', 'onosLayer', 'onosSvg',
                      'onosNav', 'ngRoute'));

    beforeEach(inject(function(_$log_, $rootScope, _$controller_, $httpBackend) {
        $log = _$log_;
        $scope = $rootScope.$new();
        $controller = _$controller_;
        $mockHttp = $httpBackend;
    }));

    beforeEach(function() {
        ctrl = $controller('OvDeviceCtrl', { $scope: $scope });
    });

});
