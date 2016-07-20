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
 ONOS GUI -- Topo View -- Topo Toolbar Service - Unit Tests
 */
describe('factory: view/topo/topoToolbar.js', function() {
    var $log, fs, ttbs, prefs, ps,
        d3Elem;

    beforeEach(module('ovTopo', 'onosUtil', 'onosLayer', 'ngRoute', 'onosNav',
        'onosWidget', 'onosMast'));

    beforeEach(inject(function (_$log_, FnService,
                                TopoToolbarService, PanelService, PrefsService) {
        $log = _$log_;
        fs = FnService;
        ttbs = TopoToolbarService;
        prefs = PrefsService;
        ps = PanelService;
        d3Elem = d3.select('body').append('div').attr('id', 'floatpanels');
        ps.init();
    }));

    it('should define TopoToolbarService', function () {
        expect(ttbs).toBeDefined();
    });

    xit('should define api functions', function () {
        // FIXME
        // areFunctions check that each key of the object is a fn, what if it is a string or whatever?
        expect(fs.areFunctions(ttbs, [
            'init', 'createToolbar', 'destroyToolbar',
            'keyListener', 'toggleToolbar', 'setDefaultOverlay',
            'fnkey'
        ])).toBeTruthy();
    });

    // NOTE: topoToolbar relies too much on topo's closure variables
    // to adequately test it

});