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
 ONOS GUI -- Topology Layout Module.
 Module that contains the d3.force.layout logic
 */

(function () {
    'use strict';

    // Injected Services
    var Panel, gs, wss, flash;

    // Internal State
    var summaryPanel, summaryData;

    // configuration
    var id = 'topo2-p-summary',
        className = 'topo-p',
        panelOpts = {
            show: true,
            width: 260          // summary and detail panel width
        },
        handlerMap = {
            showSummary: handleSummaryData
        };

    function init() {

        bindHandlers();
        wss.sendEvent('requestSummary');

        var options = angular.extend({}, panelOpts, {
            class: className
        });

        summaryPanel = new Panel(id, options);

        summaryPanel.p.classed(className, true);
    }

    function addProp(tbody, label, value) {
        var tr = tbody.append('tr'),
            lab;
        if (typeof label === 'string') {
            lab = label.replace(/_/g, ' ');
        } else {
            lab = label;
        }

        function addCell(cls, txt) {
            tr.append('td').attr('class', cls).html(txt);
        }
        addCell('label', lab + ' :');
        addCell('value', value);
    }

    function addSep(tbody) {
        tbody.append('tr').append('td').attr('colspan', 2).append('hr');
    }

    function listProps(tbody, data) {
        summaryData.propOrder.forEach(function (p) {
            if (p === '-') {
                addSep(tbody);
            } else {
                addProp(tbody, p, summaryData.props[p]);
            }
        });
    }

    function render() {
        summaryPanel.emptyRegions();

        var svg = summaryPanel.appendToHeader('div')
                .classed('icon', true)
                .append('svg'),
            title = summaryPanel.appendToHeader('h2'),
            table = summaryPanel.appendToBody('table'),
            tbody = table.append('tbody');

        title.text(summaryData.title);
        gs.addGlyph(svg, 'bird', 24, 0, [1, 1]);
        listProps(tbody);
    }

    function handleSummaryData(data) {
        summaryData = data;
        render();
    }

    function bindHandlers() {
        wss.bindHandlers(handlerMap);
    }

    function toggle() {
        var on = summaryPanel.p.toggle(),
            verb = on ? 'Show' : 'Hide';
        flash.flash(verb + ' Summary Panel');
    }

    function destroy() {
        wss.unbindHandlers(handlerMap);
        summaryPanel.destroy();
    }

    angular.module('ovTopo2')
    .factory('Topo2SummaryPanelService',
    ['Topo2PanelService', 'GlyphService', 'WebSocketService', 'FlashService',
        function (_ps_, _gs_, _wss_, _flash_) {

            Panel = _ps_;
            gs = _gs_;
            wss = _wss_;
            flash = _flash_;

            return {
                init: init,

                toggle: toggle,
                destroy: destroy
            };
        }
    ]);

})();
