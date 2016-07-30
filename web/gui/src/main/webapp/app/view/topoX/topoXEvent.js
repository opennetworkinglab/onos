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
 ONOS GUI -- Topology Event Module.

 Defines the conduit between the client and the server:
 - provides a clean API for sending events to the server
 - dispatches incoming events from the server to the appropriate sub-module

 */

(function () {
    'use strict';

    // injected refs
    var $log, wss, tXfs;

    // internal state
    var handlerMap,
        openListener;

    // ========================== Helper Functions

    function createHandlerMap() {
        handlerMap = {
            topo2AllInstances: tXfs,
            topo2CurrentLayout: tXfs,
            topo2CurrentRegion: tXfs,
            topo2PeerRegions: tXfs,
            topo2StartDone: tXfs

            // Add further event names / module references as needed
        };
    }

    function wsOpen(host, url) {
        $log.debug('topoXEvent: WSopen - cluster node:', host, 'URL:', url);
        // tell the server we are ready to receive topo events
        wss.sendEvent('topo2Start');
    }

    // bind our event handlers to the web socket service, so that our
    //  callbacks get invoked for incoming events
    function bindHandlers() {
        wss.bindHandlers(handlerMap);
        $log.debug('topoX event handlers bound');
    }

    // tell the server we are ready to receive topology events
    function start() {
        // in case we fail over to a new server,
        // listen for wsock-open events
        openListener = wss.addOpenListener(wsOpen);
        wss.sendEvent('topo2Start');
        $log.debug('topoX comms started');
    }

    // tell the server we no longer wish to receive topology events
    function stop() {
        wss.sendEvent('topo2Stop');
        wss.unbindHandlers(handlerMap);
        wss.removeOpenListener(openListener);
        openListener = null;
        $log.debug('topoX comms stopped');
    }

    // ========================== Main Service Definition

    angular.module('ovTopoX')
    .factory('TopoXEventService',
        ['$log', 'WebSocketService', 'TopoXForceService',

        function (_$log_, _wss_, _tXfs_) {
            $log = _$log_;
            wss = _wss_;
            tXfs = _tXfs_;

            // deferred creation of handler map, so module references are good
            createHandlerMap();

            return {
                bindHandlers: bindHandlers,
                start: start,
                stop: stop
            };
        }]);
}());
