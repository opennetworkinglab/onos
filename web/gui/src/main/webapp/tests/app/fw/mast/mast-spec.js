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
 ONOS GUI -- Masthead Controller - Unit Tests
 */

describe('Controller: MastCtrl', function () {
    // instantiate the masthead module
    beforeEach(module('onosMast', 'onosUtil', 'onosLayer', 'onosWidget', 'onosSvg', 'onosRemote'));

    var $log, ctrl, ms, fs;

    // we need an instance of the controller
    beforeEach(inject(function(_$log_, $controller, MastService, FnService) {
        $log = _$log_;
        var $scope = {}
        ctrl = $controller('MastCtrl', {$scope: $scope});
        ms = MastService;
        fs = FnService;
    }));

    it('should declare height to be 48', function () {
        expect(ms.mastHeight()).toBe(48);
    })
});
