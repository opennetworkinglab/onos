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
 sfc web gui overlay implementation.
 */

(function () {
    'use strict';

    // injected refs
    var $log, fs, flash, wss, tds, ds, handlerMap = {};

    // constants
    var displayStart     = 'sfcwebTopovDisplayStart',
        showSfcInf       = 'showSfcInfo',
        clearMessage     = 'sfcTopovClear',
        configSfpMessage = 'configSfpMessage',
        sfcPath          = 'showSfcPath' ;

    // internal state
    var currentMode = null;
    var sfpInfo;

    // === Main API functions

    function start() {
        handlerMap[showSfcInf] = showSfcInformation;
        handlerMap[sfcPath] = showSfcPath;
        wss.bindHandlers(handlerMap);
        wss.sendEvent(displayStart);
    }

    function dOkSfp() {
        var tdString;
        var i = 0;

        sfpInfo.a.forEach( function () {
            var sfpId = d3.select("#sfp-value-id-"+i).property("checked");
            if (sfpId)
            {
                tdString = sfpInfo.a[i];
            }
            i++;
        } );

        if (!tdString) {
            $log.debug("No SFP ID is selected.");
        }

       wss.sendEvent(configSfpMessage, {
            id: tdString
        });

        flash.flash('SFP ID query:');
    }

    function dClose() {
        $log.debug('Dialog Close button clicked (or Esc pressed)');
    }

    function createUserTextSfp(data) {
        console.log(data);

        var content = ds.createDiv();
        var form = content.append('form');
        var p = form.append('p');
        var i = 0;

        p.append('span').text('SFP IDs');
        p.append('br');
        sfpInfo = data;
        data.a.forEach( function () {

            p.append('input').attr({
                id: 'sfp-value-id-'+i,
                type: 'radio',
                name: 'sfp-id-name',
                value: data.a[i]
            });

            p.append('span').text(data.a[i]);
            p.append('br');
            i++;
        } );

        return content;
    }

    function showSfcInformation(data) {
        tds.openDialog()
            .setTitle('List of active service functions')
            .addContent(createUserTextSfp(data))
            .addOk(dOkSfp, 'Select SFP ID')
            .addCancel(dClose, 'Close')
            .bindKeys();

    }

    function createSfcPathText(data) {

        var content = ds.createDiv();
        var form = content.append('form');
        var p = form.append('p');
        var i = 0;

        p.append('span').text('SFC Path');
        p.append('br');
        data.sfcPathList.forEach( function (val, idx) {
            p.append('span').text(val);
            p.append('br')
        } );

        return content;
    }

    function showSfcPath(data) {
        tds.openDialog()
            .setTitle('Service function path')
            .addContent(createSfcPathText(data))
            .addCancel(dClose, 'Close')
            .bindKeys();
    }

    function clear() {
        wss.sendEvent(clearMessage);
        flash.flash('Cleared SFC overlay');
    }

    // === ---------------------------
    // === Module Factory Definition
    angular.module('ovSfcwebTopov', [])
        .factory('SfcwebTopovDemoService',
        ['$log', 'FnService', 'FlashService', 'WebSocketService', 'TopoDialogService', 'DialogService',
        function (_$log_, _fs_, _flash_, _wss_, _tds_, _ds_) {
            $log = _$log_;
            fs = _fs_;
            flash = _flash_;
            wss = _wss_;
            tds = _tds_;
            ds = _ds_;
            return {
                start: start,
                showSfcInformation: showSfcInformation,
                showSfcPath : showSfcPath,
                clear: clear
            };
        }]);

}());
