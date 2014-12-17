/*
 * Copyright 2014 Open Networking Laboratory
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
 ONOS GUI -- General Purpose Functions

 @author Simon Hunt
 */
(function (onos) {
    'use strict';

    function isF(f) {
        return $.isFunction(f) ? f : null;
    }

    function isA(a) {
        return $.isArray(a) ? a : null;
    }

    function isS(s) {
        return typeof s === 'string' ? s : null;
    }

    function isO(o) {
        return $.isPlainObject(o) ? o : null;
    }

    function contains(a, x) {
        return isA(a) && a.indexOf(x) > -1;
    }

    onos.factory('FnService', [function () {
        return {
            isF: isF,
            isA: isA,
            isS: isS,
            isO: isO,
            contains: contains
        };
    }]);

}(ONOS));
