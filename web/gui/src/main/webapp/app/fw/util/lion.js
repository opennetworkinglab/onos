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
 *
 */

/*
 ONOS GUI -- Lion -- Localization Utilities
 */
(function () {
    'use strict';

    // injected services
    var $log, fs, wss;

    // private state
    var handlers = {
            uberlion: uberlion
        },
        ubercache = {};

    // handler for uberlion event..
    function uberlion(data) {
        $log.debug('LION service: uber-lion bundle received:', data);
        ubercache = data.lion;
    }

    function init() {
        wss.bindHandlers(handlers);
    }

    // returns a lion bundle (function) for the given bundle ID
    function bundle(bundleId) {
        var bundle = ubercache[bundleId];

        if (!bundle) {
            $log.warn('No lion bundle registered:', bundleId);
            bundle = {};
        }

        return function (key) {
            return bundle[key] || '%' + key + '%';
        };
    }

    angular.module('onosUtil')
        .factory('LionService', ['$log', 'FnService', 'WebSocketService',

        function (_$log_, _fs_, _wss_) {
            $log = _$log_;
            fs = _fs_;
            wss = _wss_;

            return {
                init: init,
                bundle: bundle
            };
        }]);
}());
