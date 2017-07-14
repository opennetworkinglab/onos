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
 ONOS GUI -- Random -- Encapsulated randomness
 */
(function () {
    'use strict';

    var halfRoot2 = 0.7071;

    // given some value, s, returns an integer between -s/2 and s/2
    // e.g. s = 100; result in the range [-50..50)
    function spread(s) {
        return Math.floor((Math.random() * s) - s / 2);
    }

    // for a given dimension, d, choose a random value somewhere between
    // 0 and d where the value is within (d / (2 * sqrt(2))) of d/2.
    function randDim(d) {
        return d / 2 + spread(d * halfRoot2);
    }

    angular.module('onosUtil')
        .factory('RandomService', ['$log', 'FnService',

        function () {
            return {
                spread: spread,
                randDim: randDim,
            };
        }]);
}());
