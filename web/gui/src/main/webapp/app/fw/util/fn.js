/*
 * Copyright 2014-present Open Networking Laboratory
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

    // injected services
    var $window, $log;

    // internal state
    var debugFlags = {};

    // function references
    var fcc = String.fromCharCode,
        cca = String.prototype.charCodeAt;

    function _parseDebugFlags(dbgstr) {
        var bits = dbgstr ? dbgstr.split(",") : [];
        bits.forEach(function (key) {
            debugFlags[key] = true;
        });
        $log.debug('Debug flags:', dbgstr);
    }

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

    // Returns true if current browser determined to be a mobile device
    function isMobile() {
        var ua = $window.navigator.userAgent,
            patt = /iPhone|iPod|iPad|Silk|Android|BlackBerry|Opera Mini|IEMobile/;
        return patt.test(ua);
    }

    // Returns true if the current browser determined to be Chrome
    function isChrome() {
        var isChromium = $window.chrome,
            vendorName = $window.navigator.vendor,
            isOpera = $window.navigator.userAgent.indexOf("OPR") > -1;
        return (isChromium !== null &&
        isChromium !== undefined &&
        vendorName === "Google Inc." &&
        isOpera == false);
    }

    // Returns true if the current browser determined to be Safari
    function isSafari() {
        return ($window.navigator.userAgent.indexOf('Safari') !== -1 &&
        $window.navigator.userAgent.indexOf('Chrome') === -1);
    }

    // Returns true if the current browser determined to be Firefox
    function isFirefox() {
        return typeof InstallTrigger !== 'undefined';
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

    // search through array to find (the first occurrence of) item,
    // returning its index if found; otherwise returning -1.
    function inArray(item, array) {
        var i;
        if (isA(array)) {
            for (i=0; i<array.length; i++) {
                if (array[i] === item) {
                    return i;
                }
            }
        }
        return -1;
    }

    // remove (the first occurrence of) the specified item from the given
    // array, if any. Return true if the removal was made; false otherwise.
    function removeFromArray(item, array) {
        var found = false,
            i = inArray(item, array);
        if (i >= 0) {
            array.splice(i, 1);
            found = true;
        }
        return found;
    }

    // return true if the object is empty, return false otherwise
    function isEmptyObject(obj) {
        var key;
        for (key in obj) {
            return false;
        }
        return true;
    }

    // returns true if the two objects have all the same properties
    function sameObjProps(obj1, obj2) {
        var key;
        for (key in obj1) {
            if (obj1.hasOwnProperty(key)) {
                if (!(obj1[key] === obj2[key])) {
                    return false;
                }
            }
        }
        return true;
    }

    // returns true if the array contains the object
    // does NOT use strict object reference equality,
        // instead checks each property individually for equality
    function containsObj(arr, obj) {
        var i,
            len = arr.length;
        for (i = 0; i < len; i++) {
            if (sameObjProps(arr[i], obj)) {
                return true;
            }
        }
        return false;
    }

    // return the given string with the first character capitalized.
    function cap(s) {
        return s ? s[0].toUpperCase() + s.slice(1).toLowerCase() : s;
    }

    // return encoding structure for given parameters
    function eecode(h, w) {
        var m = 65,
            x = 90,
            d = x - m + 1,
            s = x + m,
            o = [],
            n, i, c, e;

        for (i = 0, n = w.length; i<n; i++) {
            c = cca.call(w, i);
            e = s - c + h;
            e = e > x ? e - d : e;
            o.push(e);
        }
        return {
            o: w,
            d: o.join(''),
            e: fcc.apply(o, o)
        };
    }

    // return the parameter without a px suffix
    function noPx(num) {
        return Number(num.replace(/px$/, ''));
    }

    // return an element's given style property without px suffix
    function noPxStyle(elem, prop) {
        return Number(elem.style(prop).replace(/px$/, ''));
    }

    function endsWith(str, suffix) {
        return str.indexOf(suffix, str.length - suffix.length) !== -1;
    }

    function parseBitRate(str) {
        return Number(str.replace(/,/, '')
                        .replace(/\s+.bps/i, '')
                        .replace(/\.\d*/, ''));
    }

    // return true if the given debug flag was specified in the query params
    function debugOn(tag) {
        return debugFlags[tag];
    }

    // output debug message to console, if debug tag set...
    // e.g. fs.debug('mytag', arg1, arg2, ...)
    function debug(tag) {
        var args;
        if (debugOn(tag)) {
            args = Array.prototype.slice.call(arguments, 1);
            args.unshift('['+tag+']');
            $log.debug.apply(this, args);
        }
    }

    // trie operation
    function _trieOp(op, trie, word, data) {
        var p = trie,
            w = word.toUpperCase(),
            s = w.split(''),
            c = { p: p, s: s },
            t = [],
            x = 0,
            f1 = op === '+' ? add : probe,
            f2 = op === '+' ? insert : remove;

        function add(c) {
            var q = c.s.shift(),
                np = c.p[q];

            if (!np) {
                c.p[q] = {};
                np = c.p[q];
                x = 1;
            }
            return { p: np, s: c.s }
        }

        function probe(c) {
            var q = c.s.shift(),
                k = Object.keys(c.p).length,
                np = c.p[q];

            t.push({ q:q, k:k, p:c.p });
            if (!np) {
                t = [];
                return { s: [] };
            }
            return { p: np, s: c.s }
        }

        function insert() {
            c.p._data = data;
            return x ? 'added' : 'updated';
        }

        function remove() {
            if (t.length) {
                t = t.reverse();
                while (t.length) {
                    c = t.shift();
                    delete c.p[c.q];
                    if (c.k > 1) {
                        t = [];
                    }
                }
                return 'removed';
            }
            return 'absent';
        }

        while (c.s.length) {
            c = f1(c);
        }
        return f2();
    }

    // add word to trie (word will be converted to uppercase)
    // data associated with the word
    // returns 'added' or 'updated'
    function addToTrie(trie, word, data) {
        return _trieOp('+', trie, word, data);
    }

    // remove word from trie (word will be converted to uppercase)
    // returns 'removed' or 'absent'
    function removeFromTrie(trie, word) {
        return _trieOp('-', trie, word);
    }

    // lookup word (converted to uppercase) in trie
    // returns:
    //    undefined if the word is not in the trie
    //    -1 for a partial match (word is a prefix to an existing word)
    //    data for the word for an exact match
    function trieLookup(trie, word) {
        var s = word.toUpperCase().split(''),
            p = trie,
            n;

        while (s.length) {
            n = s.shift();
            p = p[n];
            if (!p) {
                return undefined;
            }
        }
        if (p._data) {
            return p._data;
        }
        return -1;
    }


    var hasOwn = {}.hasOwnProperty;

    function classNames () {
        var classes = [];

        for (var i = 0; i < arguments.length; i++) {
            var arg = arguments[i];
            if (!arg) continue;

            var argType = typeof arg;

            if (argType === 'string' || argType === 'number') {
                classes.push(arg);
            } else if (Array.isArray(arg)) {
                classes.push(classNames.apply(null, arg));
            } else if (argType === 'object') {
                for (var key in arg) {
                    if (hasOwn.call(arg, key) && arg[key]) {
                        classes.push(key);
                    }
                }
            }
        }

        return classes.join(' ');
    }


    angular.module('onosUtil')
        .factory('FnService',
        ['$window', '$location', '$log', function (_$window_, $loc, _$log_) {
            $window = _$window_;
            $log = _$log_;

            _parseDebugFlags($loc.search().debug);

            return {
                isF: isF,
                isA: isA,
                isS: isS,
                isO: isO,
                contains: contains,
                areFunctions: areFunctions,
                areFunctionsNonStrict: areFunctionsNonStrict,
                windowSize: windowSize,
                isMobile: isMobile,
                isChrome: isChrome,
                isSafari: isSafari,
                isFirefox: isFirefox,
                debugOn: debugOn,
                debug: debug,
                find: find,
                inArray: inArray,
                removeFromArray: removeFromArray,
                isEmptyObject: isEmptyObject,
                sameObjProps: sameObjProps,
                containsObj: containsObj,
                cap: cap,
                eecode: eecode,
                noPx: noPx,
                noPxStyle: noPxStyle,
                endsWith: endsWith,
                parseBitRate: parseBitRate,
                addToTrie: addToTrie,
                removeFromTrie: removeFromTrie,
                trieLookup: trieLookup,
                classNames: classNames
            };
    }]);

}());
