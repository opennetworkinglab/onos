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

/*
 ONOS GUI -- Topology View Module.
 Module that displays the details panel for selected nodes
 */

(function () {
    'use strict';

    // Injected Services
    var Panel;

    // Internal State
    var detailsPanel;

    // configuration
    var id = 'topo2-p-detail',
        className = 'topo-p',
        panelOpts = {
            width: 260          // summary and detail panel width
        };

    function getInstance() {
        if (detailsPanel) {
            return detailsPanel;
        }

        var options = angular.extend({}, panelOpts, {
            class: className
        });

        detailsPanel = new Panel(id, options);
        detailsPanel.el.classed(className, true);

        return detailsPanel;
    }

    angular.module('ovTopo2')
    .factory('Topo2DetailsPanelService',
    ['Topo2PanelService',
        function (_ps_) {

            Panel = _ps_;

            return getInstance;
        }
    ]);
})();
