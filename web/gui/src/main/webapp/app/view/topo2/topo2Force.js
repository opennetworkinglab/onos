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

/*
 ONOS GUI -- Topology Force Module.
 Visualization of the topology in an SVG layer, using a D3 Force Layout.
 */

(function () {
    'use strict';

    // injected refs
    var $log,
        wss;

    // SVG elements;
    var linkG,
        linkLabelG,
        numLinkLblsG,
        portLabelG,
        nodeG;

    // internal state
    var settings,   // merged default settings and options
        force,      // force layout object
        drag,       // drag behavior handler
        network = {
            nodes: [],
            links: [],
            linksByDevice: {},
            lookup: {},
            revLinkToKey: {}
        },
        lu,                     // shorthand for lookup
        rlk,                    // shorthand for revLinktoKey
        showHosts = false,      // whether hosts are displayed
        showOffline = true,     // whether offline devices are displayed
        nodeLock = false,       // whether nodes can be dragged or not (locked)
        fTimer,                 // timer for delayed force layout
        fNodesTimer,            // timer for delayed nodes update
        fLinksTimer,            // timer for delayed links update
        dim,                    // the dimensions of the force layout [w,h]
        linkNums = [];          // array of link number labels

    // D3 selections;
    var link,
        linkLabel,
        node;

    var $log, wss, t2is, t2rs, t2ls, t2vs, t2bcs;
    var svg, forceG, uplink, dim, opts;

    // ========================== Helper Functions

    function init(_svg_, _forceG_, _uplink_, _dim_, _opts_) {
        svg = _svg_;
        forceG = _forceG_;
        uplink = _uplink_;
        dim = _dim_;
        opts = _opts_
    }

    function destroy() {
        $log.debug('Destroy topo force layout');
    }

    // ========================== Temporary Code (to be deleted later)

    function request(dir, rid) {
        wss.sendEvent('topo2navRegion', {
            dir: dir,
            rid: rid
        });
    }

    function doTmpCurrentLayout(data) {
        var topdiv = d3.select('#topo2tmp');
        var parentRegion = data.parent;
        var span = topdiv.select('.parentRegion').select('span');
        span.text(parentRegion || '[no parent]');
        span.classed('nav-me', !!parentRegion);
    }

    function doTmpCurrentRegion(data) {
        var topdiv = d3.select('#topo2tmp');
        var span = topdiv.select('.thisRegion').select('span');
        var div;

        span.text(data.id);

        div = topdiv.select('.subRegions').select('div');
        data.subregions.forEach(function (r) {

            function nav() {
                request('down', r.id);
            }

            div.append('p')
                .classed('nav-me', true)
                .text(r.id)
                .on('click', nav);
        });

        div = topdiv.select('.devices').select('div');
        data.layerOrder.forEach(function (tag, idx) {
            var devs = data.devices[idx];
            devs.forEach(function (d) {
                div.append('p')
                    .text('[' + tag + '] ' + d.id);
            });

        });

        div = topdiv.select('.hosts').select('div');
        data.layerOrder.forEach(function (tag, idx) {
            var hosts = data.hosts[idx];
            hosts.forEach(function (h) {
                div.append('p')
                    .text('[' + tag + '] ' + h.id);
            });
        });

        div = topdiv.select('.links').select('div');
        var links = data.links;
        links.forEach(function (lnk) {
            div.append('p')
                .text(lnk.id);
        });
    }

    function doTmpPeerRegions(data) {

    }

    // ========================== Event Handlers

    function allInstances(data) {
        $log.debug('>> topo2AllInstances event:', data);
        doTmpCurrentLayout(data);
        t2is.allInstances(data);
    }

    function currentLayout(data) {
        $log.debug('>> topo2CurrentLayout event:', data);
        t2bcs.addBreadcrumb(data.crumbs);
    }

    function currentRegion(data) {
        $log.debug('>> topo2CurrentRegion event:', data);
        doTmpCurrentRegion(data);
        t2rs.addRegion(data);
        t2ls.init(svg, forceG, uplink, dim, opts);
        t2ls.update();
        t2ls.start();
    }

    function topo2PeerRegions(data) {
        $log.debug('>> topo2PeerRegions event:', data)
        doTmpPeerRegions(data);
    }

    function topo2PeerRegions(data) {
        $log.debug('>> topo2PeerRegions event:', data)
    }

    function startDone(data) {
        $log.debug('>> topo2StartDone event:', data);
    }


    function showMastership(masterId) {
        if (!masterId) {
            restoreLayerState();
        } else {
            showMastershipFor(masterId);
        }
    }

    function restoreLayerState() {
        // NOTE: this level of indirection required, for when we have
        //          the layer filter functionality re-implemented
        suppressLayers(false);
    }

    // ========================== Main Service Definition

    function showMastershipFor(id) {
        suppressLayers(true);
        node.each(function (n) {
            if (n.master === id) {
                n.el.classed('suppressedmax', false);
            }
        });
    }

    function supAmt(less) {
        return less ? 'suppressed' : 'suppressedmax';
    }

    function suppressLayers(b, less) {
        var cls = supAmt(less);
        node.classed(cls, b);
        // link.classed(cls, b);
    }

    function newDim(_dim_) {
        dim = _dim_;
        t2vs.newDim(dim);
        // force.size(dim);
        // tms.newDim(dim);
        t2ls.setDimensions();
    }

    function getDim() {
        return dim;
    }

    // ========================== Main Service Definition

    angular.module('ovTopo2')
    .factory('Topo2ForceService',
        ['$log', 'WebSocketService', 'Topo2InstanceService', 'Topo2RegionService',
        'Topo2LayoutService', 'Topo2ViewService', 'Topo2BreadcrumbService',
        function (_$log_, _wss_, _t2is_, _t2rs_, _t2ls_, _t2vs_, _t2bcs_) {

            $log = _$log_;
            wss = _wss_;
            t2is = _t2is_;
            t2rs = _t2rs_;
            t2ls = _t2ls_;
            t2vs = _t2vs_;
            t2bcs = _t2bcs_;

            return {

                init: init,
                newDim: newDim,

                destroy: destroy,
                topo2AllInstances: allInstances,
                topo2CurrentLayout: currentLayout,
                topo2CurrentRegion: currentRegion,
                topo2StartDone: startDone,

                showMastership: showMastership,
                topo2PeerRegions: topo2PeerRegions
            };
        }]);
}());
