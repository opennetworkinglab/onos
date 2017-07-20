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
    var Panel, gs, wss, flash, ls;

    // Internal State
    var summaryPanel, summaryData, detailsPanel;

    // configuration
    var id = 'topo2-p-summary',
        className = 'topo2-p',
        panelOpts = {
            show: true,
            width: 260 // summary and detail panel width
        },
        handlerMap = {
            showSummary: handleSummaryData
        };

    function init(_detailsPanel_) {

        detailsPanel = _detailsPanel_;

        bindHandlers();
        wss.sendEvent('requestSummary');

        var options = angular.extend({}, panelOpts, {
            class: className
        });

        summaryPanel = new Panel(id, options);
        summaryPanel.el.classed(className, true);
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
        ls.listProps(tbody, summaryData);
    }

    function handleSummaryData(data) {
        summaryData = data;
        render();
    }

    function bindHandlers() {
        wss.bindHandlers(handlerMap);
    }

    function hide() {
        summaryPanel.el.hide(detailsPanel.getInstance().up);
    }

    function show() {

        var _show = function () {
            summaryPanel.el.show();
        };

        var summaryHeight = summaryPanel.el.bbox().height;
        if (detailsPanel.isVisible()) {
            detailsPanel.getInstance().down(summaryHeight, _show);
        } else {
            _show();
        }
    }


    function toggle() {
        var on = summaryPanel.isVisible(),
            verb = on ? 'Hide' : 'Show';

        flash.flash(verb + ' Summary Panel');
        wss.sendEvent(on ? 'requestSummary' : 'cancelSummary');
        on ? hide(): show();
    }

    function destroy() {
        wss.unbindHandlers(handlerMap);
        wss.sendEvent('cancelSummary');
        summaryPanel.destroy();
    }

    angular.module('ovTopo2')
    .factory('Topo2SummaryPanelService', [
        'Topo2PanelService', 'GlyphService', 'WebSocketService', 'FlashService', 'ListService',
        function (_ps_, _gs_, _wss_, _flash_, _ls_) {

            Panel = _ps_;
            gs = _gs_;
            wss = _wss_;
            flash = _flash_;
            ls = _ls_;

            return {
                init: init,
                toggle: toggle,
                destroy: destroy,
                isVisible: function () { return summaryPanel.isVisible(); },
                getInstance: function () { return summaryPanel; }
            };
        }
    ]);

})();
