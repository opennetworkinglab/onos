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
        configSfpMessage = 'configSfpMessage' ;

    // internal state
    var currentMode = null;

    // === Main API functions

    function start() {
        handlerMap[showSfcInf] = showSfcInformation;
        wss.bindHandlers(handlerMap);
        wss.sendEvent(displayStart);
    }

    function dOk() {
        var sfcId = null;
        sfcId = d3.select('#sfp-value').property("value");

        if (sfcId) {
            console.log(sfcId);
        }

       $log.debug('Dialog OK button clicked');

       wss.sendEvent(configSfpMessage, {
            id: sfcId
        });

        flash.flash('SFP ID query:');
    }

    function dClose() {
        $log.debug('Dialog Close button clicked (or Esc pressed)');
    }

    function createUserText() {
        var content = ds.createDiv();
        var form = content.append('form');
        var p = form.append('p');

        p.append('input').attr({
            id: 'sfp-value',
            type: 'string',
            name: 'sfp-value-name'
        });
        p.append('span').text('ID');
        p.append('br');

        return content;
    }

    function configSfp() {
        tds.openDialog()
        .setTitle('SFP ID User Input')
        .addContent(createUserText())
        .addOk(dOk, 'OK')
        .addCancel(dClose, 'Close')
        .bindKeys();
    }

    function showSfcInformation(data) {
        console.log(data);
        wss.unbindHandlers(handlerMap);

        // Get the modal
        var modal = document.getElementById('myModal');

        // Get the button that opens the modal
        var btn = document.getElementById("myBtn");

        // Get the <span> element that closes the modal
        var span = document.getElementsByClassName("close")[0];

        modal.style.display = "block";

        var tBody = document.getElementById('sfc-info-body');

        var tdString = '' ;

        for (var i = 0; i < data.a.length; i++) {
            tdString += '<tr> <td>'+ data.a[i] +'</td></tr>';
        }

        tBody.innerHTML = tdString;

        // When the user clicks on <span> (x), close the modal
        span.onclick = function() {
            modal.style.display = "none";
        }

        // When the user clicks anywhere outside of the modal, close it
        window.onclick = function(event) {
            if (event.target == modal) {
                modal.style.display = "none";
            }
        }

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
                clear: clear,
                configSfp: configSfp
            };
        }]);

}());
