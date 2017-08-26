/*
 * Copyright 2017-present Open Networking Foundation
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
 ONOS GUI -- Flow Controller - Unit Tests
 */
describe('Controller: OvFlowCtrl', function () {

    var $scope, $controller, createController;

    beforeEach(module('ovFlow', 'onosRemote', 'onosLayer', 'onosSvg',
        'onosNav', 'ngRoute', 'onosWidget', 'onosMast'));

    beforeEach(inject(function ($rootScope, _$controller_) {
        $scope = $rootScope.$new();
        $controller = _$controller_;

        createController = function () {
            return $controller('OvFlowCtrl', {
                '$scope': $scope,
            });
        };
    }));

    it('should create controller', function () {
        var controller = createController();
    });

    // Unit test for defect ONOS-6332
    // See lines 164-167 addLabVal(...) of flow.js which handles display of
    //   properties in the details panel
    //  (this is if we keep the device ID in the extension rendered string)
    it('should break into two pieces, using first colon only', function () {
        var lv = 'EXTENSION:of:0000000000000205/Ofdpa3SetMplsType{mplsType=32}',
            b1 = 'EXTENSION',
            b2 = 'of:0000000000000205/Ofdpa3SetMplsType{mplsType=32}';

        var bits = lv.match(/^([^:]*):(.*)/);

        // console.log(bits);

        expect(bits.length).toBe(3);
        expect(bits[0]).toBe(lv);
        expect(bits[1]).toBe(b1);
        expect(bits[2]).toBe(b2);
    });

    // Unit test for defect ONOS-6332
    // See lines 164-167 addLabVal(...) of flow.js which handles display of
    //   properties in the details panel
    //  (this is if we DON'T keep the device ID in the extension rendered string)
    it('should break into two pieces, using first colon only (2)', function () {
        var lv = 'EXTENSION:Ofdpa3SetMplsType{mplsType=32}',
            b1 = 'EXTENSION',
            b2 = 'Ofdpa3SetMplsType{mplsType=32}';

        var bits = lv.match(/^([^:]*):(.*)/);

        // console.log(bits);

        expect(bits.length).toBe(3);
        expect(bits[0]).toBe(lv);
        expect(bits[1]).toBe(b1);
        expect(bits[2]).toBe(b2);
    });
});
