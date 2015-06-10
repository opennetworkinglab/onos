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
 ONOS GUI -- Util -- User Preference Service
 */
(function () {
    'use strict';

    // injected refs
    var $log, $cookies, fs;

    // internal state
    var cache = {};

    // NOTE: in Angular 1.3.5, $cookies is just a simple object, and
    //       cookie values are just strings. From the 1.3.5 docs:
    //
    //       "Only a simple Object is exposed and by adding or removing
    //        properties to/from this object, new cookies are created/deleted
    //        at the end of current $eval. The object's properties can only
    //        be strings."
    //
    //       We may want to upgrade the version of Angular sometime soon
    //        since later version support objects as cookie values.

    // NOTE: prefs represented as simple name/value pairs
    //       => a temporary restriction while we are encoding into cookies
    /*
        {
          foo: 1,
          bar: 0,
          goo: 2
        }

        stored as "foo:1,bar:0,goo:2"
     */

    // reads cookie with given name and returns an object rep of its value
    // or null if no such cookie is set
    function getPrefs(name) {
        var cook = $cookies[name],
            bits,
            obj = {};

        if (cook) {
            bits = cook.split(',');
            bits.forEach(function (value) {
                var x = value.split(':');
                obj[x[0]] = x[1];
            });

            // update the cache
            cache[name] = obj;
            return obj;
        }
        // perhaps we have a cached copy..
        return cache[name];
    }

    // converts string values to numbers for selected (or all) keys
    function asNumbers(obj, keys) {
        if (!obj) return null;

        if (!keys) {
            // do them all
            angular.forEach(obj, function (v, k) {
                obj[k] = Number(obj[k]);
            });
        } else {
            keys.forEach(function (k) {
                obj[k] = Number(obj[k]);
            });
        }
        return obj;
    }

    function setPrefs(name, obj) {
        var bits = [],
            str;

        angular.forEach(obj, function (value, key) {
            bits.push(key + ':' + value);
        });
        str = bits.join(',');

        // keep a cached copy of the object
        cache[name] = obj;

        // The angular way of doing this...
        // $cookies[name] = str;
        //  ...but it appears that this gets delayed, and doesn't 'stick' ??

        // FORCE cookie to be set by writing directly to document.cookie...
        document.cookie = name + '=' + encodeURIComponent(str);
        if (fs.debugOn('prefs')) {
            $log.debug('<<>> Wrote cookie <'+name+'>:', str);
        }
    }

    angular.module('onosUtil')
    .factory('PrefsService', ['$log', '$cookies', 'FnService',
        function (_$log_, _$cookies_, _fs_) {
            $log = _$log_;
            $cookies = _$cookies_;
            fs = _fs_;

            return {
                getPrefs: getPrefs,
                asNumbers: asNumbers,
                setPrefs: setPrefs
            };
        }]);

}());
