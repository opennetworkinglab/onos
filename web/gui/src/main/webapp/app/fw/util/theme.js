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
 ONOS GUI -- Util -- Theme Service
 */
(function () {
    'use strict';

    var $log, fs;

    var themes = ['light', 'dark'],
        themeStr = themes.join(' '),
        thidx,
        listeners = {},
        nextListenerId = 1;

    function init() {
        thidx = 0;
        updateBodyClass();
    }

    function getTheme() {
        return themes[thidx];
    }

    function setTheme(t) {
        var idx = themes.indexOf(t);
        if (idx > -1 && idx !== thidx) {
            thidx = idx;
            updateBodyClass();
            themeEvent('set');
        }
    }

    function toggleTheme() {
        var i = thidx + 1;
        thidx = (i===themes.length) ? 0 : i;
        updateBodyClass();
        themeEvent('toggle');
        return getTheme();
    }

    function updateBodyClass() {
        var body = d3.select('body');
        body.classed(themeStr, false);
        body.classed(getTheme(), true);
    }

    function themeEvent(w) {
        var t = getTheme(),
            m = 'Theme-Change-('+w+'): ' + t;
        $log.debug(m);
        angular.forEach(listeners, function(value) {
            value.cb(
                {
                    event: 'themeChange',
                    value: t
                }
            );
        });
    }

    function addListener(callback) {
        var id = nextListenerId++,
            cb = fs.isF(callback),
            o = { id: id, cb: cb };

        if (cb) {
            listeners[id] = o;
        } else {
            $log.error('ThemeService.addListener(): callback not a function');
            o.error = 'No callback defined';
        }
        return o;
    }

    function removeListener(lsnr) {
        var id = lsnr && lsnr.id,
            o = listeners[id];
        if (o) {
            delete listeners[id];
        }
    }

    angular.module('onosUtil')
        .factory('ThemeService', ['$log', 'FnService',
        function (_$log_, _fs_) {
            $log = _$log_;
            fs = _fs_;
            thidx = 0;

            return {
                init: init,
                theme: function (x) {
                    if (x === undefined) {
                        return getTheme();
                    } else {
                        setTheme(x);
                    }
                },
                toggleTheme: toggleTheme,
                addListener: addListener,
                removeListener: removeListener
            };
    }]);

}());
