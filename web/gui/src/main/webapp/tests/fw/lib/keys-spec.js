/*
 * Copyright 2014 Open Networking Laboratory
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
 ONOS GUI -- Key Handler Service - Unit Tests

 @author Simon Hunt
 */
describe('factory: fw/lib/keys.js', function() {
    var ks,
        fs,
        d3Elem;

    beforeEach(module('onosApp'));

    beforeEach(inject(function (KeyService, FnService) {
        ks = KeyService;
        fs = FnService;
        d3Elem = d3.select('body').append('p').attr('id', 'ptest');
        ks.installOn(d3Elem);
    }));

    afterEach(function () {
        d3.select('#ptest').remove();
    });

    it('should have injected stuff defined', function () {
        expect(ONOS).toBeDefined();
        expect(ks).toBeDefined();
        expect(fs).toBeDefined();
    });

    // NOTE: kinda messy, but it seems to get the job done.
    function jsKeyDown(element, code) {
        var ev = document.createEvent('KeyboardEvent');

        // Chromium Hack
        if (navigator.userAgent.toLowerCase().indexOf('chrome') > -1) {
            Object.defineProperty(ev, 'keyCode', {
                get: function () { return this.keyCodeVal; }
            });
            Object.defineProperty(ev, 'which', {
                get: function () { return this.keyCodeVal; }
            });
        }

        if (ev.initKeyboardEvent) {
            ev.initKeyboardEvent('keydown', true, true, document.defaultView,
                                false, false, false, false, code, code);
        } else {
            ev.initKeyEvent('keydown', true, true, document.defaultView,
                            false, false, false, false, code, 0);
        }

        ev.keyCodeVal = code;

        if (ev.keyCode !== code) {
            console.warn("keyCode mismatch " + ev.keyCode +
                        "(" + ev.which + ") -> "+ code);
        }
        element.dispatchEvent(ev);
    }

    it('should start in light theme', function () {
        expect(ks.theme()).toEqual('light');
    });
    it('should toggle to dark theme', function () {
        jsKeyDown(d3Elem.node(), 84); // 'T'
        expect(ks.theme()).toEqual('dark');
    });

    // key code lookups
    // NOTE: should be injecting keydown events, rather than exposing whatKey()
    it('whatKey: 13', function () {
        expect(ks.whatKey(13)).toEqual('enter');
    });
    it('whatKey: 16', function () {
        expect(ks.whatKey(16)).toEqual('shift');
    });
    it('whatKey: 40', function () {
        expect(ks.whatKey(40)).toEqual('downArrow');
    });
    it('whatKey: 65', function () {
        expect(ks.whatKey(65)).toEqual('A');
    });
    it('whatKey: 84', function () {
        expect(ks.whatKey(84)).toEqual('T');
    });
    it('whatKey: 49', function () {
        expect(ks.whatKey(49)).toEqual('1');
    });
    it('whatKey: 55', function () {
        expect(ks.whatKey(55)).toEqual('7');
    });
    it('whatKey: 112', function () {
        expect(ks.whatKey(112)).toEqual('F1');
    });
    it('whatKey: 123', function () {
        expect(ks.whatKey(123)).toEqual('F12');
    });
    it('whatKey: 1', function () {
        expect(ks.whatKey(1)).toEqual('.');
    });

});
