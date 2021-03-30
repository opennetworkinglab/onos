/*
 * Copyright 2015-present Open Networking Foundation
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
    var $log, flash, wss, api;

    // function to be replaced by the localization bundle function
    var topoLion = function (x) {
        return '#ttraf#' + x + '#';
    };

    /*
       API to topoForce
         hovered()
         somethingSelected()
         selectOrder()
     */

    var allTrafficTypes = [
            'flowStatsBytes',
            'portStatsBitSec',
            'portStatsPktSec',
        ],
        allTrafficMsgs = []; // filled in with localized messages

    // internal state
    var trafficMode = null,
        hoverMode = null,
        allTrafficIndex = 0,
        customTrafficIndex = 0;


    // === -----------------------------------------------------
    //  Helper functions

    function setLionBundle(bundle) {
        $log.debug('topoTraffic: setting Lion bundle');
        topoLion = bundle;
        allTrafficMsgs = [
            topoLion('tr_fl_fstats_bytes'),
            topoLion('tr_fl_pstats_bits'),
            topoLion('tr_fl_pstats_pkts'),
        ];
    }

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
                hover: hoverValid() ? hov.id : '',
            });
        }
    }

    function requestRelatedIntents() {
        // generates payload based on current hover-state
        var hov = api.hovered();

        function hoverValid() {
            return hoverMode === 'intents' && hov && (
            hov.class === 'host' ||
            hov.class === 'device' ||
            hov.class === 'link');
        }

        if (api.somethingSelected()) {
            wss.sendEvent('requestRelatedIntents', {
                ids: api.selectOrder(),
                hover: hoverValid() ? hov.id : '',
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
        flash.flash(topoLion('fl_monitoring_canceled'));
        return true;
    }

    function showAllTraffic() {
        trafficMode = 'allFlowPort';
        hoverMode = null;
        wss.sendEvent('requestAllTraffic', {
            trafficType: allTrafficTypes[allTrafficIndex],
        });
        flash.flash(allTrafficMsgs[allTrafficIndex]);
        allTrafficIndex = (allTrafficIndex + 1) % 3;
    }

    function showCustomTraffic() {
        trafficMode = 'allCustom';
        hoverMode = null;
        wss.sendEvent('requestCustomTraffic', {
            index: customTrafficIndex,
        });
        flash.flash('Custom Traffic');
        customTrafficIndex = customTrafficIndex + 1;
    }

    function showDeviceLinkFlows() {
        trafficMode = hoverMode = 'flows';
        requestDeviceLinkFlows();
        flash.flash(topoLion('tr_fl_dev_flows'));
    }

    function showRelatedIntents() {
        trafficMode = hoverMode = 'intents';
        requestRelatedIntents();
        flash.flash(topoLion('tr_fl_rel_paths'));
    }

    function showPrevIntent() {
        if (trafficMode === 'intents') {
            hoverMode = null;
            wss.sendEvent('requestPrevRelatedIntent');
            flash.flash(topoLion('tr_fl_prev_rel_int'));
        }
    }

    function showNextIntent() {
        if (trafficMode === 'intents') {
            hoverMode = null;
            wss.sendEvent('requestNextRelatedIntent');
            flash.flash(topoLion('tr_fl_next_rel_int'));
        }
    }

    function showSelectedIntentTraffic() {
        if (trafficMode === 'intents') {
            hoverMode = null;
            wss.sendEvent('requestSelectedIntentTraffic');
            flash.flash(topoLion('tr_fl_traf_on_path'));
        }
    }

    // force the system to create a single intent selection
    function selectIntent(data) {
        trafficMode = 'intents';
        hoverMode = null;
        wss.sendEvent('selectIntent', data);
        flash.flash(topoLion('fl_selecting_intent') + ' ' + data.key);
    }


    // === ------------------------------------------------------
    // action buttons on detail panel (multiple selection)

    function addHostIntent() {
        var so = api.selectOrder();

        wss.sendEvent('addHostIntent', {
            one: so[0],
            two: so[1],
            ids: so,
        });
        trafficMode = 'intents';
        hoverMode = null;
        flash.flash(topoLion('tr_fl_h2h_flow_added'));
    }

    function removeIntent(d) {
        var action = topoLion(d.intentPurge ? 'purged' : 'withdrawn');

        wss.sendEvent('removeIntent', {
            appId: d.appId,
            appName: d.appName,
            key: d.key,
            purge: d.intentPurge,
        });
        trafficMode = 'intents';
        hoverMode = null;
        flash.flash(topoLion('intent') + ' ' + action);
    }

    function resubmitIntent(d) {
        wss.sendEvent('resubmitIntent', {
            appId: d.appId,
            appName: d.appName,
            key: d.key,
            purge: d.intentPurge,
        });
        trafficMode = 'intents';
        hoverMode = null;
        flash.flash(topoLion('intent') + ' ' + topoLion('resubmitted'));
    }

    function addMultiSourceIntent() {
        var so = api.selectOrder();

        wss.sendEvent('addMultiSourceIntent', {
            src: so.slice(0, so.length - 1),
            dst: so[so.length - 1],
            ids: so,
        });
        trafficMode = 'intents';
        hoverMode = null;
        flash.flash(topoLion('tr_fl_multisrc_flow') + ' ' + topoLion('added'));
    }

    function removeIntents() {
        wss.sendEvent('removeIntents', {});
        trafficMode = 'intents';
        hoverMode = null;
        flash.flash(topoLion('intents') + ' ' + topoLion('purged'));
    }


    // === -----------------------------------------------------
    // === MODULE DEFINITION ===

    angular.module('ovTopo')
    .factory('TopoTrafficService',
        ['$log', 'FlashService', 'WebSocketService',

        function (_$log_, _flash_, _wss_) {
            $log = _$log_;
            flash = _flash_;
            wss = _wss_;

            return {
                initTraffic: function (_api_) { api = _api_; },
                destroyTraffic: function () { },

                // invoked from toolbar overlay buttons or keystrokes
                cancelTraffic: cancelTraffic,
                showAllTraffic: showAllTraffic,
                showDeviceLinkFlows: showDeviceLinkFlows,
                showRelatedIntents: showRelatedIntents,
                showPrevIntent: showPrevIntent,
                showNextIntent: showNextIntent,
                showSelectedIntentTraffic: showSelectedIntentTraffic,
                selectIntent: selectIntent,
                showCustomTraffic: showCustomTraffic,

                // invoked from mouseover/mouseout and selection change
                requestTrafficForMode: requestTrafficForMode,

                // invoked from buttons on detail (multi-select) panel
                addHostIntent: addHostIntent,
                addMultiSourceIntent: addMultiSourceIntent,
                removeIntent: removeIntent,
                resubmitIntent: resubmitIntent,
                removeIntents: removeIntents,

                setLionBundle: setLionBundle,
            };
        }]);
}());
