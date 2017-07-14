/*
 * Copyright 2016-present Open Networking Foundation
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
 ONOS GUI -- Topology View Module.
 Module that contains the topology view variables
 */

(function () {
    'use strict';

    // Injected Services
    var flash;

    // Internal State
    var dimensions,
        viewOptions = {
            linkPortHighlighting: true,
        };

    function newDim(_dimensions) {
        dimensions = _dimensions;
    }

    function getDimensions() {
        return dimensions;
    }

    function togglePortHighlights(x) {
        var kev = (x === 'keyev'),
            on = kev ? !viewOptions.linkPortHighlighting : Boolean(x),
            what = on ? 'Enable' : 'Disable';

        viewOptions.linkPortHighlighting = on;
        flash.flash(what + ' port highlighting');
        return on;
    }

    function getPortHighlighting() {
        return viewOptions.linkPortHighlighting;
    }

    angular.module('ovTopo2')
    .factory('Topo2ViewService',
        ['FlashService',
            function (_flash_) {

                flash = _flash_;

                return {
                    newDim: newDim,
                    getDimensions: getDimensions,

                    togglePortHighlights: togglePortHighlights,
                    getPortHighlighting: getPortHighlighting,
                };
            },
        ]
    );
})();
