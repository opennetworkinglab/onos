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
    .factory('WebSocketService', ['$location', 'UrlFnService', 'FnService',
        function ($loc, ufs, _fs_) {
            fs = _fs_;

            // creates a web socket for the given path, returning a "handle".
            // cb is the callbacks block.
            function createWebSocket(path, cb) {
                //var fullUrl = ufs.wsUrl(path),
                var fullUrl = 'ws://localhost:8123/foo',
                    ws = new WebSocket(fullUrl),
                    api = {
                        meta: { path: fullUrl, ws: ws },
                        send: send,
                        close: close
                    };

                ws.onopen = (cb && cb.onOpen) || null;
                ws.onmessage = (cb && cb.onMessage) || null;
                ws.onclose = (cb && cb.onClose) || null;

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
