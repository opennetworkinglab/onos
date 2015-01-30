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
        panelOpts = {
            width: 260
        };

    // panels
    var summaryPanel,
        detailPanel;

    // ==========================
    // *** SHOW SUMMARY ***

    function showSummary(data) {
        populateSummary(data);
        showSummaryPanel();
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

    function showSummaryPanel() {
        summaryPanel.show();
        // TODO: augment, once we have the details pane also
    }

    // ==========================

    function initPanels() {
        summaryPanel = ps.createPanel(idSum, panelOpts);
        detailPanel = ps.createPanel(idDet, panelOpts);
    }

    function destroyPanels() {
        ps.destroyPanel(idSum);
        ps.destroyPanel(idDet);
        summaryPanel = detailPanel = null;
    }

    // ==========================

    angular.module('ovTopo')
    .factory('TopoPanelService',
        ['$log', 'PanelService', 'GlyphService',

        function (_$log_, _ps_, _gs_) {
            $log = _$log_;
            ps = _ps_;
            gs = _gs_;

            return {
                initPanels: initPanels,
                destroyPanels: destroyPanels,
                showSummary: showSummary
            };
        }]);
}());
