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
 ONOS GUI -- Remote -- Web Socket Service
 */
(function () {
    'use strict';

    // injected refs
    var $log, $loc, fs, ufs, wsock, vs, ls;

    // internal state
    var webSockOpts,            // web socket options
        ws = null,              // web socket reference
        wsUp = false,           // web socket is good to go
        sid = 0,                // event sequence identifier
        handlers = {},          // event handler bindings
        pendingEvents = [],     // events TX'd while socket not up
        host,                   // web socket host
        url,                    // web socket URL
        clusterNodes = [],      // ONOS instances data for failover
        clusterIndex = -1,      // the instance to which we are connected
        connectRetries = 0,     // limit our attempts at reconnecting
        openListeners = {},     // registered listeners for websocket open()
        nextListenerId = 1,     // internal ID for open listeners
        loggedInUser = null;    // name of logged-in user

    // =======================
    // === Bootstrap Handler

    var builtinHandlers = {
        bootstrap: function (data) {
            $log.debug('bootstrap data', data);
            loggedInUser = data.user;
            clusterNodes = data.clusterNodes;
            clusterNodes.forEach(function (d, i) {
                if (d.uiAttached) {
                    clusterIndex = i;
                    $log.info('Connected to cluster node ' + d.ip);
                    // TODO: add connect info to masthead somewhere
                }
            });
        }
    };


    // ==========================
    // === Web socket callbacks

    function handleOpen() {
        $log.info('Web socket open - ', url);
        vs && vs.hide();

        if (fs.debugOn('txrx')) {
            $log.debug('Sending ' + pendingEvents.length + ' pending event(s)...');
        }
        pendingEvents.forEach(function (ev) {
            _send(ev);
        });
        pendingEvents = [];

        connectRetries = 0;
        wsUp = true;
        informListeners(host, url);
    }

    // Handles the specified (incoming) message using handler bindings.
    function handleMessage(msgEvent) {
        var ev, h;

        try {
            ev = JSON.parse(msgEvent.data);
        } catch (e) {
            $log.error('Message.data is not valid JSON', msgEvent.data, e);
            return null;
        }
        if (fs.debugOn('txrx')) {
            $log.debug(' << *Rx* ', ev.event, ev.payload);
        }

        if (h = handlers[ev.event]) {
            try {
                h(ev.payload);
            } catch (e) {
                $log.error('Problem handling event:', ev, e);
                return null;
            }
        } else {
            $log.warn('Unhandled event:', ev);
        }

    }

    function handleClose() {
        var gsucc;

        $log.info('Web socket closed');
        ls && ls.stop();
        wsUp = false;

        if (gsucc = findGuiSuccessor()) {
            createWebSocket(webSockOpts, gsucc);
        } else {
            // If no controllers left to contact, show the Veil...
            vs && vs.show([
                'Oops!',
                'Web-socket connection to server closed...',
                'Try refreshing the page.'
            ]);
        }
    }


    // ==============================
    // === Private Helper Functions

    function findGuiSuccessor() {
        var ncn = clusterNodes.length,
            ip, node;

        while (connectRetries < ncn && !ip) {
            connectRetries++;
            clusterIndex = (clusterIndex + 1) % ncn;
            node = clusterNodes[clusterIndex];
            ip = node && node.ip;
        }

        return ip;
    }

    function informListeners(host, url) {
        angular.forEach(openListeners, function (lsnr) {
            lsnr.cb(host, url);
        });
    }

    function _send(ev) {
        if (fs.debugOn('txrx')) {
            $log.debug(' *Tx* >> ', ev.event, ev.payload);
        }
        ws.send(JSON.stringify(ev));
    }

    function noHandlersWarn(handlers, caller) {
        if (!handlers || fs.isEmptyObject(handlers)) {
            $log.warn('WSS.' + caller + '(): no event handlers');
            return true;
        }
        return false;
    }

    // ===================
    // === API Functions

    // Required for unit tests to set to known state
    function resetSid() {
        sid = 0;
    }
    function resetState() {
        webSockOpts = undefined;
        ws = null;
        wsUp = false;
        host = undefined;
        url = undefined;
        pendingEvents = [];
        handlers = {};
        sid = 0;
        clusterNodes = [];
        clusterIndex = -1;
        connectRetries = 0;
        openListeners = {};
        nextListenerId = 1;
    }

    // Currently supported opts:
    //   wsport: web socket port (other than default 8181)
    // host: if defined, is the host address to use
    function createWebSocket(opts, _host_) {
        var wsport = (opts && opts.wsport) || null;

        webSockOpts = opts; // preserved for future calls

        host = _host_ || $loc.host();
        url = ufs.wsUrl('core', wsport, _host_);

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

    // Binds the message handlers to their message type (event type) as
    //  specified in the given map. Note that keys are the event IDs; values
    //  are either:
    //     * the event handler function, or
    //     * an API object which has an event handler for the key
    //
    function bindHandlers(handlerMap) {
        var m,
            dups = [];

        if (noHandlersWarn(handlerMap, 'bindHandlers')) {
            return null;
        }
        m = d3.map(handlerMap);

        m.forEach(function (eventId, api) {
            var fn = fs.isF(api) || fs.isF(api[eventId]);
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
        var m;

        if (noHandlersWarn(handlerMap, 'unbindHandlers')) {
            return null;
        }
        m = d3.map(handlerMap);

        m.forEach(function (eventId) {
            delete handlers[eventId];
        });
    }

    // TODO: simplify listener handling (see theme.js for sample code)

    function addOpenListener(callback) {
        var id = nextListenerId++,
            cb = fs.isF(callback),
            o = { id: id, cb: cb };

        if (cb) {
            openListeners[id] = o;
        } else {
            $log.error('WSS.addOpenListener(): callback not a function');
            o.error = 'No callback defined';
        }
        return o;
    }

    function removeOpenListener(lsnr) {
        var id = fs.isO(lsnr) && lsnr.id,
            o;
        if (!id) {
            $log.warn('WSS.removeOpenListener(): invalid listener', lsnr);
            return null;
        }
        o = openListeners[id];

        if (o) {
            delete openListeners[id];
        }
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

    // Binds the veil service as a delegate
    function setVeilDelegate(vd) {
        vs = vd;
    }

    // Binds the loading service as a delegate
    function setLoadingDelegate(ld) {
        ls = ld;
    }


    // ============================
    // ===== Definition of module
    angular.module('onosRemote')
    .factory('WebSocketService',
        ['$log', '$location', 'FnService', 'UrlFnService', 'WSock',

        function (_$log_, _$loc_, _fs_, _ufs_, _wsock_) {
            $log = _$log_;
            $loc = _$loc_;
            fs = _fs_;
            ufs = _ufs_;
            wsock = _wsock_;

            bindHandlers(builtinHandlers);

            return {
                resetSid: resetSid,
                resetState: resetState,
                createWebSocket: createWebSocket,
                bindHandlers: bindHandlers,
                unbindHandlers: unbindHandlers,
                addOpenListener: addOpenListener,
                removeOpenListener: removeOpenListener,
                sendEvent: sendEvent,
                isConnected: function () { return wsUp; },
                loggedInUser: function () { return loggedInUser || '(no-one)'; },

                _setVeilDelegate: setVeilDelegate,
                _setLoadingDelegate: setLoadingDelegate
            };
        }
    ]);

}());
