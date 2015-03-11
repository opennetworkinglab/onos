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

    beforeEach(module('onosRemote', 'onosLayer', 'ngRoute', 'onosNav', 'onosSvg'));

    beforeEach(module(function($provide) {
        $provide.factory('$location', function () {
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
            'resetSid', 'createWebSocket', 'bindHandlers', 'unbindHandlers',
            'addOpenListener', 'removeOpenListener', 'sendEvent'
        ])).toBeTruthy();
    });

    it('should use the appropriate URL', function () {
        var url = wss.createWebSocket();
        expect(url).toEqual('ws://foo:80/onos/ui/websock/core');
    });

    it('should use the appropriate URL with modified port', function () {
        var url = wss.createWebSocket({ wsport: 1243 });
        expect(url).toEqual('ws://foo:1243/onos/ui/websock/core');
    });

    // TODO: inject mock WSock service and write more tests ...

});
