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
 */

/*
 ONOS GUI -- Mapping Management Topology Overlay
 */
(function () {
    'use strict';

    // injected references
    var $log, tov, mts, ns;

    var viewbox = '-1 -1 19 19';

    // mapping glyph (view box 20x20)
    var mapping = 'M8,0C4.687,0,2,2.687,2,6c0,3.854,4.321,8.663,5,9.398C7.281,' +
    '15.703,7.516,16,8,16s0.719-0.297,1-0.602  C9.679,14.663,14,9.854,14,6C14,' +
    '2.687,11.313,0,8,0z M8,10c-2.209,0-4-1.791-4-4s1.791-4,4-4s4,1.791,4,' +
    '4S10.209,10,8,10z M8,4  C6.896,4,6,4.896,6,6s0.896,2,2,2s2-0.896,2-2S9.104,4,8,4z';

    // overlay definition
    var overlay = {
        overlayId: 'mapping-overlay',
        glyphId: '*mapping',
        tooltip: 'Mappings Overlay',
        glyphs: {
            mapping: {
                vb: viewbox,
                d: mapping
            }
        },
        activate: function () {
            $log.debug("Mapping topology overlay ACTIVATED");
            mts.startDisplay();
        },
        deactivate: function () {
            mts.stopDisplay();
            $log.debug("Mapping topology overlay DEACTIVATED");
        },
        buttons: {
            mappings: {
                gid: '*mapping',
                tt: 'Show mappings for this device',
                cb: function (data) {
                    $log.debug('Show mappings for this device. data:', data);
                    ns.navTo("mapping", {devId: data.id});
                }
            }
        },
        hooks: {
            // hooks for when the selection changes...
            empty: function () {
                selectionCallback('empty');
            },
            single: function (data) {
                selectionCallback('single', data);
            },
            multi: function (selectOrder) {
                selectionCallback('multi', selectOrder);
                tov.addDetailButton('mappings');
            }
        }
    };

    function selectionCallback(x, d) {
        $log.debug('Selection callback', x, d);
    }

    // invoke code to register with the overlay service
    angular.module('ovMappingTopo')
        .run(['$log', 'TopoOverlayService', 'MappingTopoService', 'NavService',

        function (_$log_, _tov_, _mts_, _ns_) {
            $log = _$log_;
            tov = _tov_;
            mts = _mts_;
            ns = _ns_;
            tov.register(overlay);
        }]);

}());
