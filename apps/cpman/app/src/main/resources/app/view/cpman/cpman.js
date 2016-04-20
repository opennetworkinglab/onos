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
    var $log, $scope, $location, ks, fs, cbs;

    var labels = new Array(60);
    var data = new Array(new Array(60), new Array(60), new Array(60),
                         new Array(60), new Array(60), new Array(60));

    angular.module('ovCpman', ["chart.js"])
        .controller('OvCpmanCtrl',
        ['$log', '$scope', '$location', 'FnService', 'ChartBuilderService',

        function (_$log_, _$scope_, _$location_, _fs_, _cbs_) {
            var params;
            $log = _$log_;
            $scope = _$scope_;
            $location = _$location_;
            fs = _fs_;
            cbs = _cbs_;

            params = $location.search();
            if (params.hasOwnProperty('devId')) {
                $scope.devId = params['devId'];
            }

            cbs.buildChart({
                scope: $scope,
                tag: 'cpman',
                query: params
            });

            var idx = 0;
            var date;
            $scope.$watch('chartData', function () {
                idx = 0;
                if (!fs.isEmptyObject($scope.chartData)) {
                    $scope.chartData.forEach(function (cm) {
                        data[0][idx] = cm.inbound_packet;
                        data[1][idx] = cm.outbound_packet;
                        data[2][idx] = cm.flow_mod_packet;
                        data[3][idx] = cm.flow_removed_packet;
                        data[4][idx] = cm.request_packet;
                        data[5][idx] = cm.reply_packet;
                        date = new Date(cm.label);
                        labels[idx] = date.getHours() + ":" + date.getMinutes();
                        idx++;
                    });
                }
            });

            $scope.series = ['INBOUND', 'OUTBOUND', 'FLOW-MOD',
                             'FLOW-REMOVED', 'STATS-REQUEST', 'STATS-REPLY'];
            $scope.labels = labels;

            $scope.data = data;

            $log.log('OvCpmanCtrl has been created');
        }]);

}());
