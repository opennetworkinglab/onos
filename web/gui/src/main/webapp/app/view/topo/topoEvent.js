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
 ONOS GUI -- Topology Event Module.

 Defines the conduit between the client and the server:
    - provides a clean API for sending events to the server
    - dispatches incoming events from the server to the appropriate sub-module

 */

(function () {
    'use strict';

    // injected refs
    var $log, wss, tps, tis, tfs, tss, tts;

    // internal state
    var handlerMap;

    // ==========================

    function createHandlerMap() {
        handlerMap = {
            showSummary: tps,

            showDetails: tss,

            showTraffic: tts,

            addInstance: tis,
            updateInstance: tis,
            removeInstance: tis,

            addDevice: tfs,
            updateDevice: tfs,
            removeDevice: tfs,
            addHost: tfs,
            updateHost: tfs,
            removeHost: tfs,
            addLink: tfs,
            updateLink: tfs,
            removeLink: tfs
        };
    }

    angular.module('ovTopo')
    .factory('TopoEventService',
        ['$log', '$location', 'WebSocketService',
            'TopoPanelService', 'TopoInstService', 'TopoForceService',
            'TopoSelectService', 'TopoTrafficService',

        function (_$log_, $loc, _wss_, _tps_, _tis_, _tfs_, _tss_, _tts_) {
            $log = _$log_;
            wss = _wss_;
            tps = _tps_;
            tis = _tis_;
            tfs = _tfs_;
            tss = _tss_;
            tts = _tts_;

            createHandlerMap();

            function start() {
                wss.bindHandlers(handlerMap);
                wss.sendEvent('topoStart');
                wss.sendEvent('requestSummary');
                $log.debug('topo comms started');
            }

            function stop() {
                wss.sendEvent('topoStop');
                wss.unbindHandlers(handlerMap);
                $log.debug('topo comms stopped');
            }

            return {
                start: start,
                stop: stop
            };
        }]);
}());
