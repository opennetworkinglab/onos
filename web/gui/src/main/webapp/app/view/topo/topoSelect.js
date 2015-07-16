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
    var $log, fs, wss, tps, tts, ns;

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

    // constants
    var devPath = 'device',
        flowPath = 'flow',
        portPath ='port',
        groupPath = 'group';

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
            //$log.debug("MouseOver()...", m);
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
            //$log.debug("MouseOut()...", m);
        }
    }

    // ==========================

    function selectObject(obj) {
        var el = this,
            nodeEv = el && el.tagName === 'g',
            ev = d3.event.sourceEvent || {},
            n;

        if (api.zoomingOrPanning(ev)) {
            return;
        }

        if (nodeEv) {
            n = d3.select(el);
        } else {
            api.node().each(function (d) {
                if (d == obj) {
                    n = d3.select(el = this);
                }
            });
        }
        if (!n) return;

        if (nodeEv) {
            consumeClick = true;
        }
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
        tps.addAction({
            id: '-mult-rel-traf-btn',
            gid: 'allTraffic',
            cb:  tts.showRelatedIntentsAction,
            tt: 'Show Related Traffic'
        });

        // add other actions, based on what is selected...
        if (nSel() === 2 && allSelectionsClass('host')) {
            tps.addAction({
                id: 'host-flow-btn',
                gid: 'endstation',
                cb: tts.addHostIntentAction,
                tt: 'Create Host-to-Host Flow'
            });
        } else if (nSel() >= 2 && allSelectionsClass('host')) {
            tps.addAction({
                id: 'mult-src-flow-btn',
                gid: 'flows',
                cb: tts.addMultiSourceIntentAction,
                tt: 'Create Multi-Source Flow'
            });
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
        tps.addAction({
            id: '-sin-rel-traf-btn',
            gid: 'intentTraffic',
            cb: tts.showRelatedIntentsAction,
            tt: 'Show Related Traffic'
        });

        // add other actions, based on what is selected...
        if (data.type === 'switch') {
            tps.addAction({
                id: 'sin-dev-flows-btn',
                gid: 'flows',
                cb: tts.showDeviceLinkFlowsAction,
                tt: 'Show Device Flows'
            });
        }
        // TODO: have the server return explicit class and ID of each node
        // for now, we assume the node is a device if it has a URI
        if ((data.props).hasOwnProperty('URI')) {
            tps.addAction({
                id: 'device-table-btn',
                gid: data.type,
                cb: function () {
                    ns.navTo(devPath, { devId: data.props['URI'] });
                },
                tt: 'Show device view'
            });
            tps.addAction({
                id: 'flows-table-btn',
                gid: 'flowTable',
                cb: function () {
                    ns.navTo(flowPath, { devId: data.props['URI'] });
                },
                tt: 'Show flow view for this device'
            });
            tps.addAction({
                id: 'ports-table-btn',
                gid: 'portTable',
                cb: function () {
                    ns.navTo(portPath, { devId: data.props['URI'] });
                },
                tt: 'Show port view for this device'
            });
            tps.addAction({
                id: 'groups-table-btn',
                gid: 'groupTable',
                cb: function () {
                    ns.navTo(groupPath, { devId: data.props['URI'] });
                },
                tt: 'Show group view for this device'
            });
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
            'TopoPanelService', 'TopoTrafficService', 'NavService',

        function (_$log_, _fs_, _wss_, _tps_, _tts_, _ns_) {
            $log = _$log_;
            fs = _fs_;
            wss = _wss_;
            tps = _tps_;
            tts = _tts_;
            ns = _ns_;

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
