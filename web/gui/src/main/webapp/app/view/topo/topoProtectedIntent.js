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
 ONOS GUI -- Topology Protected Intents Module.
 Defines behavior for viewing protected intents .
 */

(function () {
    'use strict';

    // injected refs
    var flash, wss;

    // function to be replaced by the localization bundle function
    var topoLion = function (x) {
        return '#tproti#' + x + '#';
    };

    // internal state
    var showingProtectedIntent = null;

    // === -------------------------------------------------------------
    //  protected intent requests invoked from keystrokes or toolbar buttons...

    function cancelHighlights() {
        if (!showingProtectedIntent) {
            return false;
        }

        showingProtectedIntent = false;
        wss.sendEvent('cancelProtectedIntentHighlight');
        flash.flash(topoLion('fl_monitoring_canceled'));
        return true;
    }

    // force the system to create a single intent selection
    function showProtectedIntent(data) {
        wss.sendEvent('selectProtectedIntent', data);
        flash.flash(topoLion('fl_selecting_intent') + ' ' + data.key);
        showingProtectedIntent = true;
    }

    // === -----------------------------------------------------
    // === MODULE DEFINITION ===

    angular.module('ovTopo')
    .factory('TopoProtectedIntentsService',
        ['FlashService', 'WebSocketService',

        function (_flash_, _wss_) {
            flash = _flash_;
            wss = _wss_;

            return {
                // TODO: Remove references
                initProtectedIntents: function (_api_) {},
                destroyProtectedIntents: function () { },
                setLionBundle: function (bundle) { topoLion = bundle; },

                // invoked from toolbar overlay buttons or keystrokes
                cancelHighlights: cancelHighlights,
                showProtectedIntent: showProtectedIntent,
            };
        }]);
}());
