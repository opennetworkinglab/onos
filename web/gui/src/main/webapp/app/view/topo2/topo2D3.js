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

    var is;

    // Configuration
    var hostRadius = 14;

    function init() {}

    function nodeEnter(node) {
        node.onEnter(this, node);
    }

    function nodeExit(node) {
        node.onExit(this, node);
    }

    function hostLabel(d) {
        return d.get('id');

        // var idx = (hostLabelIndex < d.get('labels').length) ? hostLabelIndex : 0;
        // return d.labels[idx];
    }

    function hostEnter(d) {
        var node = d3.select(this),
            gid = d.get('type') || 'unknown',
            textDy = hostRadius + 10;

        d.el = node;
        // sus.visible(node, api.showHosts());

        is.addHostIcon(node, hostRadius, gid);

        node.append('text')
            .text(hostLabel)
            .attr('dy', textDy)
            .attr('text-anchor', 'middle');
    }

    function linkEntering(link) {
        link.onEnter(this);
    }

    angular.module('ovTopo2')
    .factory('Topo2D3Service',
    ['IconService',

        function (_is_) {
            is = _is_;

            return {
                init: init,
                nodeEnter: nodeEnter,
                nodeExit: nodeExit,
                hostEnter: hostEnter,
                linkEntering: linkEntering
            };
        }
    ]
);
})();
