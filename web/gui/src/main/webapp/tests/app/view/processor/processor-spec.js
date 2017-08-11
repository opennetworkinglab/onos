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
 ONOS GUI -- Processor Controller - Unit Tests
 */
describe('Controller: OvProcessorCtrl', function () {

    var $scope, $controller, createController;

    beforeEach(module('ovProcessor', 'onosRemote', 'onosLayer', 'onosSvg',
        'onosNav', 'ngRoute', 'onosWidget', 'onosMast'));

    beforeEach(inject(function ($rootScope, _$controller_) {
        $scope = $rootScope.$new();
        $controller = _$controller_;

        createController = function () {
            return $controller('OvProcessorCtrl', {
                '$scope': $scope,
            });
        };
    }));

    it('should create controller', function () {
        var controller = createController();
    });

});
