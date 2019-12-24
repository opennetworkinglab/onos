/*
 * Copyright 2018-present Open Networking Foundation
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
import { TestBed, inject } from '@angular/core/testing';

import { LogService } from '../log.service';
import { WebSocketService, WsOptions, Callback, EventType } from './websocket.service';
import { FnService } from '../util/fn.service';
import { GlyphService } from '../svg/glyph.service';
import { ActivatedRoute, Params } from '@angular/router';
import { UrlFnService } from './urlfn.service';
import { WSock } from './wsock.service';
import { of } from 'rxjs';

class MockActivatedRoute extends ActivatedRoute {
    constructor(params: Params) {
        super();
        this.queryParams = of(params);
    }
}

class MockGlyphService {}

/**
 * ONOS GUI -- Remote -- Web Socket Service - Unit Tests
 */
describe('WebSocketService', () => {
    let wss: WebSocketService;
    let fs: FnService;
    let ar: MockActivatedRoute;
    let windowMock: Window;
    let logServiceSpy: jasmine.SpyObj<LogService>;

    const noop = () => ({});
    const send = jasmine.createSpy('send')
        .and.callFake((ev) => ev);
    const mockWebSocket = {
        send: send,
        onmessage: (msgEvent) => ({}),
        onopen: () => ({}),
        onclose: () => ({}),
    };

    beforeEach(() => {
        const logSpy = jasmine.createSpyObj('LogService', ['info', 'debug', 'warn', 'error']);
        ar = new MockActivatedRoute({'debug': 'txrx'});

        windowMock = <any>{
            location: <any> {
                hostname: 'foo',
                host: 'foo',
                port: '80',
                protocol: 'http',
                search: { debug: 'true'},
                href: 'ws://foo:123/onos/ui/websock/path',
                absUrl: 'ws://foo:123/onos/ui/websock/path'
            }
        };
        fs = new FnService(ar, logSpy, windowMock);

        TestBed.configureTestingModule({
            providers: [WebSocketService,
                { provide: FnService, useValue: fs },
                { provide: LogService, useValue: logSpy },
                { provide: GlyphService, useClass: MockGlyphService },
                { provide: UrlFnService, useValue: new UrlFnService(logSpy, windowMock) },
                { provide: 'Window', useFactory: (() => windowMock ) },
                { provide: WSock, useFactory: (() => {
                        return {
                            newWebSocket: (() => mockWebSocket)
                        };
                    })
                }
            ]
        });

        wss = TestBed.get(WebSocketService);
        logServiceSpy = TestBed.get(LogService);
    });

    it('should define WebSocketService', () => {
        expect(wss).toBeDefined();
    });

    it('should define api functions', () => {
        expect(fs.areFunctions(wss, ['bootstrap', 'error',
            'handleOpen', 'handleMessage', 'handleClose',
            'findGuiSuccessor', 'informListeners', 'send',
            'noHandlersWarn', 'resetState',
            'createWebSocket', 'bindHandlers', 'unbindHandlers',
            'addOpenListener', 'removeOpenListener', 'sendEvent',
            'setVeilDelegate', 'setLoadingDelegate', 'isConnected',
             'closeWebSocket', 'isHandling'
        ])).toBeTruthy();
    });

    it('should use the appropriate URL, createWebsocket', () => {
        const url = wss.createWebSocket();
        expect(url).toEqual('ws://foo:80/onos/ui/websock/core');
    });

    it('should use the appropriate URL with modified port, createWebsocket',
        () => {
            const url = wss.createWebSocket(<WsOptions>{ wsport: 1243 });
            expect(url).toEqual('ws://foo:1243/onos/ui/websock/core');
    });

    it('should verify websocket event handlers, createWebsocket', () => {
        wss.createWebSocket({ wsport: 1234 });
        expect(fs.isF(mockWebSocket.onopen)).toBeTruthy();
        expect(fs.isF(mockWebSocket.onmessage)).toBeTruthy();
        expect(fs.isF(mockWebSocket.onclose)).toBeTruthy();
    });

    it('should invoke listener callbacks when websocket is up, handleOpen',
        () => {
            let num = 0;
            function incrementNum(host: string, url: string) {
                expect(host).toEqual('foo');
                num++;
            }
            wss.addOpenListener(incrementNum);
            wss.createWebSocket({ wsport: 1234 });

            mockWebSocket.onopen();
            expect(num).toBe(1);
    });

    xit('should send pending events, handleOpen', () => {
        const fakeEvent = {
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

    it('should handle an incoming bad JSON message, handleMessage', () => {
        const badMsg = {
            data: 'bad message'
        };
        wss.createWebSocket({ wsport: 1234 });
        expect(mockWebSocket.onmessage(badMsg)).toBeNull();
        expect(logServiceSpy.error).toHaveBeenCalled();
    });

    it('should verify message was handled, handleMessage', () => {
        let num = 0;
        function fakeHandler(data1: Object) { num++; }
        const data = JSON.stringify(<EventType>{
            event: 'mockEvResp',
            payload: {}
        });
        const event = {
            data: data
        };

        wss.createWebSocket({ wsport: 1234 });
        wss.bindHandlers(new Map<string, (data) => void>([
            ['mockEvResp', (data2) => fakeHandler(data2)]
        ]));
        expect(mockWebSocket.onmessage(event)).toBe(undefined);
        expect(num).toBe(1);
    });

    it('should warn if there is an unhandled event, handleMessage', () => {
        const data = { foo: 'bar', bar: 'baz'};
        const dataString = JSON.stringify(data);
        const badEv = {
                data: dataString
            };
        wss.createWebSocket({ wsport: 1234 });
        mockWebSocket.onmessage(badEv);
        expect(logServiceSpy.warn).toHaveBeenCalledWith('Unhandled event:', data);
    });

    it('should not warn if valid input, bindHandlers', () => {
        expect(wss.bindHandlers(new Map<string, (data) => void>([
            ['test', noop ],
            ['bar', noop ]
        ]))).toBe(undefined);

        expect(logServiceSpy.warn).not.toHaveBeenCalled();
    });

    it('should warn if no arguments, bindHandlers', () => {
        expect(wss.bindHandlers(
            new Map<string, (data) => void>([])
        )).toBeNull();
        expect(logServiceSpy.warn).toHaveBeenCalledWith(
            'WSS.bindHandlers(): no event handlers'
        );
    });

    it('should warn if duplicate handlers were given, bindHandlers',
        () => {
            wss.bindHandlers(
                new Map<string, (data) => void>([
                    ['noop', noop ]
                ])
            );
            expect(wss.bindHandlers(
                new Map<string, (data) => void>([
                    ['noop', noop ]
                ])
            )).toBe(undefined);
            expect(logServiceSpy.warn).toHaveBeenCalledWith('duplicate bindings ignored:',
                                                    ['noop']);
    });

    it('should warn if no arguments, unbindHandlers', () => {
        expect(wss.unbindHandlers([])).toBeNull();
        expect(logServiceSpy.warn).toHaveBeenCalledWith(
            'WSS.unbindHandlers(): no event handlers'
        );
    });
    // Note: cannot test unbindHandlers' forEach due to it using closure variable

    it('should not warn if valid argument, addOpenListener', () => {
        let o = wss.addOpenListener(noop);
        expect(o.id).toEqual(1);
        expect(o.cb).toEqual(noop);
        expect(logServiceSpy.warn).not.toHaveBeenCalled();
        o = wss.addOpenListener(noop);
        expect(o.id).toEqual(2);
        expect(o.cb).toEqual(noop);
        expect(logServiceSpy.warn).not.toHaveBeenCalled();
    });

    it('should log error if callback not a function, addOpenListener',
        () => {
            const o = wss.addOpenListener(null);
            expect(o.id).toEqual(1);
            expect(o.cb).toEqual(null);
            expect(o.error).toEqual('No callback defined');
            expect(logServiceSpy.error).toHaveBeenCalledWith(
                'WSS.addOpenListener(): callback not a function'
            );
    });

    it('should not warn if valid listener object, removeOpenListener', () => {
        expect(wss.removeOpenListener(<Callback>{
            id: 1,
            error: 'error',
            cb: noop
        })).toBe(undefined);
        expect(logServiceSpy.warn).not.toHaveBeenCalled();
    });

    it('should warn if listener is invalid, removeOpenListener', () => {
        expect(wss.removeOpenListener(<Callback>{})).toBeNull();
        expect(logServiceSpy.warn).toHaveBeenCalledWith(
            'WSS.removeOpenListener(): invalid listener', {}
        );
    });

    // Note: handleClose is not currently tested due to all work it does relies
    //       on closure variables that cannot be mocked

});
