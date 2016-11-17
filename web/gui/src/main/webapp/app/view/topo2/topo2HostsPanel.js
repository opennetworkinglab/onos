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
    var hostPanel, hostData;

    function init() {
        hostPanel = Panel();
    }

    function formatHostData(data) {
        return {
            title: data.get('id'),
            propOrder: ['MAC', 'IP', 'VLAN', '-', 'Latitude', 'Longitude'],
            props: {
                '-': '',
                'MAC': data.get('id'),
                'IP': data.get('ips')[0],
                'VLAN': 'None', // TODO
                'Latitude': data.get('location').lat,
                'Longitude': data.get('location').lng,
            }
        }
    };

    function displayPanel(data) {
        init();

        hostData = formatHostData(data);
        render();
    }

    function render() {
        hostPanel.el.show();
        hostPanel.emptyRegions();

        var svg = hostPanel.appendToHeader('div')
                .classed('icon', true)
                .append('svg'),
            title = hostPanel.appendToHeader('h2'),
            table = hostPanel.appendToBody('table'),
            tbody = table.append('tbody');

        title.text(hostData.title);
        gs.addGlyph(svg, 'bird', 24, 0, [1, 1]);
        listProps(tbody, hostData);
    }

    function show() {
        hostPanel.el.show();
    }

    function hide() {
        hostPanel.el.hide();
    }

    function toggle() {
        var on = hostPanel.el.toggle(),
            verb = on ? 'Show' : 'Hide';
        flash.flash(verb + ' host Panel');
    }

    function destroy() {
        hostPanel.destroy();
    }

    angular.module('ovTopo2')
    .factory('Topo2HostsPanelService',
    ['Topo2DetailsPanelService', 'GlyphService', 'WebSocketService', 'FlashService', 'ListService',
        function (_ps_, _gs_, _wss_, _flash_, _listService_) {

            Panel = _ps_;
            gs = _gs_;
            wss = _wss_;
            flash = _flash_;
            listProps = _listService_;

            return {
                displayPanel: displayPanel,
                init: init,
                show: show,
                hide: hide,
                toggle: toggle,
                destroy: destroy,
                isVisible: function () { return hostPanel.isVisible(); }
            };
        }
    ]);

})();
