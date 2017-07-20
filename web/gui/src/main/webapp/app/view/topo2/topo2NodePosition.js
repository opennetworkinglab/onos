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
    var rs, t2mcs, t2sls, t2bgs;

    // Internal state;
    var nearDist = 15;

    function setElCoord(el, coord) {
        el.fix(true);
        el.x = el.px = coord[0];
        el.y = el.py = coord[1];
        return true;
    }

    function positionNode(node, forUpdate) {

        var meta = node.get('metaUi'),
            x = meta && meta.x,
            y = meta && meta.y,
            hasMeta = x !== undefined && y !== undefined,
            dim = [800, 600],
            xy;

        // If the node has metaUI data attached, it indicates that the user
        //  has dragged the node to a new position on the view; so we should
        //  respect that above any script-configured position...
        // (NOTE: This is slightly different to the "classic" topology code)

        if (hasMeta) {
            node.fix(true);
            node.px = node.x = x;
            node.py = node.y = y;
            return;
        }

        // Otherwise, use a precomputed location for peer regions, or
        // LONG/LAT (or GRID) locations for regions/devices/hosts

        if (node.nodeType === 'peer-region') {
            if (t2bgs.getBackgroundType() === 'geo') {
                setLongLat(node);
                return true;
            }

            // assumed to be grid
            var loc = node.get('location');

            // fallback to default placement if not defined.
            if (!loc.latOrY && !loc.longOrX) {
                loc = {
                    longOrX: -20,
                    latOrY: 10 * node.index()
                };
            }

            setElCoord(node, coordFromXY(loc));
            return;
        }

        // If the device contains explicit LONG/LAT data, use that to position
        if (setLongLat(node)) {
            // Indicate we want to update cached meta data...
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
        var loc = el.get('location');

        // bail if no location set
        if (!loc || (loc.latOrY === 0 && loc.longOrX === 0)) {
            return false;
        }

        if (loc.locType === 'geo') {
            return setElCoord(el, coordFromLngLat(loc));
        }
        if (loc.locType === 'grid') {
            return setElCoord(el, coordFromXY(loc));
        }

        return false;
    }

    function coordFromLngLat(loc) {
        var p = t2mcs.projection();
        return p ? p([loc.longOrX, loc.latOrY]) : [0, 0];
    }

    function coordFromXY(loc) {
        var bgWidth = t2sls.getWidth() || 100,
            bgHeight = t2sls.getHeight() || 100;

        var scale = 1000 / bgWidth,
            yOffset = (1000 - (bgHeight * scale)) / 2;

        // 1000 is a hardcoded HTML value of the SVG element (topo2.html)
        var x = scale * loc.longOrX,
            y = (scale * loc.latOrY) + yOffset;

        return [x, y];
    }

    angular.module('ovTopo2')
    .factory('Topo2NodePositionService',
        ['RandomService', 'Topo2MapConfigService',
            'Topo2SpriteLayerService', 'Topo2BackgroundService',
            function (_rs_, _t2mcs_, _t2sls_, _t2bgs_) {

                rs = _rs_;
                t2mcs = _t2mcs_;
                t2sls = _t2sls_;
                t2bgs = _t2bgs_;

                return {
                    positionNode: positionNode,
                    setLongLat: setLongLat
                };
            }
        ]);
})();
