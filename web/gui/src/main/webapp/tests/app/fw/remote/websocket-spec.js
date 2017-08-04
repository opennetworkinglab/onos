/*
 * Copyright 2015-present Open Networking Foundation
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

    var noop = function () {},
        send = jasmine.createSpy('send').and.callFake(function (ev) {
            return ev;
        }),
        mockWebSocket = {
            send: send
        };

    beforeEach(module('onosRemote', 'onosLayer', 'ngRoute', 'onosNav', 'onosSvg'));

    beforeEach(function () {
        mockWebSocket = {
            send: send
        };
    });

    beforeEach(function () {
        module(function ($provide) {
            $provide.factory('WSock', function () {
                return {
                    newWebSocket: function () {
                        return mockWebSocket;
                    }
                };
            });
        });
    });

    beforeEach(module(function($provide) {
        $provide.factory('$location', function () {
            return {
                protocol: function () { return 'http'; },
                host: function () { return 'foo'; },
                port: function () { return '80'; },
                search: function() {
                    return {debug: 'true'};
                },
                absUrl: function () {
                    return 'ws://foo:123/onos/ui/websock/path';
                }
            };
        })
    }));

    beforeEach(inject(function (_$log_, FnService, WebSocketService) {
        $log = _$log_;
        fs = FnService;
        wss = WebSocketService;
        wss.resetState();
    }));


    it('should define WebSocketService', function () {
        expect(wss).toBeDefined();
    });

    it('should define api functions', function () {
        expect(fs.areFunctions(wss, [
            'resetState',
            'createWebSocket', 'bindHandlers', 'unbindHandlers',
            'addOpenListener', 'removeOpenListener', 'sendEvent',
            'isConnected', 'loggedInUser',
            '_setVeilDelegate', '_setLoadingDelegate'
        ])).toBeTruthy();
    });

    it('should use the appropriate URL, createWebsocket', function () {
        var url = wss.createWebSocket();
        expect(url).toEqual('ws://foo:80/onos/ui/websock/core');
    });

    it('should use the appropriate URL with modified port, createWebsocket',
        function () {
            var url = wss.createWebSocket({ wsport: 1243 });
            expect(url).toEqual('ws://foo:1243/onos/ui/websock/core');
    });

    it('should verify websocket event handlers, createWebsocket', function () {
        wss.createWebSocket({ wsport: 1234 });
        expect(fs.isF(mockWebSocket.onopen)).toBeTruthy();
        expect(fs.isF(mockWebSocket.onmessage)).toBeTruthy();
        expect(fs.isF(mockWebSocket.onclose)).toBeTruthy();
    });

    it('should invoke listener callbacks when websocket is up, handleOpen',
        function () {
            var num = 0;
            function incrementNum() { num++; }
            wss.addOpenListener(incrementNum);
            wss.createWebSocket({ wsport: 1234 });
            mockWebSocket.onopen();
            expect(num).toBe(1);
    });

    it('should send pending events, handleOpen', function () {
        var fakeEvent = {
            event: 'mockEv',
            payload: { mock: 'thing' }
        };
        wss.sendEvent(fakeEvent.event, fakeEvent.payload);
        // on opening the socket, a single authentication event should have
        // been sent already...
        expect(mockWebSocket.send.calls.count()).toEqual(1);

        wss.createWebSocket({ wsport: 1234 });
        mockWebSocket.onopen();
        expect(mockWebSocket.send).toHaveBeenCalledWith(JSON.stringify(fakeEvent));
    });

    it('should handle an incoming bad JSON message, handleMessage', function () {
        spyOn($log, 'error');
        var badMsg = {
            data: 'bad message'
        };
        wss.createWebSocket({ wsport: 1234 });
        expect(mockWebSocket.onmessage(badMsg)).toBeNull();
        expect($log.error).toHaveBeenCalled();
    });

    it('should verify message was handled, handleMessage', function () {
        var num = 0,
            fakeHandler = {
                mockEvResp: function () { num++; }
            },
            data = JSON.stringify({
                event: 'mockEvResp',
                payload: {}
            }),
            event = {
                data: data
            };
        wss.createWebSocket({ wsport: 1234 });
        wss.bindHandlers(fakeHandler);
        expect(mockWebSocket.onmessage(event)).toBe(undefined);
        expect(num).toBe(1);
    });

    it('should warn if there is an unhandled event, handleMessage', function () {
        spyOn($log, 'warn');
        var data = { foo: 'bar', bar: 'baz'},
            dataString = JSON.stringify(data),
            badEv = {
                data: dataString
            };
        wss.createWebSocket({ wsport: 1234 });
        mockWebSocket.onmessage(badEv);
        expect($log.warn).toHaveBeenCalledWith('Unhandled event:', data);
    });

    it('should not warn if valid input, bindHandlers', function () {
        spyOn($log, 'warn');
        expect(wss.bindHandlers({
            foo: noop,
            bar: noop
        })).toBe(undefined);
        expect($log.warn).not.toHaveBeenCalled();
    });

    it('should warn if no arguments, bindHandlers', function () {
        spyOn($log, 'warn');
        expect(wss.bindHandlers()).toBeNull();
        expect($log.warn).toHaveBeenCalledWith(
            'WSS.bindHandlers(): no event handlers'
        );
        expect(wss.bindHandlers({})).toBeNull();
        expect($log.warn).toHaveBeenCalledWith(
            'WSS.bindHandlers(): no event handlers'
        );
    });

    it('should warn if handler is not a function, bindHandlers', function () {
        spyOn($log, 'warn');
        expect(wss.bindHandlers({
            foo: 'handler1',
            bar: 3,
            baz: noop
        })).toBe(undefined);
        expect($log.warn).toHaveBeenCalledWith('foo handler not a function');
        expect($log.warn).toHaveBeenCalledWith('bar handler not a function');
    });

    it('should warn if duplicate handlers were given, bindHandlers',
        function () {
            spyOn($log, 'warn');
            wss.bindHandlers({
                foo: noop
            });
            expect(wss.bindHandlers({
                foo: noop
            })).toBe(undefined);
            expect($log.warn).toHaveBeenCalledWith('duplicate bindings ignored:',
                                                    ['foo']);
    });

    it('should warn if no arguments, unbindHandlers', function () {
        spyOn($log, 'warn');
        expect(wss.unbindHandlers()).toBeNull();
        expect($log.warn).toHaveBeenCalledWith(
            'WSS.unbindHandlers(): no event handlers'
        );
        expect(wss.unbindHandlers({})).toBeNull();
        expect($log.warn).toHaveBeenCalledWith(
            'WSS.unbindHandlers(): no event handlers'
        );
    });
    // Note: cannot test unbindHandlers' forEach due to it using closure variable

    it('should not warn if valid argument, addOpenListener', function () {
        spyOn($log, 'warn');
        var o = wss.addOpenListener(noop);
        expect(o.id === 1);
        expect(o.cb === noop);
        expect($log.warn).not.toHaveBeenCalled();
        o = wss.addOpenListener(noop);
        expect(o.id === 2);
        expect(o.cb === noop);
        expect($log.warn).not.toHaveBeenCalled();
    });

    it('should log error if callback not a function, addOpenListener',
        function () {
            spyOn($log, 'error');
            var o = wss.addOpenListener('foo');
            expect(o.id === 1);
            expect(o.cb === 'foo');
            expect(o.error === 'No callback defined');
            expect($log.error).toHaveBeenCalledWith(
                'WSS.addOpenListener(): callback not a function'
            );
    });

    it('should not warn if valid listener object, removeOpenListener', function () {
        spyOn($log, 'warn');
        expect(wss.removeOpenListener({
            id: 1,
            cb: noop
        })).toBe(undefined);
        expect($log.warn).not.toHaveBeenCalled();
    });

    it('should warn if listener is invalid, removeOpenListener', function () {
        spyOn($log, 'warn');
        expect(wss.removeOpenListener({})).toBeNull();
        expect($log.warn).toHaveBeenCalledWith(
            'WSS.removeOpenListener(): invalid listener', {}
        );
        expect(wss.removeOpenListener('listener')).toBeNull();
        expect($log.warn).toHaveBeenCalledWith(
            'WSS.removeOpenListener(): invalid listener', 'listener'
        );
    });

    // Note: handleClose is not currently tested due to all work it does relies
    //       on closure variables that cannot be mocked

});
