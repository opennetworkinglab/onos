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
 */
describe('factory: fw/nav/nav.js', function() {
    var ns, $log, fs;
    var d3Elem;

    beforeEach(module('onosNav', 'onosUtil'));

    beforeEach(inject(function (NavService, _$log_, FnService) {
        ns = NavService;
        $log = _$log_;
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
            'showNav', 'hideNav', 'toggleNav', 'hideIfShown'
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

});
