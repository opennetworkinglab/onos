/*
 * Copyright 2014,2015 Open Networking Laboratory
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

 @author Simon Hunt
 */
describe('factory: fw/util/theme.js', function() {
    var ts, $log;

    beforeEach(module('onosUtil'));

    beforeEach(inject(function (ThemeService, _$log_) {
        ts = ThemeService;
        $log = _$log_;
        ts.init();
    }));

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

        expect(ts.theme()).toEqual('light');
        verifyBodyClass('light', 'dark');

        ts.theme('turquoise');
        expect(ts.theme()).toEqual('light');
        expect($log.debug).not.toHaveBeenCalled();
        verifyBodyClass('light', 'dark');
    });
});
