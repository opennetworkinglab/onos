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
 Defines event handling for events received from the server.
 */

(function () {
    'use strict';

    // injected refs
    var $log, wss, wes, tps, tis;

    // internal state
    var wsock;

    var evHandler = {
        showSummary: showSummary,
        addInstance: addInstance,
        updateInstance: updateInstance,
        removeInstance: removeInstance
        // TODO: implement remaining handlers..

    };

    function unknownEvent(ev) {
        $log.warn('Unknown event (ignored):', ev);
    }

    // === Event Handlers ===

    function showSummary(ev) {
        $log.debug('  **** Show Summary ****  ', ev.payload);
        tps.showSummary(ev.payload);
    }

    function addInstance(ev) {
        $log.debug('  **** Add Instance **** ', ev.payload);
        tis.addInstance(ev.payload);
    }

    function updateInstance(ev) {
        $log.debug('  **** Update Instance **** ', ev.payload);
        tis.updateInstance(ev.payload);
    }

    function removeInstance(ev) {
        $log.debug('  **** Remove Instance **** ', ev.payload);
        tis.removeInstance(ev.payload);
    }

    // ==========================

    var dispatcher = {
        handleEvent: function (ev) {
            (evHandler[ev.event] || unknownEvent)(ev);
        },
        sendEvent: function (evType, payload) {
            if (wsock) {
                wes.sendEvent(wsock, evType, payload);
            } else {
                $log.warn('sendEvent: no websocket open:', evType, payload);
            }
        }
    };

    // ===  Web Socket functions ===

    function onWsOpen() {
        $log.debug('web socket opened...');
        // kick off request for periodic summary data...
        dispatcher.sendEvent('requestSummary');
    }

    function onWsMessage(ev) {
        dispatcher.handleEvent(ev);
    }

    function onWsClose(reason) {
        $log.log('web socket closed; reason=', reason);
        wsock = null;
    }

    // ==========================

    angular.module('ovTopo')
    .factory('TopoEventService',
        ['$log', '$location', 'WebSocketService', 'WsEventService',
            'TopoPanelService', 'TopoInstService',

        function (_$log_, $loc, _wss_, _wes_, _tps_, _tis_) {
            $log = _$log_;
            wss = _wss_;
            wes = _wes_;
            tps = _tps_;
            tis = _tis_;

            function bindDispatcher(TopoDomElementsPassedHere) {
                // TODO: store refs to topo DOM elements...

                return dispatcher;
            }

            // TODO: handle "guiSuccessor" functionality (replace host)
            // TODO: implement retry on close functionality
            function openSock() {
                wsock = wss.createWebSocket('topology', {
                    onOpen: onWsOpen,
                    onMessage: onWsMessage,
                    onClose: onWsClose,
                    wsport: $loc.search().wsport
                });
                $log.debug('web socket opened:', wsock);
            }

            function closeSock() {
                var path;
                if (wsock) {
                    path = wsock.meta.path;
                    wsock.close();
                    wsock = null;
                    $log.debug('web socket closed. path:', path);
                }
            }

            return {
                bindDispatcher: bindDispatcher,
                openSock: openSock,
                closeSock: closeSock
            };
        }]);
}());
