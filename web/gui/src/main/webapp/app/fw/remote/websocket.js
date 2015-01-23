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

    var fs;

    function fnOpen(f) {
        // wrap the onOpen function; we will handle any housekeeping here...
        if (!fs.isF(f)) {
            return null;
        }

        return function (openEvent) {
            // NOTE: nothing worth passing to the caller?
            f();
        };
    }

    function fnMessage(f) {
        // wrap the onMessage function; we will attempt to decode the
        // message event payload as JSON and pass that in...
        if (!fs.isF(f)) {
            return null;
        }

        return function (msgEvent) {
            var ev;
            try {
                ev = JSON.parse(msgEvent.data);
            } catch (e) {
                ev = {
                    error: 'Failed to parse JSON',
                    e: e
                };
            }
            f(ev);
        };
    }

    function fnClose(f) {
        // wrap the onClose function; we will handle any parameters to the
        // close event here...
        if (!fs.isF(f)) {
            return null;
        }

        return function (closeEvent) {
            // NOTE: only seen {reason == ""} so far, nevertheless...
            f(closeEvent.reason);
        };
    }

    angular.module('onosRemote')
    .factory('WebSocketService',
            ['$log', '$location', 'UrlFnService', 'FnService',

        function ($log, $loc, ufs, _fs_) {
            fs = _fs_;

            // creates a web socket for the given path, returning a "handle".
            // opts contains the event handler callbacks, etc.
            function createWebSocket(path, opts) {
                var o = opts || {},
                    wsport = opts && opts.wsport,
                    fullUrl = ufs.wsUrl(path, wsport),
                    api = {
                        meta: { path: fullUrl, ws: null },
                        send: send,
                        close: close
                    },
                    ws;

                try {
                    ws = new WebSocket(fullUrl);
                    api.meta.ws = ws;
                } catch (e) {
                }

                $log.debug('Attempting to open websocket to: ' + fullUrl);

                if (ws) {
                    ws.onopen = fnOpen(o.onOpen);
                    ws.onmessage = fnMessage(o.onMessage);
                    ws.onclose = fnClose(o.onClose);
                }

                // messages are expected to be event objects..
                function send(ev) {
                    if (ev) {
                        if (ws) {
                            ws.send(JSON.stringify(ev));
                        } else {
                            $log.warn('ws.send() no web socket open!',
                                fullUrl, ev);
                        }
                    }
                }

                function close() {
                    if (ws) {
                        ws.close();
                        ws = null;
                        api.meta.ws = null;
                    }
                }

                return api;
            }

            return {
                createWebSocket: createWebSocket
            };
    }]);

}());
