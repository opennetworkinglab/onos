/*
 * Copyright 2015-present Open Networking Laboratory
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
 ONOS GUI -- Remote -- Web Socket Wrapper Service

 This service provided specifically so that it can be mocked in unit tests.
 */
(function () {
    'use strict';

    angular.module('onosRemote')
        .factory('WSock', ['$log', function ($log) {

            function newWebSocket(url) {
                var ws = null;
                try {
                    ws = new WebSocket(url);
                } catch (e) {
                    $log.error('Unable to create web socket:', e);
                }
                return ws;
            }

            return {
                newWebSocket: newWebSocket
            };
        }]);
}());
