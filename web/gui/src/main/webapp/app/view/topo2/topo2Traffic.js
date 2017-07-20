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
 *
 */

(function () {
    'use strict';

    // injected refs
    var $log, fs, flash, wss, api;

    // configuration
    var allTrafficTypes = [
            'flowStatsBytes',
            'portStatsBitSec',
            'portStatsPktSec'
        ],
        allTrafficMsgs = [
            'Flow Stats (bytes)',
            'Port Stats (bits / second)',
            'Port Stats (packets / second)'
        ];

    // internal state
    var mode = null,
        currentIndex = 0,
        allIndex = 0;

    // === -----------------------------------------------------

    function cancelTraffic() {
        $log.debug('Topo2Traffic: Cancel Traffic');

        if (!mode) {
            return false;
        }
        mode = null;
        wss.sendEvent('topo2CancelTraffic');
        flash.flash('Traffic monitoring canceled');
        return true;
    }

    function showAllTraffic() {
        $log.debug('Topo2Traffic: Show All Traffic:', allTrafficTypes[allIndex]);

        mode = 'allFlowPort';
        wss.sendEvent('topo2RequestAllTraffic', {
            trafficType: allTrafficTypes[allIndex]
        });
        flash.flash(allTrafficMsgs[allIndex]);
        currentIndex = allIndex;
        allIndex = (allIndex + 1) % 3;
    }

    function selectedTrafficOverlay() {
        return allTrafficTypes[currentIndex];
    }

    // === -----------------------------------------------------

    angular.module('ovTopo2')
        .factory('Topo2TrafficService', [
            '$log', 'FnService', 'FlashService', 'WebSocketService',

            function (_$log_, _fs_, _flash_, _wss_) {
                $log = _$log_;
                fs = _fs_;
                flash = _flash_;
                wss = _wss_;

                return {
                    initTraffic: function (_api_) { api = _api_; },
                    destroyTraffic: function () {},

                    // invoked from toolbar overlay buttons or keystrokes
                    cancelTraffic: cancelTraffic,
                    showAllTraffic: showAllTraffic,
                    selectedTrafficOverlay: selectedTrafficOverlay
                }
            }
        ]);
}());