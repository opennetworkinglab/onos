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
 ONOS GUI -- Remote -- Web Socket Service - Unit Tests
 */
describe('factory: fw/remote/websocket.js', function () {
    var $log, fs, wss;

    beforeEach(module('onosRemote'));

    beforeEach(module(function($provide) {
        $provide.factory('$location', function (){
            return {
                protocol: function () { return 'http'; },
                host: function () { return 'foo'; },
                port: function () { return '80'; }
            };
        })
    }));

    beforeEach(inject(function (_$log_, FnService, WebSocketService) {
        $log = _$log_;
        fs = FnService;
        wss = WebSocketService;
    }));


    it('should define WebSocketService', function () {
        expect(wss).toBeDefined();
    });

    it('should define api functions', function () {
        expect(fs.areFunctions(wss, [
            'createWebSocket'
        ])).toBeTruthy();
    });

    it('should use the appropriate URL', function () {
        var ws = wss.createWebSocket('foo/path');
        expect(ws.meta.path).toEqual('ws://foo:80/onos/ui/ws/foo/path');
    });

    it('should use the appropriate URL with modified port', function () {
        var ws = wss.createWebSocket('foo/path', { wsport: 1243 });
        expect(ws.meta.path).toEqual('ws://foo:1243/onos/ui/ws/foo/path');
    });

    var oCalled, mCalled, cCalled, json, reason;

    function oo() {
        oCalled++;
    }
    function om(j) {
        mCalled++;
        json = j;
    }
    function oc(r) {
        cCalled++;
        reason = r;
    }

    function resetCounters() {
        oCalled = mCalled = cCalled = 0;
        json = reason = null;
    }

    function validateCallbacks(ws, op, msg, cl) {
        // we have to cheat a little, by digging into the websocket structure
        var onO = fs.isF(ws.meta.ws.onopen),
            onM = fs.isF(ws.meta.ws.onmessage),
            onC = fs.isF(ws.meta.ws.onclose);

        expect(!!onO).toEqual(op);
        expect(!!onM).toEqual(msg);
        expect(!!onC).toEqual(cl);

        onO && onO({});
        onM && onM({ data: '{ "item": "ivalue" }'});
        onC && onC({ reason: 'rvalue' });

        expect(oCalled).toEqual(op ? 1 : 0);
        expect(mCalled).toEqual(msg ? 1 : 0);
        expect(cCalled).toEqual(cl ? 1 : 0);

        expect(json).toEqual(msg ? { item: 'ivalue' } : null);
        expect(reason).toEqual(cl ? 'rvalue' : null);
    }

    it('should install the appropriate callbacks', function () {
        resetCounters();

        var ws = wss.createWebSocket('foo', {
            onOpen: oo,
            onMessage: om,
            onClose: oc
        });

        validateCallbacks(ws, true, true, true);
    });

    it('should install partial callbacks', function () {
        resetCounters();

        var ws = wss.createWebSocket('foo', {
            onOpen: oo,
            onMessage: om
        });

        validateCallbacks(ws, true, true, false);
    });

    it('should install no callbacks', function () {
        resetCounters();

        var ws = wss.createWebSocket('foo');

        validateCallbacks(ws, false, false, false);
    });

    // can't really test send without faking out the WebSocket.
/*
    it('should stringify objects for sending', function () {
        var ws = wss.createWebSocket('foo');
        ws.send({ item: 'itemVal' });

        // what to assert?
    });
*/

    it('should remove websocket reference on close', function () {
        var ws = wss.createWebSocket('foo');
        expect(ws.meta.ws instanceof WebSocket).toBeTruthy();

        ws.close();
        expect(ws.meta.ws).toBeNull();
    });
});
