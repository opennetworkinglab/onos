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
    var hostPanel, hostData;

    function init() {
        hostPanel = panel();
    }

    function formatHostData(data) {
        var format = {
            title: data.get('id'),
            propOrder: ['MAC', 'IP', 'VLAN'],
            props: {
                '-': '',
                'MAC': data.get('id'),
                'IP': data.get('ips')[0],
                'VLAN': 'None' // TODO: VLAN is not currently in the data received from backend
            }
        };

        if (data.get('location')) {
            format.propOrder.push('-', 'Latitude', 'Longitude');
            format.props['Latitude'] = data.get('location').lat;
            format.props['Longitude'] = data.get('location').lng;
        }

        return format;
    }

    function displayPanel(data) {
        init();

        hostData = formatHostData(data);
        render();
    }

    function render() {
        hostPanel.show();
        hostPanel.emptyRegions();

        var svg = hostPanel.appendToHeader('div')
                .classed('icon', true)
                .append('svg'),
            title = hostPanel.appendToHeader('h2'),
            table = hostPanel.appendToBody('table'),
            tbody = table.append('tbody');

        title.text(hostData.title);
        gs.addGlyph(svg, 'bird', 24, 0, [1, 1]);
        ls.listProps(tbody, hostData);
    }

    function show() {
        hostPanel.show();
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
    .factory('Topo2HostsPanelService', [
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
                isVisible: function () { return hostPanel.isVisible(); }
            };
        }
    ]);

})();
