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

   // constants
    var srcMessage = 'pceTopovSetSrc',
        dstMessage = 'pceTopovSetDst',
        clearMessage = 'pceTopovClear',
        setModemsg =  'pceTopovSetMode',
        L3dev = 'requestIpDevDetails';
    // internal state
    var currentMode = null;

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

    function dOk() {
        var bandWidth = d3.select('#band-width-box').property("checked");
        var bandValue = null;
        var bandType = null;

        if (bandWidth) {

            bandValue = d3.select('#band-width-value').property("value");

            if (d3.select("#band-kpbs-val").property("checked")) {
                bandType = 'kbps';
            } else if (d3.select('#band-mpbs-val').property("checked")) {
                bandType = 'mbps';
            }
        }

        var costType = d3.select('#pce-cost-type').property("checked");
        var costTypeVal = null;

        if (costType) {

            if (d3.select("#pce-cost-type-igp").property("checked")) {
                costTypeVal = 'igp';
            } else if (d3.select('#pce-cost-type-te').property("checked")) {
               costTypeVal = 'te';
            }
       }

       var lspType = d3.select('#pce-lsp-type').property("checked");
       var lspTypeVal = null;

       if (lspType) {

           if (d3.select("#pce-lsp-type-cr").property("checked")) {
               lspTypeVal = 'cr';
           } else if (d3.select('#pce-lsp-type-srbe').property("checked")) {
               lspTypeVal = 'srbe';
           } else if (d3.select('#pce-lsp-type-srte').property("checked")) {
               lspTypeVal = 'srte';
           }
       }

         //TBD: Read the user inputs and need to send the event for calculating the path based on constrainsts.
         // TBD: Need to read IGP cost type and LSP type.
         //wss.sendEvent(setModemsg);
         //flash.flash('creat path message');

       $log.debug('Dialog OK button clicked');
    }

    function dClose() {
        $log.debug('Dialog Close button clicked (or Esc pressed)');
    }

    function createUserText() {
        var content = ds.createDiv();
        var form = content.append('form');
        var p = form.append('p');

        //Add the bandwidth related inputs.
        p.append('input').attr({
            id: 'band-width-box',
            type: 'checkbox',
            name: 'band-width-name'
        });
        p.append('span').text('Band Width');
        p.append('br');
        p.append('input').attr({
            id: 'band-width-value',
            type: 'number',
            name: 'band-width-value-name'
        });
        p.append('input').attr({
            id: 'band-kpbs-val',
            type: 'radio',
            name: 'pce-band-type'
        });
        p.append('span').text('kpbs');
        p.append('input').attr({
            id: 'band-mpbs-val',
            type: 'radio',
            name: 'pce-band-type'
        });
        p.append('span').text('mpbs');
        p.append('br');

        //Add the cost type related inputs.
        p.append('input').attr({
            id: 'pce-cost-type',
            type: 'checkbox',
            name: 'pce-cost-type-name'
        });
        p.append('span').text('Cost Type');
        p.append('br');
        p.append('input').attr({
            id: 'pce-cost-type-igp',
            type: 'radio',
            name: 'pce-cost-type-valname'
        });
        p.append('span').text('IGP');
        p.append('input').attr({
            id: 'pce-cost-type-te',
            type: 'radio',
            name: 'pce-cost-type-valname'
        });
        p.append('span').text('TE');
        p.append('br');

        //Add the LSP type related inputs.
        p.append('input').attr({
            id: 'pce-lsp-type',
            type: 'checkbox',
            name: 'pce-lsp-type-name'
        });
        p.append('span').text('Lsp Type');
        p.append('br');
        p.append('input').attr({
            id: 'pce-lsp-type-cr',
            type: 'radio',
            name: 'pce-lsp-type-valname'
        });
        p.append('span').text('CR');
        p.append('input').attr({
            id: 'pce-lsp-type-srbe',
            type: 'radio',
            name: 'pce-lsp-type-valname'
        });
        p.append('span').text('SR BE');
        p.append('input').attr({
            id: 'pce-lsp-type-srte',
            type: 'radio',
            name: 'pce-lsp-type-valname'
        });
        p.append('span').text('SR TE');

        return content;
    }

    function setMode() {
        tds.openDialog()
        .setTitle('constraints user')
        .addContent(createUserText())
        .addOk(dOk, 'OK')
        .addCancel(dClose, 'Close')
        .bindKeys();
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
                setMode: setMode

            };
        }]);
}());
