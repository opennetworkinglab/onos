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
 ONOS GUI -- Widget -- Toolbar Service - Unit Tests
 */
describe('factory: fw/widget/toolbar.js', function () {
    var $log, fs, tbs, ps, bns, is;

    beforeEach(module('onosWidget', 'onosUtil', 'onosLayer', 'onosSvg'));

    beforeEach(inject(function (_$log_, FnService, ToolbarService,
                                PanelService, ButtonService, IconService) {
        $log = _$log_;
        fs = FnService;
        tbs = ToolbarService;
        ps = PanelService;
        bns = ButtonService;
        is = IconService;
    }));

    beforeEach(function () {
        // panel service expects #floatpanels div into which panels are placed
        d3.select('body').append('div').attr('id', 'floatpanels');
        tbs.init();
        ps.init();
    });

    afterEach(function () {
        tbs.init();
        ps.init();
        d3.select('#floatpanels').remove();
    });

    function nullFunc() { }

    it('should define ToolbarService', function () {
        expect(tbs).toBeDefined();
    });

    it('should define api functions', function () {
        expect(fs.areFunctions(tbs, [
            'init',
            'createToolbar', 'destroyToolbar'
        ])).toBeTruthy();
    });

    it('should warn when no id is given', function () {
        spyOn($log, 'warn');
        expect(tbs.createToolbar()).toBeNull();
        expect($log.warn).toHaveBeenCalledWith('createToolbar: ' +
                                    'no ID given: [undefined]');
    });

    it('should warn when a duplicate id is given', function () {
        spyOn($log, 'warn');
        expect(tbs.createToolbar('test')).toBeTruthy();
        expect(tbs.createToolbar('test')).toBeNull();
        expect($log.warn).toHaveBeenCalledWith('createToolbar: ' +
                                    'duplicate ID given: [test]');
    });

    it('should verify the toolbar arrow div exists', function () {
        tbs.createToolbar('test');

        // NOTE: toolbar service prefixes id with 'toolbar-'
        var tbar = d3.select('#toolbar-test'),
            arrow = tbar.select('.tbar-arrow');

        expect(arrow.size()).toBe(1);
        expect(arrow.select('svg').size()).toBe(1);
        expect(arrow.select('svg').select('g').select('use')
            .attr('xlink:href')).toEqual('#triangleUp');
    });


    it('should create a button', function () {
        spyOn($log, 'warn');
        var toolbar = tbs.createToolbar('foo'),
            btn = toolbar.addButton('btn0', 'gid');
        expect(btn).not.toBeNull();
        expect(btn.id).toBe('toolbar-foo-btn0');
        expect($log.warn).not.toHaveBeenCalled();
    });

    it('should not create an item with a duplicate id', function () {
        spyOn($log, 'warn');
        var toolbar = tbs.createToolbar('foo'),
            btn = toolbar.addButton('btn0', 'gid'),
            dup;
        expect(btn).not.toBeNull();
        expect(btn.id).toBe('toolbar-foo-btn0');

        dup = toolbar.addButton('btn0', 'gid');
        expect($log.warn).toHaveBeenCalledWith('addButton: duplicate ID:', 'btn0');
        expect(dup).toBeNull();

        dup = toolbar.addToggle('btn0', 'gid');
        expect($log.warn).toHaveBeenCalledWith('addToggle: duplicate ID:', 'btn0');
        expect(dup).toBeNull();

        dup = toolbar.addRadioSet('btn0', []);
        expect($log.warn).toHaveBeenCalledWith('addRadioSet: duplicate ID:', 'btn0');
        expect(dup).toBeNull();
    });

    it('should create a toggle', function () {
        spyOn($log, 'warn');
        var toolbar = tbs.createToolbar('foo'),
            tog = toolbar.addButton('tog0', 'gid');
        expect(tog).not.toBeNull();
        expect(tog.id).toBe('toolbar-foo-tog0');
        expect($log.warn).not.toHaveBeenCalled();
    });

    it('should create a radio button set', function () {
        spyOn($log, 'warn');
        var toolbar = tbs.createToolbar('foo'),
            rset = [
                { gid: 'crown', cb: nullFunc, tooltip: 'A Crown' },
                { gid: 'bird', cb: nullFunc, tooltip: 'A Bird' }
            ],
            rad = toolbar.addRadioSet('rad0', rset);
        expect(rad).not.toBeNull();
        expect(rad.selectedIndex()).toBe(0);
        expect($log.warn).not.toHaveBeenCalled();
    });

    it('should create a separator div', function () {
        spyOn($log, 'warn');
        var toolbar = tbs.createToolbar('foo'),
            tbar = d3.select('#toolbar-foo');

        toolbar.addSeparator();
        expect($log.warn).not.toHaveBeenCalled();

        expect(tbar.select('.separator').size()).toBe(1);
    });

    it('should add another row of buttons', function () {
        var toolbar = tbs.createToolbar('foo'),
            tbar = d3.select('#toolbar-foo'),
            rows;
        toolbar.addButton('btn0', 'gid');
        toolbar.addRow();
        toolbar.addButton('btn1', 'gid');

        rows = tbar.selectAll('.tbar-row');
        expect(rows.size()).toBe(2);
        rows.each(function (d, i) {
            expect(d3.select(this)
                .select('div')
                .attr('id','toolbar-foo-btn' + i)
                .empty())
                .toBe(false);
        });
    });

    it('should not add a row if current row is empty', function () {
        var toolbar = tbs.createToolbar('foo');
        expect(toolbar.addRow()).toBeNull();
        toolbar.addButton('btn0', 'gid');
        expect(toolbar.addRow()).not.toBeNull();
        expect(toolbar.addRow()).toBeNull();
    });

});
