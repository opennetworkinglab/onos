/*
 * Copyright 2018-present Open Networking Foundation
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
 ONOS GUI -- Openstack Vtap View Module
 */
(function () {
    'use strict';

    // injected refs
    var $log, tov, ovts, fs, wss, tts, api, gs, ps, ds;

    // constants
    var pCls = 'topo-p',
        idVta = 'topo-p-vtap',
        idVDi = 'vtap-dialog',
        vtapPanelOpts = {
            edge: 'left',
            width: 330, // vtap panel width
        }

    // panels
    var vtap;

    // selected info
    var selectedItem;

    // constants
    var osvIsActReq = 'openstackVtapIsActivatedRequest',
        osvIsActResp =  'openstackVtapIsActivatedResponse',
        osvCreateReq = 'openstackVtapCreateRequest',
        osvCreateResp = 'openstackVtapCreateResponse';

    // function to be replaced by the localization bundle function
    var topoLion = function (x) {
        return '#ttrafov#' + x + '#';
    };

    var overlay = {
        overlayId: 'vtap-overlay',
        glyphId: 'm_details',
        tooltip: 'Openstack Vtap Overlay',

        activate: function () {
            $log.debug("Openstack Vtap topology overlay ACTIVATED");
        },
        deactivate: function () {
            destroyVtapPanel();
            $log.debug("Openstack Vtap topology overlay DEACTIVATED");
        },

        // detail panel button definitions
        buttons: {
            createHostVtapBtn: {
                gid: 'm_details',
                tt: 'Create Host-to-Host Vtap',
                cb: displayVtap
            },
            showRelatedTraffic: {
                gid: 'm_relatedIntents',
                tt: function () { return topoLion('tr_btn_show_related_traffic'); },
                cb: function (data) { tts.showRelatedIntents(); },
            },
        },

        hooks: {
            multi: function (selectOrder) {
                var selectedHost = new Object();
                for (var index in selectOrder) {
                    if (index == 0) {
                        selectedHost.src = selectOrder[index];
                    } else if (index == 1) {
                        selectedHost.dst = selectOrder[index];
                    }
                }

                selectedItem = selectedHost;

                tov.addDetailButton('showRelatedTraffic');
                if(selectOrder.length == 2) {
                    tov.addDetailButton('createHostVtapBtn');
                }
            }
        }
    };

    // Panel API
    function createTopoPanel(id, opts) {
        var p = ps.createPanel(id, opts),
            pid = id,
            header, body, footer;
        p.classed(pCls, true);

        function panel() {
            return p;
        }

        function hAppend(x) {
            return header.append(x);
        }

        function bAppend(x) {
            return body.append(x);
        }

        function fAppend(x) {
            return footer.append(x);
        }

        function setup() {
            p.empty();

            p.append('div').classed('header', true);
            p.append('div').classed('body', true);
            p.append('div').classed('footer', true);

            header = p.el().select('.header');
            body = p.el().select('.body');
            footer = p.el().select('.footer');
        }

        function destroy() {
            ps.destroyPanel(pid);
        }

        return {
            panel: panel,
            setup: setup,
            destroy: destroy,
            appendHeader: hAppend,
            appendBody: bAppend,
            appendFooter: fAppend,
        };
    }

    function hideVtapPanel() {
        vtap.panel().hide();
    }

    function destroyVtapPanel() {
        if(vtap != null) {
            vtap.destroy();
        }
        vtap = null;
    }

    function addInput(tbody, type, id, label, value) {
        var tr = tbody.append('tr'),
            lab;
        if (typeof label === 'string') {
            lab = label.replace(/_/g, ' ');
        } else {
            lab = label;
        }

        tr.append('td').attr('class', 'label').text(lab + ' :');

        if (type == 'radio') {
            var td = tr.append('td');
            for(var index in value) {
                if(index == 0) {
                    td.append('input').classed( type + '-input', true)
                                          .attr('type', type)
                                          .attr('value', value[index])
                                          .attr('name', label)
                                          .attr('id', id)
                                          .attr('checked', 'true');
                } else {
                    td.append('input').classed( type + '-input', true)
                                          .attr('type', type)
                                          .attr('name', label)
                                          .attr('id', id)
                                          .attr('value', value[index]);
                }
                td.append('span').text(value[index]);
            }
        } else {
            tr.append('td').append('input').classed(type + '-input', true).attr('type', type)
                .attr('id', id).attr('value', value);
        }
    }

    function addButton(tr, callback, value) {
        tr.append('td').append('input').classed('button-input', true).attr('type', 'button')
                        .attr('value', value).on('click', callback);
    }

    function makeButton(callback, text, keyName) {
        var cb = fs.isF(callback),
            key = fs.isS(keyName);

        function invoke() {
            cb && cb();
        }

        return createDiv('vtap-button')
            .text(text)
            .on('click', invoke);
    }

    function createDiv(cls) {
        var div = d3.select(document.createElement('div'));
        if (cls) {
            div.classed(cls, true);
        }
        return div;
    }

    function displayVtap() {
        $log.debug("sendEvent openstackVtapIsActivatedRequest: ", selectedItem);
        wss.sendEvent(osvIsActReq, selectedItem);
    }

    function respIsActCb(selected) {
        $log.debug("openstackVtapIsActivatedResponse: ", selected);
        if(vtap == null) {
            vtap = createTopoPanel(idVta, vtapPanelOpts);
        }
        vtap.setup();

        var svg = vtap.appendHeader('div')
                     .classed('icon', true)
                     .append('svg'),
            title = vtap.appendHeader('h2'),
            table = vtap.appendBody('table'),
            tbody = table.append('tbody'),
            glyphId = 'm_details';

        gs.addGlyph(svg, glyphId, 30, 0, [1, 1]);

        title.text('Create OpenstackVtap');

        addInput(tbody, 'text', 'srcIp', 'Source IP', selected.srcName);
        addInput(tbody, 'text', 'dstIp', 'Destination IP', selected.dstName);
        addInput(tbody, 'radio', 'ipProto', 'Protocol', selected.ipProtoList);
        addInput(tbody, 'number', 'srcPort', 'Source Port', "");
        addInput(tbody, 'number', 'dstPort', 'Destination Port', "");
        addInput(tbody, 'radio', 'vtapType', 'Type', selected.vtapTypeList);

        vtap.appendFooter('hr');
        var footTr = vtap.appendFooter('table').append('tbody').append('tr');

        addButton(footTr, createVtap, 'Create');
        addButton(footTr, hideVtapPanel, 'Cancel');

        vtap.panel().show();
    }

    function createVtap() {
        var vtapInfo = {};

        vtapInfo.srcIp = document.getElementById('srcIp').value;
        vtapInfo.dstIp = document.getElementById('dstIp').value;
        vtapInfo.ipProto = document.querySelector('input[name="Protocol"]:checked').value;
        vtapInfo.srcPort = document.getElementById('srcPort').value;
        vtapInfo.dstPort = document.getElementById('dstPort').value;
        vtapInfo.vtapType = document.querySelector('input[name="Type"]:checked').value;

        if(vtapInfo.srcPort == ""){
            vtapInfo.srcPort = 0;
        }
        if(vtapInfo.dstPort == ""){
            vtapInfo.dstPort = 0;
        }

        $log.debug("sendEvent openstackVtapCreateRequest: ", vtapInfo);
        wss.sendEvent(osvCreateReq, vtapInfo);
        hideVtapPanel();

    }

    function respCreateCb(result) {
        $log.debug("respCreateCb: ", result);

        function dOK() {
            if(result.result == "Failed"){
                displayVtap(selectedItem);
            } else {
                destroyVtapPanel();
            }
        }

        function createContent(value) {
            var content = ds.createDiv();
            content.append('p').text(value);
            return content;
        }

        ds.openDialog(idVDi, vtapPanelOpts)
            .setTitle("Create Vtap " + result.result)
            .addContent(createContent(result.value))
            .addOk(dOK);
    }

    // invoke code to register with the overlay service
    angular.module('ovOpenstackvtap', [])
        .factory('OpenstackVtapTopovService',
        ['$log', 'FnService', 'WebSocketService', 'GlyphService', 'PanelService', 'DialogService',

        function (_$log_, _fs_, _wss_, _gs_, _ps_, _ds_) {
            $log = _$log_;
            fs = _fs_;
            wss = _wss_;
            gs = _gs_;
            ps = _ps_;
            ds = _ds_;

            var handlers = {},
                vtapOverlay = 'vtap-overlay',
                defaultOverlay = 'traffic';

            handlers[osvIsActResp] = respIsActCb;
            handlers[osvCreateResp] = respCreateCb;

            wss.bindHandlers(handlers);

            return {
                displayVtap: displayVtap
            };
        }])
        .run(['$log', 'TopoOverlayService', 'OpenstackVtapTopovService', 'TopoTrafficService',

            function (_$log_, _tov_, _ovts_, _tts_) {
                $log = _$log_;
                tov = _tov_;
                ovts = _ovts_;
                tts = _tts_;
                tov.register(overlay);
            }]
        );
}());
