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
 ONOS GUI -- Topology Toolbar Module.
 Functions for creating and interacting with the toolbar.
 */

(function () {
    'use strict';

    // injected references
    var $log, fs, tbs, ps, tov, api;

    // internal state
    var toolbar, keyData, cachedState;

    // constants
    var name = 'topo-tbar',
        cooktag = 'topo_prefs';

    // key to button mapping data
    var k2b = {
        O: { id: 'summary-tog', gid: 'summary', isel: true},
        I: { id: 'instance-tog', gid: 'uiAttached', isel: true },
        D: { id: 'details-tog', gid: 'details', isel: true },

        H: { id: 'hosts-tog', gid: 'endstation', isel: false },
        M: { id: 'offline-tog', gid: 'switch', isel: true },
        P: { id: 'ports-tog', gid: 'ports', isel: true },
        B: { id: 'bkgrnd-tog', gid: 'map', isel: false },
        S: { id: 'sprite-tog', gid: 'cloud', isel: false },

        //X: { id: 'nodelock-tog', gid: 'lock', isel: false },
        Z: { id: 'oblique-tog', gid: 'oblique', isel: false },
        N: { id: 'filters-btn', gid: 'filters' },
        L: { id: 'cycleLabels-btn', gid: 'cycleLabels' },
        R: { id: 'resetZoom-btn', gid: 'resetZoom' },

        E: { id: 'eqMaster-btn', gid: 'eqMaster' },

        V: { id: 'relatedIntents-btn', gid: 'relatedIntents' },
        leftArrow: { id: 'prevIntent-btn', gid: 'prevIntent' },
        rightArrow: { id: 'nextIntent-btn', gid: 'nextIntent' },
        W: { id: 'intentTraffic-btn', gid: 'intentTraffic' },
        A: { id: 'allTraffic-btn', gid: 'allTraffic' },
        F: { id: 'flows-btn', gid: 'flows' }
    };

    // initial toggle state: default settings and tag to key mapping
    var defaultPrefsState = {
            summary: 1,
            insts: 1,
            detail: 1,
            hosts: 0,
            offdev: 1,
            porthl: 1,
            bg: 0,
            spr: 0,
            toolbar: 0
        },
        prefsMap = {
            summary: 'O',
            insts: 'I',
            detail: 'D',
            hosts: 'H',
            offdev: 'M',
            porthl: 'P',
            bg: 'B',
            spr: 'S'
            // NOTE: toolbar state is handled separately
        };

    function init(_api_) {
        api = _api_;

        // retrieve initial toggle button settings from user prefs
        setInitToggleState();
    }

    function topoDefPrefs() {
        return angular.extend({}, defaultPrefsState);
    }

    function setInitToggleState() {
        cachedState = ps.asNumbers(ps.getPrefs(cooktag));
        $log.debug('TOOLBAR---- read prefs state:', cachedState);

        if (!cachedState) {
            cachedState = topoDefPrefs();
            ps.setPrefs(cooktag, cachedState);
            $log.debug('TOOLBAR---- Set default prefs state:', cachedState);
        }

        angular.forEach(prefsMap, function (v, k) {
            var cfg = k2b[v];
            cfg && (cfg.isel = !!cachedState[k]);
        });
    }

    function initKeyData() {
        keyData = d3.map(k2b);
        keyData.forEach(function(key, value) {
            var data = api.getActionEntry(key);
            value.cb = data[0];                     // on-click callback
            value.tt = data[1] + ' (' + key + ')';  // tooltip
        });
    }

    function addButton(key) {
        var v = keyData.get(key);
        v.btn = toolbar.addButton(v.id, v.gid, v.cb, v.tt);
    }
    function addToggle(key, suppressIfMobile) {
        var v = keyData.get(key);
        if (suppressIfMobile && fs.isMobile()) { return; }
        v.tog = toolbar.addToggle(v.id, v.gid, v.isel, v.cb, v.tt);
    }

    function addFirstRow() {
        addToggle('I');
        addToggle('O');
        addToggle('D');
        toolbar.addSeparator();

        addToggle('H');
        addToggle('M');
        addToggle('P', true);
        addToggle('B');
        addToggle('S', true);
    }

    function addSecondRow() {
        //addToggle('X');
        addToggle('Z');
        addButton('N');
        addButton('L');
        addButton('R');
        toolbar.addSeparator();
        addButton('E');
    }

    function addOverlays() {
        toolbar.addSeparator();

        // generate radio button set for overlays; start with 'none'
        var rset = [{
                gid: 'unknown',
                tooltip: 'No Overlay',
                cb: function () {
                    tov.tbSelection(null);
                }
            }];

        tov.list().forEach(function (key) {
            var ov = tov.overlay(key);
            rset.push({
                gid: ov._glyphId,
                tooltip: (ov.tooltip || '(no tooltip)'),
                cb: function () {
                    tov.tbSelection(ov.overlayId);
                }
            });
        });

        toolbar.addRadioSet('topo-overlays', rset);
    }

    // TODO: 3rd row needs to be swapped in/out based on selected overlay
    // NOTE: This particular row of buttons is for the traffic overlay
    function addThirdRow() {
        addButton('V');
        addButton('leftArrow');
        addButton('rightArrow');
        addButton('W');
        addButton('A');
        addButton('F');
    }

    function createToolbar() {
        initKeyData();
        toolbar = tbs.createToolbar(name);
        addFirstRow();
        toolbar.addRow();
        addSecondRow();
        addOverlays();
        toolbar.addRow();
        addThirdRow();

        if (cachedState.toolbar) {
            toolbar.show();
        } else {
            toolbar.hide();
        }
    }

    function destroyToolbar() {
        tbs.destroyToolbar(name);
    }

    // allows us to ensure the button states track key strokes
    function keyListener(key) {
        var v = keyData.get(key);

        if (v) {
            // we have a valid button mapping
            if (v.tog) {
                // it's a toggle button
                v.tog.toggleNoCb();
            }
        }
    }

    function toggleToolbar() {
        toolbar.toggle();
    }

    angular.module('ovTopo')
        .factory('TopoToolbarService',
        ['$log', 'FnService', 'ToolbarService', 'PrefsService',
            'TopoOverlayService',

        function (_$log_, _fs_, _tbs_, _ps_, _tov_) {
            $log = _$log_;
            fs = _fs_;
            tbs = _tbs_;
            ps = _ps_;
            tov = _tov_;

            return {
                init: init,
                createToolbar: createToolbar,
                destroyToolbar: destroyToolbar,
                keyListener: keyListener,
                toggleToolbar: toggleToolbar
            };
        }]);
}());