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
 ONOS GUI -- Widget -- Toolbar Service - Unit Tests
 */
describe('factory: fw/widget/toolbar.js', function () {
    var $log, fs, tbs, ps, bns, is,
        d3Elem;

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

    // TODO: figure out solution for calling tests with new info instead of calling init

    beforeEach(function () {
        d3Elem = d3.select('body').append('div').attr('id', 'testToolbar');
        tbs.init();
        ps.init();
    });

    afterEach(function () {
        d3.select('#testToolbar').remove();
        tbs.init();
        ps.init();
    });

    it('should define ToolbarService', function () {
        expect(tbs).toBeDefined();
    });

    it('should define api functions', function () {
        expect(fs.areFunctions(tbs, [
            'init',
            'createToolbar', 'destroyToolbar'
        ])).toBeTruthy();
    });

    it('should warn if createToolbar id is invalid', function () {
        spyOn($log, 'warn');
        expect(tbs.createToolbar()).toBeNull();
        expect($log.warn).toHaveBeenCalledWith('createToolbar: ' +
                                                'no ID given: [undefined]');

        expect(tbs.createToolbar('test')).toBeTruthy();
        expect(tbs.createToolbar('test')).toBeNull();
        expect($log.warn).toHaveBeenCalledWith('createToolbar: ' +
                                            'duplicate ID given: [undefined]');
    });

    it('should create an unpopulated toolbar', function () {
        spyOn($log, 'warn');
        expect(tbs.createToolbar('test')).toBeTruthy();
        expect($log.warn).not.toHaveBeenCalled();
    });

    it('should verify the toolbar arrow div exists', function () {
        tbs.createToolbar('test');

        var arrow = d3Elem.select('.tbarArrow');
        expect(arrow).toBeTruthy();
        expect(arrow.select('svg')).toBeTruthy();
        expect(arrow.select('svg').select('g')
            .classed('tableColSortAsc')).toBeTruthy();
    });

    it('should create a button', function () {
        spyOn($log, 'warn');
        var toolbar = tbs.createToolbar('test'),
            btn = toolbar.addButton('btn0', 'gid', function () {});
        expect(btn).not.toBeNull();
        expect(btn.id).toBe('tbar-test-btn0');
        expect($log.warn).not.toHaveBeenCalled();
    });

    it('should not create a button with a duplicate id', function () {
        spyOn($log, 'warn');
        var toolbar = tbs.createToolbar('test'),
            btn = toolbar.addButton('btn0', 'gid', function () {}),
            btn1 = toolbar.addButton('btn0', 'gid', function () {});
        expect(btn).not.toBeNull();
        expect(btn.id).toBe('tbar-test-btn0');
        expect($log.warn).toHaveBeenCalledWith('addButton: ID already exists');
        expect(btn1).toBeNull();
    });

    it('should create a toggle', function () {
        spyOn($log, 'warn');
        var toolbar = tbs.createToolbar('test'),
            tog = toolbar.addButton('tog0', 'gid', false, function () {});
        expect(tog).not.toBeNull();
        expect(tog.id).toBe('tbar-test-tog0');
        expect($log.warn).not.toHaveBeenCalled();
    });

    it('should not create a toggle with a duplicate id', function () {
        spyOn($log, 'warn');
        var toolbar = tbs.createToolbar('test'),
            tog = toolbar.addToggle('tog0', 'gid', false, function () {}),
            tog1 = toolbar.addToggle('tog0', 'gid', true, function () {});
        expect(tog).not.toBeNull();
        expect(tog.id).toBe('tbar-test-tog0');
        expect($log.warn).toHaveBeenCalledWith('addToggle: ID already exists');
        expect(tog1).toBeNull();
    });


    it('should create a radio button set', function () {
        spyOn($log, 'warn');
        var toolbar = tbs.createToolbar('test'),
            rset = [
                { gid: 'crown', cb: function () {}, tooltip: 'nothing' },
                { gid: 'bird', cb: function () {}, tooltip: 'nothing' }
            ],
            rad = toolbar.addRadioSet('rad0', rset);
        expect(rad).not.toBeNull();
        expect(rad.rads[0].id).toBe('tbar-test-rad0-0');
        expect(rad.rads[1].id).toBe('tbar-test-rad0-1');
        expect($log.warn).not.toHaveBeenCalled();
    });

    it('should create a separator div', function () {
        spyOn($log, 'warn');
        var toolbar = tbs.createToolbar('test'),
            sep = toolbar.addSeparator();
        expect(sep).not.toBeNull();
        expect($log.warn).not.toHaveBeenCalled();

        expect(d3Elem.select('.sep')).toBeTruthy();
        expect(d3Elem.select('.sep').style('width')).toBe('2px');
    });

    it('should not append to a destroyed toolbar', function () {
        spyOn($log, 'warn');
        var toolbar = tbs.createToolbar('test');
        expect(toolbar).not.toBeNull();
        tbs.destroyToolbar('tbar-test');
        expect(toolbar.addButton('btn', 'gid', function () {})).toBeNull();
        expect($log.warn).toHaveBeenCalledWith('Button cannot append to div');
    });

});
