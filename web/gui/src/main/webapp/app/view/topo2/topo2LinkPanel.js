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
    var Panel, gs, wss, flash, listProps;

    // Internal State
    var linkPanel, linkData;

    function init() {
        linkPanel = Panel();
    }

    function formatLinkData(data) {

        var source = data.get('source'),
            target = data.get('target');

        return {
            title: 'Link',
            propOrder: [
                'Type', '-',
                'A Type', 'A Id', 'A Label', 'A Port', '-',
                'B Type', 'B Id', 'B Label', 'B Port'
            ],
            props: {
                '-': '',
                'Type': data.get('type'),
                'A Type': source.get('nodeType'),
                'A Id': source.get('id'),
                'A Label': 'Label',
                'A Port': data.get('portA') || '',
                'B Type': target.get('nodeType'),
                'B Id': target.get('id'),
                'B Label': 'Label',
                'B Port': data.get('portB') || '',
            }
        }
    };

    function displayLink(data) {
        init();

        linkData = formatLinkData(data);
        render();
    }

    function render() {
        linkPanel.el.show();
        linkPanel.emptyRegions();

        var svg = linkPanel.appendToHeader('div')
                .classed('icon', true)
                .append('svg'),
            title = linkPanel.appendToHeader('h2'),
            table = linkPanel.appendToBody('table'),
            tbody = table.append('tbody');

        title.text(linkData.title);
        gs.addGlyph(svg, 'bird', 24, 0, [1, 1]);
        listProps(tbody, linkData);
    }

    function show() {
        linkPanel.el.show();
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
        wss.unbindHandlers(handlerMap);
        linkPanel.destroy();
    }

    angular.module('ovTopo2')
    .factory('Topo2LinkPanelService',
    ['Topo2DetailsPanelService', 'GlyphService', 'WebSocketService', 'FlashService', 'ListService',
        function (_ps_, _gs_, _wss_, _flash_, _listService_) {

            Panel = _ps_;
            gs = _gs_;
            wss = _wss_;
            flash = _flash_;
            listProps = _listService_;

            return {
                displayLink: displayLink,
                init: init,
                show: show,
                hide: hide,
                toggle: toggle,
                destroy: destroy,
                isVisible: function () { return linkPanel.isVisible(); }
            };
        }
    ]);

})();
