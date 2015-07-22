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
 *
 */

/*
 ONOS GUI -- Topology Traffic Module.
 Defines behavior for viewing different traffic modes.
 Installed as a Topology Overlay.
 */
(function () {
    'use strict';

    // injected refs
    var $log;

    // traffic overlay definition
    var overlay = {
        overlayId: 'traffic',
        glyph: { id: 'allTraffic' },
        tooltip: 'Traffic Overlay',

        activate: activateTraffic,
        deactivate: deactivateTraffic
    };

    // === implementation of overlay API (essentially callbacks)
    function activateTraffic() {
        $log.debug("Topology traffic overlay ACTIVATED");
    }

    function deactivateTraffic() {
        $log.debug("Topology traffic overlay DEACTIVATED");
    }


    // invoke code to register with the overlay service
    angular.module('ovTopo')
        .run(['$log', 'TopoOverlayService',

        function (_$log_, tov) {
            $log = _$log_;
            tov.register(overlay);
        }]);

}());
