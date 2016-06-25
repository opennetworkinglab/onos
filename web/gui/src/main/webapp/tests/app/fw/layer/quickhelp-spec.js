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
 ONOS GUI -- Layer -- Flash Service - Unit Tests
 */
describe('factory: fw/layer/quickhelp.js', function () {
    var $log, fs, qhs, d3Elem,
        fade = 500,
        noop = function () {},
        mockBindings = {
            globalKeys: {
                slash: [noop, 'Show / hide Quick Help'],
                T: [noop, 'Toggle Theme']
            },
            globalFormat: ['slash', 'T'],
            viewKeys: {
                H: [noop, 'Show / hide hosts'],
                I: [noop, 'Toggle instances panel']
            },
            viewGestures: []
        };

    // list of needed bindings to use in aggregateData
    var neededBindings = [
        'globalKeys', 'globalFormat', 'viewKeys', 'viewGestures'
    ];

    beforeEach(module('onosUtil', 'onosSvg', 'onosLayer'));

    beforeEach(inject(function (_$log_, FnService, QuickHelpService) {
        $log = _$log_;
        fs = FnService;
        qhs = QuickHelpService;

        jasmine.clock().install();
        d3Elem = d3.select('body').append('div').attr('id', 'quickhelp');
        qhs.initQuickHelp();
    }));

    afterEach(function () {
        jasmine.clock().uninstall();
        d3.select('#quickhelp').remove();
    });

    function helpItemSelection() {
        return d3Elem.selectAll('.help');
    }

    it('should define QuickHelpService', function () {
        expect(qhs).toBeDefined();
    });

    it('should define api functions', function () {
        expect(fs.areFunctions(qhs, [
            'initQuickHelp', 'showQuickHelp', 'hideQuickHelp'
        ])).toBeTruthy();
    });

    it('should have no items to start', function () {
        expect(helpItemSelection().size()).toBe(0);
    });

    // === showQuickHelp

    it('should warn if bad bindings are provided', function () {
        var warning =
            'Quickhelp Service: showQuickHelp(), invalid bindings object';
        spyOn($log, 'warn');

        expect(qhs.showQuickHelp()).toBeNull();
        expect($log.warn).toHaveBeenCalledWith(warning);

        expect(qhs.showQuickHelp({})).toBeNull();
        expect($log.warn).toHaveBeenCalledWith(warning);

        expect(qhs.showQuickHelp([1, 2, 3])).toBeNull();
        expect($log.warn).toHaveBeenCalledWith(warning);
    });

    it('should warn if not all needed bindings are provided', function () {
        var warning =
            'Quickhelp Service: showQuickHelp(),' +
            ' needed bindings for help panel not provided:';
        spyOn($log, 'warn');

        expect(qhs.showQuickHelp({
            foo: 'foo', bar: 'bar'
        })).toBeNull();
        expect($log.warn).toHaveBeenCalledWith(warning, neededBindings);

        expect(qhs.showQuickHelp({
            globalKeys: {}
        })).toBeNull();
        expect($log.warn).toHaveBeenCalledWith(warning, neededBindings);

        expect(qhs.showQuickHelp({
            globalKeys: {},
            globalFormat: {},
            viewKeys: {}
        })).toBeNull();
        expect($log.warn).toHaveBeenCalledWith(warning, neededBindings);
    });

    it('should not warn if bindings are provided', function () {
        spyOn($log, 'warn');
        expect(qhs.showQuickHelp(mockBindings)).toBe(undefined);
        expect($log.warn).not.toHaveBeenCalled();
    });

    it('should append an svg', function () {
        var svg = d3Elem.select('svg');
        expect(d3Elem.empty()).toBe(false);
        expect(svg.empty()).toBe(true);

        qhs.showQuickHelp(mockBindings);

        svg = d3Elem.select('svg');
        expect(svg.empty()).toBe(false);
        expect(svg.attr('width')).toBe('100%');
        expect(svg.attr('height')).toBe('80%');
        expect(svg.attr('viewBox')).toBe('-200 0 400 400');
    });

    it('should create the quick help panel', function () {
        var helpItems, g, rect, text, rows;
        qhs.showQuickHelp(mockBindings);

        helpItems = helpItemSelection();
        expect(helpItems.size()).toBe(1);

        g = d3.select('g.help');
        expect(g.attr('opacity')).toBe('0');

        rect = g.select('rect');
        expect(rect.attr('rx')).toBe('8');

        text = g.select('text');
        expect(text.text()).toBe('Quick Help');
        expect(text.classed('title')).toBe(true);
        expect(text.attr('dy')).toBe('1.2em');
        expect(text.attr('transform')).toBeTruthy();

        rows = g.select('g');
        expect(rows.empty()).toBe(false);

        jasmine.clock().tick(fade + 1);
        setTimeout(function () {
            expect(g.attr('opacity')).toBe('1');
        }, fade);

        // TODO: test aggregate data helper function
    });

    it('should show panel with custom fade time', function () {
        var g,
            ctmFade = 200;
        qhs.initQuickHelp({ fade: ctmFade });
        qhs.showQuickHelp(mockBindings);

        g = d3.select('g.help');
        expect(g.attr('opacity')).toBe('0');

        jasmine.clock().tick(ctmFade + 1);
        setTimeout(function () {
            expect(g.attr('opacity')).toBe('1');
        }, ctmFade);
    });

    // === hideQuickHelp

    it('should hide quick help if svg exists', function () {
        var svg;

        expect(qhs.hideQuickHelp()).toBe(false);

        svg = d3.select('#quickhelp')
            .append('svg');
        svg.append('g')
            .classed('help', true)
            .attr('opacity', 1);

        expect(qhs.hideQuickHelp()).toBe(true);

        jasmine.clock().tick(fade + 1);
        setTimeout(function () {
            expect(svg.select('g.help').attr('opacity')).toBe('0');
        }, fade);

        jasmine.clock().tick(20);
        setTimeout(function () {
            expect(svg.empty()).toBe(true);
        }, fade + 20);
    });

    it('should not hide quick help if svg does not exist', function () {
        expect(qhs.hideQuickHelp()).toBe(false);
    });

});

