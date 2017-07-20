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
    var $log, fs, ks, flash, wss, t2ps, t2bgs, ps, t2is, t2sp, t2vs, t2rs,
        t2fs, t2sls, t2tbs;

    // Commmands
    function actionMap() {
        return {
            L: [cycleDeviceLabels, 'Cycle device labels'],
            B: [toggleBackground, 'Toggle background'],
            I: [toggleInstancePanel, 'Toggle ONOS Instance Panel'],
            O: [toggleSummary, 'Toggle the Summary Panel'],
            R: [resetZoom, 'Reset pan / zoom'],
            P: [togglePorts, 'Toggle Port Highlighting'],
            E: [equalizeMasters, 'Equalize mastership roles'],
            X: [resetNodeLocation, 'Reset Node Location'],
            U: [unpinNode, 'Unpin node (mouse over)'],
            H: [toggleHosts, 'Toggle host visibility'],
            M: [toggleOfflineDevices, 'Toggle offline visibility'],
            dot: [toggleToolbar, 'Toggle Toolbar'],

            esc: handleEscape,

            _keyListener: t2tbs.keyListener.bind(t2tbs)
        }
    }

    function init(_t2fs_, _t2tbs_) {
        t2fs = _t2fs_;
        t2tbs = _t2tbs_;
        bindCommands();
    }

    function bindCommands(additional) {

        var am = actionMap(),
            add = fs.isO(additional);

        if (add) {
            _.each(add, function (value, key) {
                // filter out meta properties (e.g. _keyOrder)
                if (!(key.startsWith('_'))) {
                    // don't allow re-definition of existing key bindings
                    if (am[key]) {
                        $log.warn('keybind: ' + key + ' already exists');
                    } else {
                        am[key] = [value.cb, value.tt];
                    }
                }
            });
        }

        ks.keyBindings(am);

        ks.gestureNotes([
            ['click', 'Select the item and show details'],
            ['shift-click', 'Toggle selection state'],
            ['drag', 'Reposition (and pin) device / host'],
            ['cmd-scroll', 'Zoom in / out'],
            ['cmd-drag', 'Pan']
        ]);
    }

    function handleEscape() {

        if (false) {
            // TODO: Cancel show mastership
            // TODO: Cancel Active overlay

        } else if (t2rs.deselectAllNodes()) {
            // else if we have node selections, deselect them all
            // (work already done)
        } else if (t2rs.deselectLink()) {
            // else if we have a link selection, deselect it
            // (work already done)
        } else if (t2is.isVisible()) {
            // If the instance panel is visible, close it
            t2is.toggle();
        } else if (t2sp.isVisible()) {
            // If the summary panel is visible, close it
            t2sp.toggle();
        }
    }

    var prefsState = {};

    function updatePrefsState(what, b) {
        prefsState[what] = b ? 1 : 0;
        ps.setPrefs('topo2_prefs', prefsState);
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
            newDeviceLabelIndex = deviceLabelIndex % 3;

        t2ps.set('dlbls', newDeviceLabelIndex);
        t2fs.updateNodes();
        flash.flash(deviceLabelFlashMessage(newDeviceLabelIndex));
    }

    function toggleBackground(x) {
        t2bgs.toggle(x);
    }

    function toggleInstancePanel(x) {
        updatePrefsState('insts', t2is.toggle(x));
    }

    function toggleSummary() {
        t2sp.toggle();
    }

    function resetZoom() {
        t2bgs.resetZoom();
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

    function resetNodeLocation() {
        t2fs.resetNodeLocation();
        flash.flash('Reset node locations');
    }

    function unpinNode() {
        t2fs.unpin();
        flash.flash('Unpin node');
    }

    function toggleToolbar() {
        t2tbs.toggle();
    }

    function actionedFlashed(action, message) {
        flash.flash(action + ' ' + message);
    }

    function toggleHosts() {
        var on = t2rs.toggleHosts();
        actionedFlashed(on ? 'Show': 'Hide', 'Hosts')
    }

    function toggleOfflineDevices() {
        var on = t2rs.toggleOfflineDevices();
        actionedFlashed(on ? 'Show': 'Hide', 'offline devices')
    }

    function notValid(what) {
        $log.warn('topo.js getActionEntry(): Not a valid ' + what);
    }

    function getActionEntry(key) {
        var entry;

        if (!key) {
            notValid('key');
            return null;
        }

        entry = actionMap()[key];

        if (!entry) {
            notValid('actionMap (' + key + ') entry');
            return null;
        }
        return fs.isA(entry) || [entry, ''];
    }

    angular.module('ovTopo2')
    .factory('Topo2KeyCommandService', [
        '$log', 'FnService', 'KeyService', 'FlashService', 'WebSocketService',
        'Topo2PrefsService', 'Topo2BackgroundService', 'PrefsService',
        'Topo2InstanceService', 'Topo2SummaryPanelService', 'Topo2ViewService',
        'Topo2RegionService', 'Topo2SpriteLayerService',

        function (_$log_, _fs_, _ks_, _flash_, _wss_, _t2ps_, _t2bgs_, _ps_,
                  _t2is_, _t2sp_, _t2vs_, _t2rs_, _t2sls_) {

            $log = _$log_;
            fs = _fs_;
            ks = _ks_;
            flash = _flash_;
            wss = _wss_;
            t2ps = _t2ps_;
            t2bgs = _t2bgs_;
            t2is = _t2is_;
            ps = _ps_;
            t2sp = _t2sp_;
            t2vs = _t2vs_;
            t2rs = _t2rs_;
            t2sls = _t2sls_;

            return {
                init: init,
                bindCommands: bindCommands,
                getActionEntry: getActionEntry
            };
        }
    ]);
})();
