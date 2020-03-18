/*
 * Copyright 2020-present Open Networking Foundation
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
 ONOS GUI -- Memory View Module
 */
(function () {
    'use strict';

    // injected references
    var $log, $scope, $location, ks, fs, cbs, ns;

    var hasDeviceId;
    var barsNb = 3;

    var labels = new Array(1);
    var data = new Array(barsNb);
    for (var i = 0; i < barsNb; i++) {
        data[i] = new Array(1);
        data[i][0] = 0;
    }

    angular.module('ovMemory', ["chart.js"])
        .controller('OvMemoryCtrl',
        ['$log', '$scope', '$location', 'FnService', 'ChartBuilderService', 'NavService',

        function (_$log_, _$scope_, _$location_, _fs_, _cbs_, _ns_) {
            var params;
            $log = _$log_;
            $scope = _$scope_;
            $location = _$location_;
            fs = _fs_;
            cbs = _cbs_;
            ns = _ns_;

            params = $location.search();

            if (params.hasOwnProperty('devId')) {
                $scope.devId = params['devId'];
                hasDeviceId = true;
            } else {
                hasDeviceId = false;
            }

            cbs.buildChart({
                scope: $scope,
                tag: 'memory',
                query: params
            });

            $scope.$watch('chartData', function () {
                if (!fs.isEmptyObject($scope.chartData)) {
                    $scope.showLoader = false;
                    var length = $scope.chartData.length;
                    labels = new Array(length);
                    for (var i = 0; i < barsNb; i++) {
                        data[i] = new Array(length);
                    }

                    $scope.chartData.forEach(
                        function (cm, idx) {
                            data[0][idx]  = cm.memory_used;
                            data[1][idx]  = cm.memory_free;
                            data[2][idx]  = cm.memory_total;

                            labels[idx] = cm.label;
                        }
                    );
                }

                $scope.labels = labels;
                $scope.data = data;

                $scope.options = {
                    scales: {
                        yAxes: [{
                            type: 'linear',
                            position: 'left',
                            id: 'y-axis-memory',
                            ticks: {
                                beginAtZero: true,
                                fontSize: 28,
                            },
                            scaleLabel: {
                                display: true,
                                labelString: 'Memory Utilization (GBytes)',
                                fontSize: 28,
                            }
                        }],
                        xAxes: [{
                            id: 'x-axis-servers',
                            ticks: {
                                fontSize: 28,
                            },
                            scaleLabel: {
                                display: false,
                                fontSize: 28,
                            }
                        }]
                    }
                };

                $scope.onClick = function (points, evt) {
                    var label = labels[points[0]._index];
                    if (label) {
                        ns.navTo('memory', { devId: label });
                        $log.log(label);
                    }
                };

                if (!fs.isEmptyObject($scope.annots)) {
                    $scope.deviceIds = JSON.parse($scope.annots.deviceIds);
                }

                $scope.onChange = function (deviceId) {
                    ns.navTo('memory', { devId: deviceId });
                };
            });

            $scope.series = new Array(barsNb);
            $scope.series[0] = 'Memory - Used';
            $scope.series[1] = 'Memory - Free';
            $scope.series[2] = 'Memory - Total';

            $scope.labels = labels;
            $scope.data = data;

            $scope.showLoader = true;

            $log.log('OvMemoryCtrl has been created');
        }]);

}());
