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
 */

/*
 ONOS GUI -- Topology Traffic Module.
 Defines behavior for viewing different traffic modes.
 */

(function () {
    'use strict';

    // injected refs
    var $log, fs, flash, wss, api;

    /*
       API to topoForce
         hovered()
         somethingSelected()
         selectOrder()
     */

    // internal state
    var trafficMode = null,
        hoverMode = null;


    // === -----------------------------------------------------
    //  Helper functions

    // invoked in response to change in selection and/or mouseover/out:
    function requestTrafficForMode(mouse) {
        if (trafficMode === 'flows') {
            requestDeviceLinkFlows();
        } else if (trafficMode === 'intents') {
            if (!mouse || hoverMode === 'intents') {
                requestRelatedIntents();
            }
        } else {
            // do nothing
        }
    }

    function requestDeviceLinkFlows() {
        // generates payload based on current hover-state
        var hov = api.hovered();

        function hoverValid() {
            return hoverMode === 'flows' &&
                hov && (hov.class === 'device');
        }

        if (api.somethingSelected()) {
            wss.sendEvent('requestDeviceLinkFlows', {
                ids: api.selectOrder(),
                hover: hoverValid() ? hov.id : ''
            });
        }
    }

    function requestRelatedIntents() {
        // generates payload based on current hover-state
        var hov = api.hovered();

        function hoverValid() {
            return hoverMode === 'intents' &&
                hov && (hov.class === 'host' || hov.class === 'device');
        }

        if (api.somethingSelected()) {
            wss.sendEvent('requestRelatedIntents', {
                ids: api.selectOrder(),
                hover: hoverValid() ? hov.id : ''
            });
        }
    }


    // === -------------------------------------------------------------
    //  Traffic requests invoked from keystrokes or toolbar buttons...

    function cancelTraffic(forced) {
        if (!trafficMode || (!forced && trafficMode === 'allFlowPort')) {
            return false;
        }

        trafficMode = hoverMode = null;
        wss.sendEvent('cancelTraffic');
        flash.flash('Traffic monitoring canceled');
        return true;
    }

    function showAllFlowTraffic() {
        trafficMode = 'allFlowPort';
        hoverMode = null;
        wss.sendEvent('requestAllFlowTraffic');
        flash.flash('All Flow Traffic');
    }

    function showAllPortTraffic() {
        trafficMode = 'allFlowPort';
        hoverMode = null;
        wss.sendEvent('requestAllPortTraffic');
        flash.flash('All Port Traffic');
    }

    function showDeviceLinkFlows () {
        trafficMode = hoverMode = 'flows';
        requestDeviceLinkFlows();
        flash.flash('Device Flows');
    }

    function showRelatedIntents () {
        trafficMode = hoverMode = 'intents';
        requestRelatedIntents();
        flash.flash('Related Paths');
    }

    function showPrevIntent() {
        if (trafficMode === 'intents') {
            hoverMode = null;
            wss.sendEvent('requestPrevRelatedIntent');
            flash.flash('Previous related intent');
        }
    }

    function showNextIntent() {
        if (trafficMode === 'intents') {
            hoverMode = null;
            wss.sendEvent('requestNextRelatedIntent');
            flash.flash('Next related intent');
        }
    }

    function showSelectedIntentTraffic() {
        if (trafficMode === 'intents') {
            hoverMode = null;
            wss.sendEvent('requestSelectedIntentTraffic');
            flash.flash('Traffic on Selected Path');
        }
    }

    // force the system to create a single intent selection
    function selectIntent(data) {
        trafficMode = 'intents';
        hoverMode = null;
        wss.sendEvent('selectIntent', data);
        flash.flash('Selecting Intent ' + data.key);
    }


    // === ------------------------------------------------------
    // action buttons on detail panel (multiple selection)

    function addHostIntent () {
        var so = api.selectOrder();
        wss.sendEvent('addHostIntent', {
            one: so[0],
            two: so[1],
            ids: so
        });
        trafficMode = 'intents';
        hoverMode = null;
        flash.flash('Host-to-Host flow added');
    }

    function removeIntent (d) {
        $log.debug('Entering removeIntent');
        wss.sendEvent('removeIntent', {
            appId: d.appId,
            appName: d.appName,
            key: d.key,
            purge: d.intentPurge
        });
        trafficMode = 'intents';
        hoverMode = null;
        var txt = d.intentPurge ? 'purged' : 'withdrawn';
        flash.flash('Intent ' + txt);
    }

    function addMultiSourceIntent () {
        var so = api.selectOrder();
        wss.sendEvent('addMultiSourceIntent', {
            src: so.slice(0, so.length - 1),
            dst: so[so.length - 1],
            ids: so
        });
        trafficMode = 'intents';
        hoverMode = null;
        flash.flash('Multi-Source flow added');
    }


    // === -----------------------------------------------------
    // === MODULE DEFINITION ===

    angular.module('ovTopo')
    .factory('TopoTrafficService',
        ['$log', 'FnService', 'FlashService', 'WebSocketService',

        function (_$log_, _fs_, _flash_, _wss_) {
            $log = _$log_;
            fs = _fs_;
            flash = _flash_;
            wss = _wss_;

            return {
                initTraffic: function (_api_) { api = _api_; },
                destroyTraffic: function () { },

                // invoked from toolbar overlay buttons or keystrokes
                cancelTraffic: cancelTraffic,
                showAllFlowTraffic: showAllFlowTraffic,
                showAllPortTraffic: showAllPortTraffic,
                showDeviceLinkFlows: showDeviceLinkFlows,
                showRelatedIntents: showRelatedIntents,
                showPrevIntent: showPrevIntent,
                showNextIntent: showNextIntent,
                showSelectedIntentTraffic: showSelectedIntentTraffic,
                selectIntent: selectIntent,

                // invoked from mouseover/mouseout and selection change
                requestTrafficForMode: requestTrafficForMode,

                // TODO: these should move to new UI demo app
                // invoked from buttons on detail (multi-select) panel
                addHostIntent: addHostIntent,
                addMultiSourceIntent: addMultiSourceIntent,
                removeIntent: removeIntent
            };
        }]);
}());
