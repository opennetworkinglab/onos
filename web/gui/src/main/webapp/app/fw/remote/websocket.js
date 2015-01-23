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
        return fs.isF(f);
    }

    function fnMessage(f) {
        // wrap the onMessage function; we will attempt to decode the
        // message event payload as JSON and pass that in...
        var fn = fs.isF(f);
        if (!fn) {
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
            fn(ev);
        }
    }

    function fnClose(f) {
        return fs.isF(f);
    }

    angular.module('onosRemote')
    .factory('WebSocketService',
            ['$log', '$location', 'UrlFnService', 'FnService',

        function ($log, $loc, ufs, _fs_) {
            fs = _fs_;

            // creates a web socket for the given path, returning a "handle".
            // opts contains the event handler callbacks.
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

                function send(msg) {
                    if (msg) {
                        if (ws) {
                            ws.send(msg);
                        } else {
                            $log.warn('ws.send() no web socket open!',
                                fullUrl, msg);
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
