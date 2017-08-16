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
 ONOS GUI -- Topo Controller - Unit Tests
 */
describe('Controller: OvTopoCtrl', function () {

    var $scope, $controller, createController, d3Topo, d3Map;

    beforeEach(module('ovTopo', 'onosRemote', 'onosLayer', 'onosSvg',
        'onosNav', 'ngRoute', 'onosWidget', 'onosMast'));

    beforeEach(inject(function ($rootScope, _$controller_) {
        $scope = $rootScope.$new();
        $controller = _$controller_;

        d3Topo = d3.select('body').append('div').attr('id', 'ov-topo');
        d3Topo.append('svg');

        d3.select('body').append('div').attr('id', 'topo-p-summary');
        d3.select('body').append('div').attr('id', 'topo-p-details');

        createController = function () {
            return $controller('OvTopoCtrl', {
                '$scope': $scope,
            });
        };
    }));

    afterEach(function () {
        d3.select('#ov-topo').remove();
        d3.select('#ov-map').remove();
    });

    it('should create controller', function () {
        var controller = createController();
    });

});
