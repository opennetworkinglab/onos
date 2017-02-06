/*
 * Copyright 2017-present Open Networking Laboratory
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
 ONOS GUI -- Topology Protected Intents Overlay Module.
 Defines behavior for viewing different multiple Protected Intents.
 Installed as a Topology Overlay.
 */
(function () {
    'use strict';

    // injected refs
    var $log, tov, tpis;

    // NOTE: no internal state here -- see topoProtectedIntents for that

    // NOTE: providing button disabling requires too big a refactoring of
    //       the button factory etc. Will have to be done another time.


    // traffic overlay definition
    var overlay = {
        overlayId: 'protectedIntent',
        glyphId: 'm_ips',
        tooltip: 'Protected Intents Overlay',

        activate: function () {
            $log.debug("Protected Intent overlay ACTIVATED");
        },

        deactivate: function () {
            tpis.cancelHighlights();
            $log.debug("Protected Intent DEACTIVATED");
        },

        hooks: {
            // hook for handling escape key
            escape: function () {
                // Must return true to consume ESC, false otherwise.
                return tpis.cancelHighlights();
            },
            // intent visualization hook
            acceptIntent: function (type) {
                // accept only intents with type "Protected"
                return (type.startsWith('Protected'));
            },
            showIntent: function (info) {
                $log.debug('^^ topoProtectedIntentsOverlay.showintent() ^^', info);
                tpis.showProtectedIntent(info);
            }
        }
    };

    // invoke code to register with the overlay service
    angular.module('ovTopo')
        .run(['$log', 'TopoOverlayService', 'TopoProtectedIntentsService',

        function (_$log_, _tov_, _tpis_) {
            $log = _$log_;
            tov = _tov_;
            tpis = _tpis_;
            tov.register(overlay);
        }]);

}());
