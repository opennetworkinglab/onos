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
 ONOS GUI -- Group View Module
 */

(function () {
    'use strict';

    // injected references
    var $log, $scope, $location, fs, tbs, ns;

    angular.module('ovGroup', [])
    .controller('OvGroupCtrl',
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
            $scope.meterTip = 'Show meter view for selected device';
            $scope.briefTip = 'Switch to brief view';
            $scope.detailTip = 'Switch to detailed view';
            $scope.brief = true;
            params = $location.search();
            if (params.hasOwnProperty('devId')) {
                $scope.devId = params['devId'];
            }

            tbs.buildTable({
                scope: $scope,
                tag: 'group',
                query: params
            });

            $scope.$watch('tableData', function () {
                if (!fs.isEmptyObject($scope.tableData)) {
                    $scope.tableData.forEach(function (group) {
                        group.buckets = $sce.trustAsHtml(group.buckets);
                    });
                }
            });

            $scope.nav = function (path) {
                if ($scope.devId) {
                    ns.navTo(path, { devId: $scope.devId });
                }
            };
            $scope.briefToggle = function () {
                $scope.brief = !$scope.brief;
            };

            $log.log('OvGroupCtrl has been created');
        }]);
}());
