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
 ONOS GUI -- Topology Panel Module.
 Defines functions for manipulating the summary, detail, and instance panels.
 */

(function () {
    'use strict';

    // injected refs
    var $log, ps;

    // internal state
    var settings;


    // SVG elements;
    var fooPane;

    // D3 selections;
    var summaryPanel,
        detailPanel,
        instancePanel;

    // default settings for force layout
    var defaultSettings = {
        foo: 2
    };


    // ==========================

    angular.module('ovTopo')
    .factory('TopoPanelService',
        ['$log', 'PanelService',

        function (_$log_, _ps_) {
            $log = _$log_;
            ps = _ps_;

            function initPanels() {
                summaryPanel = ps.createPanel('topo-p-summary');
                // TODO: set up detail and instance panels..
            }

            function showSummary(payload) {
                summaryPanel.empty();
                summaryPanel.append('h2').text(payload.id);
                // TODO: complete the formatting...

                summaryPanel.show();
            }

            return {
                initPanels: initPanels,
                showSummary: showSummary
            };
        }]);
}());
