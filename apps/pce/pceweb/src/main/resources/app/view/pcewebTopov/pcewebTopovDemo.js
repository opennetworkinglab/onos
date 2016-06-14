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

/*PCE topology overlay web application implementation.*/

(function () {
    'use strict';

    // injected refs
    var $log, fs, flash, wss, tps, ns, tds, ds;
    var tunnelNameData, tunnelNameDataRemove;
   // constants
    var srcMessage = 'pceTopovSetSrc',
        dstMessage = 'pceTopovSetDst',
        clearMessage = 'pceTopovClear',
        setPathmsg =  'pceTopovSetMode',
        updatePathmsgQuery = 'pceTopovUpdateQuery',
        remPathmsgQuery = 'pceTopovRemQuery',
        updatePathmsg = 'pceTopovUpdate',
        remPathmsg = 'pceTopovRem',
        showTunnelInfoMsg = 'pceTopovShowTunnels',
        queryDisplayTunnelMsg = 'pceTopovTunnelDisplay',
        showTunnelInfoRemoveMsg = 'pceTopovShowTunnelsRem';
    // internal state
    var currentMode = null;
    var handlerMap = {},
        handlerMapRem = {};
    // === ---------------------------
    // === Helper functions

    // === ---------------------------
    // === Main API functions

    function setSrc(node) {
        wss.sendEvent(srcMessage, {
            id: node.id,
            type: node.type
        });
        flash.flash('Source node: ' + node.id);
    }

    function setDst(node) {
        wss.sendEvent(dstMessage, {
            id: node.id,
            type: node.type
        });
        flash.flash('Destination node: ' + node.id);
    }

    function clear() {
        wss.sendEvent(clearMessage);
        flash.flash('Cleared source and destination');
    }

    function dClose() {
        $log.debug('Dialog Close button clicked (or Esc pressed)');
    }

    function createUserText() {
        var content = ds.createDiv('constraints-input'),
            form = content.append('form'),
            p = form.append('p');

        function addAttribute(name, id, nameField, type) {
            if (type == 'radio') {
                p.append('input').attr({
                    type: type,
                    name: name,
                    id: id,
                    class: 'radioButtonSpace'
                });
            } else {
                p.append('input').attr({
                    type: type,
                    name: name,
                    id: id
                });
            }


            p.append('span').text(nameField);
            p.append('br');
        }

        //Add the bandwidth related inputs.
        addAttribute('band-width-name', 'band-width-box', 'Band Width', 'checkbox');
        addAttribute('band-width-value-name', 'band-width-value', null, 'number');
        addAttribute('pce-band-type', 'band-kpbs-val', 'kbps', 'radio');
        addAttribute('pce-band-type', 'band-mpbs-val', 'mbps', 'radio');
        //Add the cost type related inputs.
        addAttribute('pce-cost-type-name', 'pce-cost-type', 'Cost Type', 'checkbox');
        addAttribute('pce-cost-type-valname', 'pce-cost-type-igp', 'IGP', 'radio');
        addAttribute('pce-cost-type-valname', 'pce-cost-type-te', 'TE', 'radio');
        //Add the LSP type related inputs.
        addAttribute('pce-lsp-type-name', 'pce-lsp-type', 'Lsp Type', 'checkbox');
        addAttribute('pce-lsp-type-valname', 'pce-lsp-type-cr', 'With signalling', 'radio');
        addAttribute('pce-lsp-type-valname', 'pce-lsp-type-srbe', 'Without SR without signalling', 'radio');
        addAttribute('pce-lsp-type-valname', 'pce-lsp-type-srte', 'With SR without signalling', 'radio');
        //Add the tunnel name
        addAttribute('pce-tunnel-name', 'pce-tunnel-name-id', 'Tunnel Name', 'text');

        return content;
    }

    function createUserTextUpdate(data) {
        var content = ds.createDiv(),
            form = content.append('form'),
            p = form.append('p');

        p.append('span').text('Tunnel IDs');
        p.append('br');

        data.a.forEach( function (val, idx) {
            p.append('input').attr({
                id: 'tunnel-id-'+idx,
                type: 'radio',
                name: 'tunnel-id-name',
                value: val
            });

            p.append('span').text(val);
            p.append('br');

        } );

        return content;
    }

    function createUserTextUpdatePathEvent() {
        var content = ds.createDiv(),
            form = content.append('form'),
            p = form.append('p');

        function addAttribute(name, id, nameField, type) {
            if (type == 'radio') {
                p.append('input').attr({
                    type: type,
                    name: name,
                    id: id,
                    class: 'radioButtonSpace'
                });
            }
            else {
                p.append('input').attr({
                    type: type,
                    name: name,
                    id: id
                });
            }

            p.append('span').text(nameField);
            p.append('br');
        }

        //Add the bandwidth related inputs.
        addAttribute('band-width-name', 'update-band-width-box', 'Band Width', 'checkbox');
        addAttribute('band-width-value-name', 'update-band-width-value', null, 'number');
        addAttribute('pce-band-type', 'update-band-kpbs-val', 'kbps', 'radio');
        addAttribute('pce-band-type', 'update-band-mpbs-val', 'mbps', 'radio');
        //Add the cost type related inputs.
        addAttribute('pce-cost-type', 'update-pce-cost-type', 'Cost Type', 'checkbox');
        addAttribute('pce-cost-type-value', 'update-pce-cost-type-igp', 'IGP', 'radio');
        addAttribute('pce-cost-type-value', 'update-pce-cost-type-te', 'TE', 'radio');

        return content;
    }

    function createUserTextRemove(data) {

        var content = ds.createDiv(),
            form = content.append('form'),
            p = form.append('p');

        p.append('span').text('Tunnel IDs');
        p.append('br');

        data.a.forEach( function (val, idx) {
            p.append('input').attr({
                id: 'tunnel-id-remove-'+idx,
                type: 'checkbox',
                name: 'tunnel-id-name-remove',
                value: val
            });

            p.append('span').text(val);
            p.append('br');
        } );

        return content;
    }

    function isChecked(cboxId) {
        return d3.select('#' + cboxId).property('checked');
    }

    function getCheckedValue(cboxId) {
        return d3.select('#' + cboxId).property('value');
    }

    function showTunnelInformation(data) {
        wss.unbindHandlers(handlerMap);
        tunnelNameData = data;

        function dOkUpdate() {
            var tdString = '' ;
            tunnelNameData.a.forEach( function (val, idx) {
                var tunnelName = isChecked('tunnel-id-'+idx);
                if (tunnelName) {
                    tdString = val;
                }
            } );

            constraintsUpdateDialog(tdString);
            $log.debug('Dialog OK button clicked');
        }

        tds.openDialog()
            .setTitle('Available LSPs with selected device')
            .addContent(createUserTextUpdate(data))
            .addOkChained(dOkUpdate, 'OK')
            .addCancel(dClose, 'Close')
            .bindKeys();
    }

    function constraintsUpdateDialog(tunnelId) {

        // invoked when the OK button is pressed on this dialog
        function dOkUpdateEvent() {
            $log.debug('Select constraints for update path Dialog OK button pressed');

            var bandWidth = isChecked('update-band-width-box'),
                bandValue = null,
                bandType = null;

            if (bandWidth) {
                bandValue = d3.select('#update-band-width-value');

                if (isChecked('update-band-kpbs-val')) {
                    bandType = 'kbps';
                } else if (isChecked('update-band-mpbs-val')) {
                    bandType = 'mbps';
                }
            }

            var costType = isChecked('update-pce-cost-type'),
                costTypeVal = null;

            if (costType) {
                if (isChecked('update-pce-cost-type-igp')) {
                    costTypeVal = 'igp';
                } else if (isChecked('update-pce-cost-type-te')) {
                   costTypeVal = 'te';
                }
            }

            wss.sendEvent(updatePathmsg, {
                    bw: bandValue,
                    ctype: costTypeVal,
                    tunnelname: tunnelId
            });

            flash.flash('update path message');

        }

        tds.openDialog()
            .setTitle('Select constraints for update path')
            .addContent(createUserTextUpdatePathEvent())
            .addCancel()
            .addOk(dOkUpdateEvent, 'OK')     // NOTE: NOT the "chained" version!
            .bindKeys();

    }

    function showTunnelInformationRemove(data) {

        wss.unbindHandlers(handlerMapRem);
        tunnelNameDataRemove = data;
        tds.openDialog()
            .setTitle('Available Tunnels for remove')
            .addContent(createUserTextRemove(data))
            .addOk(dOkRemove, 'OK')
            .addCancel(dClose, 'Close')
            .bindKeys();
    }

    //setup path
    function setMode(node) {

        function dOk() {
            var bandWidth = isChecked('band-width-box'),
                bandValue = null,
                bandType = null;

            if (bandWidth) {
                bandValue = getCheckedValue('band-width-value');

                if (isChecked('band-kpbs-val')) {
                    bandType = 'kbps';
                } else if (isChecked('band-mpbs-val')) {
                    bandType = 'mbps';
                }
            }

            var costType = isChecked('pce-cost-type'),
                costTypeVal = null;

            if (costType) {
                if (isChecked('pce-cost-type-igp')) {
                    costTypeVal = 'igp';
                } else if (isChecked('pce-cost-type-te')) {
                   costTypeVal = 'te';
                }
            }

            var lspType = isChecked('pce-lsp-type'),
                lspTypeVal = null;

            if (lspType) {
                if (isChecked('pce-lsp-type-cr')) {
                   lspTypeVal = 'cr';
                } else if (isChecked('pce-lsp-type-srbe')) {
                   lspTypeVal = 'srbe';
                } else if (isChecked('pce-lsp-type-srte')) {
                   lspTypeVal = 'srte';
                }
            }

            wss.sendEvent(setPathmsg, {
                srid: node[0],
                dsid: node[1],
                bw: bandValue,
                bwtype: bandType,
                ctype: costTypeVal,
                lsptype: lspTypeVal,
                tunnelname: getCheckedValue('pce-tunnel-name-id')
            });

            flash.flash('create path message');
            $log.debug('Dialog OK button clicked');
        }

        tds.openDialog()
        .setTitle('constraints selection')
        .addContent(createUserText())
        .addOk(dOk, 'OK')
        .addCancel(dClose, 'Close')
        .bindKeys();
    }

    function updatePath(node) {

        wss.sendEvent(updatePathmsgQuery, {
            srid: node[0],
            dsid: node[1]
        });

        handlerMap[showTunnelInfoMsg] = showTunnelInformation;
        wss.bindHandlers(handlerMap);

        flash.flash('update path message');
    }

    function dOkRemove() {

        tunnelNameDataRemove.a.forEach( function (val, idx) {
            var tunnelNameVal = isChecked('tunnel-id-remove-'+idx);
            if (tunnelNameVal) {
                wss.sendEvent(remPathmsg, {
                    tunnelid: val
                });
            }
        } );

        flash.flash('remove path message');
    }

    function remPath(node) {
        wss.sendEvent(remPathmsgQuery, {
            srid: node[0],
            dsid: node[1]
        });

        handlerMapRem[showTunnelInfoRemoveMsg] = showTunnelInformationRemove;
        wss.bindHandlers(handlerMapRem);
    }

  function queryTunnelDisplay() {
        wss.sendEvent(queryDisplayTunnelMsg);
    }
    // === ---------------------------
    // === Module Factory Definition

    angular.module('ovPcewebTopov', [])
        .factory('PcewebTopovDemoService',
        ['$log', 'FnService', 'FlashService', 'WebSocketService',
        'TopoPanelService', 'NavService', 'TopoDialogService', 'DialogService',

        function (_$log_, _fs_, _flash_, _wss_, _tps_, _ns_,_tds_, _ds_) {
            $log = _$log_;
            fs = _fs_;
            flash = _flash_;
            wss = _wss_;
            tps = _tps_;
            ns = _ns_;
            tds = _tds_;
            ds = _ds_;

            return {
                setSrc: setSrc,
                setDst: setDst,
                clear: clear,
                setMode: setMode,
                updatePath: updatePath,
                remPath: remPath,
                queryTunnelDisplay: queryTunnelDisplay
            };
        }]);
}());
