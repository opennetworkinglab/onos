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
 ONOS GUI -- Topology Toolbar Module.
 Functions for creating and interacting with the toolbar.
 */

(function () {
    'use strict';

    // injected references
    var $log, fs, tbs, ps, tov, tds, api;

    // API:
    //  getActionEntry
    //  setUpKeys

    // internal state
    var toolbar, keyData, cachedState, thirdRow, ovRset, ovIndex;

    // constants
    var name = 'topo-tbar',
        cooktag = 'topo_prefs',
        soa = 'switchOverlayActions: ',
        selOver = 'Select overlay here &#x21e7;',
        defaultOverlay = 'traffic';


    // key to button mapping data
    var k2b = {
        O: { id: 'summary-tog', gid: 'm_summary', isel: true},
        I: { id: 'instance-tog', gid: 'm_uiAttached', isel: true },
        D: { id: 'details-tog', gid: 'm_details', isel: true },
        H: { id: 'hosts-tog', gid: 'm_endstation', isel: false },
        M: { id: 'offline-tog', gid: 'm_switch', isel: true },
        P: { id: 'ports-tog', gid: 'm_ports', isel: true },
        B: { id: 'bkgrnd-tog', gid: 'm_map', isel: false },
        G: { id: 'bkgrnd-sel', gid: 'm_selectMap' },
        S: { id: 'sprite-tog', gid: 'm_cloud', isel: false },

        Z: { id: 'oblique-tog', gid: 'm_oblique', isel: false },
        N: { id: 'filters-btn', gid: 'm_filters' },
        L: { id: 'cycleLabels-btn', gid: 'm_cycleLabels' },
        R: { id: 'resetZoom-btn', gid: 'm_resetZoom' },

        E: { id: 'eqMaster-btn', gid: 'm_eqMaster' }
    };

    var prohibited = [
        'T', 'backSlash', 'slash',
        'X' // needed until we re-instate X above.
    ];
    prohibited = prohibited.concat(d3.map(k2b).keys());


    // initial toggle state: default settings and tag to key mapping
    var defaultPrefsState = {
            insts: 1,
            summary: 1,
            detail: 1,
            hosts: 0,
            offdev: 1,
            dlbls: 0,
            porthl: 1,
            bg: 0,
            spr: 0,
            ovidx: 1,   // default to traffic overlay
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
        cachedState = ps.asNumbers(ps.getPrefs(cooktag, defaultPrefsState));
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
        // TODO: use angular forEach instead of d3.map
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
        addButton('G');
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
                gid: 'm_unknown',
                tooltip: 'No Overlay',
                cb: function () {
                    tov.tbSelection(null, switchOverlayActions);
                }
            }];
        ovIndex = tov.augmentRbset(rset, switchOverlayActions);
        ovRset = toolbar.addRadioSet('topo-overlays', rset);
    }

    // invoked by overlay service to switch out old buttons and switch in new
    function switchOverlayActions(oid, keyBindings) {
        var prohibits = [],
            kb = fs.isO(keyBindings) || {},
            order = fs.isA(kb._keyOrder) || [];

        if (keyBindings && !keyBindings._keyOrder) {
            $log.warn(soa + 'no _keyOrder property defined');
        } else {
            // sanity removal of reserved property names
            ['esc', '_keyListener', '_helpFormat'].forEach(function (k) {
                fs.removeFromArray(k, order);
            });
        }

        // ensure dialog has closed (if opened by outgoing overlay)
        tds.closeDialog();
        thirdRow.clear();

        // persist our choice of overlay...
        persistTopoPrefs('ovidx', ovIndex[oid] || 0);

        if (!order.length) {
            thirdRow.setText(selOver);
            thirdRow.classed('right', true);
            api.setUpKeys(); // clear previous overlay key bindings

        } else {
            thirdRow.classed('right', false);
            angular.forEach(order, function (key) {
                var value, bid, gid, tt;

                if (prohibited.indexOf(key) > -1) {
                    prohibits.push(key);

                } else {
                    value = keyBindings[key];
                    bid = oid + '-' + key;
                    gid = tov.mkGlyphId(oid, value.gid);
                    tt = value.tt + ' (' + key + ')';
                    thirdRow.addButton(bid, gid, value.cb, tt);
                }
            });
            api.setUpKeys(keyBindings); // add overlay key bindings
        }

        if (prohibits.length) {
            $log.warn(soa + 'Prohibited key bindings ignored:', prohibits);
        }
    }

    function createToolbar() {
        initKeyData();
        toolbar = tbs.createToolbar(name);
        addFirstRow();
        toolbar.addRow();
        addSecondRow();
        addOverlays();
        thirdRow = toolbar.addRow();
        thirdRow.setText(selOver);
        thirdRow.classed('right', true);

        if (cachedState.toolbar) {
            toolbar.show();
        } else {
            toolbar.hide();
        }
    }

    function destroyToolbar() {
        tbs.destroyToolbar(name);
        tov.resetOnToolbarDestroy();
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

    function persistTopoPrefs(key, val) {
        var prefs = ps.getPrefs(cooktag, defaultPrefsState);
        prefs[key] = val === undefined ? !prefs[key] : val;
        ps.setPrefs('topo_prefs', prefs);
    }

    function toggleToolbar() {
        toolbar.toggle();
        persistTopoPrefs('toolbar');
    }
    
    function setDefaultOverlay(prefsIdx) {
        var idx = ovIndex[defaultOverlay] || 0;
        if (prefsIdx >= 0 && prefsIdx < ovRset.size()) {
            idx = prefsIdx;
        }
        ovRset.selectedIndex(idx);
    }

    // an overlay was selected via Function-Key press
    function fnkey(idx) {
        if (idx < ovRset.size() && idx !== ovRset.selectedIndex()) {
            ovRset.selectedIndex(idx);
        }
    }

    angular.module('ovTopo')
        .factory('TopoToolbarService',
        ['$log', 'FnService', 'ToolbarService', 'PrefsService',
            'TopoOverlayService', 'TopoDialogService',

        function (_$log_, _fs_, _tbs_, _ps_, _tov_, _tds_) {
            $log = _$log_;
            fs = _fs_;
            tbs = _tbs_;
            ps = _ps_;
            tov = _tov_;
            tds = _tds_;

            return {
                init: init,
                createToolbar: createToolbar,
                destroyToolbar: destroyToolbar,
                keyListener: keyListener,
                toggleToolbar: toggleToolbar,
                setDefaultOverlay: setDefaultOverlay,
                defaultPrefs: defaultPrefsState,
                fnkey: fnkey
            };
        }]);
}());