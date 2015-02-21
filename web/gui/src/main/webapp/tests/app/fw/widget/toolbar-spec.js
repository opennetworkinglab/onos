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
        d3Elem = d3.select('body').append('div').attr('id', 'floatpanels');
        tbs.init();
        ps.init();
    });

    afterEach(function () {
        d3.select('#floatpanels').remove();
        tbs.init();
        ps.init();
    });

    it('should define ToolbarService', function () {
        expect(tbs).toBeDefined();
    });

    it('should define api functions', function () {
        expect(fs.areFunctions(tbs, [
            'init', 'makeButton', 'makeToggle', 'makeRadio', 'separator',
            'createToolbar', 'createToolbar1', 'destroyToolbar'
        ])).toBeTruthy();
    });

    function toolbarSelection() {
        return d3Elem.selectAll('.toolbar');
    }

    it('should have no toolbars to start', function () {
        expect(toolbarSelection().size()).toBe(0);
    });

    it('should log a warning if no ID is given', function () {
        spyOn($log, 'warn');
        var tbar = tbs.createToolbar();
        expect(tbar).toBeNull();
        expect($log.warn).toHaveBeenCalledWith('createToolbar: no ID given');
        expect(toolbarSelection().size()).toBe(0);
    });

    it('should log a warning if no tools are given', function () {
        spyOn($log, 'warn');
        var tbar = tbs.createToolbar(true);
        expect(tbar).toBeNull();
        expect($log.warn).toHaveBeenCalledWith('createToolbar: no tools given');
        expect(toolbarSelection().size()).toBe(0);
    });

    it('should log a warning if tools are not an array', function () {
        spyOn($log, 'warn');
        var tbar = tbs.createToolbar(true, {});
        expect(tbar).toBeNull();
        expect($log.warn).toHaveBeenCalledWith('createToolbar: no tools given');
        expect(toolbarSelection().size()).toBe(0);
    });

    it('should log a warning if tools array is empty', function () {
        spyOn($log, 'warn');
        var tbar = tbs.createToolbar(true, []);
        expect(tbar).toBeNull();
        expect($log.warn).toHaveBeenCalledWith('createToolbar: no tools given');
        expect(toolbarSelection().size()).toBe(0);
    });

    it("should verify makeButton's returned object", function () {
        var button = tbs.makeButton('foo', 'glyph-bar', function () {});

        expect(button.t).toBe('btn');
        expect(button.id).toBe('foo');
        expect(button.gid).toBe('glyph-bar');
        expect(fs.isF(button.cb)).toBeTruthy();
    });

    it("should verify makeToggle's returned object", function () {
        var toggle = tbs.makeToggle('foo', 'glyph-bar', function () {});

        expect(toggle.t).toBe('tog');
        expect(toggle.id).toBe('foo');
        expect(toggle.gid).toBe('glyph-bar');
        expect(fs.isF(toggle.cb)).toBeTruthy();
    });

    it("should verify makeRadio's returned object", function () {
        var rFoo0 = tbs.makeRadio('foo', 'glyph-foo0', function () {});
        var rFoo1 = tbs.makeRadio('foo', 'glyph-foo1', function () {});
        var rFoo2 = tbs.makeRadio('foo', 'glyph-foo2', function () {});
        var rBar0 = tbs.makeRadio('bar', 'glyph-bar0', function () {});
        var rBar1 = tbs.makeRadio('bar', 'glyph-bar1', function () {});
        var rFoo3 = tbs.makeRadio('foo', 'glyph-foo3', function () {});

        expect(rFoo0.t).toBe('rad');
        expect(rFoo0.id).toBe('foo-0');
        expect(rFoo0.rid).toBe('0');
        expect(rFoo0.gid).toBe('glyph-foo0');
        expect(fs.isF(rFoo0.cb)).toBeTruthy();

        expect(rFoo1.t).toBe('rad');
        expect(rFoo1.id).toBe('foo-1');
        expect(rFoo1.rid).toBe('1');
        expect(rFoo1.gid).toBe('glyph-foo1');
        expect(fs.isF(rFoo1.cb)).toBeTruthy();

        expect(rFoo2.t).toBe('rad');
        expect(rFoo2.id).toBe('foo-2');
        expect(rFoo2.rid).toBe('2');
        expect(rFoo2.gid).toBe('glyph-foo2');
        expect(fs.isF(rFoo2.cb)).toBeTruthy();

        expect(rFoo3.t).toBe('rad');
        expect(rFoo3.id).toBe('foo-3');
        expect(rFoo3.rid).toBe('3');
        expect(rFoo3.gid).toBe('glyph-foo3');
        expect(fs.isF(rFoo3.cb)).toBeTruthy();

        expect(rBar0.t).toBe('rad');
        expect(rBar0.id).toBe('bar-0');
        expect(rBar0.rid).toBe('0');
        expect(rBar0.gid).toBe('glyph-bar0');
        expect(fs.isF(rBar0.cb)).toBeTruthy();

        expect(rBar1.t).toBe('rad');
        expect(rBar1.id).toBe('bar-1');
        expect(rBar1.rid).toBe('1');
        expect(rBar1.gid).toBe('glyph-bar1');
        expect(fs.isF(rBar1.cb)).toBeTruthy();
    });

    it("should verify separator's returned object", function () {
        var separator = tbs.separator();
        expect(separator.t).toBe('sep');
    });

    it('should log a warning if btn id is already in use', function () {
        var tools = [
            tbs.makeButton('id0', 'glyph-id0', function () {}),
            tbs.makeButton('id0', 'glyph-id0', function () {})
        ];

        spyOn($log, 'warn');
        var tbar = tbs.createToolbar('someId', tools);
        expect(tbar).toBeNull();
        expect($log.warn).toHaveBeenCalledWith('createToolbar: item with id ' +
                                                'id0 already exists');
        expect(toolbarSelection().size()).toBe(0);
    });

    it('should log a warning if tog id is already in use', function () {
        var tools = [
            tbs.makeToggle('id0', 'glyph-id0', function () {}),
            tbs.makeToggle('id1', 'glyph-id1', function () {}),
            tbs.makeToggle('id0', 'glyph-id0', function () {})
        ];

        spyOn($log, 'warn');
        var tbar = tbs.createToolbar('someId', tools);
        expect(tbar).toBeNull();
        expect($log.warn).toHaveBeenCalledWith('createToolbar: item with id ' +
                                                'id0 already exists');
        expect(toolbarSelection().size()).toBe(0);
    });

    it('should create a toolbar', function () {
        // need to create a button so it does not throw errors
        var tools = [
            tbs.makeButton('btn0', 'glyph0', function () {})
        ];
        spyOn($log, 'warn');
        var tbar = tbs.createToolbar('test', tools);
        expect($log.warn).not.toHaveBeenCalled();
        expect(toolbarSelection().size()).toBe(1);
    });

    it('should test multiple separators in a row', function () {
        var tools = [
            tbs.separator(),
            tbs.separator(),
            tbs.separator()
        ];
        spyOn($log, 'warn');
        var tbar = tbs.createToolbar('test', tools);
        expect($log.warn).not.toHaveBeenCalled();
        expect(toolbarSelection().size()).toBe(1);
    });

    it('should create a button div', function () {
        var tools = [
            tbs.makeButton('btn0', 'glyph0', function () {})
        ];
        spyOn($log, 'warn');
        var tbar = tbs.createToolbar('test', tools);
        expect($log.warn).not.toHaveBeenCalled();
        expect(toolbarSelection().size()).toBe(1);

        expect(d3Elem.select('.btn')).toBeTruthy();
    });

    it('should create a toggle div', function () {
        var tools = [
            tbs.makeToggle('tog0', 'glyph0', function () {})
        ];
        spyOn($log, 'warn');
        var tbar = tbs.createToolbar('test', tools);
        expect($log.warn).not.toHaveBeenCalled();
        expect(toolbarSelection().size()).toBe(1);

        expect(d3Elem.select('.tog')).toBeTruthy();
    });

    it('should create a radio btn div', function () {
        var tools = [
            tbs.makeRadio('rad0', 'glyph0', function () {})
        ];
        spyOn($log, 'warn');
        var tbar = tbs.createToolbar('test', tools);
        expect($log.warn).not.toHaveBeenCalled();
        expect(toolbarSelection().size()).toBe(1);

        expect(d3Elem.select('.rad')).toBeTruthy();
    });


    it('should create a separator div', function () {
        var tools = [
            tbs.separator()
        ];
        tbs.createToolbar('test', tools);

        var sepDiv = d3Elem.select('.sep');
        expect(sepDiv).toBeTruthy();
        expect(sepDiv.style('width')).toBe('2px');
        expect(sepDiv.style('border-width')).toBe('1px');
        expect(sepDiv.style('border-style')).toBe('solid');
    });

    // ==== new Toolbar Unit tests --------------------------------------------

    it('should warn if createToolbar id is invalid', function () {
        spyOn($log, 'warn');
        expect(tbs.createToolbar1()).toBeNull();
        expect($log.warn).toHaveBeenCalledWith('createToolbar: no ID given');

        expect(tbs.createToolbar1('test')).toBeTruthy();
        expect(tbs.createToolbar1('test')).toBeNull();
        expect($log.warn).toHaveBeenCalledWith('createToolbar: ID already exists');
    });

    it('should create an unpopulated toolbar', function () {
        spyOn($log, 'warn');
        expect(tbs.createToolbar1('test')).toBeTruthy();
        expect($log.warn).not.toHaveBeenCalled();
    });

    it('should create a button', function () {
        spyOn($log, 'warn');
        var toolbar = tbs.createToolbar1('test'),
            btn = toolbar.addButton1('btn0', 'gid', function () {});
        expect(btn).not.toBeNull();
        expect(btn.id).toBe('tbar-test-btn0');
        expect($log.warn).not.toHaveBeenCalled();
    });

    it('should not create a button with a duplicate id', function () {
        spyOn($log, 'warn');
        var toolbar = tbs.createToolbar1('test'),
            btn = toolbar.addButton1('btn0', 'gid', function () {}),
            btn1 = toolbar.addButton1('btn0', 'gid', function () {});
        expect(btn).not.toBeNull();
        expect(btn.id).toBe('tbar-test-btn0');
        expect($log.warn).toHaveBeenCalledWith('addButton: ID already exists');
        expect(btn1).toBeNull();
    });

    it('should create a toggle', function () {
        spyOn($log, 'warn');
        var toolbar = tbs.createToolbar1('test'),
            tog = toolbar.addButton1('tog0', 'gid', false, function () {});
        expect(tog).not.toBeNull();
        expect(tog.id).toBe('tbar-test-tog0');
        expect($log.warn).not.toHaveBeenCalled();
    });

    it('should not create a toggle with a duplicate id', function () {
        spyOn($log, 'warn');
        var toolbar = tbs.createToolbar1('test'),
            tog = toolbar.addToggle1('tog0', 'gid', false, function () {}),
            tog1 = toolbar.addToggle1('tog0', 'gid', true, function () {});
        expect(tog).not.toBeNull();
        expect(tog.id).toBe('tbar-test-tog0');
        expect($log.warn).toHaveBeenCalledWith('addToggle: ID already exists');
        expect(tog1).toBeNull();
    });


    it('should create a radio button set', function () {
        spyOn($log, 'warn');
        var toolbar = tbs.createToolbar1('test'),
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

    //it('should not append to a destroyed toolbar', function () {
    //    spyOn($log, 'warn');
    //    var toolbar = tbs.createToolbar1('test');
    //    expect(toolbar).not.toBeNull();
    //    tbs.destroyToolbar('tbar-test');
    //    expect(toolbar.addButton1('btn', 'gid', function () {})).toBeNull();
    //    expect($log.warn).toHaveBeenCalledWith('Button cannot append to div');
    //});

});
