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
 ONOS GUI -- Topo2 Controller - Unit Tests
 */
describe('Controller: OvTopo2Ctrl', function () {

    var $scope, $controller, createController;

    beforeEach(module('ovTopo2', 'onosRemote', 'onosLayer', 'onosSvg',
        'onosNav', 'ngRoute', 'onosWidget', 'onosMast'));

    beforeEach(inject(function ($rootScope, _$controller_) {
        $scope = $rootScope.$new();
        $controller = _$controller_;

        d3Topo = d3.select('body').append('div').attr('id', 'ov-topo2');
        topoSvg = d3Topo.append('svg').attr('id', 'topo2');
        topoSvg.append('g').attr('id', 'topo2-noDevsLayer')

        createController = function () {
            return $controller('OvTopo2Ctrl', {
                '$scope': $scope,
            });
        };
    }));

    it('should create controller', function () {
        var controller = createController();
    });

});
