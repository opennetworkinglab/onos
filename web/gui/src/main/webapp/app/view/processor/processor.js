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
 ONOS GUI -- Packet Processor View Module
 */

(function () {
    'use strict';

    // injected references
    var $log, $scope, $location, tbs, ns;

    angular.module('ovProcessor', [])
    .controller('OvProcessorCtrl',
        ['$log', '$scope', '$location', 'TableBuilderService', 'NavService',

        function (_$log_, _$scope_, _$location_, _tbs_, _ns_) {
            var params;
            $log = _$log_;
            $scope = _$scope_;
            $location = _$location_;
            tbs = _tbs_;
            ns = _ns_;
            $scope.requestTip = 'Show packet requests';

            params = $location.search();

            tbs.buildTable({
                scope: $scope,
                tag: 'processor',
                query: params,
            });

            $scope.nav = function (path) {
                if ($scope.devId) {
                    ns.navTo(path);
                }
            };

            $log.log('OvProcessorCtrl has been created');
        }]);
}());
