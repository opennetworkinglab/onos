/*
 * Copyright 2015 Open Networking Laboratory
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
 ONOS GUI -- Device View Module
 */

(function () {
    'use strict';

    angular.module('ovDevice', [])
    .controller('OvDeviceCtrl',
        ['$log', '$scope', '$location', 'RestService',

        function ($log, $scope, $location, rs) {
            var self = this;
            self.deviceData = [];

            $scope.sortCallback = function (urlSuffix) {
                if (!urlSuffix) {
                    urlSuffix = '';
                }
                var url = 'device' + urlSuffix;
                rs.get(url, function (data) {
                    self.deviceData = data.devices;
                });
            };
            $scope.sortCallback();

            // Cleanup on destroyed scope
            $scope.$on('$destroy', function () {

            });

            $log.log('OvDeviceCtrl has been created');
        }]);
}());
