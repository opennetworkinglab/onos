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
 ONOS GUI -- Topology Oblique View Module.
 Provides functionality to view the topology as two planes (packet & optical)
 from an oblique (side-on) perspective.
 */

(function () {
    'use strict';

    // injected refs
    var sus, flash;

    // api to topoForce
    var api;
    /*
     force()                        // get ref to force layout object
     zoomLayer()                    // get ref to zoom layer
     nodeGBBox()                    // get bounding box of node group layer
     node()                         // get ref to D3 selection of nodes
     link()                         // get ref to D3 selection of links
     nodes()                        // get ref to network nodes array
     tickStuff                      // ref to tick functions
     nodeLock(b)                    // test-and-set nodeLock state
     opacifyMap(b)                  // show or hide map layer
     inLayer(d, layer)              // return true if d in layer {'pkt'|'opt'}
     calcLinkPos()                  // recomputes link pos based on node data
     */

    // configuration
    var xsky = -.7, // x skew y factor
        xsk = -35, // x skew angle
        ysc = .5, // y scale
        pad = 50,
        time = 1500,
        fill = {
            pkt: 'rgba(130,130,170,0.3)', // blue-ish
            opt: 'rgba(170,130,170,0.3)', // magenta-ish
        };

    // internal state
    var oblique = false,
        xffn = null,
        plane = {},
        oldNodeLock;

    // function to be replaced by the localization bundle function
    var topoLion = function (x) {
        return '#tobq#' + x + '#';
    };

    function planeId(tag) {
        return 'topo-obview-' + tag + 'Plane';
    }

    function ytfn(h, dir) {
        return h * ysc * dir * 1.1;
    }

    function obXform(h, dir) {
        var yt = ytfn(h, dir);
        return sus.scale(1, ysc) + sus.translate(0, yt) + sus.skewX(xsk);
    }

    function noXform() {
        return sus.skewX(0) + sus.translate(0, 0) + sus.scale(1, 1);
    }

    function padBox(box, p) {
        box.x -= p;
        box.y -= p;
        box.width += p*2;
        box.height += p*2;
    }

    function toObliqueView() {
        var box = api.nodeGBBox(),
            ox, oy;

        padBox(box, pad);

        ox = box.x + box.width / 2;
        oy = box.y + box.height / 2;

        // remember node lock state, then lock the nodes down
        oldNodeLock = api.nodeLock(true);
        api.opacifyMap(false);

        insertPlanes(ox, oy);

        xffn = function (xy, dir) {
            var yt = ytfn(box.height, dir),
                ax = xy.x - ox,
                ay = xy.y - oy,
                x = ax + ay * xsky,
                y = (ay + yt) * ysc;
            return { x: ox + x, y: oy + y };
        };

        showPlane('pkt', box, -1);
        showPlane('opt', box, 1);
        obTransitionNodes();
    }

    function toNormalView() {
        xffn = null;

        hidePlane('pkt');
        hidePlane('opt');
        obTransitionNodes();

        removePlanes();

        // restore node lock state
        api.nodeLock(oldNodeLock);
        api.opacifyMap(true);
    }

    function obTransitionNodes() {
        // return the direction for the node
        // -1 for pkt layer, 1 for optical layer
        function dir(d) {
            return api.inLayer(d, 'pkt') ? -1 : 1;
        }

        if (xffn) {
            api.nodes().forEach(function (d) {
                var oldxy = { x: d.x, y: d.y },
                    coords = xffn(oldxy, dir(d));
                d.oldxy = oldxy;
                d.px = d.x = coords.x;
                d.py = d.y = coords.y;
            });
        } else {
            api.nodes().forEach(function (d) {
                var old = d.oldxy || { x: d.x, y: d.y };
                d.px = d.x = old.x;
                d.py = d.y = old.y;
                delete d.oldxy;
            });
        }

        api.node().transition()
            .duration(time)
            .attr(api.tickStuff.nodeAttr);
        api.link().transition()
            .duration(time)
            .call(api.calcLinkPos)
            .attr(api.tickStuff.linkAttr)
            .call(api.applyNumLinkLabels);
        api.linkLabel().transition()
            .duration(time)
            .attr(api.tickStuff.linkLabelAttr);
    }

    function showPlane(tag, box, dir) {
        // set box origin at center..
        box.x = -box.width/2;
        box.y = -box.height/2;

        plane[tag].select('rect')
            .attr(box)
            .attr('opacity', 0)
            .transition()
            .duration(time)
            .attr('opacity', 1)
            .attr('transform', obXform(box.height, dir));
    }

    function hidePlane(tag) {
        plane[tag].select('rect')
            .transition()
            .duration(time)
            .attr('opacity', 0)
            .attr('transform', noXform());
    }

    function insertPlanes(ox, oy) {
        function ins(tag) {
            var id = planeId(tag),
                g = api.zoomLayer().insert('g', '#topo-G')
                    .attr('id', id)
                    .attr('transform', sus.translate(ox, oy));
            g.append('rect')
                .attr('fill', fill[tag])
                .attr('opacity', 0);
            plane[tag] = g;
        }
        ins('opt');
        ins('pkt');
    }

    function removePlanes() {
        function rem(tag) {
            var id = planeId(tag);
            api.zoomLayer().select('#'+id)
                .transition()
                .duration(time + 50)
                .remove();
            delete plane[tag];
        }
        rem('opt');
        rem('pkt');
    }

    // invoked after the localization bundle has been received from the server
    function setLionBundle(bundle) {
        topoLion = bundle;
    }

// === -----------------------------------------------------
// === MODULE DEFINITION ===

angular.module('ovTopo')
    .factory('TopoObliqueService',
    ['SvgUtilService', 'FlashService',

    function (_sus_, _flash_) {
        sus = _sus_;
        flash = _flash_;

        function initOblique(_api_) {
            api = _api_;
        }

        function destroyOblique() { }

        function toggleOblique() {
            oblique = !oblique;
            if (oblique) {
                api.force().stop();
                flash.flash(topoLion('fl_oblique_view'));
                toObliqueView();
            } else {
                flash.flash(topoLion('fl_normal_view'));
                toNormalView();
            }
        }

        return {
            initOblique: initOblique,
            destroyOblique: destroyOblique,

            isOblique: function () { return oblique; },
            toggleOblique: toggleOblique,
            setLionBundle: setLionBundle,
        };
    }]);
}());
