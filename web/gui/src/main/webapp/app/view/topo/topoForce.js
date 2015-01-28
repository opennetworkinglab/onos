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
 ONOS GUI -- Topology Event Module.
 Defines event handling for events received from the server.
 */

(function () {
    'use strict';

    // injected refs
    var $log, sus;

    // internal state
    var settings,
        force,      // force layout object
        drag,       // drag behavior handler
        network = {
            nodes: [],
            links: [],
            lookup: {},
            revLinkToKey: {}
        };


    // SVG elements;
    var linkG, linkLabelG, nodeG;

    // D3 selections;
    var link, linkLabel, node;

    // default settings for force layout
    var defaultSettings = {
        gravity: 0.4,
        friction: 0.7,
        charge: {
            // note: key is node.class
            device: -8000,
            host: -5000,
            _def_: -12000
        },
        linkDistance: {
            // note: key is link.type
            direct: 100,
            optical: 120,
            hostLink: 3,
            _def_: 50
        },
        linkStrength: {
            // note: key is link.type
            // range: {0.0 ... 1.0}
            //direct: 1.0,
            //optical: 1.0,
            //hostLink: 1.0,
            _def_: 1.0
        }
    };


    // force layout tick function
    function tick() {

    }


    function selectCb() { }
    function atDragEnd() {}
    function dragEnabled() {}
    function clickEnabled() {}


    // ==========================

    angular.module('ovTopo')
    .factory('TopoForceService',
        ['$log', 'SvgUtilService',

        function (_$log_, _sus_) {
            $log = _$log_;
            sus = _sus_;

            // forceG is the SVG group to display the force layout in
            // w, h are the initial dimensions of the SVG
            // opts are, well, optional :)
            function initForce (forceG, w, h, opts) {
                // TODO: create the force layout and initialize
                settings = angular.extend({}, defaultSettings, opts);

                linkG = forceG.append('g').attr('id', 'topo-links');
                linkLabelG = forceG.append('g').attr('id', 'topo-linkLabels');
                nodeG = forceG.append('g').attr('id', 'topo-nodes');

                link = linkG.selectAll('.link');
                linkLabel = linkLabelG.selectAll('.linkLabel');
                node = nodeG.selectAll('.node');

                force = d3.layout.force()
                    .size(w, h)
                    .nodes(network.nodes)
                    .links(network.links)
                    .gravity(settings.gravity)
                    .friction(settings.friction)
                    .charge(settings.charge._def_)
                    .linkDistance(settings.linkDistance._def_)
                    .linkStrength(settings.linkStrength._def_)
                    .on('tick', tick);

                drag = sus.createDragBehavior(force,
                    selectCb, atDragEnd, dragEnabled, clickEnabled);
            }

            function resize(w, h) {
                force.size(w, h);
                // Review -- do we need to nudge the layout ?
            }

            return {
                initForce: initForce,
                resize: resize
            };
        }]);
}());
