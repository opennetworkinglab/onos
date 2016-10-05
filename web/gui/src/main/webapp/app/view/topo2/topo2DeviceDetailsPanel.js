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
    var Panel, gs, wss, flash, bs, fs, ns;

    // Internal State
    var detailsPanel;

    // configuration
    var id = 'topo2-p-detail',
        className = 'topo-p',
        panelOpts = {
            width: 260          // summary and detail panel width
        },
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

        var options = angular.extend({}, panelOpts, {
            class: className
        });

        detailsPanel = new Panel(id, options);
        detailsPanel.p.classed(className, true);
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
        data.propOrder.forEach(function (p) {
            if (p === '-') {
                addSep(tbody);
            } else {
                addProp(tbody, p, data.props[p]);
            }
        });
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
            // else if (btn = _getButtonDef(id, data)) {
            //     addAction(btn);
            // }
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
            tbody = table.append('tbody'),
            navFn;

        gs.addGlyph(svg, (data.type || 'unknown'), 26);
        title.text(data.title);

        // // only add navigation when displaying a device
        // if (isDevice[data.type]) {
        //     navFn = function () {
        //         ns.navTo(devPath, { devId: data.id });
        //     };
        //
        //     svg.on('click', navFn);
        //     title.on('click', navFn);
        // }

        listProps(tbody, data);
        addBtnFooter();
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
        var on = detailsPanel.p.toggle(),
            verb = on ? 'Show' : 'Hide';
        flash.flash(verb + ' Summary Panel');
    }

    function show() {
        detailsPanel.p.show();
    }

    function hide() {
        detailsPanel.p.hide();
    }

    function destroy() {
        wss.unbindHandlers(handlerMap);
        detailsPanel.destroy();
    }

    angular.module('ovTopo2')
    .factory('Topo2DeviceDetailsPanel',
    ['Topo2PanelService', 'GlyphService', 'WebSocketService', 'FlashService',
    'ButtonService', 'FnService', 'NavService',
        function (_ps_, _gs_, _wss_, _flash_, _bs_, _fs_, _ns_) {

            Panel = _ps_;
            gs = _gs_;
            wss = _wss_;
            flash = _flash_;
            bs = _bs_;
            fs = _fs_;
            ns = _ns_;

            return {
                init: init,
                updateDetails: updateDetails,

                toggle: toggle,
                show: show,
                hide: hide,
                destroy: destroy
            };
        }
    ]);
})();
