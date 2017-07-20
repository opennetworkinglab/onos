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
 ONOS GUI -- Meter View Module
 */
(function () {
    'use strict';

    // injected references
    var $log, $scope, $location, fs, tbs, ns;

    angular.module('ovMeter', [])
    .controller('OvMeterCtrl',
        ['$log', '$scope', '$location', '$sce',
            'FnService', 'TableBuilderService', 'NavService',

        function (_$log_, _$scope_, _$location_, $sce, _fs_, _tbs_, _ns_) {
            var params;
            $log = _$log_;
            $scope = _$scope_;
            $location = _$location_;
            fs = _fs_;
            tbs = _tbs_;
            ns = _ns_;
            $scope.deviceTip = 'Show device table';
            $scope.flowTip = 'Show flow view for this device';
            $scope.portTip = 'Show port view for this device';
            $scope.groupTip = 'Show group view for this device';

            params = $location.search();
            if (params.hasOwnProperty('devId')) {
                $scope.devId = params['devId'];
            }

            tbs.buildTable({
                scope: $scope,
                tag: 'meter',
                query: params
            });

            $scope.$watch('tableData', function () {
                if (!fs.isEmptyObject($scope.tableData)) {
                    $scope.tableData.forEach(function (meter) {
                        meter.bands = $sce.trustAsHtml(meter.bands);
                    });
                }
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


            $log.log('OvMeterCtrl has been created');
        }]);
}());
