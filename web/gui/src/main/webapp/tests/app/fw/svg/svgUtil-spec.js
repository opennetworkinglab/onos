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
 ONOS GUI -- SVG -- SVG Util Service - Unit Tests
 */
describe('factory: fw/svg/svgUtil.js', function() {
    var $log, fs, sus, svg, d3Elem;

    beforeEach(module('onosUtil', 'onosSvg'));

    beforeEach(inject(function (_$log_, FnService, SvgUtilService) {
        $log = _$log_;
        fs = FnService;
        sus = SvgUtilService;
        svg = d3.select('body').append('svg').attr('id', 'mySvg');
        d3Elem = svg.append('defs');
    }));

    afterEach(function () {
        d3.select('svg').remove();
    });

    it('should define SvgUtilService', function () {
        expect(sus).toBeDefined();
    });

    it('should define api functions', function () {
        expect(fs.areFunctions(sus, [
            'createDragBehavior', 'loadGlowDefs', 'cat7',
            'translate',
            'stripPx', 'safeId', 'visible'
        ])).toBeTruthy();
    });


    // TODO: add unit tests for drag behavior
    // TODO: add unit tests for loadGlowDefs

    // === cat7

    it('should define two methods on the api', function () {
        var cat7 = sus.cat7();
        expect(fs.areFunctions(cat7, [
            'testCard', 'getColor'
        ])).toBeTruthy();
    });

    it('should provide a certain shade of blue', function () {
       expect(sus.cat7().getColor('foo', false, 'light')).toEqual('#3E5780');
    });

    it('should not matter what the ID really is for shade of blue', function () {
       expect(sus.cat7().getColor('bar', false, 'light')).toEqual('#3E5780');
    });

    it('should provide different shade of blue for muted', function () {
        expect(sus.cat7().getColor('foo', true, 'light')).toEqual('#A8B8CC');
    });


    it('should provide an alternate (dark) shade of blue', function () {
       expect(sus.cat7().getColor('foo', false, 'dark')).toEqual('#304860');
    });

    it('should provide an alternate (dark) shade of blue for muted', function () {
        expect(sus.cat7().getColor('foo', true, 'dark')).toEqual('#304860');
    });

    it('should iterate across the colors', function () {
        expect(sus.cat7().getColor('foo', false, 'light')).toEqual('#3E5780');
        expect(sus.cat7().getColor('bar', false, 'light')).toEqual('#78533B');
        expect(sus.cat7().getColor('baz', false, 'light')).toEqual('#CB4D28');
        expect(sus.cat7().getColor('goo', false, 'light')).toEqual('#018D61');
        expect(sus.cat7().getColor('zoo', false, 'light')).toEqual('#8A2979');
        expect(sus.cat7().getColor('pip', false, 'light')).toEqual('#006D73');
        expect(sus.cat7().getColor('sdh', false, 'light')).toEqual('#56AF00');
        // and cycle back to the first color for item #8
        expect(sus.cat7().getColor('bri', false, 'light')).toEqual('#3E5780');
        // and return the same color for the same ID
        expect(sus.cat7().getColor('zoo', false, 'light')).toEqual('#8A2979');
    });

    // === translate()

    it('should translate from two args', function () {
        expect(sus.translate(1,2)).toEqual('translate(1,2)');
    });

    it('should translate from an array', function () {
        expect(sus.translate([3,4])).toEqual('translate(3,4)');
    });


    // === stripPx()

    it('should not affect a number', function () {
        expect(sus.stripPx('4')).toEqual('4');
    });

    it('should remove trailing px', function () {
        expect(sus.stripPx('4px')).toEqual('4');
    });

    // === visible()

    it('should hide and show an element', function () {
        var r = svg.append('rect');

        sus.visible(r, false);
        expect(r.style('visibility')).toEqual('hidden');
        expect(sus.visible(r)).toBe(false);

        sus.visible(r, true);
        expect(r.style('visibility')).toEqual('visible');
        expect(sus.visible(r)).toBe(true);
    });
});
