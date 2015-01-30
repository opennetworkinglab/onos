/*
 * Copyright 2014,2015 Open Networking Laboratory
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
 ONOS GUI -- Util -- General Purpose Functions
 */
(function () {
    'use strict';

    var $window;

    function isF(f) {
        return typeof f === 'function' ? f : null;
    }

    function isA(a) {
        // NOTE: Array.isArray() is part of EMCAScript 5.1
        return Array.isArray(a) ? a : null;
    }

    function isS(s) {
        return typeof s === 'string' ? s : null;
    }

    function isO(o) {
        return (o && typeof o === 'object' && o.constructor === Object) ? o : null;
    }

    function contains(a, x) {
        return isA(a) && a.indexOf(x) > -1;
    }

    // Returns true if all names in the array are defined as functions
    // on the given api object; false otherwise.
    // Also returns false if there are properties on the api that are NOT
    //  listed in the array of names.
    function areFunctions(api, fnNames) {
        var fnLookup = {},
            extraFound = false;

        if (!isA(fnNames)) {
            return false;
        }
        var n = fnNames.length,
            i, name;
        for (i=0; i<n; i++) {
            name = fnNames[i];
            if (!isF(api[name])) {
                return false;
            }
            fnLookup[name] = true;
        }

        // check for properties on the API that are not listed in the array,
        angular.forEach(api, function (value, key) {
            if (!fnLookup[key]) {
                extraFound = true;
            }
        });
        return !extraFound;
    }

    // Returns true if all names in the array are defined as functions
    // on the given api object; false otherwise. This is a non-strict version
    // that does not care about other properties on the api.
    function areFunctionsNonStrict(api, fnNames) {
        if (!isA(fnNames)) {
            return false;
        }
        var n = fnNames.length,
            i, name;
        for (i=0; i<n; i++) {
            name = fnNames[i];
            if (!isF(api[name])) {
                return false;
            }
        }
        return true;
    }

    // Returns width and height of window inner dimensions.
    // offH, offW : offset width/height are subtracted, if present
    function windowSize(offH, offW) {
        var oh = offH || 0,
            ow = offW || 0;
        return {
            height: $window.innerHeight - oh,
            width: $window.innerWidth - ow
        };
    }

    // search through an array of objects, looking for the one with the
    // tagged property matching the given key. tag defaults to 'id'.
    // returns the index of the matching object, or -1 for no match.
    function find(key, array, tag) {
        var _tag = tag || 'id',
            idx, n, d;
        for (idx = 0, n = array.length; idx < n; idx++) {
            d = array[idx];
            if (d[_tag] === key) {
                return idx;
            }
        }
        return -1;
    }

    angular.module('onosUtil')
        .factory('FnService', ['$window', function (_$window_) {
            $window = _$window_;

            return {
                isF: isF,
                isA: isA,
                isS: isS,
                isO: isO,
                contains: contains,
                areFunctions: areFunctions,
                areFunctionsNonStrict: areFunctionsNonStrict,
                windowSize: windowSize,
                find: find
            };
    }]);

}());
