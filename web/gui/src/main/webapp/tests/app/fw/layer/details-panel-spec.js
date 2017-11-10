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
 ONOS GUI -- Layer -- Veil Service - Unit Tests
 */

describe('factory: fw/layer/details-panel.js', function () {
    var $log, $timeout, fs, ps, dps, d3Elem, $scope;

    beforeEach(module('onosLayer', 'onosMast', 'onosRemote', 'onosSvg'));

    beforeEach(inject(function (_$log_, _$timeout_, $rootScope, FnService, PanelService, DetailsPanelService) {
        $log = _$log_;
        $timeout = _$timeout_;
        $scope = $rootScope.$new();
        fs = FnService;
        dps = DetailsPanelService;
        ps = PanelService;

        spyOn(fs, 'debugOn').and.returnValue(true);
        d3Elem = d3.select('body').append('div').attr('id', 'floatpanels');
        d3.select('body').append('div').attr('class', 'tabular-header').style({ height: 10 });
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
        expect(fs.areFunctions(dps, [
            'create', 'setResponse',
            'addContainers', 'addCloseButton', 'addHeading', 'addPropsList',
            'container', 'top', 'bottom', 'select',
            'empty', 'hide', 'destroy',
        ])).toBeTruthy();
    });

    it('should create a default panel', function () {
        var p = dps.create('foo', { scope: $scope });
        expect(p).toBeDefined();
    });

    it('should add response handler', function () {
        var p = dps.create('foo', { scope: $scope });
        dps.setResponse('responseName', function () {});
    });

    it('should add content', function () {
        var p = dps.create('foo', { scope: $scope });

        dps.addContainers();
        dps.addCloseButton(function () {});

        expect(dps.container()).toBeDefined();
        expect(dps.top()).toBeDefined();
        expect(dps.bottom()).toBeDefined();

        var top = dps.top();
        top.append('div').classed('top-content', true);
        dps.addHeading('icon', false);
        dps.addPropsList(dps.select('.top-content'), { id: 'foo', name: 'bar' });

        // TODO: props have been added to the table
        // TODO: close button has been created
        // TODO: heading placeholder has been added
    });

    it('should add editable header', function () {
        var p = dps.create('foo', { scope: $scope });
        dps.addHeading('icon', true);
        // TODO: editable heading placeholder has been added
    });

    it('should add unbind on destroy', function () {
        var p = dps.create('foo', { scope: $scope });
        dps.setResponse('responseName', function () {});
        dps.destroy();
    });

});