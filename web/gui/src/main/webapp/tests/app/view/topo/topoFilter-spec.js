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
 ONOS GUI -- Topo View -- Topo Filter Service - Unit Tests
 */
describe('factory: view/topo/topoFilter.js', function() {
    var $log, fs, fltr, d3Elem, api;

    var mockNodes = {
            each: function () {},
            classed: function () {}
        },
        mockLinks = {
            each: function () {},
            classed: function () {}
        };

    beforeEach(module('ovTopo', 'onosUtil', 'onosLayer', 'ngRoute', 'onosNav'));

    beforeEach(inject(function (_$log_, FnService, TopoFilterService) {
        $log = _$log_;
        fs = FnService;
        fltr = TopoFilterService;
        d3Elem = d3.select('body').append('div').attr('id', 'myMastDiv');

        api = {
            node: function () { return mockNodes; },
            link: function () { return mockLinks; }
        };
    }));

    afterEach(function () {
        d3.select('#myMastDiv').remove();
    });

    it('should define TopoFilterService', function () {
        expect(fltr).toBeDefined();
    });

    it('should define api functions', function () {
        expect(fs.areFunctions(fltr, [
            'initFilter', 'destroyFilter',
            'clickAction', 'selected', 'inLayer',
        ])).toBeTruthy();
    });

    it('should inject the buttons into the given div', function () {
        fltr.initFilter(api, d3Elem);
        var grpdiv = d3Elem.select('#topo-radio-group');
        expect(grpdiv.size()).toBe(1);

        var btns = grpdiv.selectAll('span');
        expect(btns.size()).toBe(3);

        var prefix = 'topo-rb-',
            expIds = [ 'all', 'pkt', 'opt' ];

        btns.each(function (d, i) {
            var b = d3.select(this);
            expect(b.attr('id')).toEqual(prefix + expIds[i]);
            // 0th button is active - others are not
            expect(b.classed('active')).toEqual(i === 0);
        });
    });

    it('should remove the buttons from the given div', function () {
        fltr.initFilter(api, d3Elem);
        var grpdiv = d3Elem.select('#topo-radio-group');
        expect(grpdiv.size()).toBe(1);

        fltr.destroyFilter();
        grpdiv = d3Elem.select('#topo-radio-group');
        expect(grpdiv.size()).toBe(0);
    });

    it('should report the selected button', function () {
        fltr.initFilter(api, d3Elem);
        expect(fltr.selected()).toEqual('all');
    });

    // TODO: figure out how to trigger the click function on the spans..

});
