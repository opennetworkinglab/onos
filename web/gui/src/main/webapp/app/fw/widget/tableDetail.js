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
 ONOS GUI -- Widget -- Table Detail Panel Service
 */
(function () {
    'use strict';

    // injected refs
    var $log, $interval, $timeout, fs, wss;

    // constants
    // var refreshInterval = 2000;

    function noop() {}

    // TODO: describe the input object for the main function
    // example params to (functionX):
    // {
    //    ...
    // }
    function buildBasePanel(opts) {
        var popTopF = fs.isF(opts.popTop) || noop,
            popMidF = fs.isF(opts.popMid) || noop,
            popBotF = fs.isF(opts.popBot) || noop;

        $log.debug('options are', opts);

        // TODO use panel service to create base panel

        // TODO: create divs, and pass into invocations of popTopF(div), etc.
    }

    // more functions

    // TODO: add ref to PanelService
    angular.module('onosWidget')
    .factory('TableDetailService',
    ['$log', '$interval', '$timeout', 'FnService', 'WebSocketService',

    function (_$log_, _$interval_, _$timeout_, _fs_, _wss_) {
        $log = _$log_;
        $interval = _$interval_;
        $timeout = _$timeout_;
        fs = _fs_;
        wss = _wss_;

        return {
            buildBasePanel: buildBasePanel
        };
    }]);
}());
