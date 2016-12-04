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
 ONOS GUI -- Topology View Module.
 Module that displays the details panel for selected nodes
 */

(function () {
    'use strict';

    // Injected Services
    var Panel, gs, wss, flash, bs, fs, ns, listProps;

    // Internal State
    var detailsPanel;

    // configuration
    var id = 'topo2-p-detail',
        handlerMap = {
            'showDetails': showDetails
        };

    var coreButtons = {
        showDeviceView: {
            gid: 'switch',
            tt: 'Show Device View',
            path: 'device'
        },
        showFlowView: {
            gid: 'flowTable',
            tt: 'Show Flow View for this Device',
            path: 'flow'
        },
        showPortView: {
            gid: 'portTable',
            tt: 'Show Port View for this Device',
            path: 'port'
        },
        showGroupView: {
            gid: 'groupTable',
            tt: 'Show Group View for this Device',
            path: 'group'
        },
        showMeterView: {
            gid: 'meterTable',
            tt: 'Show Meter View for this Device',
            path: 'meter'
        }
    };

    function init() {

        bindHandlers();
        detailsPanel = Panel();
    }

    function addBtnFooter() {
        detailsPanel.appendToFooter('hr');
        detailsPanel.appendToFooter('div').classed('actionBtns', true);
    }

    function addAction(o) {
        var btnDiv = d3.select('#' + id)
            .select('.actionBtns')
            .append('div')
            .classed('actionBtn', true);
        bs.button(btnDiv, id + '-' + o.id, o.gid, o.cb, o.tt);
    }

    function installButtons(buttons, data, devId) {
        buttons.forEach(function (id) {
            var btn = coreButtons[id],
                gid = btn && btn.gid,
                tt = btn && btn.tt,
                path = btn && btn.path;

            if (btn) {
                addAction({
                    id: 'core-' + id,
                    gid: gid,
                    tt: tt,
                    cb: function () { ns.navTo(path, { devId: devId }); }
                });
            }
        });
    }

    function renderSingle(data) {

        detailsPanel.emptyRegions();

        var svg = detailsPanel.appendToHeader('div')
                .classed('icon clickable', true)
                .append('svg'),
            title = detailsPanel.appendToHeader('h2')
                .classed('clickable', true),
            table = detailsPanel.appendToBody('table'),
            tbody = table.append('tbody');

        gs.addGlyph(svg, (data.type || 'unknown'), 26);
        title.text(data.title);

        listProps(tbody, data);
        addBtnFooter();
    }

    function renderMulti(nodes) {
        detailsPanel.emptyRegions();

        var title = detailsPanel.appendToHeader('h3'),
            table = detailsPanel.appendToBody('table'),
            tbody = table.append('tbody');

        title.text('Selected Items');
        nodes.forEach(function (n, i) {
            addProp(tbody, i + 1, n.get('id'));
        });

        // addBtnFooter();
        show();
    }

    function bindHandlers() {
        wss.bindHandlers(handlerMap);
    }

    function updateDetails(id, nodeType) {
        wss.sendEvent('requestDetails', {
            id: id,
            class: nodeType
        });
    }

    function showDetails(data) {
        var buttons = fs.isA(data.buttons) || [];
        renderSingle(data);
        installButtons(buttons, data, data.id);
    }

    function toggle() {
        var on = detailsPanel.el.toggle(),
            verb = on ? 'Show' : 'Hide';
        flash.flash(verb + ' Details Panel');
    }

    function show() {
        detailsPanel.el.show();
    }

    function hide() {
        detailsPanel.el.hide();
    }

    function destroy() {
        wss.unbindHandlers(handlerMap);
        detailsPanel.destroy();
    }

    angular.module('ovTopo2')
    .factory('Topo2DeviceDetailsPanel',
    ['Topo2DetailsPanelService', 'GlyphService', 'WebSocketService', 'FlashService',
    'ButtonService', 'FnService', 'NavService', 'ListService', 
        function (_ps_, _gs_, _wss_, _flash_, _bs_, _fs_, _ns_, _listService_) {

            Panel = _ps_;
            gs = _gs_;
            wss = _wss_;
            flash = _flash_;
            bs = _bs_;
            fs = _fs_;
            ns = _ns_;
            listProps = _listService_;

            return {
                init: init,
                updateDetails: updateDetails,
                showMulti: renderMulti,

                toggle: toggle,
                show: show,
                hide: hide,
                destroy: destroy,
                isVisible: function () { return detailsPanel.isVisible(); }
            };
        }
    ]);
})();
