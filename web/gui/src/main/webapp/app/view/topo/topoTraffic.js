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
 ONOS GUI -- Topology Traffic Module.
 Defines behavior for viewing different traffic modes.
 */

(function () {
    'use strict';

    // injected refs
    var $log, fs, flash;

    // api to topoForce
    var api;
    /*
     clearLinkTrafficStyle()
     removeLinkLabels()
     updateLinks()
     findLinkById( id )
     hovered()
     validateSelectionContext()
     sendEvent( type, {payload} )
     */

    // constants
    var hoverModeNone = 0,
        hoverModeAll = 1,
        hoverModeFlows = 2,
        hoverModeIntents = 3;

    // internal state
    var hoverMode = hoverModeNone;


    // === -----------------------------------------------------
    //  Event Handlers

    function showTraffic(data) {
        var paths = data.paths;

        api.clearLinkTrafficStyle();
        api.removeLinkLabels();

        // Now highlight all links in the paths payload, and attach
        //  labels to them, if they are defined.
        paths.forEach(function (p) {
            var n = p.links.length,
                i, ldata;

            for (i=0; i<n; i++) {
                ldata = api.findLinkById(p.links[i]);
                if (ldata && ldata.el) {
                    ldata.el.classed(p.class, true);
                    ldata.label = p.labels[i];
                }
            }
        });

        api.updateLinks();
    }

    // === -----------------------------------------------------
    //  Helper functions

    function requestDeviceLinkFlows() {
        var hov = api.hovered();

        function hoverValid() {
            return hoverMode === hoverModeFlows &&
                hov && (hov.class === 'device');
        }

        if (api.validateSelectionContext()) {
            api.sendEvent('requestDeviceLinkFlows', {
                ids: api.selectOrder(),
                hover: hoverValid() ? hov.id : ''
            });
        }
    }

    function requestRelatedIntents() {
        var hov = api.hovered();

        function hoverValid() {
            return hoverMode === hoverModeIntents &&
                hov && (hov.class === 'host' || hov.class === 'device');
        }

        if (api.validateSelectionContext()) {
            api.sendEvent('requestRelatedIntents', {
                ids: api.selectOrder(),
                hover: hoverValid() ? hov.id : ''
            });
        }
    }


    // === -----------------------------------------------------
    //  Traffic requests

    function cancelTraffic() {
        api.sendEvent('cancelTraffic');
    }

    // invoked in response to change in selection and/or mouseover/out:
    function requestTrafficForMode() {
        if (hoverMode === hoverModeFlows) {
            requestDeviceLinkFlows();
        } else if (hoverMode === hoverModeIntents) {
            requestRelatedIntents();
        }
    }

    // === -----------------------------
    // keystroke commands

    // keystroke-right-arrow (see topo.js)
    function showNextIntentAction() {
        hoverMode = hoverModeNone;
        api.sendEvent('requestNextRelatedIntent');
        flash.flash('>');
    }

    // keystroke-left-arrow (see topo.js)
    function showPrevIntentAction() {
        hoverMode = hoverModeNone;
        api.sendEvent('requestPrevRelatedIntent');
        flash.flash('<');
    }

    // keystroke-W (see topo.js)
    function showSelectedIntentTrafficAction() {
        hoverMode = hoverModeNone;
        api.sendEvent('requestSelectedIntentTraffic');
        flash.flash('Traffic on Selected Path');
    }

    // keystroke-A (see topo.js)
    function showAllTrafficAction() {
        hoverMode = hoverModeAll;
        api.sendEvent('requestAllTraffic');
        flash.flash('All Traffic');
    }

    // === -----------------------------
    // action buttons on detail panel

    // also, keystroke-V (see topo.js)
    function showRelatedIntentsAction () {
        hoverMode = hoverModeIntents;
        requestRelatedIntents();
        flash.flash('Related Paths');
    }

    function addHostIntentAction () {
        var so = api.selectOrder();
        api.sendEvent('addHostIntent', {
            one: so[0],
            two: so[1],
            ids: so
        });
        flash.flash('Host-to-Host flow added');
    }

    function addMultiSourceIntentAction () {
        var so = api.selectOrder();
        api.sendEvent('addMultiSourceIntent', {
            src: so.slice(0, so.length - 1),
            dst: so[so.length - 1],
            ids: so
        });
        flash.flash('Multi-Source flow added');
    }

    // also, keystroke-F (see topo.js)
    function showDeviceLinkFlowsAction () {
        hoverMode = hoverModeFlows;
        requestDeviceLinkFlows();
        flash.flash('Device Flows');
    }


    // === -----------------------------------------------------
    // === MODULE DEFINITION ===

    angular.module('ovTopo')
    .factory('TopoTrafficService',
        ['$log', 'FnService', 'FlashService',

        function (_$log_, _fs_, _flash_) {
            $log = _$log_;
            fs = _fs_;
            flash = _flash_;

            function initTraffic(_api_) {
                api = _api_;
            }

            function destroyTraffic() { }

            return {
                initTraffic: initTraffic,
                destroyTraffic: destroyTraffic,

                showTraffic: showTraffic,

                cancelTraffic: cancelTraffic,
                requestTrafficForMode: requestTrafficForMode,
                showRelatedIntentsAction: showRelatedIntentsAction,
                addHostIntentAction: addHostIntentAction,
                addMultiSourceIntentAction: addMultiSourceIntentAction,
                showDeviceLinkFlowsAction: showDeviceLinkFlowsAction,
                showNextIntentAction: showNextIntentAction,
                showPrevIntentAction: showPrevIntentAction,
                showSelectedIntentTrafficAction: showSelectedIntentTrafficAction,
                showAllTrafficAction: showAllTrafficAction
            };
        }]);
}());
