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
 ONOS GUI -- Port View Module
 */

(function () {
    'use strict';

    // injected references
    var $log, $scope, $location, fs, tbs, ns;

    angular.module('ovPort', [])
    .controller('OvPortCtrl',
        ['$log', '$scope', '$location',
            'FnService', 'TableBuilderService', 'NavService',

        function (_$log_, _$scope_, _$location_, _fs_, _tbs_, _ns_) {
            var params;
            $log = _$log_;
            $scope = _$scope_;
            $location = _$location_;
            fs = _fs_;
            tbs = _tbs_;
            ns = _ns_;
            $scope.deviceTip = 'Show device table';
            $scope.flowTip = 'Show flow view for this device';
            $scope.groupTip = 'Show group view for this device';
            $scope.meterTip = 'Show meter view for selected device';

            params = $location.search();
            if (params.hasOwnProperty('devId')) {
                $scope.devId = params['devId'];
            }

            tbs.buildTable({
                scope: $scope,
                tag: 'port',
                query: params
            });

            $scope.nav = function (path) {
                if ($scope.devId) {
                    ns.navTo(path, { devId: $scope.devId });
                }
            };

             Object.defineProperty($scope, "queryFilter", {
                 get: function() {
                     var out = {};
                     out[$scope.queryBy || "$"] = $scope.query;
                     return out;
                 }
             });

            $log.log('OvPortCtrl has been created');
        }]);
}());
