/*
 * Copyright 2016-present Open Networking Laboratory
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
 ONOS GUI -- Control Plane Manager View Module
 */
(function () {
    'use strict';

    // injected references
    var $log, $scope, $location, ks, fs, cbs, ns;

    var hasDeviceId;

    var labels = new Array(1);
    var data = new Array(6);
    for (var i = 0; i < 6; i++) {
        data[i] = new Array(1);
    }

    var max;

    function ceil(num) {
        if (isNaN(num)) {
            return 0;
        }
        var pre = num.toString().length - 1
        var pow = Math.pow(10, pre);
        return (Math.ceil(num / pow)) * pow;
    }

    function maxInArray(array) {
        var merged = [].concat.apply([], array);
        return Math.max.apply(null, merged);
    }

    angular.module('ovCpman', ["chart.js"])
        .controller('OvCpmanCtrl',
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
                tag: 'cpman',
                query: params
            });

            $scope.$watch('chartData', function () {
                if (!fs.isEmptyObject($scope.chartData)) {
                    $scope.showLoader = false;
                    var length = $scope.chartData.length;
                    labels = new Array(length);
                    for (var i = 0; i < 6; i++) {
                        data[i] = new Array(length);
                    }

                    $scope.chartData.forEach(function (cm, idx) {
                        data[0][idx] = cm.inbound_packet;
                        data[1][idx] = cm.outbound_packet;
                        data[2][idx] = cm.flow_mod_packet;
                        data[3][idx] = cm.flow_removed_packet;
                        data[4][idx] = cm.request_packet;
                        data[5][idx] = cm.reply_packet;

                        labels[idx] = cm.label;
                    });
                }

                max = maxInArray(data)
                $scope.labels = labels;
                $scope.data = data;
                $scope.options = {
                    scaleOverride : true,
                    scaleSteps : 10,
                    scaleStepWidth : ceil(max) / 10,
                    scaleStartValue : 0,
                    scaleFontSize : 16
                };
                $scope.onClick = function (points, evt) {
                    var label = labels[points[0]._index];
                    if (label) {
                        ns.navTo('cpman', { devId: label });
                        $log.log(label);
                    }
                };

                if (!fs.isEmptyObject($scope.annots)) {
                    $scope.deviceIds = JSON.parse($scope.annots.deviceIds);
                }

                $scope.onChange = function (deviceId) {
                    ns.navTo('cpman', { devId: deviceId });
                };
            });

            $scope.series = ['INBOUND', 'OUTBOUND', 'FLOW-MOD',
                             'FLOW-REMOVED', 'REQUEST', 'REPLY'];
            $scope.labels = labels;
            $scope.data = data;

            $scope.chartColors = [
                      '#286090',
                      '#F7464A',
                      '#46BFBD',
                      '#FDB45C',
                      '#97BBCD',
                      '#4D5360',
                      '#8c4f9f'
                    ];
            Chart.defaults.global.colours = $scope.chartColors;

            $scope.showLoader = true;

            $log.log('OvCpmanCtrl has been created');
        }]);

}());
