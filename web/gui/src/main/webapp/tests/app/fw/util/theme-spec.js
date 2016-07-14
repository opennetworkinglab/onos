/*
 * Copyright 2014-present Open Networking Laboratory
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
 ONOS GUI -- Util -- Theme Service - Unit Tests
 */
describe('factory: fw/util/theme.js', function() {
    var ts, $log, fs;

    beforeEach(module('onosUtil', 'onosRemote'));

    beforeEach(inject(function (ThemeService, _$log_, FnService) {
        ts = ThemeService;
        $log = _$log_;
        fs = FnService;
        ts.init();
    }));

    it('should define ThemeService', function () {
        expect(ts).toBeDefined();
    });

    it('should define api functions', function () {
        expect(fs.areFunctions(ts, [
            'init', 'theme', 'toggleTheme', 'addListener', 'removeListener'
        ])).toBeTruthy();
    });


    function verifyBodyClass(yes, no) {
        function bodyHasClass(c) {
            return d3.select('body').classed(c);
        }
        expect(bodyHasClass(yes)).toBeTruthy();
        expect(bodyHasClass(no)).toBeFalsy();
    }

    it('should default to light theme', function () {
        expect(ts.theme()).toEqual('light');
    });

    it('should toggle to dark, then to light again', function () {
        // Note: re-work this once theme-change listeners are implemented
        spyOn($log, 'debug');

        expect(ts.toggleTheme()).toEqual('dark');
        expect(ts.theme()).toEqual('dark');
        expect($log.debug).toHaveBeenCalledWith('Theme-Change-(toggle): dark');
        verifyBodyClass('dark', 'light');

        expect(ts.toggleTheme()).toEqual('light');
        expect(ts.theme()).toEqual('light');
        expect($log.debug).toHaveBeenCalledWith('Theme-Change-(toggle): light');
        verifyBodyClass('light', 'dark');
    });

    it('should let us set the theme by name', function () {
        // Note: re-work this once theme-change listeners are implemented
        spyOn($log, 'debug');

        expect(ts.theme()).toEqual('light');
        ts.theme('dark');
        expect(ts.theme()).toEqual('dark');
        expect($log.debug).toHaveBeenCalledWith('Theme-Change-(set): dark');
        verifyBodyClass('dark', 'light');
    });

    it('should ignore unknown theme names', function () {
        // Note: re-work this once theme-change listeners are implemented
        spyOn($log, 'debug');

        ts.theme('light'); // setting theme lo light (was set to dark by the previous test)
        $log.debug.calls.reset(); // resetting the spy

        expect(ts.theme()).toEqual('light');
        verifyBodyClass('light', 'dark');

        ts.theme('turquoise');
        expect(ts.theme()).toEqual('light');
        expect($log.debug).not.toHaveBeenCalled();
        verifyBodyClass('light', 'dark');
    });


    // === Unit Tests for listeners

    it('should report lack of callback', function () {
        spyOn($log, 'error');
        var list = ts.addListener();
        expect($log.error).toHaveBeenCalledWith(
            'ThemeService.addListener(): callback not a function'
        );
        expect(list.error).toEqual('No callback defined');
    });

    it('should report non-functional callback', function () {
        spyOn($log, 'error');
        var list = ts.addListener(['some array']);
        expect($log.error).toHaveBeenCalledWith(
            'ThemeService.addListener(): callback not a function'
        );
        expect(list.error).toEqual('No callback defined');
    });

    it('should invoke our callback with an event', function () {
        var event;

        function cb(ev) {
            event = ev;
        }

        expect(event).toBeUndefined();
        ts.addListener(cb);
        ts.theme('dark');
        expect(event).toEqual({
            event: 'themeChange',
            value: 'dark'
        });
    });

    it('should invoke our callback at appropriate times', function () {

        var cb = jasmine.createSpy('cb');

        var listener;

        expect(cb.calls.count()).toEqual(0);

        ts.toggleTheme(); // -> dark

        listener = ts.addListener(cb);
        ts.toggleTheme(); // -> light

        ts.theme('light');  // (still light - no event)

        ts.theme('dark');   // -> dark

        ts.removeListener(listener);
        ts.toggleTheme();   // -> light

        expect(cb.calls.count()).toEqual(3);
    });

});
