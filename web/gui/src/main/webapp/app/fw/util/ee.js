/*
 *  Copyright 2016-present Open Networking Foundation
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
    var $log, fs, flash;

    // function references
    var fcc = String.fromCharCode;

    // magic beans
    var beans = [
        'umpxwnwcw',
        'eufdvexoc',
        'egpdytgv',
        'xcjvte',
        'bgvest',
        'sevlr',
        'ias',
        'jweeidkpizn',
        'fmfv',
        'hwdoc',
        'acxefcsdgt',
        'pmmn',
        'ugaryc',
        'xcedrcxekzg',
        'qit'
        // Add more beans...
    ];

    function pickBean() {
        return beans[Math.floor(Math.random() * beans.length)] + '.foo';
    }

    function computeTransform(x) {
        var m = x.split(':'),
            h = Number(m[0]),
            d = m[1],
            n = d.length,
            w = [],
            i;

        for (i = 0; i<n; i+=2)
            { w.push(fcc(Number(d.slice(i, i+2)))); }

        return fs.eecode(h, w.join(''));
    }

    function genMap(data) {
        var map = {};

        data.forEach(function (x) {
            var r = computeTransform(x);
            map[r.e] = r.o.toLowerCase() + '.foo';
        });
        return map;
    }

    function cluck(foo) {
        var f = fs.isF(foo),
            s = fs.isS(foo);

        $log.debug('>>> CLUCK! <<<', foo);

        if (s === 'fgfb.foo') {
            s = pickBean();
            $log.debug('bean picked:', s);
        }

        if (s && fs.endsWith(s, '.foo')) {
            flash.tempDiv().append('img').attr('src', 'raw/'+s);
        }

        f && f();
    }

    angular.module('onosUtil')
    .factory('EeService', ['$log', 'FnService', 'FlashService',
        function (_$log_, _fs_, _flash_) {
            $log = _$log_;
            fs = _fs_;
            flash = _flash_;

            return {
                genMap: genMap,
                cluck: cluck,
            };
        }]);
}());
