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
 ONOS GUI -- Topo2 Hosts Panel
 Module that builds the Hosts Panel for the selected Host
 */

(function () {
    'use strict';

    // Injected Services
    var panel, gs, flash, ls, ns;

    // Internal State
    var hostPath = 'host',
        hostPanel, hostData;

    function init() {
        hostPanel = panel();
    }

    function formatHostData(data) {
        var format = {
            title: data.title(),
            propOrder: ['MAC', 'IP', 'VLAN', 'Configured'],
            propLabels: {
                'MAC': 'MAC',
                'Configured': 'Configured',
                'IP': 'IP',
                'VLAN': 'VLAN',
            },
            propValues: {
                '-': '',
                'MAC': data.get('id'),
                'IP': data.get('ips')[0],
                'VLAN': 'None', // TODO: VLAN is not currently in the data received from backend
                'Configured': data.get('configured'),
            },
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

        var navFn = function () {
            ns.navTo(hostPath, { hostId: hostData.title });
        };

        var svg = hostPanel.appendToHeader('div')
                .classed('icon', true)
                .append('svg'),
            title = hostPanel.appendToHeader('h2')
                .on('click', navFn)
                .classed('clickable', true),
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
        'NavService',
        function (_ps_, _gs_, _flash_, _ls_, _ns_) {

            panel = _ps_;
            gs = _gs_;
            flash = _flash_;
            ls = _ls_;
            ns = _ns_;

            return {
                displayPanel: displayPanel,
                init: init,
                show: show,
                hide: hide,
                toggle: toggle,
                destroy: destroy,
                isVisible: function () { return hostPanel.isVisible(); },
            };
        },
    ]);

})();
