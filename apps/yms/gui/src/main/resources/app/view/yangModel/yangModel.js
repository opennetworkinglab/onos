/*
 * Copyright 2017-present Open Networking Laboratory
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
  ONOS GUI -- YANG Model table view
 */

(function () {
    'use strict';

    // injected refs
    var $log, $scope, fs, ps, wss;

    // internal state
    var detailsPanel,
        ymodel;

    // constants
    var pName = 'yang-model-details-panel',
        detailsReq = 'yangModelDetailsRequest',
        detailsResp = 'yangModelDetailsResponse';

    // callback invoked when data from a details request returns from server
    function respDetailsCb(data) {
        $scope.panelData = data.details;
        ymodel = data.yangModel;
        $scope.$apply();
        // TODO: complete the detail panel directive.
        $log.debug('YANG_MODEL>', detailsResp, data);
    }

    angular.module('ovYangModel', [])
        .controller('OvYangModelCtrl', [
            '$log', '$scope', 'TableBuilderService', 'TableDetailService',
            'FnService', 'PanelService', 'WebSocketService',

            function (_$log_, _$scope_, tbs, tds, _fs_, _ps_, _wss_) {
                var handlers = {};

                $log = _$log_;
                $scope = _$scope_;
                fs = _fs_;
                ps = _ps_;
                wss = _wss_;

                $scope.panelData = {};

                // register response handler
                handlers[detailsResp] = respDetailsCb;
                wss.bindHandlers(handlers);

                // row selection callback
                function selCb($event, row) {
                    if ($scope.selId) {
                        wss.sendEvent(detailsReq, { id: row.id });
                    } else {
                        $scope.hidePanel();
                    }
                    $log.debug('Got a click on:', row);
                }

                tbs.buildTable({
                    scope: $scope,
                    tag: 'yangModel',
                    selCb: selCb
                });

                $scope.$on('$destroy', function () {
                    wss.unbindHandlers(handlers);
                });

                $log.log('OvYangModelCtrl has been created');
            }
        ]);

        // .directive('yangModelDetailsPanel', [
        //     '$rootScope', '$window',
        //     function ($rootScope, $window) {
        //         return function (scope) {
        //             // TODO: details panel internals (see device.js as example
        //         }
        //     }
        // ]);

}());