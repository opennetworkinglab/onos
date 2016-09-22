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
    var ks, t2ps, t2ms;
    var topo2ForceService;

    // Commmands
    var actionMap = {
        L: [cycleDeviceLabels, 'Cycle device labels'],
        G: [openMapSelection, 'Select background geo map'],
        B: [toggleMap, 'Toggle background geo map'],
    };

    function init(t2fs) {
        topo2ForceService = t2fs;
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

    function cycleDeviceLabels() {
        var deviceLabelIndex = t2ps.get('dlbls') + 1;
        t2ps.set('dlbls', deviceLabelIndex % 3);
        topo2ForceService.updateNodes();
    }

    function openMapSelection() {
        t2ms.openMapSelection();
    }

    function toggleMap(x) {
        t2ms.toggle(x);
    }

    angular.module('ovTopo2')
    .factory('Topo2KeyCommandService',
    ['KeyService', 'Topo2PrefsService', 'Topo2MapService',

        function (_ks_, _t2ps_, _t2ms_) {

            t2ps = _t2ps_;
            t2ms = _t2ms_;
            ks = _ks_;

            return {
                init: init,
                bindCommands: bindCommands
            };
        }
    ]);
})();
