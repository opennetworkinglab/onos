/*
 * Copyright 2014-present Open Networking Foundation
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
 ONOS GUI -- Main App Controller - Unit Tests
 */
describe('Controller: OnosCtrl', function () {
    // instantiate the main module
    beforeEach(module('onosApp'));

    var $log, ctrl;

    // we need an instance of the controller
    beforeEach(inject(function(_$log_, $controller) {
        $log = _$log_;
        var $scope = {};
        ctrl = $controller('OnosCtrl', { $scope: $scope });
    }));

    it('should report version 1.5.0', function () {
        expect(ctrl.version).toEqual('1.5.0');
    });
});