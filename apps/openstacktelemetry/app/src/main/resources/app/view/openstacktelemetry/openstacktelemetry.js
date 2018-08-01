/*
 * Copyright 2016-present Open Networking Foundation
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
 ONOS GUI -- Openstack Telemetry View Module
 */
(function () {
    'use strict';

    // injected references
    var $log, $scope, $location, ks, fs, cbs, ns;

    var hasFlow;

    var gFlowId;
    var gPeriod;

    var labels = new Array(1);
    var data = new Array(2);
    for (var i = 0; i < 2; i++) {
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

    angular.module('ovOpenstacktelemetry', ["chart.js"])
        .controller('OvOpenstacktelemetryCtrl',
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

            if (params.hasOwnProperty('flowOpt')) {
                $scope.flowOpt = params['flowOpt'];
                hasFlow = true;
            } else if (params.hasOwnProperty('periodOpt')) {
                $scope.periodOpt = params['periodOpt'];
                hasFlow = true;
            } else {
                hasFlow = false;
            }

            cbs.buildChart({
                scope: $scope,
                tag: 'openstacktelemetry',
                query: params
            });

            $scope.$watch('chartData', function () {
                if (!fs.isEmptyObject($scope.chartData)) {
                    $scope.showLoader = false;
                    var length = $scope.chartData.length;
                    labels = new Array(length);
                    for (var i = 0; i < 2; i++) {
                        data[i] = new Array(length);
                    }

                    $scope.chartData.forEach(function (cm, idx) {
                        data[0][idx] = (cm.curr_acc_packet - cm.prev_acc_packet);
                        data[1][idx] = (cm.curr_acc_byte - cm.prev_acc_byte);

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
                        ns.navTo('openstacktelemetry', { flowOpt: label });
                        $log.log(label);
                    }
                };

                if (!fs.isEmptyObject($scope.annots)) {
                    $scope.flowIds = JSON.parse($scope.annots.flowIds);
                    $scope.periodOptions = JSON.parse($scope.annots.periodOptions);
                }

                $scope.onChange = function (flowId) {
                    gFlowId = flowId;
                    ns.navTo('openstacktelemetry', { periodOpt: gPeriod , flowOpt: flowId });
                };

                $scope.onPeriodChange = function (period) {
                    gPeriod = period;
                    ns.navTo('openstacktelemetry', { periodOpt: period , flowOpt: gFlowId });
                };
            });

            $scope.series = ['Current Packet', 'Current Byte'];

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

            $log.log('OvOpenstacktelemetryCtrl has been created');
        }]);

}());
