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
 ONOS GUI -- Topology Oblique View Module.
 Provides functionality to view the topology as two planes (packet & optical)
 from an oblique (side-on) perspective.
 */

(function () {
    'use strict';

    // injected refs
    var $log, fs;

    // api to topoForce
    var api;
    /*
     node()                         // get ref to D3 selection of nodes
     link()                         // get ref to D3 selection of links
     */

    // internal state
    var foo;

    // ==========================


    function toggleOblique() {
        $log.log("TOGGLING OBLIQUE VIEW");
    }

// === -----------------------------------------------------
// === MODULE DEFINITION ===

angular.module('ovTopo')
    .factory('TopoObliqueService',
    ['$log', 'FnService',

    function (_$log_, _fs_) {
        $log = _$log_;
        fs = _fs_;

        function initOblique(_api_) {
            api = _api_;
        }

        function destroyOblique() { }

        return {
            initOblique: initOblique,
            destroyOblique: destroyOblique,

            toggleOblique: toggleOblique
        };
    }]);
}());
