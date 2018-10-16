/*
 * Copyright 2017-present Open Networking Laboratory
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
 OpenStack Networking UI Service

  Provides a mechanism to highlight hosts, devices and links according to
  a virtual network. Also provides trace functionality to prove that
  flow rules for the specific vm are installed appropriately.
 */

(function () {
    'use strict';

    // injected refs
    var $log, fs, flash, wss, ds;

    var traceSrc = null;
    var traceDst = null;
    var srcDeviceId = null;
    var dstDeviceId = null;
    var uplink = null;

    // constants
    var displayStart = 'openstackNetworkingUiStart',
        displayUpdate = 'openstackNetworkingUiUpdate',
        displayStop = 'openstackNetworkingUiStop',
        flowTraceRequest = 'flowTraceRequest';

    // internal state
    var currentMode = null;

    // === ---------------------------
    // === Helper functions

    function sendDisplayStart(mode) {
        wss.sendEvent(displayStart, {
            mode: mode
        });
    }

    function sendDisplayUpdate(what) {
        wss.sendEvent(displayUpdate, {
            id: what ? what.id : ''
        });
    }

    function sendDisplayStop() {
        wss.sendEvent(displayStop);
    }

    function sendFlowTraceRequest(src, dst, srcDeviceId, dstDeviceId, uplink) {
        wss.sendEvent(flowTraceRequest, {
            srcIp: src,
            dstIp: dst,
            srcDeviceId: srcDeviceId,
            dstDeviceId: dstDeviceId,
            uplink: uplink,
        });
        flash.flash('sendFlowTraceRequest called');
    }

    // === ---------------------------
    // === Main API functions

    function startDisplay(mode) {
        if (currentMode === mode) {
            $log.debug('(in mode', mode, 'already)');
        } else {
            currentMode = mode;
            sendDisplayStart(mode);

            flash.flash('Starting Openstack Networking UI mode');
        }
    }

    function updateDisplay(m) {
        if (currentMode) {
            sendDisplayUpdate(m);
        }
    }

    function stopDisplay() {
        if (currentMode) {
            currentMode = null;
            sendDisplayStop();
            flash.flash('Canceling Openstack Networking UI Overlay mode');
            return true;
        }
        return false;
    }

    function dOk() {
        ds.closeDialog();
    }

    function openFlowTraceResultDialog(data) {
        var flowTraceResultDialogId = 'flowTraceResultDialogId',
            flowTraceResultDialogOpt = {
                width: 650,
                edge: 'left',
                margin: 20,
                hideMargin: -20
            }
        var traceSuccess = data.traceSuccess == true ? "SUCCESS" : "FALSE";
        traceSrc = data.srcIp;
        traceDst = data.dstIp;
        srcDeviceId = data.srcDeviceId;
        dstDeviceId = data.dstDeviceId;
        uplink = data.uplink;

        if (data.uplink == true) {
            ds.openDialog(flowTraceResultDialogId, flowTraceResultDialogOpt)
                .setTitle('Flow Trace Result: ' + traceSuccess)
                .addContent(createTraceResultInfoDiv(data))
                .addOk(downlinkTraceRequestBtn, 'Downlink Trace')
                .bindKeys();
        } else {
            ds.openDialog(flowTraceResultDialogId, flowTraceResultDialogOpt)
                .setTitle('Flow Trace Result: ' + traceSuccess)
                .addContent(createTraceResultInfoDiv(data))
                .addOk(dOk, 'Close')
                .bindKeys();
        }

    }

    function downlinkTraceRequestBtn() {
        sendFlowTraceRequest(traceSrc, traceDst, srcDeviceId, dstDeviceId, false);
        ds.closeDialog();
        flash.flash('Send Downlink Flow Trace Request')
    }

    function createTraceResultInfoDiv(data) {
        var texts = ds.createDiv('flowTraceResult');

        texts.append('div').attr("class", "table-header");
        texts.append('div').attr("class", "table-body");

        texts.select('.table-header').append('table').append('tbody').append('tr');
        texts.select('.table-body').append('table').append('tbody');


        var theaderSelection = texts.select('.table-header')
            .select('table').select('tbody').select('tr');

        theaderSelection.append('td').text('Node');
        theaderSelection.append('td').text('Table Id');
        theaderSelection.append('td').text('Priority');
        theaderSelection.append('td').text('Selector');
        theaderSelection.append('td').text('Action');

        var tbodySelection = texts.select('.table-body').select('table').select('tbody');
        var rowNum = 1;

        data.traceResult.forEach(function(result) {
            result.flowRules.forEach(function(flowRule) {
                tbodySelection.append('tr');
                var tbodyTrSelection = tbodySelection.select('tr:nth-child(' + rowNum + ')');
                tbodyTrSelection.append('td').text(result.traceNodeName);
                tbodyTrSelection.append('td').text(flowRule.table);
                tbodyTrSelection.append('td').text(flowRule.priority);
                tbodyTrSelection.append('td').text(flowRule.selector);
                tbodyTrSelection.append('td').text(flowRule.actions);
                if (jsonToSring(flowRule.actions).includes("drop")) {
                    tbodyTrSelection.attr("class", "drop");
                }
                rowNum++;
            });

        });

        return texts;
    }

    function jsonToSring(jsonData) {
        var result = [];
        for (var key in jsonData) {
            result.push(key + ':' + jsonData[key]);
        }

        return result.join('/');

    }

    function flowTraceResult(data) {
        flash.flash('flowTraceResult called');
        $log.debug(data);

        openFlowTraceResultDialog(data)
    }

    // === ---------------------------
    // === Module Factory Definition

    angular.module('ovSonaTopov', [])
        .factory('SonaTopovService',
        ['$log', 'FnService', 'FlashService', 'WebSocketService', 'DialogService',

        function (_$log_, _fs_, _flash_, _wss_, _ds_) {
            $log = _$log_;
            fs = _fs_;
            flash = _flash_;
            wss = _wss_;
            ds = _ds_;

            return {
                startDisplay: startDisplay,
                updateDisplay: updateDisplay,
                stopDisplay: stopDisplay,
                flowTraceResult: flowTraceResult,
                sendFlowTraceRequest: sendFlowTraceRequest,
            };
        }]);
}());
