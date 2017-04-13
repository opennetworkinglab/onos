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
    var panel, gs, flash, ls;

    // Internal State
    var subRegionPanel, subRegionData;

    function init() {
        subRegionPanel = panel();
    }

    function formatSubRegionData(data) {
        return {
            title: data.get('name'),
            propOrder: ['Id', 'Type', '-', 'Number of Devices', 'Number of Hosts'],
            props: {
                '-': '',
                'Id': data.get('id'),
                'Type': data.get('nodeType'),
                'Number of Devices': data.get('nDevs'),
                'Number of Hosts': data.get('nHosts')
            }
        };
    }

    function displayPanel(data) {
        init();
        subRegionData = formatSubRegionData(data);
        render();
    }

    function render() {
        subRegionPanel.show();
        subRegionPanel.emptyRegions();

        var svg = subRegionPanel.appendToHeader('div')
                .classed('icon', true)
                .append('svg'),
            title = subRegionPanel.appendToHeader('h2'),
            table = subRegionPanel.appendToBody('table'),
            tbody = table.append('tbody');

        title.text(subRegionData.title);
        gs.addGlyph(svg, 'bird', 24, 0, [1, 1]);
        ls.listProps(tbody, subRegionData);
    }

    function show() {
        subRegionPanel.show();
    }

    function hide() {
        subRegionPanel.el.hide();
    }

    function toggle() {
        var on = subRegionPanel.el.toggle(),
            verb = on ? 'Show' : 'Hide';
        flash.flash(verb + ' subRegion Panel');
    }

    function destroy() {
        subRegionPanel.destroy();
    }

    angular.module('ovTopo2')
    .factory('Topo2SubRegionPanelService', [
        'Topo2DetailsPanelService', 'GlyphService', 'FlashService', 'ListService',
        function (_ps_, _gs_, _flash_, _ls_) {

            panel = _ps_;
            gs = _gs_;
            flash = _flash_;
            ls = _ls_;

            return {
                displayPanel: displayPanel,
                init: init,
                show: show,
                hide: hide,
                toggle: toggle,
                destroy: destroy,
                isVisible: function () { return subRegionPanel.isVisible(); }
            };
        }
    ]);

})();
