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

    angular.module('onosRemote')
    .factory('WebSocketService',
            ['$log', '$location', 'UrlFnService', 'FnService',

        function ($log, $loc, ufs, _fs_) {
            fs = _fs_;

            // creates a web socket for the given path, returning a "handle".
            // opts contains the event handler callbacks.
            function createWebSocket(path, opts) {
                var wsport = opts && opts.wsport,
                    fullUrl = ufs.wsUrl(path, wsport),
                    ws = new WebSocket(fullUrl),
                    api = {
                        meta: { path: fullUrl, ws: ws },
                        send: send,
                        close: close
                    };

                $log.debug('Attempting to open websocket to: ' + fullUrl);

                ws.onopen = (opts && opts.onOpen) || null;
                ws.onmessage = (opts && opts.onMessage) || null;
                ws.onclose = (opts && opts.onClose) || null;

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
                    }
                }

                return api;
            }

            return {
                createWebSocket: createWebSocket
            };
    }]);

}());
