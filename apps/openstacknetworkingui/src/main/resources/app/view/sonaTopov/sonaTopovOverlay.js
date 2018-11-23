/*
 * Copyright 2017-present Open Networking Foundation
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

/* OpenStack Networking UI Overlay description */
(function () {
    'use strict';

    // injected refs
    var $log, tov, sts, flash, ds, wss;
    var traceSrc = null;
    var traceDst = null;
    var srcDeviceId = null;
    var dstDeviceId = null;

    var traceInfoDialogId = 'traceInfoDialogId',
        traceInfoDialogOpt =   {
            width: 350,
            edge: 'left',
            margin: 20,
            hideMargin: -20
        }

       var overlay = {
        // NOTE: this must match the ID defined in AppUiTopovOverlay
        overlayId: 'sona-overlay',
        glyphId: '*star4',
        tooltip: 'OpenstackNetworking UI',

        glyphs: {
            star4: {
                vb: '0 0 8 8',
                d: 'M1,4l2,-1l1,-2l1,2l2,1l-2,1l-1,2l-1,-2z'
            },
            banner: {
                vb: '0 0 6 6',
                d: 'M1,1v4l2,-2l2,2v-4z'
            }
        },

        activate: function () {
            $log.debug("OpenstackNetworking UI ACTIVATED");
        },
        deactivate: function () {
            sts.stopDisplay();
            $log.debug("OpenstackNetworking UI DEACTIVATED");
        },

        // detail panel button definitions
        buttons: {
            flowtrace: {
                gid: 'checkMark',
                tt: 'Flow Trace',
                cb: function (data) {

                    if (traceSrc == null && data.navPath == 'host') {
                        traceSrc = data.propValues.ip;
                        srcDeviceId = data.propValues.DeviceId

                        flash.flash('Src ' + traceSrc + ' selected. Please select the dst');
                    } else if (traceDst == null && data.title != traceSrc && data.navPath == 'host') {
                        traceDst = data.propValues.ip;
                        dstDeviceId = data.propValues.DeviceId;
                        openTraceInfoDialog();
                        flash.flash('Dst ' + traceDst + ' selected. Press Request button');
                    }

                    $log.debug('Perform flow trace test between VMs:', data);
                }
            },

            reset: {
                gid: 'xMark',
                tt: 'Reset',
                cb: function (data) {
                    flash.flash('Reset flow trace');
                    traceSrc = null;
                    traceDst = null;
                    ds.closeDialog();
                    $log.debug('BAR action invoked with data:', data);
                }
            },
            toGateway: {
                gid: 'm_switch',
                tt: 'Trace to Gateway',
                cb: function (data) {
                    if (traceSrc != null && data.title == traceSrc && data.navPath == 'host') {
                        //Set traceSrc to traceDst in case trace to gateway
                        traceDst = traceSrc;
                        dstDeviceId = 'toGateway';
                        openTraceInfoDialog();
                        flash.flash('Trace to Gateway');
                    }
                }
            },
            toExternal: {
                gid: 'm_cloud',
                tt: 'Trace to External',
                cb: function (data) {
                    if (traceSrc != null && data.title == traceSrc && data.navPath == 'host') {
                        //Set traceDst to 8.8.8.8 to check external connection
                        traceDst = '8.8.8.8';
                        dstDeviceId = 'toExternal';
                        openTraceInfoDialog();
                        flash.flash('Trace to External')
                    }
               }
            },
        },

        keyBindings: {
                    0: {
                        cb: function () { sts.stopDisplay(); },
                        tt: 'Cancel OpenstackNetworking UI Overlay Mode',
                        gid: 'xMark'
                    },
                    V: {
                        cb: function () {
                            wss.bindHandlers({
                                flowTraceResult: sts,
                            });
                            sts.startDisplay('mouse');
                        },
                        tt: 'Start OpenstackNetworking UI Overlay Mode',
                        gid: 'crown'
                    },

                    _keyOrder: [
                        '0', 'V'
                    ]
                },

        hooks: {
            // hook for handling escape key
            // Must return true to consume ESC, false otherwise.
            escape: function () {
                // Must return true to consume ESC, false otherwise.
                return sts.stopDisplay();
            },
            mouseover: function (m) {
                // m has id, class, and type properties
                $log.debug('mouseover:', m);
                sts.updateDisplay(m);
            },
            mouseout: function () {
                $log.debug('mouseout');
                sts.updateDisplay();
            }
        }
    };

    function openTraceInfoDialog() {
        ds.openDialog(traceInfoDialogId, traceInfoDialogOpt)
            .setTitle('Flow Trace Information')
            .addContent(createTraceInfoDiv(traceSrc, traceDst))
            .addOk(flowTraceResultBtn, 'Request')
            .bindKeys();
    }

    function createTraceInfoDiv(src, dst) {
        var texts = ds.createDiv('traceInfo');
        texts.append('hr');
        texts.append('table').append('tbody').append('tr');

        var tBodySelection = texts.select('table').select('tbody').select('tr');

        tBodySelection.append('td').text('Source IP:').attr("class", "label");
        tBodySelection.append('td').text(src).attr("class", "value");

        texts.select('table').select('tbody').append('tr');

        tBodySelection = texts.select('table').select('tbody').select('tr:nth-child(2)');

        tBodySelection.append('td').text('Destination IP:').attr("class", "label");
        if (dst == src) {
            tBodySelection.append('td').text('toGateway').attr("class", "value");
        } else {
            tBodySelection.append('td').text(dst).attr("class", "value");
        }

        texts.select('table').select('tbody').append('tr');
        tBodySelection = texts.select('table').select('tbody').select('tr:nth-child(3)');
        tBodySelection.append('td').text('SrcDeviceId').attr("class", "label");
        tBodySelection.append('td').text(srcDeviceId).attr("class", "value");

        texts.select('table').select('tbody').append('tr');
        tBodySelection = texts.select('table').select('tbody').select('tr:nth-child(4)');
        tBodySelection.append('td').text('DstDeviceId').attr("class", "label");
        tBodySelection.append('td').text(dstDeviceId).attr("class", "value");

        texts.append('hr');

        return texts;
    }

    function flowTraceResultBtn() {
        sts.sendFlowTraceRequest(traceSrc, traceDst, srcDeviceId, dstDeviceId, true);
        ds.closeDialog();
        traceSrc = null;
        traceDst = null;
        flash.flash('Send Flow Trace Request');
    }

    function buttonCallback(x) {
        $log.debug('Toolbar-button callback', x);
    }

    // invoke code to register with the overlay service
    angular.module('ovSonaTopov')
        .run(['$log', 'TopoOverlayService', 'SonaTopovService',
                'FlashService', 'DialogService', 'WebSocketService',

        function (_$log_, _tov_, _sts_, _flash_, _ds_, _wss_) {
            $log = _$log_;
            tov = _tov_;
            sts = _sts_;
            flash = _flash_;
            ds = _ds_;
            wss = _wss_;
            tov.register(overlay);
        }]);

}());
