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

    var coreDependencies = [
        'ngRoute',
        'onosUtil',
        'onosMast'
    ];

    var viewDependencies = [
        // TODO: inject view dependencies server side
        // NOTE: 'ov' == 'Onos View'...
        // {INJECTED-VIEW-MODULE-DEPENDENCIES}
        'ovSample',
        'ovTopo',
        // NOTE: dummy element allows all previous entries to end with comma
        '___dummy___'
    ];

    var dependencies = coreDependencies.concat(viewDependencies);
    dependencies.pop(); // remove dummy

    angular.module('onosApp', dependencies)

        .controller('OnosCtrl', [
            '$log', '$route', '$routeParams', '$location',
            'KeyService', 'ThemeService',

        function (_$log_, $route, $routeParams, $location, ks, ts) {
            var $log = _$log_,
                self = this;

            self.$route = $route;
            self.$routeParams = $routeParams;
            self.$location = $location;
            self.version = '1.1.0';

            // initialize onos (main app) controller here...
            ts.init();
            ks.installOn(d3.select('body'));

            $log.log('OnosCtrl has been created');

            $log.debug('route: ', self.$route);
            $log.debug('routeParams: ', self.$routeParams);
            $log.debug('location: ', self.$location);
        }])

        .config(['$routeProvider', function ($routeProvider) {
            // TODO: figure out a way of handling contributed views...
            $routeProvider
                .when('/', {
                    controller: 'OvSampleCtrl',
                    controllerAs: 'ctrl',
                    templateUrl: 'view/sample/sample.html'
                })
                .when('/topo', {
                    controller: 'OvTopoCtrl',
                    controllerAs: 'ctrl',
                    templateUrl: 'view/topo/topo.html'
                })
                .otherwise({
                    redirectTo: '/'
                })
        }]);

}());
