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
 ONOS GUI -- Topology Layout Module.
 Module that contains the d3.force.layout logic
 */

(function () {
    'use strict';

    var randomService;
    var fn;

    //internal state;
    var defaultLinkType = 'direct',
        nearDist = 15;

    var devIconDim = 36,
        labelPad = 10,
        halfDevIcon = devIconDim / 2,
        nodeLabelIndex = 1;

    function positionNode(node, forUpdate) {

        var meta = node.metaUi,
            x = meta && meta.x,
            y = meta && meta.y,
            dim = [800, 600],
            xy;

        // if the device contains explicit LONG/LAT data, use that to position
        if (setLongLat(node)) {
            //indicate we want to update cached meta data...
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
                x: randomService.randDim(dim[0]),
                y: randomService.randDim(dim[1])
            };
        }

        function near(node) {
            return {
                x: node.x + nearDist + randomService.spread(nearDist),
                y: node.y + nearDist + randomService.spread(nearDist)
            };
        }

        function getDevice(cp) {
            // console.log(cp);
            // var d = lu[cp.device];
            // return d || rand();
            return rand();
        }

        xy = (node.class === 'host') ? near(getDevice(node.cp)) : rand();
        angular.extend(node, xy);
    }

    function setLongLat(node) {
        var loc = node.location,
            coord;

        if (loc && loc.type === 'lnglat') {
            coord = [0, 0];
            node.fixed = true;
            node.px = node.x = coord[0];
            node.py = node.y = coord[1];
            return true;
        }
    }

    angular.module('ovTopo2')
    .factory('Topo2NodeModel',
        ['Topo2Model', 'FnService',  'RandomService',
        function (Model, _fn_, _RandomService_) {

            randomService = _RandomService_;
            fn = _fn_;

            return Model.extend({
                initialize: function () {
                    this.node = this.createNode();
                },
                label: function () {

                    var props = this.get('props'),
                        id = this.get('id'),
                        friendlyName = props ? props.name : id,
                        labels = ['', friendlyName, id],
                        idx = (nodeLabelIndex < labels.length) ? nodeLabelIndex : 0;

                    return labels[idx];
                },
                trimLabel: function(label) {
                    return (label && label.trim()) || '';
                },
                computeLabelWidth: function(el) {
                    var text = el.select('text'),
                    box = text.node().getBBox();
                    return box.width + labelPad * 2;
                },
                addLabelElements: function(label) {
                    var rect = this.el.append('rect');
                    var text = this.el.append('text').text(label)
                        .attr('text-anchor', 'left')
                        .attr('y', '0.3em')
                        .attr('x', halfDevIcon + labelPad);

                    return {
                        rect: rect,
                        text: text
                    }
                },
                svgClassName: function () {
                    return fn.classNames('node', this.nodeType, this.get('type'), {
                        online: this.get('online')
                    });
                },
                createNode: function () {

                    var node = angular.extend({}, this.attributes);

                    // Augment as needed...
                    node.class = this.nodeType;
                    node.svgClass = this.svgClassName();
                    positionNode(node);
                    return node;
                }
            });
        }]
    );
})();
