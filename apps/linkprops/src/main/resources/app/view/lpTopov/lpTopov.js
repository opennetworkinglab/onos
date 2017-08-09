/*
 * Copyright 2015-present Open Networking Laboratory
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
 Module containing the "business logic" for the Link Props topology overlay.
 */

(function () {
    'use strict';

    // injected refs
    var $log, fs, flash, wss;

    // constants
    var displayStart = 'linkPropsTopovDisplayStart',
        displayUpdate = 'linkPropsTopovDisplayUpdate',
        displayStop = 'linkPropsTopovDisplayStop';

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

    // === ---------------------------
    // === Main API functions

    function startDisplay(mode) {
        if (currentMode === mode) {
            $log.debug('(in mode', mode, 'already)');
        } else {
            currentMode = mode;
            sendDisplayStart(mode);
            flash.flash('Starting display mode: ' + mode);
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
            flash.flash('Canceling display mode');
            return true;
        }
        return false;
    }

    // === ---------------------------
    // === Module Factory Definition

    angular.module('ovLpTopov', [])
        .factory('LinkPropsTopovService',
        ['$log', 'FnService', 'FlashService', 'WebSocketService',

        function (_$log_, _fs_, _flash_, _wss_) {
            $log = _$log_;
            fs = _fs_;
            flash = _flash_;
            wss = _wss_;

            return {
                startDisplay: startDisplay,
                updateDisplay: updateDisplay,
                stopDisplay: stopDisplay
            };
        }]);
}());
