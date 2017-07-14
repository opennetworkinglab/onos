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
 ONOS GUI -- Widget -- Chart Service
 */
(function () {
    'use strict';

    // injected references
    // fs -> FnService
    // wss -> WebSocketService
    // ls -> LoadingService
    var $log, $interval, fs, wss, ls;

    // constants
    var refreshInterval = 2000;

    // example params to buildChart:
    // {
    //    scope: $scope,     <- controller scope
    //    tag: 'device',     <- chart identifier
    //    respCb: respCb,    <- websocket response callback (optional)
    //    query: params      <- query parameters in URL (optional)
    // }
    //          Note: query is always an object (empty or containing properties)
    //                 it comes from $location.search()
    function buildChart(o) {
        var handlers = {},
            root = o.tag + 's',
            req = o.tag + 'DataRequest',
            resp = o.tag + 'DataResponse',
            onResp = fs.isF(o.respCb),
            oldChartData = [],
            refreshPromise;

        o.scope.chartData = [];
        o.scope.changedData = [];
        o.scope.reqParams = o.reqParams || {};
        o.scope.autoRefresh = true;
        o.scope.autoRefreshTip = o.lion_toggle_auto_refresh || 'Toggle auto refresh';

        // === websocket functions ===
        // response
        function respCb(data) {
            ls.stop();
            o.scope.chartData = data[root];
            o.scope.annots = data.annots;
            onResp && onResp();

            // check if data changed
            if (!angular.equals(o.scope.chartData, oldChartData)) {
                o.scope.changedData = [];
                // only refresh the chart if there are new changes
                if (oldChartData.length) {
                    angular.forEach(o.scope.chartData, function (item) {
                        if (!fs.containsObj(oldChartData, item)) {
                            o.scope.changedData.push(item);
                        }
                    });
                }
                angular.copy(o.scope.chartData, oldChartData);
            }
            o.scope.$apply();
        }
        handlers[resp] = respCb;
        wss.bindHandlers(handlers);

        // request
        function requestCb(params) {
            var p = angular.extend({}, params, o.query);
            if (wss.isConnected()) {
                wss.sendEvent(req, p);
                ls.start();
            }
        }
        o.scope.requestCallback = requestCb;

        // === autoRefresh functions ===
        function fetchDataIfNotWaiting() {
            if (!ls.waiting()) {
                if (fs.debugOn('widget')) {
                    $log.debug('Refreshing ' + root + ' page');
                }
                requestCb(o.scope.reqParams);
            }
        }

        function startRefresh() {
            refreshPromise = $interval(fetchDataIfNotWaiting, refreshInterval);
        }

        function stopRefresh() {
            if (refreshPromise) {
                $interval.cancel(refreshPromise);
                refreshPromise = null;
            }
        }

        function toggleRefresh() {
            o.scope.autoRefresh = !o.scope.autoRefresh;
            o.scope.autoRefresh ? startRefresh() : stopRefresh();
        }
        o.scope.toggleRefresh = toggleRefresh;

        // === Cleanup on destroyed scope ===
        o.scope.$on('$destroy', function () {
            wss.unbindHandlers(handlers);
            stopRefresh();
            ls.stop();
        });

        requestCb(o.scope.reqParams);
        startRefresh();
    }

    angular.module('onosWidget')
        .factory('ChartBuilderService',
        ['$log', '$interval', 'FnService', 'WebSocketService',
            'LoadingService',

            function (_$log_, _$interval_, _fs_, _wss_, _ls_) {
                $log = _$log_;
                $interval = _$interval_;
                fs = _fs_;
                wss = _wss_;
                ls = _ls_;

                return {
                    buildChart: buildChart,
                };
            }]);
}());
