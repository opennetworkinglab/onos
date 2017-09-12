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
 ONOS GUI -- Topology Selection Module.
 Defines behavior when selecting nodes.
 */

(function () {
    'use strict';

    // injected refs
    var $log, fs, wss, tov, tps, tts, sus;

    // api to topoForce
    var api;
    /*
       node()                         // get ref to D3 selection of nodes
       zoomingOrPanning( ev )
       updateDeviceColors( [dev] )
       deselectAllLinks()
     */

    // internal state
    var hovered, selections, selectOrder, consumeClick;

    // function to be replaced by the localization bundle function
    var topoLion = function (x) {
        return '#tsel#' + x + '#';
    };

    function setInitialState() {
        hovered = null; // the node over which the mouse is hovering
        selections = {}; // currently selected nodes (by id)
        selectOrder = []; // the order in which we made selections
        consumeClick = false; // used to coordinate with SVG click handler
    }

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
            if (hovered !== m) {
                hovered = m;
                tov.hooks.mouseOver({
                    id: m.id,
                    class: m.class,
                    type: m.type,
                });
            }
        }
    }

    function nodeMouseOut(m) {
        if (!m.dragStarted) {
            if (hovered) {
                hovered = null;
                tov.hooks.mouseOut();
            }
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
                if (d === obj) {
                    n = d3.select(el = this);
                }
            });
        }

        if (obj && obj.class === 'link') {
            if (selections[obj.key]) {
                deselectObject(obj.key);
            } else {
                selections[obj.key] = { obj: obj, el: el };
                selectOrder.push(obj.key);
            }
            updateDetail();
            return;
        }

        if (!n) {
            return;
        }

        if (nodeEv) {
            consumeClick = true;
        }

        if (ev.shiftKey && n.classed('selected')) {
            deselectObject(obj.id);
            updateDetail();
            return;
        }

        if (!ev.shiftKey) {
            deselectAll(true);
        }

        selections[obj.id] = { obj: obj, el: el };
        selectOrder.push(obj.id);

        n.classed('selected', true);
        if (n.classed('device')) {
            api.updateDeviceColors(obj);
        }
        updateDetail();
    }

    function reselect() {
        selectOrder.forEach(function (id) {
            var sel = d3.select('g#' + sus.safeId(id));
            sel.classed('selected', true);
        });
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

    function deselectAll(skipUpdate) {
        var something = (selectOrder.length > 0);

        // deselect all nodes in the network...
        api.node().classed('selected', false);
        selections = {};
        selectOrder = [];
        api.updateDeviceColors();
        if (!skipUpdate) {
            updateDetail();
        }

        // return true if something was selected
        return something;
    }

    // === -----------------------------------------------------

    function requestDetails(data) {
        var itemClass = data.class,
            payload = {
                class: itemClass,
                id: data.id,
            };

        // special handling for links...
        if (itemClass === 'link') {
            payload.key = data.key;
            payload.sourceId = data.source.id;
            payload.targetId = data.target.id;
            payload.targetPort = data.tgtPort;

            if (data.source.class === 'host') {
                payload.isEdgeLink = true;
            } else {
                payload.isEdgeLink = false;
                payload.sourcePort = data.srcPort;
            }
        }

        wss.sendEvent('requestDetails', payload);
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
        tov.hooks.emptySelect();
        tps.displayNothing();
    }

    function singleSelect() {
        var data = getSel(0).obj;

        $log.debug('Requesting details from server for', data);
        requestDetails(data);
        // NOTE: detail panel is shown as a response to receiving
        //       a 'showDetails' event from the server. See 'showDetails'
        //       callback function below...
    }

    function multiSelect() {
        // display the selected nodes in the detail panel
        tps.displayMulti(selectOrder);
        addHostSelectionActions();
        tov.hooks.multiSelect(selectOrder);
        tps.displaySomething();
    }

    function addHostSelectionActions() {
        if (allSelectionsClass('host')) {
            if (nSel() === 2) {
                tps.addAction({
                    id: 'host-flow-btn',
                    gid: 'm_endstation',
                    cb: tts.addHostIntent,
                    tt: function () { return topoLion('tr_btn_create_h2h_flow'); },
                });
            } else if (nSel() >= 2) {
                tps.addAction({
                    id: 'mult-src-flow-btn',
                    gid: 'flows',
                    cb: tts.addMultiSourceIntent,
                    tt: function () { return topoLion('tr_btn_create_msrc_flow'); },
                });
            }
        }
    }


    // === -----------------------------------------------------
    //  Event Handlers

    // display the data for the single selected node
    function showDetails(data) {
        var buttons = fs.isA(data.buttons) || [];
        tps.displaySingle(data);
        tov.installButtons(buttons, data, data.propValues['uri']);
        tov.hooks.singleSelect(data);
        tps.displaySomething();
    }

    // returns true if one or more nodes are selected.
    function somethingSelected() {
        return nSel();
    }

    function clickConsumed(x) {
        var cc = consumeClick;
        consumeClick = !!x;
        return cc;
    }

    // returns a selection context, providing info about what is selected
    function selectionContext() {
        var devices = [],
            hosts = [],
            types = {};

        angular.forEach(selections, function (d) {
            var o = d.obj,
                c = o.class;

            if (c === 'device') {
                devices.push(o.id);
                types[o.id] = o.type;
            }
            if (c === 'host') {
                hosts.push(o.id);
                types[o.id] = o.type;
            }
        });

        return {
            devices: devices,
            hosts: hosts,
            types: types,
        };
    }

    // === -----------------------------------------------------
    // === MODULE DEFINITION ===

    angular.module('ovTopo')
    .factory('TopoSelectService',
        ['$log', 'FnService', 'WebSocketService', 'TopoOverlayService',
        'TopoPanelService', 'TopoTrafficService', 'SvgUtilService',

        function (_$log_, _fs_, _wss_, _tov_, _tps_, _tts_, _sus_) {
            $log = _$log_;
            fs = _fs_;
            wss = _wss_;
            tov = _tov_;
            tps = _tps_;
            tts = _tts_;
            sus = _sus_;

            function initSelect(_api_) {
                api = _api_;
                if (!selections) {
                    setInitialState();
                }
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
                updateDetail: updateDetail,

                hovered: function () { return hovered; },
                selectOrder: function () { return selectOrder; },
                somethingSelected: somethingSelected,

                clickConsumed: clickConsumed,
                selectionContext: selectionContext,
                reselect: reselect,
                setLionBundle: function (bundle) { topoLion = bundle; },
            };
        }]);
}());
