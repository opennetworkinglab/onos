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
 ONOS GUI -- Layer -- Panel Service - Unit Tests
 */
describe('factory: fw/layer/panel.js', function () {
    var $log, $timeout, fs, ps, d3Elem;

    beforeEach(module('onosLayer'));

    beforeEach(inject(function (_$log_, _$timeout_, FnService, PanelService) {
        $log = _$log_;
        $timeout = _$timeout_;
        fs = FnService;
        ps = PanelService;

        spyOn(fs, 'debugOn').and.returnValue(true);
        d3Elem = d3.select('body').append('div').attr('id', 'floatpanels');
        ps.init();
    }));

    afterEach(function () {
        d3.select('#floatpanels').remove();
        ps.init();
    });

    function floatPanelSelection() {
        return d3Elem.selectAll('.floatpanel');
    }

    it('should define PanelService', function () {
        expect(ps).toBeDefined();
    });

    it('should define api functions', function () {
        expect(fs.areFunctions(ps, [
            'init', 'createPanel', 'destroyPanel'
        ])).toBeTruthy();
    });

    it('should have no panels to start', function () {
        expect(floatPanelSelection().size()).toBe(0);
    });

    it('should log a warning if no ID is given', function () {
        spyOn($log, 'warn');
        var p = ps.createPanel();
        expect(p).toBeNull();
        expect($log.warn).toHaveBeenCalledWith('createPanel: no ID given');
        expect(floatPanelSelection().size()).toBe(0);
    });

    it('should create a default panel', function () {
        spyOn($log, 'warn');
        spyOn($log, 'debug');
        var p = ps.createPanel('foo');
        expect(p).not.toBeNull();
        expect($log.warn).not.toHaveBeenCalled();
        expect(floatPanelSelection().size()).toBe(1);
        expect($log.debug).toHaveBeenCalledWith('creating panel:', 'foo', {
            edge: 'right',
            width: 200,
            margin: 20,
            hideMargin: 20,
            xtnTime: 750,
            fade: true
        });

        // check basic properties
        expect(p.width()).toEqual(200);
        expect(p.isVisible()).toBeFalsy();

        var el = floatPanelSelection();
        expect(el.style('width')).toEqual('200px');
    });

    it('should provide an api of panel functions', function () {
        var p = ps.createPanel('foo');
        expect(fs.areFunctions(p, [
            'show', 'hide', 'toggle', 'empty', 'append',
            'width', 'height', 'bbox', 'isVisible', 'classed', 'el'
        ])).toBeTruthy();
    });

    it('should complain when a duplicate ID is used', function () {
        spyOn($log, 'warn');
        var p = ps.createPanel('foo');
        expect(p).not.toBeNull();
        expect($log.warn).not.toHaveBeenCalled();
        expect(floatPanelSelection().size()).toBe(1);

        var dup = ps.createPanel('foo');
        expect(dup).toBeNull();
        expect($log.warn).toHaveBeenCalledWith('Panel with ID "foo" already exists');
        expect(floatPanelSelection().size()).toBe(1);
    });

    it('should note when there is no panel to destroy', function () {
        spyOn($log, 'debug');
        ps.destroyPanel('bar');
        expect($log.debug).toHaveBeenCalledWith('no panel to destroy:', 'bar');
    });

    it('should destroy the panel', function () {
        spyOn($log, 'debug');
        var p = ps.createPanel('foo');
        expect(floatPanelSelection().size()).toBe(1);

        ps.destroyPanel('foo');
        expect($log.debug).toHaveBeenCalledWith('destroying panel:', 'foo');
        expect(floatPanelSelection().size()).toBe(0);
    });

    it('should allow alternate settings to be given', function () {
        spyOn($log, 'debug');
        var p = ps.createPanel('foo', { width: 250, edge: 'left' });
        expect($log.debug).toHaveBeenCalledWith('creating panel:', 'foo', {
            edge: 'left',
            width: 250,
            margin: 20,
            hideMargin: 20,
            xtnTime: 750,
            fade: true
        });
    });

    it('should show and hide the panel', function () {
        var p = ps.createPanel('foo', {xtnTime:0});
        expect(p.isVisible()).toBeFalsy();

        p.show();
        expect(p.isVisible()).toBeTruthy();

        p.hide();
        expect(p.isVisible()).toBeFalsy();
    });

    it('should append content to the panel', function () {
        var p = ps.createPanel('foo');
        var span = p.append('span').attr('id', 'thisIsMySpan');

        expect(floatPanelSelection().selectAll('span').attr('id'))
            .toEqual('thisIsMySpan');
    });

    it('should remove content on empty', function () {
        var p = ps.createPanel('voop');
        p.append('span');
        p.append('span');
        p.append('span');
        expect(floatPanelSelection().selectAll('span').size()).toEqual(3);

        p.empty();
        expect(floatPanelSelection().selectAll('span').size()).toEqual(0);
        expect(floatPanelSelection().html()).toEqual('');
    });

    it('should allow programmatic setting of width', function () {
        var p = ps.createPanel('whatcha', {width:234});
        expect(floatPanelSelection().style('width')).toEqual('234px');
        expect(p.width()).toEqual(234);

        p.width(345);
        expect(floatPanelSelection().style('width')).toEqual('345px');
        expect(p.width()).toEqual(345);
    });

    it('should allow programmatic setting of height', function () {
        var p = ps.createPanel('ciao', {height:50});
        expect(floatPanelSelection().style('height')).toEqual('50px');
        expect(p.height()).toEqual(50);

        p.height(100);
        expect(floatPanelSelection().style('height')).toEqual('100px');
        expect(p.height()).toEqual(100);
    });
});
