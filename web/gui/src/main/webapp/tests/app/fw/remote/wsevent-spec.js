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
 ONOS GUI -- Remote -- Web Socket Event Service - Unit Tests
 */

// NOTE WsEventService does not exist, it has been removed?

xdescribe('factory: fw/remote/wsevent.js', function () {
    var $log, fs, wse;

    beforeEach(module('onosRemote'));

    beforeEach(inject(function (_$log_, FnService, WsEventService) {
        $log = _$log_;
        fs = FnService;
        wse = WsEventService;
        wse.resetSid();
    }));


    it('should define WsEventService', function () {
        expect(wse).toBeDefined();
    });

    it('should define api functions', function () {
        expect(fs.areFunctions(wse, [
            'sendEvent', 'resetSid'
        ])).toBeTruthy();
    });

    var event,
        fakeWs = {
            send: function (ev) {
                event = ev;
            }
        };

    it('should construct an event object with no payload', function ()  {
        wse.sendEvent(fakeWs, 'foo');
        expect(event.event).toEqual('foo');
        expect(event.sid).toEqual(1);
        expect(event.payload).toEqual({});
    });

    it('should construct an event object with some payload', function ()  {
        wse.sendEvent(fakeWs, 'bar', { val: 42 });
        expect(event.event).toEqual('bar');
        expect(event.sid).toEqual(1);
        expect(event.payload).toEqual({ val: 42 });
    });

    it('should increment the sid', function () {
        wse.sendEvent(fakeWs, 'one');
        expect(event.event).toEqual('one');
        expect(event.sid).toEqual(1);

        wse.sendEvent(fakeWs, 'dos');
        expect(event.event).toEqual('dos');
        expect(event.sid).toEqual(2);

        wse.sendEvent(fakeWs, 'tres');
        expect(event.event).toEqual('tres');
        expect(event.sid).toEqual(3);
    });

});
