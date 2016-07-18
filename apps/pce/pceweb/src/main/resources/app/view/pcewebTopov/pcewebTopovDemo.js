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
    var tunnelNameData, tunnelNameDataRemove, tunnelDataUpdateInfo, tunnelIdUpd, tunnelNameDataQuery;
   // constants
    var srcMessage = 'pceTopovSetSrc',
        dstMessage = 'pceTopovSetDst',
        clearMessage = 'pceTopovClear',
        setPathmsg =  'pceTopovSetMode',
        updatePathmsgQuery = 'pceTopovUpdateQuery',
        remPathmsgQuery = 'pceTopovRemQuery',
        updatePathmsg = 'pceTopovUpdate',
        updatePathmsgInfo = 'updatePathmsgInfo',
        remPathmsg = 'pceTopovRem',
        showTunnelInfoMsg = 'pceTopovShowTunnels',
        queryDisplayTunnelMsg = 'pceTopovTunnelDisplay',
        showTunnelInfoRemoveMsg = 'pceTopovShowTunnelsRem',
        showTunnelInfoUpdateMsg = 'pceTopovShowTunnelsUpdate',
        showTunnelInfoQuery = 'pceTopovShowTunnelsQuery',
        showTunnelHighlightMsg = 'pceTopovshowTunnelHighlightMsg';

    // internal state
    var currentMode = null;
    var handlerMap = {},
        handlerMapRem = {},
        handlerMapshowQuery = {},
        handlerMapShowUpdate = {};
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
                if (id == 'pce-lsp-type-cr') {
                    p.append('input').attr({
                        type: type,
                        name: name,
                        id: id,
                        checked: 'checked',
                        class: 'radioButtonSpace'
                    });
                } else {
                    p.append('input').attr({
                        type: type,
                        name: name,
                        id: id,
                        class: 'radioButtonSpace'
                    });
                }
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
        addAttribute('pce-band-type', 'band-bps-val', 'bps', 'radio');
        //Add the cost type related inputs.
        addAttribute('pce-cost-type-name', 'pce-cost-type', 'Cost Type', 'checkbox');
        addAttribute('pce-cost-type-valname', 'pce-cost-type-igp', 'IGP', 'radio');
        addAttribute('pce-cost-type-valname', 'pce-cost-type-te', 'TE', 'radio');
        //Add the LSP type related inputs.
        p.append('span').text("Lsp Type *");
        p.append('br');
        addAttribute('pce-lsp-type-valname', 'pce-lsp-type-cr', 'With signalling', 'radio');
        addAttribute('pce-lsp-type-valname', 'pce-lsp-type-srbe', 'Without SR without signalling', 'radio');
        addAttribute('pce-lsp-type-valname', 'pce-lsp-type-srte', 'With SR without signalling', 'radio');
        //Add the tunnel name
        p.append('span').text("Tunnel Name  *");
        p.append('br');
        addAttribute('pce-tunnel-name', 'pce-tunnel-name-id', null, 'text');
        p.append('span').text("* indicates mandatory fields");
        return content;
    }

    function createUserTextUpdate(data) {
        var content = ds.createDiv(),
            form = content.append('form'),
            p = form.append('p');

        p.append('span').text('Tunnel IDs');
        p.append('br');

        for (var idx = 0; idx < data.a.length; idx++) {
            p.append('input').attr({
                id: 'tunnel-id-'+idx,
                type: 'radio',
                name: 'tunnel-id-name',
                value: data.a[idx]
            });
            idx++;
            p.append('span').text(data.a[idx]);
            p.append('br');
        }
        return content;
    }

    function createUserTextUpdatePathEvent(data) {
        var content = ds.createDiv(),
            form = content.append('form'),
            p = form.append('p');
        var constType;

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

        data.a.forEach( function (val, idx) {
            if (val == 'Tunnel') {
                constType = 'TUNNEL';
                return;
            }

            if (val == 'BandWidth') {
                constType = 'BW';
                return;
            }

            if (val == 'CostType') {
                constType = 'CT';
                return;
            }

            if (constType == 'TUNNEL') {
                p.append('span').text('Tunnel Id: ');
                p.append('span').text(val);
                p.append('br');
                tunnelIdUpd = val;
            }

            if (constType == 'BW') {
                //addAttribute('band-width-name', 'update-band-width-box', 'Band Width', 'checkbox');
                p.append('span').text('Band Width');
                p.append('br');
                p.append('input').attr({
                    id: 'update-band-width-value',
                    type: 'number',
                    name: 'band-width-value-name',
                    value: val
                });
                p.append('br');
                p.append('input').attr({
                    id: 'update-band-bps-val',
                    type: 'radio',
                    name: 'pce-band-type',
                    checked: 'checked',
                    class: 'radioButtonSpace'
                });
                p.append('span').text('bps');
                p.append('br');
                addAttribute('pce-band-type', 'update-band-kbps-val', 'kbps', 'radio');
                addAttribute('pce-band-type', 'update-band-mbps-val', 'mbps', 'radio');
                addAttribute('pce-band-type', 'update-band-none-val', 'none', 'radio');
            }

            if (constType == 'CT') {
                //addAttribute('pce-cost-type', 'update-pce-cost-type', 'Cost Type', 'checkbox');
                p.append('span').text('Cost Type');
                p.append('br');
                if (val == 'COST') {
                    p.append('input').attr({
                        id: 'update-pce-cost-type-igp',
                        type: 'radio',
                        name: 'pce-cost-type-value',
                        checked: 'checked',
                        class: 'radioButtonSpace'
                    });
                    p.append('span').text('IGP');
                    p.append('br');
                    addAttribute('pce-cost-type-value', 'update-pce-cost-type-te', 'TE', 'radio');
                    addAttribute('pce-cost-type-value', 'update-pce-cost-type-none', 'none', 'radio');
 
                } else {
                    addAttribute('pce-cost-type-value', 'update-pce-cost-type-igp', 'IGP', 'radio');
                    p.append('input').attr({
                        id: 'update-pce-cost-type-te',
                        type: 'radio',
                        name: 'pce-cost-type-value',
                        checked: 'checked',
                        class: 'radioButtonSpace'
                    });
                    p.append('span').text('TE');
                    p.append('br');
                    addAttribute('pce-cost-type-value', 'update-pce-cost-type-none', 'none', 'radio');
                }
            }
        } );

        return content;
    }

    function createUserTextRemove(data) {

        var content = ds.createDiv(),
            form = content.append('form'),
            p = form.append('p');

        p.append('span').text('Tunnels');
        p.append('br');

        for (var idx = 0; idx < data.a.length; idx++) {
            p.append('input').attr({
                id: 'tunnel-id-remove-'+idx,
                type: 'checkbox',
                name: 'tunnel-id-name-remove',
                value: data.a[idx]
            });
            idx++;
            p.append('span').text(data.a[idx]);
            p.append('br');
        }

        return content;
    }

    function createUserTextQuery(data) {

        var content = ds.createDiv(),
            form = content.append('form'),
            p = form.append('p');

        p.append('span').text('Tunnels');
        p.append('br');

        for (var idx =0; idx < data.a.length; idx++) {
            p.append('input').attr({
                id: 'tunnel-id-query-'+idx,
                type: 'radio',
                name: 'tunnel-id-name-query',
                value: data.a[idx]
            });
            idx++;
            p.append('span').text(data.a[idx]);
            p.append('br');
        }
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
            for (var idx = 0; idx < tunnelNameData.a.length; idx++) {
                var tunnelName = isChecked('tunnel-id-'+idx);
                if (tunnelName) {
                    tdString = tunnelNameData.a[idx];
                }
                idx++;
            }
            //send event to server for getting the tunnel information.
            if (tdString != null) {
                handlerMapShowUpdate[showTunnelInfoUpdateMsg] = showTunnelInfoUpdateMsgHandle;
                wss.bindHandlers(handlerMapShowUpdate);

                wss.sendEvent(updatePathmsgInfo, {
                    tunnelid: tdString
                });
            }
            //constraintsUpdateDialog(tdString);
            $log.debug('Dialog OK button clicked');
        }

        tds.openDialog()
            .setTitle('Available LSPs with selected device')
            .addContent(createUserTextUpdate(data))
            .addOk(dOkUpdate, 'OK')
            .addCancel(dClose, 'Close')
            .bindKeys();
    }

    function dOkUpdateEvent() {
        $log.debug('Select constraints for update path Dialog OK button pressed');

        var bandValue = null,
            bandType = null;

        bandValue = getCheckedValue('update-band-width-value');

        if (isChecked('update-band-kbps-val')) {
                    bandType = 'kbps';
        } else if (isChecked('update-band-mbps-val')) {
                    bandType = 'mbps';
        } else if (isChecked('update-band-bps-val')) {
                    bandType = 'bps';
        } else if (isChecked('update-band-none-val')) {
                    bandType = null;
                    bandValue = null;
        }

        var costTypeVal = null;

        if (isChecked('update-pce-cost-type-igp')) {
            costTypeVal = 'igp';
        } else if (isChecked('update-pce-cost-type-te')) {
            costTypeVal = 'te';
        } else if (isChecked('update-pce-cost-type-none')) {
            costTypeVal = null;
        }

        wss.sendEvent(updatePathmsg, {
                bw: bandValue,
                bwtype: bandType,
                ctype: costTypeVal,
                tunnelid: tunnelIdUpd
        });

        flash.flash('update path message');

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

    function showTunnelInformationQuery(data) {

        wss.unbindHandlers(handlerMapshowQuery);
        tunnelNameDataQuery = data;
        tds.openDialog()
            .setTitle('Available Tunnels for highlight')
            .addContent(createUserTextQuery(data))
            .addOk(dOkQuery, 'OK')
            .addCancel(dClose, 'Close')
            .bindKeys();
    }

    function showTunnelInfoUpdateMsgHandle(data) {

        wss.unbindHandlers(handlerMapShowUpdate);
        tunnelDataUpdateInfo = data;
        tds.openDialog()
            .setTitle('Constrainst selection for update')
            .addContent(createUserTextUpdatePathEvent(data))
            .addOk(dOkUpdateEvent, 'OK')
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
                } else if (isChecked('band-bps-val')) {
                    bandType = 'bps';
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

            var lspTypeVal = null;

            if (isChecked('pce-lsp-type-cr')) {
                lspTypeVal = 'cr';
            } else if (isChecked('pce-lsp-type-srbe')) {
                   lspTypeVal = 'srbe';
            } else if (isChecked('pce-lsp-type-srte')) {
                   lspTypeVal = 'srte';
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

        for (var idx = 0; idx < tunnelNameDataRemove.a.length; idx++) {
            var tunnelNameVal = isChecked('tunnel-id-remove-'+idx);
            if (tunnelNameVal) {
                wss.sendEvent(remPathmsg, {
                    tunnelid: tunnelNameDataRemove.a[idx]
                });
            }
            idx++;
        }

        flash.flash('remove path message');
    }

    function dOkQuery() {

        for (var idx = 0; idx < tunnelNameDataQuery.a.length; idx++) {
            var tunnelNameVal = isChecked('tunnel-id-query-'+idx);
            if (tunnelNameVal) {
                wss.sendEvent(showTunnelHighlightMsg, {
                    tunnelid: tunnelNameDataQuery.a[idx]
                });
            }
            idx++;
        }

        flash.flash('query path message');
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
        handlerMapshowQuery[showTunnelInfoQuery] = showTunnelInformationQuery;
        wss.bindHandlers(handlerMapshowQuery);

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
