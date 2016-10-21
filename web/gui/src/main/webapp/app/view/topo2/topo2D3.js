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

    function nodeEnter(node) {
        node.onEnter(this, node);
    }

    function nodeExit(node) {
        node.onExit(this, node);
    }

    function hostEnter(node) {
        node.onEnter(this, node);
    }

    function linkEntering(link) {
        link.onEnter(this);
    }

    angular.module('ovTopo2')
    .factory('Topo2D3Service',
    [function (_is_) {
        return {
            nodeEnter: nodeEnter,
            nodeExit: nodeExit,
            hostEnter: hostEnter,
            linkEntering: linkEntering
        };
    }]
);
})();
