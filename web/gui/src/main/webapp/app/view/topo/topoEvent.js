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

    var $log, wes;

    var evHandler = {
        showSummary: showSummary,
        addInstance: addInstance
    };

    function unknownEvent(ev) {
        $log.warn('Unknown event (ignored):', ev);
    }

    // === Event Handlers ===

    function showSummary(ev) {
        $log.log('  **** Show Summary ****  ', ev.payload);
    }

    function addInstance(ev) {
        $log.log(' *** We got an ADD INSTANCE event: ', ev);
    }

    angular.module('ovTopo')
        .factory('TopoEventService', ['$log', 'WsEventService',
        function (_$log_, _wes_) {
            $log = _$log_;
            wes = _wes_;

            var wsock;

            return {
                dispatcher: {
                    handleEvent: function (ev) {
                        (evHandler[ev.event] || unknownEvent)(ev);
                    },
                    sendEvent: function (evType, payload) {
                        if (wsock) {
                            wes.sendEvent(wsock, evType, payload);
                        } else {
                            $log.warn('sendEvent: no websocket open:',
                                evType, payload);
                        }
                    }
                },
                bindSock: function (ws) {
                    wsock = ws;
                }
            }
        }]);
}());
