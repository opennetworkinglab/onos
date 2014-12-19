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
 ONOS GUI -- Main Application Module

 @author Simon Hunt
 */

(function () {
    'use strict';

    angular.module('onosApp', ['onosUtil', 'onosMast'])
        .controller('OnosCtrl', ['$log', 'KeyService', 'ThemeService',
        function (_$log_, ks, ts) {
            var $log = _$log_,
                self = this;

            self.version = '1.1.0';

            // initialize onos (main app) controller here...
            ts.init();
            ks.installOn(d3.select('body'));

            $log.log('OnosCtrl has been created');
        }]);

}());
