/*
 * Copyright 2016-present Open Networking Foundation
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
    var $log, $loc, wss;

    var t2is, t2rs, t2ls, t2vs, t2bcs, t2ss, t2bgs, t2tbs, t2mss;
    var svg, uplink, dim, opts, zoomer;

    // ========================== Helper Functions

    function init(_svg_, _forceG_, _uplink_, _dim_, _zoomer_, _opts_) {

        svg = _svg_;
        uplink = _uplink_;
        dim = _dim_;
        zoomer = _zoomer_;
        opts = _opts_;


        t2bgs.init();
        t2bgs.region = t2rs;
        t2ls.init(svg, uplink, dim, zoomer, opts);
        t2bcs.addLayout(t2ls);
        t2ss.init(svg, zoomer);
        t2ss.region = t2rs;
        t2rs.layout = t2ls;
        t2mss.region = t2rs;
        t2tbs.init();
        navToBookmarkedRegion($loc.search().regionId);
    }

    function destroy() {
        t2tbs.destroy();
        $log.debug('Destroy topo force layout');
    }

    function navToBookmarkedRegion(regionId) {
        $log.debug('navToBookmarkedRegion:', regionId);
        if (regionId) {
            wss.sendEvent('topo2navRegion', {
                rid: regionId,
            });

            t2ls.createForceElements();
            t2ls.transitionDownRegion();
        }
    }

    // ========================== Event Handlers

    function allInstances(data) {
        $log.debug('>> topo2AllInstances event:', data);
        t2is.allInstances(data);
    }

    function currentLayout(data) {
        $log.debug('>> topo2CurrentLayout event:', data);
        t2rs.clear();
        t2bcs.addBreadcrumb(data.crumbs);
        t2bgs.addLayout(data);
    }

    function currentRegion(data) {
        $log.debug('>> topo2CurrentRegion event:', data);
        t2rs.loaded('regionData', data);
    }

    function topo2PeerRegions(data) {
        $log.debug('>> topo2PeerRegions event:', data);
        t2rs.loaded('peers', data.peers);
    }

    function modelEvent(data) {
        // $log.debug('>> topo2UiModelEvent event:', data);

        // TODO: Interpret the event and update our topo model state (if needed)
        // To Decide: Can we assume that the server will only send events
        //    related to objects that we are currently showing?
        //    (e.g. filtered by subregion contents?)
        t2rs.update(data);
    }

    function showMastership(masterId) {
        if (masterId) {
            showMastershipFor(masterId);
        } else {
            restoreLayerState();
        }
    }

    function restoreLayerState() {
        // NOTE: this level of indirection required, for when we have
        //          the layer filter functionality re-implemented
        suppressLayers(false);
    }

    // ========================== Main Service Definition

    function newDim(_dim_) {
        dim = _dim_;
        t2vs.newDim(dim);
    }

    // ========================== Main Service Definition

    function update(elements) {
        angular.forEach(elements, function (el) {
            el.update();
        });
    }

    function updateNodes() {
        update(t2rs.regionNodes());
    }

    function updateLinks() {
        update(t2rs.regionLinks());
    }

    function resetNodeLocation() {

        var hovered = t2rs.filterRegionNodes(function (model) {
            return model.get('hovered');
        });

        angular.forEach(hovered, function (model) {
            model.resetPosition();
        });
    }

    function unpin() {

        var hovered = t2rs.filterRegionNodes(function (model) {
            return model.get('hovered');
        });

        angular.forEach(hovered, function (model) {
            model.fix(false);
        });

        t2ls.start();
    }

    angular.module('ovTopo2')
    .factory('Topo2ForceService', [
        '$log', '$location', 'WebSocketService', 'Topo2InstanceService',
        'Topo2RegionService', 'Topo2LayoutService', 'Topo2ViewService',
        'Topo2BreadcrumbService', 'Topo2ZoomService', 'Topo2SelectService',
        'Topo2BackgroundService', 'Topo2ToolbarService', 'Topo2MastershipService',
        function (_$log_, _$loc_, _wss_, _t2is_, _t2rs_, _t2ls_,
            _t2vs_, _t2bcs_, zoomService, _t2ss_, _t2bgs_, _t2tbs_, _t2mss_) {

            $log = _$log_;
            $loc = _$loc_;
            wss = _wss_;
            t2is = _t2is_;
            t2rs = _t2rs_;
            t2ls = _t2ls_;
            t2vs = _t2vs_;
            t2bcs = _t2bcs_;
            t2ss = _t2ss_;
            t2bgs = _t2bgs_;
            t2tbs = _t2tbs_;
            t2mss = _t2mss_;

            var onZoom = function () {
                if (!t2rs.isLoadComplete()) {
                    return;
                }

                var nodes = [].concat(
                        t2rs.regionNodes(),
                        t2rs.regionLinks()
                    );

                angular.forEach(nodes, function (node) {
                    node.setScale();
                });
            };

            zoomService.addZoomEventListener(onZoom);

            return {

                init: init,
                newDim: newDim,

                destroy: destroy,
                topo2AllInstances: allInstances,
                topo2CurrentLayout: currentLayout,
                topo2CurrentRegion: currentRegion,

                topo2UiModelEvent: modelEvent,

                showMastership: showMastership,
                topo2PeerRegions: topo2PeerRegions,

                updateNodes: updateNodes,
                updateLinks: updateLinks,
                resetNodeLocation: resetNodeLocation,
                unpin: unpin,
            };
        }]);
})();
