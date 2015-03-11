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
    var $log, $loc, fs, ufs, wsock, vs;

    // internal state
    var ws = null,              // web socket reference
        wsUp = false,           // web socket is good to go
        sid = 0,                // event sequence identifier
        handlers = {},          // event handler bindings
        pendingEvents = [],     // events TX'd while socket not up
        url,                    // web socket URL
        instances = [];

    var builtinHandlers = {
            onosInstances: function (data) {
                instances = data.instances;
            }
    }

    // ==========================
    // === Web socket callbacks

    function handleOpen() {
        $log.info('Web socket open');
        $log.debug('Sending ' + pendingEvents.length + ' pending event(s)...');
        pendingEvents.forEach(function (ev) {
            _send(ev);
        });
        pendingEvents = [];
        wsUp = true;
    }

    // Handles the specified (incoming) message using handler bindings.
    function handleMessage(msgEvent) {
        var ev, h;

        try {
            ev = JSON.parse(msgEvent.data);
        } catch (e) {
            $log.error('Message.data is not valid JSON', msgEvent.data, e);
            return;
        }
        $log.debug(' *Rx* >> ', ev.event, ev.payload);

        if (h = handlers[ev.event]) {
            try {
                h(ev.payload);
            } catch (e) {
                $log.error('Problem handling event:', ev, e);
            }
        } else {
            $log.warn('Unhandled event:', ev);
        }

    }

    function handleClose() {
        $log.info('Web socket closed');
        wsUp = false;

        // FIXME: implement controller failover logic

        // If no controllers left to contact, show the Veil...
        vs.show([
            'Oops!',
            'Web-socket connection to server closed...',
            'Try refreshing the page.'
        ]);
    }


    // ==============================
    // === Private Helper Functions

    function _send(ev) {
        $log.debug(' *Tx* >> ', ev.event, ev.payload);
        ws.send(JSON.stringify(ev));
    }


    // ===================
    // === API Functions

    // Required for unit tests to set to known state
    function resetSid() {
        sid = 0;
    }

    // Currently supported opts:
    //   wsport: web socket port (other than default 8181)
    function createWebSocket(opts) {
        var wsport = (opts && opts.wsport) || null;

        url = ufs.wsUrl('core', wsport);

        $log.debug('Attempting to open websocket to: ' + url);
        ws = wsock.newWebSocket(url);
        if (ws) {
            ws.onopen = handleOpen;
            ws.onmessage = handleMessage;
            ws.onclose = handleClose;
        }
        // Note: Wsock logs an error if the new WebSocket call fails
        return url;
    }

    // Binds the specified message handlers.
    //   keys are the event IDs
    //   values are the API on which the handler function is a property
    function bindHandlers(handlerMap) {
        var m = d3.map(handlerMap),
            dups = [];

        m.forEach(function (eventId, api) {
            var fn = fs.isF(api[eventId]);
            if (!fn) {
                $log.warn(eventId + ' handler not a function');
                return;
            }

            if (handlers[eventId]) {
                dups.push(eventId);
            } else {
                handlers[eventId] = fn;
            }
        });
        if (dups.length) {
            $log.warn('duplicate bindings ignored:', dups);
        }
    }

    // Unbinds the specified message handlers.
    //   Expected that the same map will be used, but we only care about keys
    function unbindHandlers(handlerMap) {
        var m = d3.map(handlerMap);

        m.forEach(function (eventId) {
            delete handlers[eventId];
        });
    }

    // Formulates an event message and sends it via the web-socket.
    //  If the websocket is not up yet, we store it in a pending list.
    function sendEvent(evType, payload) {
        var ev = {
                event: evType,
                sid: ++sid,
                payload: payload || {}
            };

        if (wsUp) {
            _send(ev);
        } else {
            pendingEvents.push(ev);
        }
    }


    // ============================
    // ===== Definition of module
    angular.module('onosRemote')
    .factory('WebSocketService',
        ['$log', '$location', 'FnService', 'UrlFnService', 'WSock',
            'VeilService',

        function (_$log_, _$loc_, _fs_, _ufs_, _wsock_, _vs_) {
            $log = _$log_;
            $loc = _$loc_;
            fs = _fs_;
            ufs = _ufs_;
            wsock = _wsock_;
            vs = _vs_;

            // Bind instance handlers
            bindHandlers({
                onosInstances: builtinHandlers
            });

            return {
                resetSid: resetSid,
                createWebSocket: createWebSocket,
                bindHandlers: bindHandlers,
                unbindHandlers: unbindHandlers,
                sendEvent: sendEvent
            };
        }
    ]);

}());
