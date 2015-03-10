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
 ONOS GUI -- Remote -- Web Socket Service
 */
(function () {
    'use strict';

    // injected refs
    var fs, $log;

    // internal state
    var ws, sws, sid = 0,
        handlers = {};

    function resetSid() {
        sid = 0;
    }

    // Binds the specified message handlers.
    function bindHandlers(handlerMap) {
        var m = d3.map(handlerMap),
            dups = [];

        m.forEach(function (key, value) {
            var fn = fs.isF(value[key]);
            if (!fn) {
                $log.warn(key + ' binding not a function on ' + value);
                return;
            }

            if (handlers[key]) {
                dups.push(key);
            } else {
                handlers[key] = fn;
            }
        });
        if (dups.length) {
            $log.warn('duplicate bindings ignored:', dups);
        }
    }

    // Unbinds the specified message handlers.
    function unbindHandlers(handlerMap) {
        var m = d3.map(handlerMap);
        m.forEach(function (key) {
            delete handlers[key];
        });
    }

    // Formulates an event message and sends it via the shared web-socket.
    function sendEvent(evType, payload) {
        var p = payload || {};
        if (sws) {
            $log.debug(' *Tx* >> ', evType, payload);
            sws.send({
                event: evType,
                sid: ++sid,
                payload: p
            });
        } else {
            $log.warn('sendEvent: no websocket open:', evType, payload);
        }
    }


    // Handles the specified message using handler bindings.
    function handleMessage(msgEvent) {
        var ev;
        try {
            ev = JSON.parse(msgEvent.data);
            $log.debug(' *Rx* >> ', ev.event, ev.payload);
            dispatchToHandler(ev);
        } catch (e) {
            $log.error('message is not valid JSON', msgEvent);
        }
    }

    // Dispatches the message to the appropriate handler.
    function dispatchToHandler(event) {
        var handler = handlers[event.event];
        if (handler) {
            handler(event.payload);
        } else {
            $log.warn('unhandled event:', event);
        }
    }

    function handleOpen() {
        $log.info('web socket open');
        // FIXME: implement calling external hooks
    }

    function handleClose() {
        $log.info('web socket closed');
        // FIXME: implement reconnect logic
    }

    angular.module('onosRemote')
    .factory('WebSocketService',
            ['$log', '$location', 'UrlFnService', 'FnService',

        function (_$log_, $loc, ufs, _fs_) {
            fs = _fs_;
            $log = _$log_;

            // Creates a web socket for the given path, returning a "handle".
            // opts contains the event handler callbacks, etc.
            function createWebSocket(path, opts) {
                var o = opts || {},
                    wsport = opts && opts.wsport,
                    fullUrl = ufs.wsUrl(path, wsport),
                    api = {
                        meta: { path: fullUrl, ws: null },
                        send: send,
                        close: close
                    };

                try {
                    ws = new WebSocket(fullUrl);
                    api.meta.ws = ws;
                } catch (e) {
                }

                $log.debug('Attempting to open websocket to: ' + fullUrl);

                if (ws) {
                    ws.onopen = handleOpen;
                    ws.onmessage = handleMessage;
                    ws.onclose = handleClose;
                }

                // Sends a formulated event message via the backing web-socket.
                function send(ev) {
                    if (ev && ws) {
                        ws.send(JSON.stringify(ev));
                    } else if (!ws) {
                        $log.warn('ws.send() no web socket open!', fullUrl, ev);
                    }
                }

                // Closes the backing web-socket.
                function close() {
                    if (ws) {
                        ws.close();
                        ws = null;
                        api.meta.ws = null;
                    }
                }

                sws = api; // Make the shared web-socket accessible
                return api;
            }

            return {
                resetSid: resetSid,
                createWebSocket: createWebSocket,
                bindHandlers: bindHandlers,
                unbindHandlers: unbindHandlers,
                sendEvent: sendEvent
            };
    }]);

}());
