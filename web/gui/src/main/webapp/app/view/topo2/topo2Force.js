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
 ONOS GUI -- Topology Force Module.
 Visualization of the topology in an SVG layer, using a D3 Force Layout.
 */

(function () {
    'use strict';

    // injected refs
    var $log, wss;

    // ========================== Helper Functions

    function init() {
        $log.debug('Initialize topo force layout');
    }

    function destroy() {
        $log.debug('Destroy topo force layout');
    }

    // ========================== Event Handlers

    function allInstances(data) {
        $log.debug('>> topo2AllInstances event:', data)
    }

    function currentLayout(data) {
        $log.debug('>> topo2CurrentLayout event:', data)
    }

    function currentRegion(data) {
        $log.debug('>> topo2CurrentRegion event:', data)
    }

    function startDone(data) {
        $log.debug('>> topo2StartDone event:', data)
    }
    
    // ========================== Main Service Definition

    angular.module('ovTopo2')
    .factory('Topo2ForceService',
        ['$log', 'WebSocketService',

        function (_$log_, _wss_) {
            $log = _$log_;
            wss = _wss_;
            
            return {
                init: init,
                destroy: destroy,
                topo2AllInstances: allInstances,
                topo2CurrentLayout: currentLayout,
                topo2CurrentRegion: currentRegion,
                topo2StartDone: startDone
            };
        }]);
}());
