/*
 * Copyright 2017-present Open Networking Foundation
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

(function () {
    'use strict';

    // injected refs
    var $log, t2ov, t2ts;

    // NOTE: no internal state here -- see Topo2TrafficService for that

    // traffic 2 overlay definition
    var overlay = {
        overlayId: 'traffic-2-overlay',
        glyphId: 'm_allTraffic',
        tooltip: 'Traffic Overlay',

        activate: function () {
            $log.debug('Traffic-2 overlay ACTIVATED');
        },

        deactivate: function () {
            t2ts.cancelTraffic(true);
            $log.debug('Traffic-2 overlay DEACTIVATED');
        },

        // key bindings for toolbar buttons
        // NOTE: fully qual. button ID is derived from overlay-id and key-name
        keyBindings: {
            0: {
                cb: function () { t2ts.cancelTraffic(true); },
                tt: 'Cancel traffic monitoring',
                gid: 'm_xMark',
            },

            A: {
                cb: function () { t2ts.showAllTraffic(); },
                tt: 'Monitor all traffic',
                gid: 'm_allTraffic',
            },

            _keyOrder: [
                '0', 'A',
            ],
        },

        hooks: {
            // hook for handling escape key
            escape: function () {
                // Must return true to consume ESC, false otherwise.
                return t2ts.cancelTraffic(true);
            },
            // TODO : add node selection events etc.
            // NOTE : see topoTrafficNew.js
        },
    };

    // invoke code to register with the overlay service
    angular.module('ovTopo2')
        .run(['$log', 'Topo2OverlayService', 'Topo2TrafficService',

        function (_$log_, _t2ov_, _t2ts_) {
            $log = _$log_;
            t2ov = _t2ov_;
            t2ts = _t2ts_;
            t2ov.register(overlay);
        }]);

}());
