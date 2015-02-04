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
    var currCol = {},
        prevCol = {};


    angular.module('ovDevice', [])
        .controller('OvDeviceCtrl', ['$log', '$location', 'RestService',
        function ($log, $location, rs) {
            var self = this;
            self.deviceData = [];

            // TODO: remove test code
            var testCase = $location.search().test;
            var url = testCase ? 'test/' + testCase : 'device';

            rs.get(url, function (data) {
                self.deviceData = data.devices;
            });

            d3.selectAll('th').on('click', function(d) {
                var thElem = d3.select(this);
                currCol.colId = thElem.attr('colId');

                if(currCol.colId === prevCol.colId) {
                    (currCol.icon === 'tableColSortDesc') ?
                        currCol.icon = 'tableColSortAsc' :
                        currCol.icon = 'tableColSortDesc';
                    prevCol.icon = currCol.icon;
                } else {
                    currCol.icon = 'tableColSortAsc';
                    prevCol.icon = 'tableColSortNone';
                }

                $log.debug('currCol clicked: ' + currCol.colId +
                ', with sorting icon: ' + currCol.icon);
                $log.debug('prevCol clicked: ' + prevCol.colId +
                ', with its current sorting icon as ' + prevCol.icon);

                // TODO: send the prev and currCol info to the server to use in sorting table here

                prevCol.colId = currCol.colId;

            });

            $log.log('OvDeviceCtrl has been created');
        }]);
}());
