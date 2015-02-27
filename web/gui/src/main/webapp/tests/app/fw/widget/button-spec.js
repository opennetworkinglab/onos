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
 ONOS GUI -- Widget -- Button Service - Unit Tests
 */
describe('factory: fw/widget/button.js', function () {
    var $log, fs, bns, d3Elem;

    beforeEach(module('onosWidget', 'onosSvg'));

    beforeEach(inject(function (_$log_, FnService, ButtonService) {
        $log = _$log_;
        fs = FnService;
        bns = ButtonService;
    }));

    beforeEach(function () {
        d3Elem = d3.select('body').append('div').attr('id', 'testDiv');
    });

    afterEach(function () {
        d3.select('#testDiv').remove();
    });


    // re-usable null function
    function nullFunc () {}

    it('should define ButtonService', function () {
        expect(bns).toBeDefined();
    });

    it('should define api functions', function () {
        expect(fs.areFunctions(bns, [
            'button', 'toggle', 'radioSet'
        ])).toBeTruthy();
    });

    it('should verify button glyph', function () {
        var btn = bns.button(d3Elem, 'foo-id', 'crown', nullFunc);
        var el = d3Elem.select('#foo-id');
        expect(el.classed('button')).toBeTruthy();
        expect(el.attr('id')).toBe('foo-id');
        expect(el.select('svg')).toBeTruthy();
        expect(el.select('use')).toBeTruthy();
        expect(el.select('use').classed('glyph')).toBeTruthy();
        expect(el.select('use').attr('xlink:href')).toBe('#crown');
    });


    it('should not append button to an undefined div', function () {
        spyOn($log, 'warn');
        expect(bns.button(null, 'id', 'gid', nullFunc)).toBeNull();
        expect($log.warn).toHaveBeenCalledWith('div undefined (button)');
    });

    it('should verify button callback', function () {
        var count = 0;
        function cb() { count++; }
        var btn = bns.button(d3Elem, 'test', 'nothing', cb);
        expect(count).toBe(0);
        d3Elem.select('#test').on('click')();
        expect(count).toBe(1);
    });

    it('should ignore non-function callbacks button', function () {
        var count = 0;
        var btn = bns.button(d3Elem, 'test', 'nothing', 'foo');
        expect(count).toBe(0);
        d3Elem.select('#test').on('click')();
        expect(count).toBe(0);
    });

    it('should not append toggle to an undefined div', function () {
        spyOn($log, 'warn');
        expect(bns.toggle(undefined, 'id', 'gid', false, nullFunc)).toBeNull();
        expect($log.warn).toHaveBeenCalledWith('div undefined (toggle button)');
    });

    it('should verify toggle glyph', function () {
        var tog = bns.toggle(d3Elem, 'foo-id', 'crown', false, nullFunc);
        var el = d3Elem.select('#foo-id');
        expect(el.classed('toggleButton')).toBeTruthy();
        expect(el.attr('id')).toBe('foo-id');
        expect(el.select('svg')).toBeTruthy();
        expect(el.select('use')).toBeTruthy();
        expect(el.select('use').classed('glyph')).toBeTruthy();
        expect(el.select('use').attr('xlink:href')).toBe('#crown');
    });

    it('should toggle the selected state', function () {
        var tog = bns.toggle(d3Elem, 'test', 'nothing');
        expect(tog.selected()).toBe(false);
        tog.toggle();
        expect(tog.selected()).toBe(true);
        tog.toggle();
        expect(tog.selected()).toBe(false);
    });

    it('should set toggle state', function () {
        var tog = bns.toggle(d3Elem, 'test', 'nothing');
        tog.toggle(true);
        expect(tog.selected()).toBe(true);
        tog.toggle();
        expect(tog.selected()).toBe(false);
        tog.toggle('truthy string');
        expect(tog.selected()).toBe(true);
        tog.toggle(null);
        expect(tog.selected()).toBe(false);
        tog.toggle('');
        expect(tog.selected()).toBe(false);
    });

    it('should verity toggle initial state', function () {
        var tog = bns.toggle(d3Elem, 'id', 'gid', true);
        expect(tog.selected()).toBe(true);
        tog = bns.toggle(d3Elem, 'id', 'gid', false);
        expect(tog.selected()).toBe(false);
        tog = bns.toggle(d3Elem, 'id', 'gid', '');
        expect(tog.selected()).toBe(false);
        tog = bns.toggle(d3Elem, 'id', 'gid', 'something');
        expect(tog.selected()).toBe(true);
    });

    it('should not append radio button set to an undefined div', function () {
        spyOn($log, 'warn');
        expect(bns.radioSet(undefined, 'id', [])).toBeNull();
        expect($log.warn).toHaveBeenCalledWith('div undefined (radio button set)');
    });

    it('should not create radio button set from a non-array', function () {
        var rads = {test: 'test'};
        var warning = 'invalid array (radio button set)';

        spyOn($log, 'warn');

        expect(bns.radioSet(d3Elem, 'test', rads)).toBeNull();
        expect($log.warn).toHaveBeenCalledWith(warning);
        rads = 'rads';
        expect(bns.radioSet(d3Elem, 'test', rads)).toBeNull();
        expect($log.warn).toHaveBeenCalledWith(warning);
        rads = {arr: [1, 2, 3]};
        expect(bns.radioSet(d3Elem, 'test', rads)).toBeNull();
        expect($log.warn).toHaveBeenCalledWith(warning);
    });

    // ===================================================================
    // ===================================================================
    // ===================================================================



    it('should not create radio button set from empty array', function () {
        var rads = [];
        spyOn($log, 'warn');
        expect(bns.radioSet(d3Elem, 'test', rads)).toBeNull();
        expect($log.warn).toHaveBeenCalledWith('Cannot create radio button ' +
                                                'set from empty array');
    });

    it('should verify radio button glyph structure', function () {
        var rads = [
            { gid: 'crown', cb: function () {}, tooltip: 'n/a'}
        ], rdiv;

        spyOn($log, 'warn');
        expect(bns.radioSet(d3Elem, 'test', rads)).toBeTruthy();
        expect($log.warn).not.toHaveBeenCalled();

        rdiv = d3Elem.select('div');
        expect(rdiv.classed('rset')).toBe(true);
        expect(rdiv.select('div').classed('rad')).toBe(true);
        expect(rdiv.select('div').classed('tog')).toBe(false);
        expect(rdiv.select('div').attr('id')).toBe('test-0');
        expect(rdiv.select('div').select('svg')).toBeTruthy();
        expect(rdiv.select('use').classed('glyph')).toBeTruthy();
        expect(rdiv.select('use').attr('xlink:href')).toBe('#crown');
    });

    it('should verify more than one radio button glyph was added', function () {
        var rads = [
                { gid: 'crown', cb: function () {}, tooltip: 'n/a'},
                { gid: 'router', cb: function () {}, tooltip: 'n/a'}
        ], rdiv;

        expect(bns.radioSet(d3Elem, 'test', rads)).toBeTruthy();
        rdiv = d3Elem.select('div');
        expect(rdiv.select('#test-0')).toBeTruthy();
        expect(rdiv.select('#test-1')).toBeTruthy();

        expect(rdiv.select('#test-0')
            .select('use')
            .classed('glyph'))
            .toBeTruthy();
        expect(rdiv.select('#test-0')
            .select('use')
            .attr('xlink:href'))
            .toBe('#crown');

        expect(rdiv.select('#test-1')
            .select('use')
            .classed('glyph'))
            .toBeTruthy();
        expect(rdiv.select('#test-1')
            .select('use')
            .attr('xlink:href'))
            .toBe('#router');
    });

    it('should select the correct radio button', function () {
        var count0 = 0,
            count1 = 9;
        function cb0() { count0++; }
        function cb1() { count1++; }

        var rads = [
            { gid: 'crown', cb: cb0, tooltip: 'n/a'},
            { gid: 'router', cb: cb1, tooltip: 'n/a'}
            ],
            rset = bns.radioSet(d3Elem, 'test', rads);
        spyOn($log, 'error');

        expect(rset.selected()).toBe(0);
        expect(count0).toBe(0);
        expect(count1).toBe(9);
        rset.selected(0);
        expect(rset.selected()).toBe(0);
        expect(count0).toBe(0);
        expect(count1).toBe(9);

        rset.selected(1);
        expect(rset.selected()).toBe(1);
        expect(count0).toBe(1);
        expect(count1).toBe(10);

        rset.selected(-1);
        expect($log.error).toHaveBeenCalledWith('Cannot select radio button ' +
                                                'of index -1');
        expect(rset.selected()).toBe(1);
        expect(count0).toBe(1);
        expect(count1).toBe(10);

        rset.selected(66);
        expect($log.error).toHaveBeenCalledWith('Cannot select radio button ' +
                                                'of index 66');
        expect(rset.selected()).toBe(1);
        expect(count0).toBe(1);
        expect(count1).toBe(10);
    });

    // TODO: figure out how to trigger d3 onclick for buttons and toggles

});
