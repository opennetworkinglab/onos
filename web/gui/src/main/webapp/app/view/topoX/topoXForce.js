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
    var $log, wss;

    // ========================== Helper Functions

    function init() {
        $log.debug('Initialize topo force layout');
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
        var topdiv = d3.select('#topoXtmp');
        var parentRegion = data.parent;
        var span = topdiv.select('.parentRegion').select('span');
        span.text(parentRegion || '[no parent]');
        span.classed('nav-me', !!parentRegion);
    }

    function doTmpCurrentRegion(data) {
        var topdiv = d3.select('#topoXtmp');
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
        $log.debug('>> topo2AllInstances event:', data)
        doTmpCurrentLayout(data);
    }

    function currentLayout(data) {
        $log.debug('>> topo2CurrentLayout event:', data)
    }

    function currentRegion(data) {
        $log.debug('>> topo2CurrentRegion event:', data)
        doTmpCurrentRegion(data);
    }

    function peerRegions(data) {
        $log.debug('>> topo2PeerRegions event:', data)
        doTmpPeerRegions(data);
    }

    function startDone(data) {
        $log.debug('>> topo2StartDone event:', data)
    }
    
    // ========================== Main Service Definition

    angular.module('ovTopoX')
    .factory('TopoXForceService',
        ['$log', 'WebSocketService',

        function (_$log_, _wss_) {
            $log = _$log_;
            wss = _wss_;
            
            return {
                init: init,
                destroy: destroy,
                topo2AllInstances: allInstances,
                topo2CurrentLayout: currentLayout,
                topo2CurrentRegion: currentRegion,
                topo2PeerRegions: peerRegions,
                topo2StartDone: startDone
            };
        }]);
}());
