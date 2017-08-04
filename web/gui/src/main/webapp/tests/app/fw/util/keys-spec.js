/*
 * Copyright 2014-present Open Networking Foundation
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
 */
describe('factory: fw/util/keys.js', function() {
    var $log, ks, fs, qhs,
        d3Elem, elem, last;
  

    beforeEach(module('onosUtil', 'onosSvg', 'onosLayer', 'onosNav', 'onosRemote'));

    beforeEach(inject(function (_$log_, KeyService, FnService, QuickHelpService) {
        $log = _$log_;
        ks = KeyService;
        fs = FnService;
        qhs = QuickHelpService;
        d3Elem = d3.select('body').append('p').attr('id', 'ptest');
        elem = d3Elem.node();
        ks.installOn(d3Elem);
        ks.bindQhs(qhs);
        last = {
            view: null,
            key: null,
            code: null,
            ev: null
        };
    }));

    afterEach(function () {
        d3.select('#ptest').remove();
    });

    it('should define the key service', function () {
        expect(ks).toBeDefined();
    });

    it('should define api functions', function () {
        expect(fs.areFunctions(ks, [
            'bindQhs', 'installOn', 'keyBindings', 'unbindKeys', 'dialogKeys',
            'addSeq', 'remSeq', 'gestureNotes', 'enableKeys', 'enableGlobalKeys', 'checkNotGlobal'
        ])).toBeTruthy();
    });

    // This no longer works because 'initKeyboardEvent' has been depreciated.
    // Now there is a constructor for 'KeyboardEvent' where you can modify
    //      the new event with a dictionary(?) 'KeyboardEventInit'.
    // However, the below code has been so recently depreciated, there are no
    //      examples online of how to use the new interface, and some browsers
    //      don't support it still. These tests will have to be put off for a
    //      while more. (Investigated 4/28/15)
    // Also tried was Angular's triggerHandler() function, but it doesn't seem
    //      that it can take arguments about the event.
    // Using jQuery in tests might be a good idea, for triggering test events.
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

    // === Key binding related tests
    it('should start with default key bindings', function () {
        var state = ks.keyBindings(),
            gk = state.globalKeys,
            mk = state.maskedKeys,
            vk = state.viewKeys,
            vf = state.viewFunction;

        expect(gk.length).toEqual(4);
        ['backSlash', 'slash', 'esc', 'T'].forEach(function (k) {
            expect(fs.contains(gk, k)).toBeTruthy();
        });

        expect(mk.length).toEqual(3);
        ['backSlash', 'slash', 'T'].forEach(function (k) {
            expect(fs.contains(mk, k)).toBeTruthy();
        });

        expect(vk.length).toEqual(0);
        expect(vf).toBeFalsy();
    });

    function bindTestKeys(withDescs) {
        var keys = ['A', '1', 'F5', 'equals'],
            kb = {};

        function cb(view, key, code, ev) {
            last.view = view;
            last.key = key;
            last.code = code;
            last.ev = ev;
        }

        function bind(k) {
            return withDescs ? [cb, 'desc for key ' + k] : cb;
        }

        keys.forEach(function (k) {
            kb[k] = bind(k);
        });

        ks.keyBindings(kb);
    }

    function verifyCall(key, code) {
        // TODO: update expectation, when view tokens are implemented
        expect(last.view).toEqual('NotYetAViewToken');
        last.view = null;

        expect(last.key).toEqual(key);
        last.key = null;

        expect(last.code).toEqual(code);
        last.code = null;

        expect(last.ev).toBeTruthy();
        last.ev = null;
    }

    function verifyNoCall() {
        expect(last.view).toBeNull();
        expect(last.key).toBeNull();
        expect(last.code).toBeNull();
        expect(last.ev).toBeNull();
    }

    function verifyTestKeys() {
        jsKeyDown(elem, 65); // 'A'
        verifyCall('A', 65);
        jsKeyDown(elem, 66); // 'B'
        verifyNoCall();

        jsKeyDown(elem, 49); // '1'
        verifyCall('1', 49);
        jsKeyDown(elem, 50); // '2'
        verifyNoCall();

        jsKeyDown(elem, 116); // 'F5'
        verifyCall('F5', 116);
        jsKeyDown(elem, 117); // 'F6'
        verifyNoCall();

        jsKeyDown(elem, 187); // 'equals'
        verifyCall('equals', 187);
        jsKeyDown(elem, 189); // 'dash'
        verifyNoCall();

        var vk = ks.keyBindings().viewKeys;

        expect(vk.length).toEqual(4);
        ['A', '1', 'F5', 'equals'].forEach(function (k) {
            expect(fs.contains(vk, k)).toBeTruthy();
        });

        expect(ks.keyBindings().viewFunction).toBeFalsy();
    }

    // TODO: jsKeyDown(...) no longer emulates key presses ?! :(
    // The following four unit tests ignored until we can figure this out.
    // INVESTIGATED: see jsKeyDown for details...

    xit('should allow specific key bindings', function () {
        bindTestKeys();
        verifyTestKeys();
    });

    xit('should allow specific key bindings with descriptions', function () {
        bindTestKeys(true);
        verifyTestKeys();
    });

    xit('should warn about masked keys', function () {
        var k = {'space': cb, 'T': cb},
            count = 0;

        function cb(token, key, code, ev) {
            count++;
            //console.debug('count = ' + count, token, key, code);
        }

        spyOn($log, 'warn');

        ks.keyBindings(k);

        expect($log.warn).toHaveBeenCalledWith('setKeyBindings(): Key "T" is reserved');

        // the 'T' key should NOT invoke our callback
        expect(count).toEqual(0);
        jsKeyDown(elem, 84); // 'T'
        expect(count).toEqual(0);

        // but the 'space' key SHOULD invoke our callback
        jsKeyDown(elem, 32); // 'space'
        expect(count).toEqual(1);
    });

    xit('should block keys when disabled', function () {
        var cbCount = 0;

        function cb() { cbCount++; }

        function pressA() { jsKeyDown(elem, 65); }  // 65 == 'A' keycode

        ks.keyBindings({ A: cb });

        expect(cbCount).toBe(0);

        pressA();
        expect(cbCount).toBe(1);

        ks.enableKeys(false);
        pressA();
        expect(cbCount).toBe(1);

        ks.enableKeys(true);
        pressA();
        expect(cbCount).toBe(2);
    });

    // === Gesture notes related tests
    it('should start with no notes', function () {
        expect(ks.gestureNotes()).toEqual([]);
    });

    it('should allow us to add nodes', function () {
        var notes = [
            ['one', 'something about one'],
            ['two', 'description of two']
        ];
        ks.gestureNotes(notes);

        expect(ks.gestureNotes()).toEqual(notes);
    });

    it('should ignore non-arrays', function () {
        ks.gestureNotes({foo:4});
        expect(ks.gestureNotes()).toEqual([]);
    });

    // Consider adding test to ensure array contains 2-tuples of strings
});
