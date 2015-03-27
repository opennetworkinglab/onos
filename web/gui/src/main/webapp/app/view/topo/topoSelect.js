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
 ONOS GUI -- Topology Selection Module.
 Defines behavior when selecting nodes.
 */

(function () {
    'use strict';

    // injected refs
    var $log, fs, wss, tps, tts;

    // api to topoForce
    var api;
    /*
       node()                         // get ref to D3 selection of nodes
       zoomingOrPanning( ev )
       updateDeviceColors( [dev] )
       deselectLink()
     */

    // internal state
    var hovered,                // the node over which the mouse is hovering
        selections = {},        // currently selected nodes (by id)
        selectOrder = [],       // the order in which we made selections
        consumeClick = false;   // used to coordinate with SVG click handler

    // ==========================

    function nSel() {
        return selectOrder.length;
    }
    function getSel(idx) {
        return selections[selectOrder[idx]];
    }
    function allSelectionsClass(cls) {
        for (var i=0, n=nSel(); i<n; i++) {
            if (getSel(i).obj.class !== cls) {
                return false;
            }
        }
        return true;
    }

    // ==========================

    function nodeMouseOver(m) {
        if (!m.dragStarted) {
            $log.debug("MouseOver()...", m);
            if (hovered != m) {
                hovered = m;
                tts.requestTrafficForMode();
            }
        }
    }

    function nodeMouseOut(m) {
        if (!m.dragStarted) {
            if (hovered) {
                hovered = null;
                tts.requestTrafficForMode();
            }
            $log.debug("MouseOut()...", m);
        }
    }

    // ==========================

    function selectObject(obj) {
        var el = this,
            ev = d3.event.sourceEvent,
            n;

        if (api.zoomingOrPanning(ev)) {
            return;
        }

        if (el) {
            n = d3.select(el);
        } else {
            api.node().each(function (d) {
                if (d == obj) {
                    n = d3.select(el = this);
                }
            });
        }
        if (!n) return;

        consumeClick = true;
        api.deselectLink();

        if (ev.shiftKey && n.classed('selected')) {
            deselectObject(obj.id);
            updateDetail();
            return;
        }

        if (!ev.shiftKey) {
            deselectAll();
        }

        selections[obj.id] = { obj: obj, el: el };
        selectOrder.push(obj.id);

        n.classed('selected', true);
        api.updateDeviceColors(obj);
        updateDetail();
    }

    function deselectObject(id) {
        var obj = selections[id];
        if (obj) {
            d3.select(obj.el).classed('selected', false);
            delete selections[id];
            fs.removeFromArray(id, selectOrder);
            api.updateDeviceColors(obj.obj);
        }
    }

    function deselectAll() {
        var something = (selectOrder.length > 0);

        // deselect all nodes in the network...
        api.node().classed('selected', false);
        selections = {};
        selectOrder = [];
        api.updateDeviceColors();
        updateDetail();

        // return true if something was selected
        return something;
    }

    // === -----------------------------------------------------

    function requestDetails() {
        var data = getSel(0).obj;
        wss.sendEvent('requestDetails', {
            id: data.id,
            class: data.class
        });
    }

    // === -----------------------------------------------------

    function updateDetail() {
        var nSel = selectOrder.length;
        if (!nSel) {
            emptySelect();
        } else if (nSel === 1) {
            singleSelect();
        } else {
            multiSelect();
        }
    }

    function emptySelect() {
        tts.cancelTraffic();
        tps.displayNothing();
    }

    function singleSelect() {
        // NOTE: detail is shown from 'showDetails' event callback
        requestDetails();
        tts.cancelTraffic();
        tts.requestTrafficForMode();
    }

    function multiSelect() {
        // display the selected nodes in the detail panel
        tps.displayMulti(selectOrder);

        // always add the 'show traffic' action
        tps.addAction('Show Related Traffic', tts.showRelatedIntentsAction);

        // add other actions, based on what is selected...
        if (nSel() === 2 && allSelectionsClass('host')) {
            tps.addAction('Create Host-to-Host Flow', tts.addHostIntentAction);
        } else if (nSel() >= 2 && allSelectionsClass('host')) {
            tps.addAction('Create Multi-Source Flow', tts.addMultiSourceIntentAction);
        }

        tts.cancelTraffic();
        tts.requestTrafficForMode();
        tps.displaySomething();
    }


    // === -----------------------------------------------------
    //  Event Handlers

    function showDetails(data) {
        // display the data for the single selected node
        tps.displaySingle(data);

        // always add the 'show traffic' action
        tps.addAction('Show Related Traffic', tts.showRelatedIntentsAction);

        // add other actions, based on what is selected...
        if (data.type === 'switch') {
            tps.addAction('Show Device Flows', tts.showDeviceLinkFlowsAction);
        }

        tps.displaySomething();
    }

    function validateSelectionContext() {
        if (!hovered && !nSel()) {
            tts.cancelTraffic();
            return false;
        }
        return true;
    }

    function clickConsumed(x) {
        var cc = consumeClick;
        consumeClick = !!x;
        return cc;
    }

    // === -----------------------------------------------------
    // === MODULE DEFINITION ===

    angular.module('ovTopo')
    .factory('TopoSelectService',
        ['$log', 'FnService', 'WebSocketService',
            'TopoPanelService', 'TopoTrafficService',

        function (_$log_, _fs_, _wss_, _tps_, _tts_) {
            $log = _$log_;
            fs = _fs_;
            wss = _wss_;
            tps = _tps_;
            tts = _tts_;

            function initSelect(_api_) {
                api = _api_;
            }

            function destroySelect() { }

            return {
                initSelect: initSelect,
                destroySelect: destroySelect,

                showDetails: showDetails,

                nodeMouseOver: nodeMouseOver,
                nodeMouseOut: nodeMouseOut,
                selectObject: selectObject,
                deselectObject: deselectObject,
                deselectAll: deselectAll,

                hovered: function () { return hovered; },
                selectOrder: function () { return selectOrder; },
                validateSelectionContext: validateSelectionContext,

                clickConsumed: clickConsumed
            };
        }]);
}());
