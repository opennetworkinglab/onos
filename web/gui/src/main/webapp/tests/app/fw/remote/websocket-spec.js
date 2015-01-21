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

    it('should install the appropriate callbacks', function () {
        function oo() {}
        function om() {}
        function oc() {}

        var ws = wss.createWebSocket('foo', {
            onOpen: oo,
            onMessage: om,
            onClose: oc
        });

        expect(ws.meta.ws.onopen).toBe(oo);
        expect(ws.meta.ws.onmessage).toBe(om);
        expect(ws.meta.ws.onclose).toBe(oc);
    });

    it('should install partial callbacks', function () {
        function oo() {}
        function om() {}

        var ws = wss.createWebSocket('foo', {
            onOpen: oo,
            onMessage: om
        });

        expect(ws.meta.ws.onopen).toBe(oo);
        expect(ws.meta.ws.onmessage).toBe(om);
        expect(ws.meta.ws.onclose).toBeNull();
    });

    // TODO: more testing to be done.

});
