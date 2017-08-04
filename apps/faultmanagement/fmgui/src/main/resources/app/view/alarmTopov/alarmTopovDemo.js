/*
 * Copyright 2015-present Open Networking Foundation
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
 Alarm Demo module. This contains the "business logic" for the topology
 overlay that we are implementing.
 */

(function () {
    'use strict';

    // injected refs
    var $log, fs, flash, wss;

    // constants
    var displayStart = 'alarmTopovDisplayStart',
        displayStop = 'alarmTopovDisplayStop';


    // === ---------------------------
    // === Helper functions

    function sendDisplayStart() {
        wss.sendEvent(displayStart);
    }

    function sendDisplayStop() {
        wss.sendEvent(displayStop);
    }

    // === ---------------------------
    // === Main API functions

    function startDisplay() {
        sendDisplayStart();
        flash.flash('Showing alarm counts on devices');
    }

    function stopDisplay() {
        sendDisplayStop();
        flash.flash('Canceling alarm counts on devices');
        return true;
    }

    // === ---------------------------
    // === Module Factory Definition

    angular.module('ovAlarmTopov', [])
        .factory('AlarmTopovDemoService',
        ['$log', 'FnService', 'FlashService', 'WebSocketService',

        function (_$log_, _fs_, _flash_, _wss_) {
            $log = _$log_;
            fs = _fs_;
            flash = _flash_;
            wss = _wss_;

            return {
                startDisplay: startDisplay,
                stopDisplay: stopDisplay
            };
        }]);
}());
