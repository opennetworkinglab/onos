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
    var $log, wss, wes, tps, tis, tfs;

    // internal state
    var wsock;

    var evHandler = {
        showSummary: showSummary,
        addInstance: addInstance,
        updateInstance: updateInstance,
        removeInstance: removeInstance,
        addDevice: addDevice,
        updateDevice: updateDevice,
        removeDevice: removeDevice,
        addHost: addHost,
        updateHost: updateHost,
        removeHost: removeHost,
        addLink: addLink,
        updateLink: updateLink,
        removeLink: removeLink

        // TODO: implement remaining handlers..
    };

    function unknownEvent(ev) {
        $log.warn('Unknown event (ignored):', ev);
    }

    // === Event Handlers ===

    // NOTE: --- once these are done, we will collapse them into
    // a more compact data structure... but for now, write in full..

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

    function addDevice(ev) {
        $log.debug('  **** Add Device **** ', ev.payload);
        tfs.addDevice(ev.payload);
    }

    function updateDevice(ev) {
        $log.debug('  **** Update Device **** ', ev.payload);
        tfs.updateDevice(ev.payload);
    }

    function removeDevice(ev) {
        $log.debug('  **** Remove Device **** ', ev.payload);
        tfs.removeDevice(ev.payload);
    }

    function addHost(ev) {
        $log.debug('  **** Add Host **** ', ev.payload);
        tfs.addHost(ev.payload);
    }

    function updateHost(ev) {
        $log.debug('  **** Update Host **** ', ev.payload);
        tfs.updateHost(ev.payload);
    }

    function removeHost(ev) {
        $log.debug('  **** Remove Host **** ', ev.payload);
        tfs.removeHost(ev.payload);
    }

    function addLink(ev) {
        $log.debug('  **** Add Link **** ', ev.payload);
        tfs.addLink(ev.payload);
    }

    function updateLink(ev) {
        $log.debug('  **** Update Link **** ', ev.payload);
        tfs.updateLink(ev.payload);
    }

    function removeLink(ev) {
        $log.debug('  **** Remove Link **** ', ev.payload);
        tfs.removeLink(ev.payload);
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
            'TopoPanelService', 'TopoInstService', 'TopoForceService',

        function (_$log_, $loc, _wss_, _wes_, _tps_, _tis_, _tfs_) {
            $log = _$log_;
            wss = _wss_;
            wes = _wes_;
            tps = _tps_;
            tis = _tis_;
            tfs = _tfs_;

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
                openSock: openSock,
                closeSock: closeSock,
                sendEvent: dispatcher.sendEvent
            };
        }]);
}());
