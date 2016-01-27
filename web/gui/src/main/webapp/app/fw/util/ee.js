/*
 *  Copyright 2016 Open Networking Laboratory
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/*
 ONOS GUI -- Util -- EE functions
 */
(function () {
    'use strict';

    // injected services
    var fs;

    // function references
    var fcc = String.fromCharCode;

    function computeTransform(x) {
        var m = x.split(':'),
            h = Number(m[0]),
            d = m[1],
            n = d.length,
            w = [],
            i;

        for (i = 0; i<n; i+=2)
            w.push(fcc(Number(d.slice(i, i+2))));

        return fs.eecode(h, w.join(''));
    }

    function genMap(data) {
        var map = {};

        data.forEach(function (x) {
            var r = computeTransform(x);
            map['shift' + r.e] = r.o.toLowerCase() + '.bin';
        });
        return map;
    }

    angular.module('onosUtil')
    .factory('EeService',
    ['FnService', function (_fs_) {
        fs = _fs_;

        return {
            genMap: genMap
        }
    }]);
}());
