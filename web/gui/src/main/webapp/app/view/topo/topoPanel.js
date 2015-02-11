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
    var $log, fs, ps, gs;

    var api;
    /*
      sendEvent( event, {payload} )
     */

    // constants
    var pCls = 'topo-p',
        idSum = 'topo-p-summary',
        idDet = 'topo-p-detail',
        panelOpts = {
            width: 260
        };

    // panels
    var summaryPanel,
        detailPanel;


    // === -----------------------------------------------------
    // Utility functions

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

    function listProps(tbody, data) {
        data.propOrder.forEach(function(p) {
            if (p === '-') {
                addSep(tbody);
            } else {
                addProp(tbody, p, data.props[p]);
            }
        });
    }

    function dpa(x) {
        return detailPanel.append(x);
    }

    function spa(x) {
        return summaryPanel.append(x);
    }

    // === -----------------------------------------------------
    //  Functions for populating the summary panel

    function populateSummary(data) {
        summaryPanel.empty();

        var svg = spa('svg'),
            title = spa('h2'),
            table = spa('table'),
            tbody = table.append('tbody');

        gs.addGlyph(svg, 'node', 40);
        gs.addGlyph(svg, 'bird', 24, true, [8,12]);

        title.text(data.id);
        listProps(tbody, data);
    }

    // === -----------------------------------------------------
    //  Functions for populating the detail panel

    function displaySingle(data) {
        detailPanel.empty();

        var svg = dpa('svg'),
            title = dpa('h2'),
            table = dpa('table'),
            tbody = table.append('tbody');

        gs.addGlyph(svg, (data.type || 'unknown'), 40);
        title.text(data.id);
        listProps(tbody, data);
        dpa('hr');
    }

    function displayMulti(ids) {
        detailPanel.empty();

        var title = dpa('h3'),
            table = dpa('table'),
            tbody = table.append('tbody');

        title.text('Selected Nodes');
        ids.forEach(function (d, i) {
            addProp(tbody, i+1, d);
        });
        dpa('hr');
    }

    function addAction(text, cb) {
        dpa('div')
            .classed('actionBtn', true)
            .text(text)
            .on('click', cb);
    }

    // === -----------------------------------------------------
    //  Event Handlers

    function showSummary(data) {
        populateSummary(data);
        showSummaryPanel();
    }

    function toggleSummary() {
        if (summaryPanel.isVisible()) {
            hideSummaryPanel();
        } else {
            // ask server to start sending summary data.
            api.sendEvent('requestSummary');
            // note: the summary panel will appear, once data arrives
        }
    }

    // === -----------------------------------------------------
    // === LOGIC For showing/hiding summary and detail panels...

    function showSummaryPanel() {
        if (detailPanel.isVisible()) {
            detailPanel.down(summaryPanel.show);
        } else {
            summaryPanel.show();
        }
    }

    function hideSummaryPanel() {
        // instruct server to stop sending summary data
        api.sendEvent("cancelSummary");
        summaryPanel.hide(detailPanel.up);
    }

    function showDetailPanel() {
        if (summaryPanel.isVisible()) {
            detailPanel.down(detailPanel.show);
        } else {
            detailPanel.up(detailPanel.show);
        }
    }

    function hideDetailPanel() {
        detailPanel.hide();
    }

    // ==========================

    function noop () {}

    function augmentDetailPanel() {
        var dp = detailPanel;
        dp.ypos = { up: 64, down: 320, current: 320};

        dp._move = function (y, cb) {
            var endCb = fs.isF(cb) || noop,
                yp = dp.ypos;
            if (yp.current !== y) {
                yp.current = y;
                dp.el().transition().duration(300)
                    .each('end', endCb)
                    .style('top', yp.current + 'px');
            } else {
                endCb();
            }
        };

        dp.down = function (cb) { dp._move(dp.ypos.down, cb); };
        dp.up = function (cb) { dp._move(dp.ypos.up, cb); };
    }

    // ==========================

    function initPanels(_api_) {
        api = _api_;

        summaryPanel = ps.createPanel(idSum, panelOpts);
        detailPanel = ps.createPanel(idDet, panelOpts);

        summaryPanel.classed(pCls, true);
        detailPanel.classed(pCls, true);

        augmentDetailPanel();
    }

    function destroyPanels() {
        ps.destroyPanel(idSum);
        ps.destroyPanel(idDet);
        summaryPanel = detailPanel = null;
    }

    // ==========================

    angular.module('ovTopo')
    .factory('TopoPanelService',
        ['$log', 'FnService', 'PanelService', 'GlyphService',

        function (_$log_, _fs_, _ps_, _gs_) {
            $log = _$log_;
            fs = _fs_;
            ps = _ps_;
            gs = _gs_;

            return {
                initPanels: initPanels,
                destroyPanels: destroyPanels,

                showSummary: showSummary,
                toggleSummary: toggleSummary,

                displaySingle: displaySingle,
                displayMulti: displayMulti,
                addAction: addAction,

                hideSummaryPanel: hideSummaryPanel,
                showDetailPanel: showDetailPanel,
                hideDetailPanel: hideDetailPanel,

                detailVisible: function () { return detailPanel.isVisible(); },
                summaryVisible: function () { return summaryPanel.isVisible(); }
            };
        }]);
}());
