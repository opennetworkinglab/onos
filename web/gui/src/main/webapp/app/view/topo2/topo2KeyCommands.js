/*
* Copyright 2016-present Open Networking Laboratory
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

(function () {

    // Injected Services
    var ks, flash, wss, t2ps, t2ms, ps, t2is, t2sp, t2vs;

    var t2fs;

    // Commmands
    var actionMap = {
        L: [cycleDeviceLabels, 'Cycle device labels'],
        G: [openMapSelection, 'Select background geo map'],
        B: [toggleMap, 'Toggle background geo map'],
        I: [toggleInstancePanel, 'Toggle ONOS Instance Panel'],
        O: [toggleSummary, 'Toggle the Summary Panel'],
        R: [resetZoom, 'Reset pan / zoom'],
        P: [togglePorts, 'Toggle Port Highlighting'],
        E: [equalizeMasters, 'Equalize mastership roles'],
        X: [resetAllNodeLocations, 'Reset Node Location'],
        U: [unpinNode, 'Unpin node (mouse over)'],

        esc: handleEscape
    };

    function init(_t2fs_) {
        t2fs = _t2fs_;
        bindCommands();
    }

    function bindCommands() {

        ks.keyBindings(actionMap);

        ks.gestureNotes([
            ['click', 'Select the item and show details'],
            ['shift-click', 'Toggle selection state'],
            ['drag', 'Reposition (and pin) device / host'],
            ['cmd-scroll', 'Zoom in / out'],
            ['cmd-drag', 'Pan']
        ]);
    }

    function handleEscape() {
        if (t2ddp.isVisible()) {
            t2ddp.toggle();
        } else if (t2sp.isVisible()) {
            t2sp.toggle();
        } else if (t2is.isVisible()) {
            t2is.toggle(); 
        }  
    }

    var prefsState = {};

    function updatePrefsState(what, b) {
        prefsState[what] = b ? 1 : 0;
        ps.setPrefs('topo_prefs', prefsState);
    }

    function deviceLabelFlashMessage(index) {
        switch (index) {
            case 0: return 'Hide device labels';
            case 1: return 'Show friendly device labels';
            case 2: return 'Show device ID labels';
        }
    }

    function cycleDeviceLabels() {
        var deviceLabelIndex = t2ps.get('dlbls') + 1,
            newDeviceLabelIndex =  deviceLabelIndex % 3;

        t2ps.set('dlbls', newDeviceLabelIndex);
        t2fs.updateNodes();
        flash.flash(deviceLabelFlashMessage(newDeviceLabelIndex));
    }

    function openMapSelection() {
        t2ms.openMapSelection();
    }

    function toggleMap(x) {
        t2ms.toggle(x);
    }

    function toggleInstancePanel(x) {
        updatePrefsState('insts', t2is.toggle(x));
    }

    function toggleSummary() {
        t2sp.toggle();
    }

    function resetZoom() {
        t2ms.resetZoom();
        flash.flash('Pan and zoom reset');
    }

    function togglePorts(x) {
        updatePrefsState('porthl', t2vs.togglePortHighlights(x));
        t2fs.updateLinks();
    }

    function equalizeMasters() {
        wss.sendEvent('equalizeMasters');
        flash.flash('Equalizing master roles');
    }

    function resetAllNodeLocations() {
        t2fs.resetAllLocations();
        flash.flash('Reset node locations');
    }

    function unpinNode() {
        t2fs.unpin();
        flash.flash('Unpin node');
    }

    angular.module('ovTopo2')
    .factory('Topo2KeyCommandService',
    ['KeyService', 'FlashService', 'WebSocketService', 'Topo2PrefsService',
    'Topo2MapService', 'PrefsService', 'Topo2InstanceService',
    'Topo2SummaryPanelService', 'Topo2DeviceDetailsPanel', 'Topo2ViewService',
        function (_ks_, _flash_, _wss_, _t2ps_, _t2ms_, _ps_, _t2is_, _t2sp_, _t2ddp_, _t2vs_) {

            ks = _ks_;
            flash = _flash_;
            wss = _wss_;
            t2ps = _t2ps_;
            t2ms = _t2ms_;
            t2is = _t2is_;
            ps = _ps_;
            t2sp = _t2sp_;
            t2ddp = _t2ddp_;
            t2vs = _t2vs_;

            return {
                init: init,
                bindCommands: bindCommands
            };
        }
    ]);
})();
