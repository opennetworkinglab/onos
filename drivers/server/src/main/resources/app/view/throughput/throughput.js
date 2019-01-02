/*
 * Copyright 2018-present Open Networking Foundation
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
 ONOS GUI -- Throughput View Module
 */
(function () {
    'use strict';

    // injected references
    var $log, $scope, $location, ks, fs, cbs, ns;

    var hasDeviceId;
    // TODO: Pass this dynamically
    var coresNb = 16;

    var labels = new Array(1);
    var data = new Array(coresNb);
    for (var i = 0; i < coresNb; i++) {
        data[i] = new Array(1);
        data[i][0] = 0;
    }

    angular.module('ovThroughput', ["chart.js"])
        .controller('OvThroughputCtrl',
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
                tag: 'throughput',
                query: params
            });

            $scope.$watch('chartData', function () {
                if (!fs.isEmptyObject($scope.chartData)) {
                    $scope.showLoader = false;
                    var length = $scope.chartData.length;
                    labels = new Array(length);
                    for (var i = 0; i < coresNb; i++) {
                        data[i] = new Array(length);
                    }

                    $scope.chartData.forEach(
                        function (cm, idx) {
                            // TODO: Squeeze using a working loop?
                            data[0][idx]  = cm.throughput_0;
                            data[1][idx]  = cm.throughput_1;
                            data[2][idx]  = cm.throughput_2;
                            data[3][idx]  = cm.throughput_3;
                            data[4][idx]  = cm.throughput_4;
                            data[5][idx]  = cm.throughput_5;
                            data[6][idx]  = cm.throughput_6;
                            data[7][idx]  = cm.throughput_7;
                            data[8][idx]  = cm.throughput_8;
                            data[9][idx]  = cm.throughput_9;
                            data[10][idx] = cm.throughput_10;
                            data[11][idx] = cm.throughput_11;
                            data[12][idx] = cm.throughput_12;
                            data[13][idx] = cm.throughput_13;
                            data[14][idx] = cm.throughput_14;
                            data[15][idx] = cm.throughput_15;

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
                            id: 'y-axis-throughput',
                            ticks: {
                                min: 0,
                                max: 100,
                                fontSize: 28,
                            },
                            scaleLabel: {
                                display: true,
                                labelString: 'Throughput/CPU Core (Gbps)',
                                fontSize: 28,
                            }
                        }],
                        xAxes: [{
                            id: 'x-axis-servers-cores',
                            ticks: {
                                fontSize: 28,
                            },
                            scaleLabel: {
                                display: true,
                                fontSize: 28,
                            }
                        }]
                    }
                };

                $scope.onClick = function (points, evt) {
                    var label = labels[points[0]._index];
                    if (label) {
                        ns.navTo('throughput', { devId: label });
                        $log.log(label);
                    }
                };

                if (!fs.isEmptyObject($scope.annots)) {
                    $scope.deviceIds = JSON.parse($scope.annots.deviceIds);
                }

                $scope.onChange = function (deviceId) {
                    ns.navTo('throughput', { devId: deviceId });
                };
            });

            $scope.series = new Array(coresNb);
            for (var i = 0; i < coresNb; i++) {
                $scope.series[i] = 'Throughput-CPU ' + i;
            }

            $scope.labels = labels;
            $scope.data = data;

            // TODO: For some reason, this assignment does not work
            $scope.chartColors = [
                '#e6194b',       // Red
                '#3cb44b',       // Green
                '#ffe119',       // Yellow
                '#0082c8',       // Blue
                '#f58231',       // Orange
                '#808080',       // Grey
                '#fffac8',       // Beige
                '#aaffc3',       // Mint
                '#911eb4',       // Purple
                '#46f0f0',       // Cyan
                '#d2f53c',       // Lime
                '#800000',       // Maroon
                '#000000',       // Black
                '#f032e6',       // Magenta
                '#008080',       // Teal
                '#808000',       // Olive
                '#aa6e28'        // Brown
            ];
            Chart.defaults.global.colours = $scope.chartColors;

            $scope.showLoader = true;

            $log.log('OvThroughputCtrl has been created');
        }]);

}());
