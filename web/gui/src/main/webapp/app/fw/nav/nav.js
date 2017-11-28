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
 ONOS GUI -- Navigation Module
 */
(function () {
    'use strict';

    // injected dependencies
    var $log, $location, $window, fs;

    // internal state
    var navShown = false;

    function updatePane() {
        var vis = navShown ? 'visible' : 'hidden';
        d3.select('#nav').style('visibility', vis);
    }

    function showNav() {
        navShown = true;
        updatePane();
    }
    function hideNav() {
        navShown = false;
        updatePane();
    }
    function toggleNav() {
        navShown = !navShown;
        updatePane();
    }
    function hideIfShown() {
        if (navShown) {
            hideNav();
            return true;
        }
        return false;
    }

    function navTo(path, params) {
        var url;
        if (!path) {
            $log.warn('Not a valid navigation path');
            return null;
        }
        $location.url('/' + path);

        if (fs.isO(params)) {
            $location.search(params);
        } else if (params !== undefined) {
            $log.warn('Query params not an object', params);
        }

        url = $location.absUrl();
        $log.log('Navigating to ', url);
        $window.location.href = url;
    }

    angular.module('onosNav', [])
        .controller('NavCtrl', ['$log',

            function (_$log_) {
                var self = this;
                $log = _$log_;

                self.hideNav = hideNav;
                $log.log('NavCtrl has been created');
            },
        ])
        .factory('NavService',
            ['$log', '$location', '$window', 'FnService',

            function (_$log_, _$location_, _$window_, _fs_) {
                $log = _$log_;
                $location = _$location_;
                $window = _$window_;
                fs = _fs_;

                return {
                    showNav: showNav,
                    hideNav: hideNav,
                    toggleNav: toggleNav,
                    hideIfShown: hideIfShown,
                    navTo: navTo,
                };
        }]);

}());
