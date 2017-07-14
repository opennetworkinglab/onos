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
 ONOS GUI -- Widget -- Table Builder Service
 */
(function () {
    'use strict';

    // injected refs
    var $log, $interval, fs, wss, ls;

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

    // Additional Notes:
    //   When sending a request for table data, the $scope will be checked
    //   for a .payloadParams object which, if it exists, will be merged into
    //   the event payload. By modifying this object (via toggle buttons, or
    //   other user interaction) additional parameters / state can be passed
    //   to the server in the data request.

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
        o.scope.autoRefreshTip = o.lion_toggle_auto_refresh || 'Toggle auto refresh';

        // === websocket functions --------------------

        // === Table Data Response
        function tableDataResponseCb(data) {
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
        handlers[resp] = tableDataResponseCb;
        wss.bindHandlers(handlers);

        // === Table Data Request
        function requestTableData() {
            var sortParams = o.scope.sortParams,
                pp = fs.isO(o.scope.payloadParams),
                payloadParams = pp || {},
                p = angular.extend({}, sortParams, payloadParams, o.query);

            if (wss.isConnected()) {
                if (fs.debugOn('table')) {
                    $log.debug('Table data REQUEST:', req, p);
                }
                wss.sendEvent(req, p);
                ls.start();
            }
        }
        o.scope.sortCallback = requestTableData;


        // === Row Selected
        function rowSelectionCb($event, selRow) {
            var selId = selRow[idKey];
            o.scope.selId = (o.scope.selId === selId) ? null : selId;
            onSel && onSel($event, selRow);
        }
        o.scope.selectCallback = rowSelectionCb;

        // === autoRefresh functions
        function fetchDataIfNotWaiting() {
            if (!ls.waiting()) {
                if (fs.debugOn('widget')) {
                    $log.debug('Refreshing ' + root + ' page');
                }
                requestTableData();
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

        // === Cleanup on destroyed scope
        o.scope.$on('$destroy', function () {
            wss.unbindHandlers(handlers);
            stopRefresh();
            ls.stop();
        });

        requestTableData();
        startRefresh();

        return {
            forceRefesh: requestTableData,
        };
    }

    angular.module('onosWidget')
        .factory('TableBuilderService',
        ['$log', '$interval', 'FnService', 'WebSocketService',
            'LoadingService',

            function (_$log_, _$interval_, _fs_, _wss_, _ls_) {
                $log = _$log_;
                $interval = _$interval_;
                fs = _fs_;
                wss = _wss_;
                ls = _ls_;

                return {
                    buildTable: buildTable,
                };
            }]);

}());
