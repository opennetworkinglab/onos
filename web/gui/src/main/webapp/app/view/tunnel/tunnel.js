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
 ONOS GUI -- Host View Module
 */

(function () {
    'use strict';

    angular.module('ovTunnel', [])
    .controller('OvTunnelCtrl',
        ['$log', '$scope', '$sce', 'FnService', 'TableBuilderService',

        function ($log, $scope, $sce, fs, tbs) {
            tbs.buildTable({
                scope: $scope,
                tag: 'tunnel',
            });

            $scope.$watch('tableData', function () {
                if (!fs.isEmptyObject($scope.tableData)) {
                    $scope.tableData.forEach(function (tunnel) {
                        // tunnel.direction = $sce.trustAsHtml(tunnel.direction);
                    });
                }
            });

            $log.log('OvTunnelCtrl has been created');
        }]);
}());
