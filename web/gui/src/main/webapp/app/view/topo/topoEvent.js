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
 ONOS GUI -- Topology Event Module.

 Defines the conduit between the client and the server:
    - provides a clean API for sending events to the server
    - dispatches incoming events from the server to the appropriate sub-module

 */

(function () {
    'use strict';

    // injected refs
    var $log, wss, tps, tis, tfs, tss, tov, tspr;

    // internal state
    var handlerMap,
        openListener;

    // ==========================

    function createHandlerMap() {
        handlerMap = {
            showSummary: tps,

            showDetails: tss,

            showHighlights: tov,

            addInstance: tis,
            updateInstance: tis,
            removeInstance: tis,

            addDevice: tfs,
            updateDevice: tfs,
            removeDevice: tfs,
            addHost: tfs,
            updateHost: tfs,
            moveHost: tfs,
            removeHost: tfs,
            addLink: tfs,
            updateLink: tfs,
            removeLink: tfs,

            topoStartDone: tfs,

            spriteListResponse: tspr,
            spriteDataResponse: tspr,
        };
    }

    function wsOpen(host, url) {
        $log.debug('TOPO: web socket open - cluster node:', host, 'URL:', url);
        // Request batch of initial data from the new server
        wss.sendEvent('topoStart');
    }


    angular.module('ovTopo')
    .factory('TopoEventService',
        ['$log', 'WebSocketService',
            'TopoPanelService', 'TopoInstService', 'TopoForceService',
            'TopoSelectService', 'TopoOverlayService', 'TopoSpriteService',

        function (_$log_, _wss_,
                  _tps_, _tis_, _tfs_, _tss_, _tov_, _tspr_) {
            $log = _$log_;
            wss = _wss_;
            tps = _tps_;
            tis = _tis_;
            tfs = _tfs_;
            tss = _tss_;
            tov = _tov_;
            tspr = _tspr_;

            createHandlerMap();

            function bindHandlers() {
                wss.bindHandlers(handlerMap);
                $log.debug('topo event handlers bound');
            }

            function start() {
                // in case we fail over to a new server, listen for wsock-open
                openListener = wss.addOpenListener(wsOpen);
                wss.sendEvent('topoStart');
                $log.debug('topo comms started');
            }

            function stop() {
                wss.sendEvent('topoStop');
                wss.unbindHandlers(handlerMap);
                wss.removeOpenListener(openListener);
                openListener = null;
                $log.debug('topo comms stopped');
            }

            return {
                bindHandlers: bindHandlers,
                start: start,
                stop: stop,
            };
        }]);
}());
