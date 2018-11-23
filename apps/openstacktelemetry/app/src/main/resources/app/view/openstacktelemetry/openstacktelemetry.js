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
 ONOS GUI -- Openstack Telemetry View Module
 */
(function () {
    'use strict';

    // injected references
    var $log, $scope, $location, ks, fs, cbs, ns, tov, ots, wss, tts, api, gs, ps, ds;

    // constants
    var pCls = 'topo-p',
        idTelemetry = 'topo-p-telemetry',
        idTelemetryDiag = 'telemetry-dialog',
        telemetryPanelOpts = {
            edge: 'left',
            width: 330, // telemetry panel width
        }

    // panels
    var telemetry;

    // selected info (by mouse click event)
    var selectedItem

    // constants
    var ostIsActReq = 'openstackFlowStatsIsActivatedRequest',
        ostIsActResp =  'openstackFlowStatsIsActivatedResponse',
        ostCreateReq = 'openstackFlowStatsCreateRequest',
        ostCreateResp = 'openstackFlowStatsCreateResponse';

    var topoLion = function (x) {
        return '#ttrafov#' + x + '#';
    };

    var overlay = {
        overlayId: 'ostelemetry-overlay',
        glyphId: 'm_details',
        tooltip: 'Openstack Telemetry Overlay',

        activate: function () {
            $log.debug("Openstack telemetry topology overlay is ACTIVATED");
        },
        deactivate: function () {
            destroyTelemetryPanel();
            $log.debug("Openstack telemetry topology overlay is DEACTIVATED");
        },

        // detail panel button definitions
        buttons: {
            createHostTelemetryBtn: {
                gid: 'm_details',
                tt: 'Create Host-to-Host Telemetry',
                cb: displayTelemetry
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
                $log.debug("Selected object: ", selectOrder);
                for (var index in selectOrder) {
                    if (index == 0) {
                        selectedHost.src = selectOrder[index];
                        $log.debug("Source host: ", selectedHost.src);
                    } else if (index == 1) {
                        selectedHost.dst = selectOrder[index];
                        $log.debug("Destination host: ", selectedHost.dst);
                    }
                }

                selectedItem = selectedHost;

                tov.addDetailButton('showRelatedTraffic');
                if(selectOrder.length == 2) {
                    tov.addDetailButton('createHostTelemetryBtn');
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

    function hideTelemetryPanel() {
        telemetry.panel().hide();
    }

    function destroyTelemetryPanel() {
        if(telemetry != null) {
            telemetry.destroy();
        }
        telemetry = null;
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

        return createDiv('telemetry-button')
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

    function displayTelemetry() {
        $log.debug("sendEvent openstackFlowStatsIsActivatedRequest: ", selectedItem);
        wss.sendEvent(ostIsActReq, selectedItem);
    }

    function respIsActCb(selected) {
        $log.debug("openstackFlowStatsIsActivatedResponse: ", selected);
        if(telemetry == null) {
            telemetry = createTopoPanel(idTelemetry, telemetryPanelOpts);
        }
        telemetry.setup();

        var svg = telemetry.appendHeader('div')
                     .classed('icon', true)
                     .append('svg'),
            title = telemetry.appendHeader('h2'),
            table = telemetry.appendBody('table'),
            tbody = table.append('tbody'),
            glyphId = 'm_details';

        gs.addGlyph(svg, glyphId, 30, 0, [1, 1]);

        title.text('Create Openstack Telemetry');

        addInput(tbody, 'text', 'srcIp', 'Source IP', selected.srcName);
        addInput(tbody, 'text', 'dstIp', 'Destination IP', selected.dstName);
        addInput(tbody, 'radio', 'ipProto', 'Protocol', selected.ipProtoList);
        addInput(tbody, 'number', 'srcPort', 'Source Port', "");
        addInput(tbody, 'number', 'dstPort', 'Destination Port', "");

        telemetry.appendFooter('hr');
        var footTr = telemetry.appendFooter('table').append('tbody').append('tr');

        addButton(footTr, createTelemetry, 'Create');
        addButton(footTr, hideTelemetryPanel, 'Cancel');

        telemetry.panel().show();
    }

    function createTelemetry() {
        var telemetryInfo = {};

        telemetryInfo.srcIp = document.getElementById('srcIp').value;
        telemetryInfo.dstIp = document.getElementById('dstIp').value;
        telemetryInfo.ipProto = document.querySelector('input[name="Protocol"]:checked').value;
        telemetryInfo.srcPort = document.getElementById('srcPort').value;
        telemetryInfo.dstPort = document.getElementById('dstPort').value;

        if(telemetryInfo.srcPort == ""){
            telemetryInfo.srcPort = 0;
        }
        if(telemetryInfo.dstPort == ""){
            telemetryInfo.dstPort = 0;
        }

        $log.debug("Creation request: ", telemetryInfo);
        wss.sendEvent(ostCreateReq, telemetryInfo);
        hideTelemetryPanel();
    }

    function respCreateCb(result) {
        $log.debug("Creation response: ", result);

        function dOK() {
            if(result.result == "Failed"){
                displayTelemetry(selectedItem);
            } else {
                destroyTelemetryPanel();
            }
        }

        function createContent(value) {
            var content = ds.createDiv();
            content.append('p').text(value);
            return content;
        }

        ds.openDialog(idTelemetryDiag, telemetryPanelOpts)
            .setTitle("Create Telemetry " + result.result)
            .addContent(createContent(result.value))
            .addOk(dOK);
    }

    /*
     Variable for Chart View
     */
    var hasFlow;

    var gFlowId;
    var gPeriod;

    var labels = new Array(1);
    var data = new Array(2);
    for (var i = 0; i < 2; i++) {
        data[i] = new Array(1);
    }

    var max;

    function ceil(num) {
        if (isNaN(num)) {
            return 0;
        }
        var pre = num.toString().length - 1
        var pow = Math.pow(10, pre);
        return (Math.ceil(num / pow)) * pow;
    }

    function maxInArray(array) {
        var merged = [].concat.apply([], array);
        return Math.max.apply(null, merged);
    }

    /*
     Chart View : Controller
     */
    angular.module('ovOpenstacktelemetry', ["chart.js"])
        .controller('OvOpenstacktelemetryCtrl',
        ['$log', '$scope', '$location', 'FnService', 'ChartBuilderService', 'NavService',

        function (_$log_, _$scope_, _$location_, _fs_, _cbs_, _ns_) {
            var params;
            $log = _$log_;
            $scope = _$scope_;
            $location = _$location_;
            fs = _fs_;
            cbs = _cbs_;
            ns = _ns_;

            params = $location.search();

            if (params.hasOwnProperty('flowOpt')) {
                $scope.flowOpt = params['flowOpt'];
                hasFlow = true;
            } else if (params.hasOwnProperty('periodOpt')) {
                $scope.periodOpt = params['periodOpt'];
                hasFlow = true;
            } else {
                hasFlow = false;
            }

            cbs.buildChart({
                scope: $scope,
                tag: 'openstacktelemetry',
                query: params
            });

            $scope.$watch('chartData', function () {
                if (!fs.isEmptyObject($scope.chartData)) {
                    $scope.showLoader = false;
                    var length = $scope.chartData.length;
                    labels = new Array(length);
                    for (var i = 0; i < 2; i++) {
                        data[i] = new Array(length);
                    }

                    $scope.chartData.forEach(function (cm, idx) {
                        data[0][idx] = (cm.curr_acc_packet - cm.prev_acc_packet);
                        data[1][idx] = (cm.curr_acc_byte - cm.prev_acc_byte);

                        labels[idx] = cm.label;
                    });
                }

                max = maxInArray(data)
                $scope.labels = labels;
                $scope.data = data;
                $scope.options = {
                    scaleOverride : true,
                    scaleSteps : 10,
                    scaleStepWidth : ceil(max) / 10,
                    scaleStartValue : 0,
                    scaleFontSize : 16
                };
                $scope.onClick = function (points, evt) {
                    var label = labels[points[0]._index];
                    if (label) {
                        ns.navTo('openstacktelemetry', { flowOpt: label });
                        $log.log(label);
                    }
                };

                if (!fs.isEmptyObject($scope.annots)) {
                    $scope.flowIds = JSON.parse($scope.annots.flowIds);
                    $scope.periodOptions = JSON.parse($scope.annots.periodOptions);
                }

                $scope.onChange = function (flowId) {
                    gFlowId = flowId;
                    ns.navTo('openstacktelemetry', { periodOpt: gPeriod , flowOpt: flowId });
                };

                $scope.onPeriodChange = function (period) {
                    gPeriod = period;
                    ns.navTo('openstacktelemetry', { periodOpt: period , flowOpt: gFlowId });
                };
            });

            $scope.series = ['Current Packet', 'Current Byte'];

            $scope.labels = labels;
            $scope.data = data;

            $scope.chartColors = [
                      '#286090',
                      '#F7464A',
                      '#46BFBD',
                      '#FDB45C',
                      '#97BBCD',
                      '#4D5360',
                      '#8c4f9f'
                    ];
            Chart.defaults.global.colours = $scope.chartColors;

            $scope.showLoader = true;

            $log.log('OvOpenstacktelemetryCtrl has been created');
        }])
        // Network Topology View : Factory
        .factory('OpenstacktelemetryService',
        ['$log', 'FnService', 'WebSocketService', 'GlyphService', 'PanelService', 'DialogService',

        function (_$log_, _fs_, _wss_, _gs_, _ps_, _ds_) {
            $log = _$log_;
            fs = _fs_;
            wss = _wss_;
            gs = _gs_;
            ps = _ps_;
            ds = _ds_;

            var handlers = {},
                telemetryOverlay = 'ostelemetry-overlay',
                defaultOverlay = 'traffic';

            handlers[ostIsActResp] = respIsActCb;
            handlers[ostCreateResp] = respCreateCb;

            wss.bindHandlers(handlers);

            return {
                displayTelemetry: displayTelemetry
            };
        }])
        .run(['$log', 'TopoOverlayService', 'OpenstacktelemetryService', 'TopoTrafficService',

            function (_$log_, _tov_, _ots_, _tts_) {
                $log = _$log_;
                tov = _tov_;
                ots = _ots_;
                tts = _tts_;
                tov.register(overlay);
            }]
        );
}());
