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
 ONOS GUI -- Topology Model Module.
 Auxiliary functions for the model of the topology; that is, our internal
  representations of devices, hosts, links, etc.
 */

(function () {
    'use strict';

    // injected refs
    var $log, fs, rnd, api;

    var dim;    // dimensions of layout, as [w,h]

    // configuration 'constants'
    var defaultLinkType = 'direct',
        nearDist = 15;


    function coordFromLngLat(loc) {
        var p = api.projection();
        return p ? p([loc.lng, loc.lat]) : [0, 0];
    }

    function lngLatFromCoord(coord) {
        var p = api.projection();
        return p ? p.invert(coord) : [0, 0];
    }

    function positionNode(node, forUpdate) {
        var meta = node.metaUi,
            x = meta && meta.x,
            y = meta && meta.y,
            xy;

        // If we have [x,y] already, use that...
        if (x && y) {
            node.fixed = true;
            node.px = node.x = x;
            node.py = node.y = y;
            return;
        }

        var location = node.location,
            coord;

        if (location && location.type === 'latlng') {
            coord = coordFromLngLat(location);
            node.fixed = true;
            node.px = node.x = coord[0];
            node.py = node.y = coord[1];
            return true;
        }

        // if this is a node update (not a node add).. skip randomizer
        if (forUpdate) {
            return;
        }

        // Note: Placing incoming unpinned nodes at exactly the same point
        //        (center of the view) causes them to explode outwards when
        //        the force layout kicks in. So, we spread them out a bit
        //        initially, to provide a more serene layout convergence.
        //       Additionally, if the node is a host, we place it near
        //        the device it is connected to.

        function rand() {
            return {
                x: rnd.randDim(dim[0]),
                y: rnd.randDim(dim[1])
            };
        }

        function near(node) {
            return {
                x: node.x + nearDist + rnd.spread(nearDist),
                y: node.y + nearDist + rnd.spread(nearDist)
            };
        }

        function getDevice(cp) {
            var d = api.lookup[cp.device];
            return d || rand();
        }

        xy = (node.class === 'host') ? near(getDevice(node.cp)) : rand();
        angular.extend(node, xy);
    }

    function mkSvgCls(dh, t, on) {
        var ndh = 'node ' + dh,
            ndht = t ? ndh + ' ' + t : ndh;
        return on ? ndht + ' online' : ndht;
    }

    function createDeviceNode(device) {
        var node = device;

        // Augment as needed...
        node.class = 'device';
        node.svgClass = mkSvgCls('device', device.type, device.online);
        positionNode(node);
        return node;
    }

    function createHostNode(host) {
        var node = host;

        // Augment as needed...
        node.class = 'host';
        if (!node.type) {
            node.type = 'endstation';
        }
        node.svgClass = mkSvgCls('host', node.type);
        positionNode(node);
        return node;
    }

    function createHostLink(host) {
        var src = host.id,
            dst = host.cp.device,
            id = host.ingress,
            lnk = linkEndPoints(src, dst);

        if (!lnk) {
            return null;
        }

        // Synthesize link ...
        angular.extend(lnk, {
            key: id,
            class: 'link',

            type: function () { return 'hostLink'; },
            online: function () {
                // hostlink target is edge switch
                return lnk.target.online;
            },
            linkWidth: function () { return 1; }
        });
        return lnk;
    }

    function createLink(link) {
        var lnk = linkEndPoints(link.src, link.dst);

        if (!lnk) {
            return null;
        }

        angular.extend(lnk, {
            key: link.id,
            class: 'link',
            fromSource: link,

            // functions to aggregate dual link state
            type: function () {
                var s = lnk.fromSource,
                    t = lnk.fromTarget;
                return (s && s.type) || (t && t.type) || defaultLinkType;
            },
            online: function () {
                var s = lnk.fromSource,
                    t = lnk.fromTarget,
                    both = lnk.source.online && lnk.target.online;
                return both && ((s && s.online) || (t && t.online));
            },
            linkWidth: function () {
                var s = lnk.fromSource,
                    t = lnk.fromTarget,
                    ws = (s && s.linkWidth) || 0,
                    wt = (t && t.linkWidth) || 0;
                return Math.max(ws, wt);
            }
        });
        return lnk;
    }


    function linkEndPoints(srcId, dstId) {
        var srcNode = api.lookup[srcId],
            dstNode = api.lookup[dstId],
            sMiss = !srcNode ? missMsg('src', srcId) : '',
            dMiss = !dstNode ? missMsg('dst', dstId) : '';

        if (sMiss || dMiss) {
            $log.error('Node(s) not on map for link:' + sMiss + dMiss);
            //logicError('Node(s) not on map for link:\n' + sMiss + dMiss);
            return null;
        }
        return {
            source: srcNode,
            target: dstNode,
            x1: srcNode.x,
            y1: srcNode.y,
            x2: dstNode.x,
            y2: dstNode.y
        };
    }

    function missMsg(what, id) {
        return '\n[' + what + '] "' + id + '" missing';
    }

    // ==========================
    // Module definition

    angular.module('ovTopo')
        .factory('TopoModelService',
        ['$log', 'FnService', 'RandomService',

        function (_$log_, _fs_, _rnd_) {
            $log = _$log_;
            fs = _fs_;
            rnd = _rnd_;

            function initModel(_api_, _dim_) {
                api = _api_;
                dim = _dim_;
            }

            function newDim(_dim_) {
                dim = _dim_;
            }

            return {
                initModel: initModel,
                newDim: newDim,

                positionNode: positionNode,
                createDeviceNode: createDeviceNode,
                createHostNode: createHostNode,
                createHostLink: createHostLink,
                createLink: createLink,
                coordFromLngLat: coordFromLngLat,
                lngLatFromCoord: lngLatFromCoord,
            }
        }]);
}());
