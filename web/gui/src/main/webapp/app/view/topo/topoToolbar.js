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
 Defines functions for manipulating the toolbar.
 */

(function () {

    // injected references
    var $log, tbs;

    var api, toolbar;

    // buttons
    var togSummary, togInstances, togDetails,
        togHosts, togOffline, togPorts, togBackground;

    function init(_api_) {
        api = _api_;
    }

    // TODO: fix toggle and radio sets to be selected based on the current state
    // current bug: first toggle button, then toggle with key, toggle button doesn't update appearance


    function getActions() {
        togSummary = api.getActionEntry('O');
        togInstances = api.getActionEntry('I');
        togDetails = api.getActionEntry('D');

        togHosts = api.getActionEntry('H');
        togOffline = api.getActionEntry('M');
        togPorts = api.getActionEntry('P');
        togBackground = api.getActionEntry('B');
    }

    function entryCallback(entry) {
        return entry[0];
    }

    function entryToolTip(entry) {
        return entry[1];
    }

    function addFirstRow() {
        toolbar.addToggle('summary-tog', 'unknown', true,
            entryCallback(togSummary), entryToolTip(togSummary));
        toolbar.addToggle('instance-tog', 'uiAttached', true,
            entryCallback(togInstances), entryToolTip(togInstances));
        toolbar.addToggle('details-tog', 'unknown', true,
            entryCallback(togDetails), entryToolTip(togDetails));
        toolbar.addSeparator();

        toolbar.addToggle('hosts-tog', 'endstation', false,
            entryCallback(togHosts), entryToolTip(togHosts));
        toolbar.addToggle('offline-tog', 'switch', true,
            entryCallback(togOffline), entryToolTip(togOffline));
        toolbar.addToggle('ports-tog', 'unknown', true,
            entryCallback(togPorts), entryToolTip(togPorts));
        toolbar.addToggle('bkgrnd-tog', 'unknown', true,
            entryCallback(togBackground), entryToolTip(togBackground));
    }

    function createToolbar() {
        getActions();
        toolbar = tbs.createToolbar('topo-tbar');
        addFirstRow();
        toolbar.show();
    }

    angular.module('ovTopo')
        .factory('TopoToolbarService', ['$log', 'ToolbarService',

        function (_$log_, _tbs_) {
            $log = _$log_;
            tbs = _tbs_;

            return {
                init: init,
                createToolbar: createToolbar
            };
        }]);
}());