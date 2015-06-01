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
 ONOS GUI -- Masthead Module
 */
(function () {
    'use strict';

    // injected services
    var $log;

    // configuration
    var mastHeight = 36,
        padMobile = 16;

    angular.module('onosMast', ['onosNav'])
        .controller('MastCtrl', ['$log', 'NavService', function (_$log_, ns) {
            var self = this;

            $log = _$log_;

            // initialize mast controller here...
            self.radio = null;

            // delegate to NavService
            self.toggleNav = function () {
                ns.toggleNav();
            };

            $log.log('MastCtrl has been created');
        }])

        // also define a service to allow lookup of mast height.
        .factory('MastService', ['FnService', function (fs) {
            return {
                mastHeight: function () {
                    return fs.isMobile() ? mastHeight + padMobile : mastHeight;
                }
            }
        }]);

}());
