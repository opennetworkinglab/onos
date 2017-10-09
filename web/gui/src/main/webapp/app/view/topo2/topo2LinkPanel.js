/*
 * Copyright 2016-present Open Networking Foundation
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
    var linkPanel, linkData;

    function init() {
        linkPanel = panel();
    }

    function formatLinkData(data) {

        var source = data.get('source'),
            target = data.get('target');

        return {
            title: 'Link',
            propOrder: [
                'Type', '-',
                'A Type', 'A Id', 'A Label', 'A Port', '-',
                'B Type', 'B Id', 'B Label', 'B Port',
            ],
            propLabels: {
                'Type': 'Type',
                'A Type': 'A Type',
                'A Id': 'A Id',
                'A Label': 'A Label',
                'A Port': 'A Port',
                'B Type': 'B Type',
                'B Id': 'B Id',
                'B Label': 'B Label',
                'B Port': 'B Port',
            },
            propValues: {
                '-': '',
                'Type': data.get('type'),
                'A Type': source.get('nodeType'),
                'A Id': source.get('id'),
                'A Label': source.get('props').name,
                'A Port': data.get('portA') || 'N/A',
                'B Type': target.get('nodeType'),
                'B Id': target.get('id'),
                'B Label': target.get('props').name,
                'B Port': data.get('portB') || 'N/A',
            },
        };
    }

    function displayLink(data) {
        init();

        linkData = formatLinkData(data);
        render();
    }

    function render() {
        linkPanel.show();
        linkPanel.emptyRegions();

        var svg = linkPanel.appendToHeader('div')
                .classed('icon', true)
                .append('svg'),
            title = linkPanel.appendToHeader('h2'),
            table = linkPanel.appendToBody('table'),
            tbody = table.append('tbody');

        title.text(linkData.title);
        gs.addGlyph(svg, 'bird', 24, 0, [1, 1]);
        ls.listProps(tbody, linkData);
    }

    function show() {
        linkPanel.show();
    }

    function hide() {
        linkPanel.el.hide();
    }

    function toggle() {
        var on = linkPanel.el.toggle(),
            verb = on ? 'Show' : 'Hide';
        flash.flash(verb + ' Link Panel');
    }

    function destroy() {
        linkPanel.destroy();
    }

    angular.module('ovTopo2')
    .factory('Topo2LinkPanelService', [
        'Topo2DetailsPanelService', 'GlyphService', 'FlashService', 'ListService',
        function (_ps_, _gs_, _flash_, _ls_) {

            panel = _ps_;
            gs = _gs_;
            flash = _flash_;
            ls = _ls_;

            return {
                displayLink: displayLink,
                init: init,
                show: show,
                hide: hide,
                toggle: toggle,
                destroy: destroy,
                isVisible: function () { return linkPanel.isVisible(); },
            };
        },
    ]);

})();
