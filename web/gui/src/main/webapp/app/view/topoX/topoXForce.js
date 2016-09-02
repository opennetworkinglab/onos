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

    // selected DOM refs
    var topdiv;

    // ========================== Helper Functions

    function init() {
        topdiv = d3.select('#topoXtmp');
        $log.debug('Initialize topo force layout');
    }

    function destroy() {
        $log.debug('Destroy topo force layout');
    }

    function rmP(div) {
        div.selectAll('p').remove();
    }

    function navRequest(rid) {
        wss.sendEvent('topo2navRegion', {
            rid: rid
        });
    }

    function doTmpInstances(data) {
        var idiv = topdiv.select('.instances').select('div');
        data.members.forEach(function (m) {
            idiv.append('div').text(m.id);
        });
    }

    function doTmpCurrentLayout(data) {
        var ldDiv = topdiv.select('.layoutData'),
            bcs = ldDiv.select('.l_crumbs');

        function setSpan(v, val) {
            var cls = '.l_' + v,
                span = ldDiv.select(cls).select('span'),
                value = val || data[v];
            span.html(value);
        }

        setSpan('id');
        setSpan('parent');
        setSpan('region');
        setSpan('regionName');

        addCrumbNav(bcs, data.crumbs, data.region);
    }

    function addCrumbNav(span, array, id) {
        var rev = [];

        span.selectAll('span').remove();

        array.forEach(function (a) {
            rev.unshift(a.id);
        });

        rev.forEach(function (rid, idx) {
            if (idx) {
                span.append('span').text(' +++ ');
            }
            if (rid != id) {
                addNavigable(span, 'span', rid);
            } else {
                span.append('span').text(rid);
            }
        });
    }

    function addNavigable(span, what, rid) {
        span.append(what).classed('nav-me', true)
            .text(rid)
            .on('click', function () { navRequest(rid); });
    }

    function doTmpCurrentRegion(data) {
        var span = topdiv.select('.thisRegion').select('span');
        var div;
        span.text(data.id);

        div = topdiv.select('.subRegions').select('div');
        rmP(div);
        data.subregions.forEach(function (r) {
            addNavigable(div, 'p', r.id);
        });

        div = topdiv.select('.devices').select('div');
        rmP(div);
        data.layerOrder.forEach(function (tag, idx) {
            var devs = data.devices[idx];
            devs.forEach(function (d) {
                div.append('p')
                    .text('[' + tag + '] ' + d.id);
            });

        });

        div = topdiv.select('.hosts').select('div');
        rmP(div);
        data.layerOrder.forEach(function (tag, idx) {
            var hosts = data.hosts[idx];
            hosts.forEach(function (h) {
                div.append('p')
                    .text('[' + tag + '] ' + h.id);
            });
        });

        div = topdiv.select('.links').select('div');
        rmP(div);
        data.links.forEach(function (lnk) {
            div.append('p')
                .text(lnk.id);
        });
    }

    function doTmpPeerRegions(data) {
        var peerDiv = topdiv.select('.peers').select('div');
        rmP(peerDiv);

        function logPeer(p) {
            var o = peerDiv.append('p'),
                id = p.id,
                nt = p.nodeType;
            o.text('[' + nt + '] id = ' + id);
        }

        data.peers.forEach(function (p) {
            logPeer(p);
        });
    }

    // ========================== Event Handlers

    function allInstances(data) {
        $log.debug('>> topo2AllInstances event:', data);
        doTmpInstances(data);
    }

    function currentLayout(data) {
        $log.debug('>> topo2CurrentLayout event:', data);
        doTmpCurrentLayout(data);
    }

    function currentRegion(data) {
        $log.debug('>> topo2CurrentRegion event:', data);
        doTmpCurrentRegion(data);
    }

    function peerRegions(data) {
        $log.debug('>> topo2PeerRegions event:', data);
        doTmpPeerRegions(data);
    }

    function startDone(data) {
        $log.debug('>> topo2StartDone event:', data);
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
