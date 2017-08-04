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
 ONOS GUI -- DHCP Server View Module
 */

(function () {
    'use strict';

    // injected refs
    var $log, $scope;

    angular.module('ovDhcp', [])
        .controller('OvDhcpCtrl',
        ['$log', '$scope', 'TableBuilderService',

            function (_$log_, _$scope_, tbs) {
                $log = _$log_;
                $scope = _$scope_;

                function selCb($event, row) {
                    $log.debug('Got a click on:', row);
                }

                tbs.buildTable({
                    scope: $scope,
                    tag: 'dhcp',
                    selCb: selCb
                });

                $scope.$on('$destroy', function () {
                    $log.debug('OvDhcpCtrl has been destroyed');
                });

                $log.log('OvDhcpCtrl has been created');
            }]);
}());