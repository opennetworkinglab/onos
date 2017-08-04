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
 ONOS GUI -- SVG -- SVG Util Service - Unit Tests
 */
describe('factory: fw/svg/svgUtil.js', function() {
    var $log, fs, sus, svg, defs, force;

    var noop = function () {};

    beforeEach(module('onosUtil', 'onosSvg'));

    beforeEach(inject(function (_$log_, FnService, SvgUtilService) {
        $log = _$log_;
        fs = FnService;
        sus = SvgUtilService;
        svg = d3.select('body').append('svg').attr('id', 'mySvg');
        defs = svg.append('defs');
        force = d3.layout.force();
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
            'translate', 'scale', 'skewX', 'rotate',
            'stripPx', 'safeId', 'visible'
        ])).toBeTruthy();
    });

    // === createDragBehavior
    // TODO: break up drag into separate functions for testing
    // d3 needs better testing support...

    // Note: just checking to see if error message was called
    //       because jasmine spy isn't catching the right newline char
    it('should complain if function given no parameters', function () {
        spyOn($log, 'error');
        expect(sus.createDragBehavior()).toBeNull();
        expect($log.error).toHaveBeenCalled();
    });

    it('should complain if function is not given clickEnabled', function () {
        spyOn($log, 'error');
        expect(sus.createDragBehavior(force, noop, noop, noop)).toBeNull();
        expect($log.error).toHaveBeenCalled();
    });

    it('should complain if function is not given dragEnabled', function () {
        spyOn($log, 'error');
        expect(sus.createDragBehavior(force, noop, noop)).toBeNull();
        expect($log.error).toHaveBeenCalled();
    });

    it('should complain if function is not given atDragEnd', function () {
        spyOn($log, 'error');
        expect(sus.createDragBehavior(force, noop)).toBeNull();
        expect($log.error).toHaveBeenCalled();
    });

    it('should complain if function is not given selectCb', function () {
        spyOn($log, 'error');
        expect(sus.createDragBehavior(force)).toBeNull();
        expect($log.error).toHaveBeenCalled();
    });

    // === loadGlowDefs
    function checkAttrs(glow, r, g, b) {
        var filterEffects, feColor, feBlur, feMerge, feMergeNodes;

        // filter attrs
        expect(glow.attr('x')).toBe('-50%');
        expect(glow.attr('y')).toBe('-50%');
        expect(glow.attr('width')).toBe('200%');
        expect(glow.attr('height')).toBe('200%');

        filterEffects = d3.selectAll(glow.node().childNodes);
        expect(filterEffects.size()).toBe(3);

        // Note: d3 didn't recognize 'feColorMatrix' and others as valid selectors
        //       this is a work around
        feColor = d3.select(filterEffects[0].shift());
        feBlur = d3.select(filterEffects[0].shift());
        feMerge = d3.select(filterEffects[0].shift());

        // feColorMatrix attrs
        expect(feColor.empty()).toBe(false);
        expect(feColor.attr('type')).toBe('matrix');
        expect(feColor.attr('values')).toBe(
            '0 0 0 0  ' + r + ' ' +
            '0 0 0 0  ' + g + ' ' +
            '0 0 0 0  ' + b + ' ' +
            '0 0 0 1  0 '
        );

        // feGuassianBlur attrs
        expect(feBlur.empty()).toBe(false);
        expect(feBlur.attr('stdDeviation')).toBe('3');
        expect(feBlur.attr('result')).toBe('coloredBlur');

        // feMerge attrs
        feMergeNodes = d3.selectAll(feMerge.node().childNodes);
        expect(feMergeNodes.size()).toBe(2);
        expect(d3.select(feMergeNodes[0][0]).attr('in')).toBe('coloredBlur');
        expect(d3.select(feMergeNodes[0][1]).attr('in')).toBe('SourceGraphic');
    }

    it('should load glow definitions', function () {
        var blue, yellow;
        sus.loadGlowDefs(defs);

        expect(defs.empty()).toBe(false);
        expect((defs.selectAll('filter')).size()).toBe(2);

        // blue-glow specific
        blue = defs.select('#blue-glow');
        expect(blue.empty()).toBe(false);
        checkAttrs(blue, 0.0, 0.0, 0.7);

        // yellow-glow specific
        yellow = defs.select('#yellow-glow');
        expect(yellow.empty()).toBe(false);
        checkAttrs(yellow, 1.0, 1.0, 0.3);
    });

    // === cat7

    it('should define two methods on the api', function () {
        var cat7 = sus.cat7();
        expect(fs.areFunctions(cat7, [
            'testCard', 'getColor'
        ])).toBeTruthy();
    });

    it('should provide blue', function () {
       expect(sus.cat7().getColor('foo', false, 'light')).toEqual('#5b99d2');
    });

    it('should provide lt-blue', function () {
       expect(sus.cat7().getColor('bar', false, 'light')).toEqual('#66cef6');
    });

    it('should provide paler shade of blue for muted', function () {
        expect(sus.cat7().getColor('foo', true, 'light')).toEqual('#9ebedf');
    });

    it('should provide an alternate (dark) shade of blue', function () {
       expect(sus.cat7().getColor('foo', false, 'dark')).toEqual('#5b99d2');
    });

    it('should provide an alternate (dark) shade of blue for muted', function () {
        expect(sus.cat7().getColor('foo', true, 'dark')).toEqual('#9ebedf');
    });

    it('should iterate across the colors', function () {
        expect(sus.cat7().getColor('foo', false, 'light')).toEqual('#5b99d2');
        expect(sus.cat7().getColor('bar', false, 'light')).toEqual('#66cef6');
        expect(sus.cat7().getColor('baz', false, 'light')).toEqual('#d05a55');
        expect(sus.cat7().getColor('zoo', false, 'light')).toEqual('#0f9d58');
        expect(sus.cat7().getColor('sdh', false, 'light')).toEqual('#ba7941');
        expect(sus.cat7().getColor('goo', false, 'light')).toEqual('#3dc0bf');
        expect(sus.cat7().getColor('pip', false, 'light')).toEqual('#56af00');

        // and cycle back to the first color for item #8
        expect(sus.cat7().getColor('bri', false, 'light')).toEqual('#5b99d2');

        // and return the same color for the same ID
        expect(sus.cat7().getColor('zoo', false, 'light')).toEqual('#0f9d58');
    });

    // === translate(), scale(), skewX(), rotate()

    it('should translate from two args', function () {
        expect(sus.translate(1,2)).toEqual('translate(1,2)');
    });

    it('should translate from an array', function () {
        expect(sus.translate([3,4])).toEqual('translate(3,4)');
    });

    it('should scale', function () {
        expect(sus.scale(1.5,2.5)).toEqual('scale(1.5,2.5)');
    });

    it('should skewX', function () {
        expect(sus.skewX(3.14)).toEqual('skewX(3.14)');
    });

    it('should rotate', function () {
        expect(sus.rotate(45)).toEqual('rotate(45)');
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
