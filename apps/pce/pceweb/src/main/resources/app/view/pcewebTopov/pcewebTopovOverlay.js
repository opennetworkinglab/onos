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

// PCE web topology overlay - client side
//
// This is the glue that binds our business logic (in pcewebTopovDemo.js)
// to the overlay framework.

(function () {
    'use strict';

    // injected refs
    var $log, tov, pps;

    // internal state should be kept in the service module (not here)
    var selection;
    // our overlay definition
    var overlay = {
        // NOTE: this must match the ID defined in AppUiTopovOverlay
        overlayId: 'PCE-web-overlay',
        glyphId: 'topo',
        tooltip: 'PCE web Topo Overlay',

        activate: function () {
            $log.debug("PCE web topology overlay ACTIVATED");
        },
        deactivate: function () {
            pps.clear();
            $log.debug("PCE web topology overlay DEACTIVATED");
        },

        // These glyphs get installed using the overlayId as a prefix.
        // e.g. 'src' is installed as 'PCE-web-overlay-src'
        // They can be referenced (from this overlay) as '*src'
        // That is, the '*' prefix stands in for 'PCE-web-overlay-'
        glyphs: {
             jp: {
                vb: '0 0 110 110',
                d: 'M84.3,89.3L58.9,64.2l-1.4,1.4L83,90.7L84.3,89.3z M27,7.6H7.4v19.2H27V7.6z' +
                'M59.3,47.1H39.8v19.2h19.5V47.1z M102.1,79.5H82.6v19.2h19.5V79.5z M41.7,47.6L19,25.1l-1.2,1.2l22.7,22.5L41.7,47.6z'
            },
        },

       // Key bindings for PCE web overlay buttons
        // NOTE: fully qual. button ID is derived from overlay-id and key-name
        keyBindings: {
            1: {
                cb: function () {
                    pps.setMode(selection);
                },
                tt: 'Setup path',
                gid: 'plus'
            },
            2: {
                cb: function () {
                    pps.updatePath(selection);
                },
                tt: 'Update path',
                gid: '*jp'
            },
            3: {
                cb: function () {
                    pps.remPath(selection);
                },
                tt: 'Remove path',
                gid: 'minus'
            },
            4: {
                cb: function () {
                    pps.queryTunnelDisplay();
                },
                tt: 'Show Tunnels',
                gid: 'checkMark'
            },

            _keyOrder: [
                '1', '2', '3', '4'
            ]
        },
        hooks: {
            // hook for handling escape key
            // Must return true to consume ESC, false otherwise.
           escape: function () {
                selectionCallback();
                pps.setSrc();
                pps.setDst();
                return true;
            },

            // hooks for when the selection changes...
            empty: function () {
                selectionCallback();
            },
            single: function (data) {
                selectionCallback(data);
            },
            multi: function (selectOrder) {
                selectionCallback(selectOrder);
            },
            modifylinkdata: function (data, extra) {
                $log.debug("Modify link data", data, extra);

                function sep() {
                    data.propOrder.push('-');
                }

                function add(key) {
                    var val = extra[key];
                    if (val !== undefined) {
                        data.propOrder.push(key);
                        data.props[key] = val;
                    }
                }

                sep();
                add('Src Address');
                add('Dst Address');
                add('Te metric');
                add('Bandwidth');

                return data;
            }
        }
    };

    function selectionCallback(d) {
        $log.debug('Selection callback', d);
        selection = d;
    }

    // invoke code to register with the overlay service
    angular.module('ovPcewebTopov')
        .run(['$log', 'TopoOverlayService', 'PcewebTopovDemoService',

        function (_$log_, _tov_, _pps_) {
            $log = _$log_;
            tov = _tov_;
            pps = _pps_;
            tov.register(overlay);
        }]);

}());
