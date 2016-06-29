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
 ONOS GUI -- Util -- Theme Service
 */
(function () {
    'use strict';

    // injected refs
    var $log, fs, ps;

    // configuration
    var themes = ['light', 'dark'],
        themeStr = themes.join(' ');

    // internal state
    var listeners = [],
        currentTheme,
        thidx;


    function init() {
        thidx = ps.getPrefs('theme', { idx: 0 }).idx;
        updateBodyClass();
    }

    function getTheme() {
        return themes[thidx];
    }

    function setTheme(t, force) {
        var idx = themes.indexOf(t);
        if (force || idx > -1 && idx !== thidx) {
            thidx = idx;
            ps.setPrefs('theme', { idx: thidx });
            applyTheme();
        }
    }

    function toggleTheme() {
        var i = thidx + 1;
        thidx = (i===themes.length) ? 0 : i;
        ps.setPrefs('theme', { idx: thidx });
        applyTheme('toggle');
        return getTheme();
    }

    function applyTheme(evt) {
        thidx = ps.getPrefs('theme', { idx: thidx }).idx;
        if (currentTheme != thidx) {
            $log.info('Applying theme:', thidx);
            updateBodyClass();
            themeEvent(evt || 'set');
        }
    }

    function updateBodyClass() {
        var body = d3.select('body');
        body.classed(themeStr, false);
        body.classed(getTheme(), true);
        currentTheme = thidx;
    }

    function themeEvent(w) {
        var t = getTheme(),
            m = 'Theme-Change-(' + w + '): ' + t;
        $log.debug(m);

        listeners.forEach(function (lsnr) { 
            lsnr({event: 'themeChange', value: t}); 
        });
    }

    function addListener(lsnr) {
        listeners.push(lsnr);
    }

    function removeListener(lsnr) {
        listeners = listeners.filter(function(obj) { return obj === lsnr; });
    }

    angular.module('onosUtil')
        .factory('ThemeService', ['$log', 'FnService', 'PrefsService',
        function (_$log_, _fs_, _ps_) {
            $log = _$log_;
            fs = _fs_;
            ps = _ps_;

            ps.addListener(applyTheme);

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
