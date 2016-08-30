/*
 * Copyright 2015-present Open Networking Laboratory
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
 ONOS GUI -- Topology Traffic Overlay Module.
 Defines behavior for viewing different traffic modes.
 Installed as a Topology Overlay.
 */
(function () {
    'use strict';

    // injected refs
    var $log, tov, tts;

    // NOTE: no internal state here -- see TopoTrafficService for that

    // NOTE: providing button disabling requires too big a refactoring of
    //       the button factory etc. Will have to be done another time.


    // traffic overlay definition
    var overlay = {
        overlayId: 'traffic',
        glyphId: 'm_allTraffic',
        tooltip: 'Traffic Overlay',

        // NOTE: Traffic glyphs already installed as part of the base ONOS set.

        activate: function () {
            $log.debug("Traffic overlay ACTIVATED");
        },

        deactivate: function () {
            tts.cancelTraffic(true);
            $log.debug("Traffic overlay DEACTIVATED");
        },

        // detail panel button definitions
        // (keys match button identifiers, also defined in TrafficOverlay.java)
        buttons: {
            showDeviceFlows: {
                gid: 'm_flows',
                tt: 'Show Device Flows',
                cb: function (data) { tts.showDeviceLinkFlows(); }
            },

            showRelatedTraffic: {
                gid: 'm_relatedIntents',
                tt: 'Show Related Traffic',
                cb: function (data) { tts.showRelatedIntents(); }
            }
        },

        // key bindings for traffic overlay toolbar buttons
        // NOTE: fully qual. button ID is derived from overlay-id and key-name
        keyBindings: {
            0: {
                cb: function () { tts.cancelTraffic(true); },
                tt: 'Cancel traffic monitoring',
                gid: 'm_xMark'
            },

            A: {
                cb: function () { tts.showAllFlowTraffic(); },
                tt: 'Monitor all traffic using flow stats',
                gid: 'm_allTraffic'
            },
            Q: {
                cb: function () { tts.showAllPortTraffic(); },
                tt: 'Monitor all traffic using port stats',
                gid: 'm_allTraffic'
            },
            F: {
                cb: function () { tts.showDeviceLinkFlows(); },
                tt: 'Show device link flows',
                gid: 'm_flows'
            },
            V: {
                cb: function () { tts.showRelatedIntents(); },
                tt: 'Show all related intents',
                gid: 'm_relatedIntents'
            },
            leftArrow: {
                cb: function () { tts.showPrevIntent(); },
                tt: 'Show previous related intent',
                gid: 'm_prev'
            },
            rightArrow: {
                cb: function () { tts.showNextIntent(); },
                tt: 'Show next related intent',
                gid: 'm_next'
            },
            W: {
                cb: function () { tts.showSelectedIntentTraffic(); },
                tt: 'Monitor traffic of selected intent',
                gid: 'm_intentTraffic'
            },

            _keyOrder: [
                '0', 'A', 'Q', 'F', 'V', 'leftArrow', 'rightArrow', 'W'
            ]
        },

        hooks: {
            // hook for handling escape key
            escape: function () {
                // Must return true to consume ESC, false otherwise.
                return tts.cancelTraffic(true);
            },

            // hooks for when the selection changes...
            empty: function () {
                tts.cancelTraffic();
            },
            single: function (data) {
                tts.requestTrafficForMode();
            },
            multi: function (selectOrder) {
                tts.requestTrafficForMode();
                tov.addDetailButton('showRelatedTraffic');
            },

            // mouse hooks
            mouseover: function (m) {
                // m has id, class, and type properties
                tts.requestTrafficForMode(true);
            },
            mouseout: function () {
                tts.requestTrafficForMode(true);
            }
        }
    };

    // invoke code to register with the overlay service
    angular.module('ovTopo')
        .run(['$log', 'TopoOverlayService', 'TopoTrafficService',

        function (_$log_, _tov_, _tts_) {
            $log = _$log_;
            tov = _tov_;
            tts = _tts_;
            tov.register(overlay);
        }]);

}());
