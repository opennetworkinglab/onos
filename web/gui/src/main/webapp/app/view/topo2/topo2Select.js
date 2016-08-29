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
 ONOS GUI -- Topology Select Module.
 */

(function () {
    'use strict';

    // internal state
    var hovered, selections, selectOrder, consumeClick;

    function selectObject(obj) {
        var el = this,
            nodeEv = el && el.tagName === 'g',
            ev = d3.event.sourceEvent || {},
            n;

        console.log(el, nodeEv, ev, n);
    }

    function clickConsumed(x) {
        var cc = consumeClick;
        consumeClick = !!x;
        return cc;
    }

    angular.module('ovTopo2')
    .factory('Topo2SelectService',
    [
        function () {

            return {
                selectObject: selectObject,
                clickConsumed: clickConsumed
            };
        }
    ]);

})();
