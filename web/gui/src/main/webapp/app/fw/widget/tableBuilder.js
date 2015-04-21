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
 ONOS GUI -- Widget -- Table Service
 */
(function () {
    'use strict';

    // injected refs
    var $log, fs, wss;

    // example params to buildTable:
    // {
    //    self: this,               <- controller object
    //    scope: $scope,            <- controller scope
    //    tag: 'device',            <- table identifier
    //    selCb: selCb              <- row selection callback (optional)
    // }
    //          Note: selCb() is passed the row data model of the selected row,
    //                 or null when no row is selected.

    function buildTable(o) {
        var handlers = {},
            root = o.tag + 's',
            req = o.tag + 'DataRequest',
            resp = o.tag + 'DataResponse',
            onSel = fs.isF(o.selCb);

        o.self.tableData = [];

        function respCb(data) {
            o.self.tableData = data[root];
            o.scope.$apply();
        }

        function sortCb(params) {
            wss.sendEvent(req, params);
        }
        o.scope.sortCallback = sortCb;

        function selCb($event, sel) {
            o.scope.sel = (o.scope.sel === sel) ? null : sel;
            onSel && onSel($event, o.scope.sel);
        }
        o.scope.selectCallback = selCb;


        handlers[resp] = respCb;
        wss.bindHandlers(handlers);

        // Cleanup on destroyed scope
        o.scope.$on('$destroy', function () {
            wss.unbindHandlers(handlers);
        });

        sortCb();
    }

    angular.module('onosWidget')
        .factory('TableBuilderService',
        ['$log', 'FnService', 'WebSocketService',

            function (_$log_, _fs_, _wss_) {
                $log = _$log_;
                fs = _fs_;
                wss = _wss_;

                return {
                    buildTable: buildTable
                };
            }]);

}());
