/*
 * Copyright 2015 Open Networking Laboratory
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
 ONOS GUI -- Topology layer filtering Module.
 Provides functionality to visually differentiate between the packet and
 optical layers of the topology.
 */

(function () {
    'use strict';

    // injected refs
    var $log, fs, flash, tps, tts;

    // api to topoForce
    var api;
    /*
     node()                         // get ref to D3 selection of nodes
     */

    // internal state


    // === -----------------------------------------------------
    // === MODULE DEFINITION ===

    angular.module('ovTopo')
        .factory('TopoFilterService',
        ['$log', 'FnService',
            'FlashService',
            'TopoPanelService',
            'TopoTrafficService',

            function (_$log_, _fs_, _flash_, _tps_, _tts_) {
                $log = _$log_;
                fs = _fs_;
                flash = _flash_;
                tps = _tps_;
                tts = _tts_;

                function initFilter(_api_) {
                    api = _api_;
                }

                function destroyFilter() { }

                return {
                    initFilter: initFilter,
                    destroyFilter: destroyFilter
                };
            }]);
}());
