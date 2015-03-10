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
 DEPRECATED: to be deleted
 ONOS GUI -- Remote -- Web Socket Event Service
 */
(function () {
    'use strict';

    var sid = 0;

    angular.module('onosRemote')
        .factory('WsEventService', [function () {

            function sendEvent(ws, evType, payload) {
                var p = payload || {};

                ws.send({
                    event: evType,
                    sid: ++sid,
                    payload: p
                });
            }

            function resetSid() {
                sid = 0;
            }

            return {
                sendEvent: sendEvent,
                resetSid: resetSid
            };
        }]);

}());
