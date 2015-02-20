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
    var $log, fs, bns, gs,
        d3Elem;

    beforeEach(module('onosWidget', 'onosSvg'));

    beforeEach(inject(function (_$log_, FnService,
                                ButtonService, GlyphService) {
        $log = _$log_;
        fs = FnService;
        bns = ButtonService;
        gs = GlyphService;
    }));

    beforeEach(function () {
        gs.init();
        d3Elem = d3.select('body').append('div').attr('id', 'testToolbar');
    });

    afterEach(function () {
        d3.select('#testToolbar').remove();
    });

    it('should define ButtonService', function () {
        expect(bns).toBeDefined();
    });

    it('should define api functions', function () {
        expect(fs.areFunctions(bns, [
            'button', 'toggle', 'radioSet'
        ])).toBeTruthy();
    });

    it('should verify button glyph', function () {
        var btn = bns.button(d3Elem, 'tbar0-btn-0', 'crown', function () {});
        expect((btn.el).classed('btn')).toBeTruthy();
        expect((btn.el).attr('id')).toBe('tbar0-btn-0');
        expect((btn.el).select('svg')).toBeTruthy();
        expect((btn.el).select('use')).toBeTruthy();
        expect((btn.el).select('use').classed('glyph')).toBeTruthy();
        expect((btn.el).select('use').attr('xlink:href')).toBe('#crown');
    });

    it('should not append button to an undefined div', function () {
        spyOn($log, 'warn');
        expect(bns.button(undefined, 'id', 'gid', function () {})).toBeNull();
        expect($log.warn).toHaveBeenCalledWith('Button cannot append to div');
    });

    it('should verify button callback', function () {
        var count = 0;
        function cb() { count++; }
        var btn = bns.button(d3Elem, 'test', 'nothing', cb);
        expect(count).toBe(0);
        btn.click();
        expect(count).toBe(1);
    });

    it('should ignore non-function callbacks button', function () {
        var count = 0;
        var btn = bns.button(d3Elem, 'test', 'nothing', 'foo');
        expect(count).toBe(0);
        btn.click();
        expect(count).toBe(0);
    });

    it('should verify toggle glyph', function () {
        var tog = bns.toggle(d3Elem, 'tbar0-tog-0', 'crown', function () {});
        expect((tog.el).classed('tog')).toBeTruthy();
        expect((tog.el).attr('id')).toBe('tbar0-tog-0');
        expect((tog.el).select('svg')).toBeTruthy();
        expect((tog.el).select('use')).toBeTruthy();
        expect((tog.el).select('use').classed('glyph')).toBeTruthy();
        expect((tog.el).select('use').attr('xlink:href')).toBe('#crown');
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

    it('should not append toggle to an undefined div', function () {
        spyOn($log, 'warn');
        expect(bns.toggle(undefined, 'id', 'gid', function () {})).toBeNull();
        expect($log.warn).toHaveBeenCalledWith('Toggle cannot append to div');
    });

});
