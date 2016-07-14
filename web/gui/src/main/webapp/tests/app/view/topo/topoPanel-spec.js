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
 ONOS GUI -- Topo View -- Topo Panel Service - Unit Tests
 */
describe('factory: view/topo/topoPanel.js', function() {
    var $log, fs, tps, bns, ps, panelLayer;

    var mockWindow = {
        innerWidth: 300,
        innerHeight: 100,
        navigator: {
            userAgent: 'defaultUA'
        },
        on: function () {},
        addEventListener: function () {}
    };

    beforeEach(module('ovTopo', 'onosUtil', 'onosLayer', 'ngRoute', 'onosNav',
        'onosWidget', 'onosMast'));

    beforeEach(function () {
        module(function ($provide) {
            $provide.value('$window', mockWindow);
        });
    });

    beforeEach(inject(function (_$log_, FnService,
                                TopoPanelService, ButtonService, PanelService) {
        $log = _$log_;
        fs = FnService;
        tps = TopoPanelService;
        bns = ButtonService;
        ps = PanelService;
        panelLayer = d3.select('body').append('div').attr('id', 'floatpanels');
    }));

    afterEach(function () {
        panelLayer.remove();
    });

    it('should define TopoPanelService', function () {
        expect(tps).toBeDefined();
    });

    it('should define api functions', function () {
        expect(fs.areFunctions(tps, [
            'initPanels',
            'destroyPanels',
            'createTopoPanel',

            'showSummary',
            'toggleSummary',
            'hideSummary',

            'toggleUseDetailsFlag',
            'displaySingle',
            'displayMulti',
            'displayLink',
            'displayNothing',
            'displaySomething',
            'addAction',

            'detailVisible',
            'summaryVisible'
        ])).toBeTruthy();
    });

    // === topoPanel api ------------------

    it('should define topoPanel api functions', function () {
        var panel = tps.createTopoPanel('foo');
        expect(fs.areFunctions(panel, [
            'panel', 'setup', 'destroy',
            'appendHeader', 'appendBody', 'appendFooter',
            'adjustHeight'
        ])).toBeTruthy();
        panel.destroy();
    });

    it('should allow you to get panel', function () {
        var panel = tps.createTopoPanel('foo');
        expect(panel.panel()).toBeTruthy();
        panel.destroy();
    });

    it('should set up panel', function () {
        var p = tps.createTopoPanel('foo'),
            h, b, f;
        p.setup();
        expect(p.panel().el().selectAll('div').size()).toBe(3);

        h = p.panel().el().select('.header');
        expect(h.empty()).toBe(false);
        b = p.panel().el().select('.body');
        expect(b.empty()).toBe(false);
        f = p.panel().el().select('.footer');
        expect(f.empty()).toBe(false);
        p.destroy();
    });

    it('should destroy panel', function () {
        spyOn(ps, 'destroyPanel').and.callThrough();
        var p = tps.createTopoPanel('foo');
        p.destroy();
        expect(ps.destroyPanel).toHaveBeenCalledWith('foo');
    });

    it('should append to panel', function () {
        var p = tps.createTopoPanel('foo');
        p.setup();
        p.appendHeader('div').attr('id', 'header-div');
        expect(p.panel().el().select('#header-div').empty()).toBe(false);
        p.appendBody('p').attr('id', 'body-paragraph');
        expect(p.panel().el().select('#body-paragraph').empty()).toBe(false);
        p.appendFooter('svg').attr('id', 'footer-svg');
        expect(p.panel().el().select('#footer-svg').empty()).toBe(false);
        p.destroy();
    });

    it('should warn if fromTop not given, adjustHeight', function () {
        spyOn($log, 'warn');
        var p = tps.createTopoPanel('foo');
        p.adjustHeight();
        expect($log.warn).toHaveBeenCalledWith(
            'adjustHeight: height from top of page not given'
        );
        p.destroy();
    });

    xit('should warn if panel is not setup/defined, adjustHeight', function () {
        spyOn($log, 'warn');
        var p = tps.createTopoPanel('foo');
        p.adjustHeight(50);
        expect($log.warn).toHaveBeenCalledWith(
            'adjustHeight: panel contents are not defined'
        );
        p.destroy();
    });

    // TODO: test adjustHeight height adjustment

    // TODO: more tests...
});
