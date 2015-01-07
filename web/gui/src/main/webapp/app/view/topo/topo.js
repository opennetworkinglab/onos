/*
 * Copyright 2014,2015 Open Networking Laboratory
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
 ONOS GUI -- Topology View Module

 @author Simon Hunt
 */

(function () {
    'use strict';

    var moduleDependencies = [
        'onosUtil',
        'onosSvg'
    ];

    // references to injected services etc.
    var $log, ks, gs;

    // DOM elements
    var defs;

    // Internal state
    // ...

    // Note: "exported" state should be properties on 'self' variable

    var keyBindings = {
        W: [logWarning, 'log a warning'],
        E: [logError, 'log an error']
    };

    // -----------------
    // these functions are necessarily temporary examples....
    function logWarning() {
        $log.warn('You have been warned!');
    }
    function logError() {
        $log.error('You are erroneous!');
    }
    // -----------------

    function setUpKeys() {
        ks.keyBindings(keyBindings);
    }

    function setUpDefs() {
        defs = d3.select('#ov-topo svg').append('defs');
        gs.loadDefs(defs);
    }


    angular.module('ovTopo', moduleDependencies)

        .controller('OvTopoCtrl', [
            '$log', 'KeyService', 'GlyphService',

        function (_$log_, _ks_, _gs_) {
            var self = this;

            $log = _$log_;
            ks = _ks_;
            gs = _gs_;

            self.message = 'Topo View Rocks!';

            setUpKeys();
            setUpDefs();

            $log.log('OvTopoCtrl has been created');
        }]);
}());
