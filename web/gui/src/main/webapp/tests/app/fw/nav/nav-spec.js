/*
 * Copyright 2015-present Open Networking Laboratory
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
describe('factory: fw/nav/nav.js', function() {
    var $log, $location, $window, ns, fs;
    var d3Elem;

    beforeEach(module('onosNav', 'onosUtil'));

    var mockWindow = {
        location: {
            href: 'http://server/#/mock/url'
        }
    };

    beforeEach(function () {
        module(function ($provide) {
            $provide.value('$window', mockWindow);
        });
    });

    beforeEach(inject(function (_$log_, _$location_, _$window_,
                                NavService, FnService) {
        $log = _$log_;
        $location = _$location_;
        $window = _$window_;
        ns = NavService;
        fs = FnService;
        d3Elem = d3.select('body').append('div').attr('id', 'nav');
        ns.hideNav();
    }));

    afterEach(function () {
        d3.select('#nav').remove();
    });

    it('should define NavService', function () {
        expect(ns).toBeDefined();
    });

    it('should define api functions', function () {
        expect(fs.areFunctions(ns, [
            'showNav', 'hideNav', 'toggleNav', 'hideIfShown', 'navTo'
        ])).toBeTruthy();
    });

    function checkHidden(b) {
        var what = b ? 'hidden' : 'visible';
        expect(d3.select('#nav').style('visibility')).toEqual(what);
    }

    it('should start hidden', function () {
        checkHidden(true);
    });

    it('should be shown then hidden', function () {
        ns.showNav();
        checkHidden(false);
        ns.hideNav();
        checkHidden(true);
    });

    it('should toggle hidden', function () {
        ns.toggleNav();
        checkHidden(false);
        ns.toggleNav();
        checkHidden(true);
    });

    it('should show idempotently', function () {
        checkHidden(true);
        ns.showNav();
        checkHidden(false);
        ns.showNav();
        checkHidden(false);
    });

    it('should hide idempotently', function () {
        checkHidden(true);
        ns.hideNav();
        checkHidden(true);
    });

    it('should be a noop if already hidden', function () {
        checkHidden(true);
        expect(ns.hideIfShown()).toBe(false);
        checkHidden(true);
    });

    it('should hide if shown', function () {
        ns.showNav();
        checkHidden(false);
        expect(ns.hideIfShown()).toBe(true);
        checkHidden(true);
    });

    it('should take correct navTo parameters', function () {
        spyOn($log, 'warn');

        ns.navTo('foo');
        expect($log.warn).not.toHaveBeenCalled();

        ns.navTo('bar', { q1: 'thing', q2: 'thing2' });
        expect($log.warn).not.toHaveBeenCalled();

    });

    it('should check navTo parameter warnings', function () {
        spyOn($log, 'warn');

        expect(ns.navTo()).toBeNull();
        expect($log.warn).toHaveBeenCalledWith('Not a valid navigation path');

        ns.navTo('baz', [1, 2, 3]);
        expect($log.warn).toHaveBeenCalledWith(
            'Query params not an object', [1, 2, 3]
        );

        ns.navTo('zoom', 'not a query param');
        expect($log.warn).toHaveBeenCalledWith(
            'Query params not an object', 'not a query param'
        );
    });

    it('should verify where the window is navigating', function () {
        ns.navTo('foo');
        expect($window.location.href).toBe('http://server/#/foo');

        ns.navTo('bar');
        expect($window.location.href).toBe('http://server/#/bar');

        ns.navTo('baz', { q1: 'thing1', q2: 'thing2' });
        expect($window.location.href).toBe(
            'http://server/#/baz?q1=thing1&q2=thing2'
        );

        ns.navTo('zip', { q3: 'thing3' });
        expect($window.location.href).toBe(
            'http://server/#/zip?q3=thing3'
        );

        ns.navTo('zoom', {});
        expect($window.location.href).toBe('http://server/#/zoom');

        ns.navTo('roof', [1, 2, 3]);
        expect($window.location.href).toBe('http://server/#/roof');
    });

});
