/*
 * Copyright 2017-present Open Networking Foundation
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
 *
 */

(function () {
    'use strict';

    var instance;

    // TODO: Commented k2b map and addToggles are not implement in Topo2 yet.

    // key to button mapping data
    var k2b = {
        O: { id: 'topo2-summary-tog', gid: 'm_summary', isel: true },
        I: { id: 'topo2-instance-tog', gid: 'm_uiAttached', isel: true },
        D: { id: 'details-tog', gid: 'm_details', isel: true },
        H: { id: 'hosts-tog', gid: 'm_endstation', isel: false },
        M: { id: 'offline-tog', gid: 'm_switch', isel: true },
        P: { id: 'topo2-ports-tog', gid: 'm_ports', isel: true },
        B: { id: 'topo2-bkgrnd-tog', gid: 'm_map', isel: true },

        // N: { id: 'filters-btn', gid: 'm_filters' },
        L: { id: 'topo2-cycleLabels-btn', gid: 'm_cycleLabels' },
        R: { id: 'topo2-resetZoom-btn', gid: 'm_resetZoom' },

        E: { id: 'topo2-eqMaster-btn', gid: 'm_eqMaster' },
    };

    angular.module('ovTopo2')
        .factory('Topo2ToolbarService', [
            'FnService', 'ToolbarService', 'Topo2KeyCommandService',
            'Topo2OverlayService',
            function (fs, tbs, t2kcs, t2ov) {

                var Toolbar = function () {
                    instance = this;
                };

                Toolbar.prototype = {

                    className: 'topo2-toolbar',

                    init: function () {
                        this.el = tbs.createToolbar(this.className);
                        this.radioSet;
                        this.ovIndex;

                        this.initKeyData();
                        this.addFirstRow();
                        this.el.addRow();
                        this.addSecondRow();
                        this.addOverlays();
                        this.el.hide();
                    },
                    initKeyData: function () {
                        _.each(k2b, function (value, key) {
                            var data = t2kcs.getActionEntry(key);
                            if (data) {
                                // on-click callback
                                value.cb = data[0];
                                // tooltip
                                value.tt = data[1] + ' (' + key + ')';
                            }
                        });
                    },
                    getKey: function (key) {
                        return k2b[key];
                    },
                    keyListener: function (key) {
                        var v = this.getKey(key);

                        if (v && v.tog) {
                            v.tog.toggleNoCb();
                        }
                    },
                    addButton: function (key) {
                        var v = this.getKey(key);
                        v.btn = this.el.addButton(v.id, v.gid, v.cb, v.tt);
                    },
                    addToggle: function (key, suppressIfMobile) {
                        var v = this.getKey(key);
                        if (suppressIfMobile && fs.isMobile()) { return; }
                        v.tog = this.el.addToggle(v.id, v.gid, v.isel, v.cb, v.tt);
                    },

                    toggle: function () {
                        this.el.toggle();
                    },

                    selectOverlay: function (ovid) {
                        var index = this.ovIndex[defaultOverlay] || 0,
                            pidx = (ovid === null) ? 0 : this.ovIndex[ovid] || -1;
                        if (pidx >= 0 && pidx < this.radioSet.size()) {
                            index = pidx;
                        }

                        this.radioSet.selectedIndex(index);
                    },

                    fnKey: function (index) {
                        if (index < this.radioSet.size() && index !== this.radioSet.selectedIndex()) {
                            this.radioSet.selectedIndex(index);
                        }
                    },

                    addFirstRow: function () {
                        this.addToggle('I');
                        this.addToggle('O');
                        this.addToggle('D');
                        this.el.addSeparator();

                        this.addToggle('H');
                        this.addToggle('M');
                        this.addToggle('P', true);
                        this.addToggle('B');
                    },
                    addSecondRow: function () {
                        // this.addButton('N');
                        this.addButton('L');
                        this.addButton('R');
                        this.el.addSeparator();
                        this.addButton('E');
                    },

                    switchOverlayActions: function () {
                        // TODO: see TopoToolbar.js
                        // NOTE: Should add overlay buttons on the third row
                    },

                    addOverlays: function () {
                        var _this = this; // Keep context in callback
                        this.el.addSeparator();

                        // generate radio button set for overlays; start with 'none'
                        var rset = [{
                            gid: 'm_unknown',
                            tooltip: 'ov_tt_none',
                            cb: function () {
                                t2ov.tbSelection(null, _this.switchOverlayActions);
                            },
                        }];

                        t2ov.augmentRbset(rset, this.switchOverlayActions);
                        this.radioSet = this.el.addRadioSet('topo-overlays', rset);
                    },

                    destroy: function () {
                        // TODO: Should the tbs remove button id's in the destroyToolbar method?
                        // If you go topo2 -> topo -> topo2 there's a button id conflict
                        tbs.destroyToolbar(this.className);
                    },
                };

                return instance || new Toolbar();
            },
        ]);
}());
