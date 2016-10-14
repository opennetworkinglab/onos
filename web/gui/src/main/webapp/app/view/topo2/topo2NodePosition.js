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
 ONOS GUI -- Topology Node Position Module.
 Module that helps position nodes in the topology
 */

(function () {
    'use strict';

    // Injected vars
    var rs, t2mcs;

    // Internal state;
    var nearDist = 15;

    function positionNode(node, forUpdate) {
        var meta = node.get('metaUi'),
            x = meta && meta.x,
            y = meta && meta.y,
            dim = [800, 600],
            xy;

        // If the device contains explicit LONG/LAT data, use that to position
        if (setLongLat(node)) {
            // Indicate we want to update cached meta data...
            return true;
        }

        // else if we have [x,y] cached in meta data, use that...
        if (x !== undefined && y !== undefined) {
            node.fixed = true;
            node.px = node.x = x;
            node.py = node.y = y;
            return;
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
                x: rs.randDim(dim[0]),
                y: rs.randDim(dim[1])
            };
        }

        function near(node) {
            return {
                x: node.x + nearDist + rs.spread(nearDist),
                y: node.y + nearDist + rs.spread(nearDist)
            };
        }

        function getDevice(cp) {
            return rand();
        }

        xy = (node.class === 'host') ? near(getDevice(node.cp)) : rand();

        if (node.class === 'sub-region') {
            xy = rand();
            node.x = node.px = xy.x;
            node.y = node.py = xy.y;
        }
        angular.extend(node, xy);
    }

    function setLongLat(el) {
        var loc = el.get('location'),
            coord;

        if (loc && loc.type === 'lnglat') {

            if (loc.lat === 0 && loc.lng === 0) {
                return false;
            }

            coord = coordFromLngLat(loc);
            el.fixed = true;
            el.x = el.px = coord[0];
            el.y = el.py = coord[1];

            return true;
        }
    }

    function coordFromLngLat(loc) {
        var p = t2mcs.projection();
        return p ? p([loc.lng, loc.lat]) : [0, 0];
    }

    angular.module('ovTopo2')
    .factory('Topo2NodePositionService',
        ['RandomService', 'Topo2MapConfigService',
            function (_rs_, _t2mcs_) {

                rs = _rs_;
                t2mcs = _t2mcs_;

                return {
                    positionNode: positionNode,
                    setLongLat: setLongLat
                };
            }
        ]);
})();
