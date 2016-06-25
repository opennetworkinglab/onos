/*
 * Copyright 2015-present Open Networking Laboratory
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
 ONOS GUI -- Widget -- Table Service
 */
(function () {
    'use strict';

    // injected refs
    var $log, $interval, $timeout, fs, wss, ls;

    // constants
    var refreshInterval = 2000;

    // example params to buildTable:
    // {
    //    scope: $scope,     <- controller scope
    //    tag: 'device',     <- table identifier
    //    selCb: selCb,      <- row selection callback (optional)
    //    respCb: respCb,    <- websocket response callback (optional)
    //    query: params      <- query parameters in URL (optional)
    // }
    //          Note: selCb() is passed the row data model of the selected row,
    //                 or null when no row is selected.
    //          Note: query is always an object (empty or containing properties)
    //                 it comes from $location.search()

    function buildTable(o) {
        var handlers = {},
            root = o.tag + 's',
            req = o.tag + 'DataRequest',
            resp = o.tag + 'DataResponse',
            onSel = fs.isF(o.selCb),
            onResp = fs.isF(o.respCb),
            idKey = o.idKey || 'id',
            oldTableData = [],
            refreshPromise;

        o.scope.tableData = [];
        o.scope.changedData = [];
        o.scope.sortParams = o.sortParams || {};
        o.scope.autoRefresh = true;
        o.scope.autoRefreshTip = 'Toggle auto refresh';

        // === websocket functions --------------------
        // response
        function respCb(data) {
            ls.stop();
            o.scope.tableData = data[root];
            o.scope.annots = data.annots;
            onResp && onResp();

            // checks if data changed for row flashing
            if (!angular.equals(o.scope.tableData, oldTableData)) {
                o.scope.changedData = [];
                // only flash the row if the data already exists
                if (oldTableData.length) {
                    angular.forEach(o.scope.tableData, function (item) {
                        if (!fs.containsObj(oldTableData, item)) {
                            o.scope.changedData.push(item);
                        }
                    });
                }
                angular.copy(o.scope.tableData, oldTableData);
            }
            o.scope.$apply();
        }
        handlers[resp] = respCb;
        wss.bindHandlers(handlers);

        // request
        function sortCb(params) {
            var p = angular.extend({}, params, o.query);
            if (wss.isConnected()) {
                wss.sendEvent(req, p);
                ls.start();
            }
        }
        o.scope.sortCallback = sortCb;


        // === selecting a row functions ----------------
        function selCb($event, selRow) {
            var selId = selRow[idKey];
            o.scope.selId = (o.scope.selId === selId) ? null : selId;
            onSel && onSel($event, selRow);
        }
        o.scope.selectCallback = selCb;

        // === autoRefresh functions ------------------
        function fetchDataIfNotWaiting() {
            if (!ls.waiting()) {
                if (fs.debugOn('widget')) {
                    $log.debug('Refreshing ' + root + ' page');
                }
                sortCb(o.scope.sortParams);
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

        // === Cleanup on destroyed scope -----------------
        o.scope.$on('$destroy', function () {
            wss.unbindHandlers(handlers);
            stopRefresh();
            ls.stop();
        });

        sortCb(o.scope.sortParams);
        startRefresh();
    }

    angular.module('onosWidget')
        .factory('TableBuilderService',
        ['$log', '$interval', '$timeout', 'FnService', 'WebSocketService',
            'LoadingService',

            function (_$log_, _$interval_, _$timeout_, _fs_, _wss_, _ls_) {
                $log = _$log_;
                $interval = _$interval_;
                $timeout = _$timeout_;
                fs = _fs_;
                wss = _wss_;
                ls = _ls_;

                return {
                    buildTable: buildTable
                };
            }]);

}());
