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
 ONOS GUI -- Navigation Module

 @author Simon Hunt
 @author Bri Prebilic Cole
 */
(function () {
    'use strict';

    // injected dependencies
    var $log;

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

    angular.module('onosNav', [])
        .controller('NavCtrl', [
            '$log', function (_$log_) {
                var self = this;
                $log = _$log_;

                self.hideNav = hideNav;
                $log.log('NavCtrl has been created');
            }
        ])
        .factory('NavService', ['$log', function (_$log_) {
            $log = _$log_;

            return {
                showNav: showNav,
                hideNav: hideNav,
                toggleNav: toggleNav
            };
        }]);

}());
