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
    var $log, ps, gs;

    // constants
    var idSum = 'topo-p-summary',
        idDet = 'topo-p-detail',
        idIns = 'topo-p-instance',
        panelOpts = {
            width: 260
        };

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

    function addSep(tbody) {
        tbody.append('tr').append('td').attr('colspan', 2).append('hr');
    }

    function addProp(tbody, label, value) {
        var tr = tbody.append('tr');

        function addCell(cls, txt) {
            tr.append('td').attr('class', cls).text(txt);
        }
        addCell('label', label + ' :');
        addCell('value', value);
    }

    function populateSummary(data) {
        summaryPanel.empty();

        var svg = summaryPanel.append('svg'),
            title = summaryPanel.append('h2'),
            table = summaryPanel.append('table'),
            tbody = table.append('tbody');

        gs.addGlyph(svg, 'node', 40);
        gs.addGlyph(svg, 'bird', 24, true, [8,12]);

        title.text(data.id);

        data.propOrder.forEach(function(p) {
            if (p === '-') {
                addSep(tbody);
            } else {
                addProp(tbody, p, data.props[p]);
            }
        });
    }

    function showSummaryPanel() {
        summaryPanel.show();

    }

    // ==========================

    angular.module('ovTopo')
    .factory('TopoPanelService',
        ['$log', 'PanelService', 'GlyphService',

        function (_$log_, _ps_, _gs_) {
            $log = _$log_;
            ps = _ps_;
            gs = _gs_;

            function initPanels() {
                summaryPanel = ps.createPanel(idSum, panelOpts);
                // TODO: set up detail and instance panels..
            }

            function destroyPanels() {
                ps.destroyPanel(idSum);
                summaryPanel = null;
                // TODO: destroy detail and instance panels..
            }

            function showSummary(payload) {
                populateSummary(payload);
                showSummaryPanel();
            }

            return {
                initPanels: initPanels,
                destroyPanels: destroyPanels,
                showSummary: showSummary
            };
        }]);
}());
