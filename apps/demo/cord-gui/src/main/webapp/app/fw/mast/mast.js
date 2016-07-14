/*
 * Copyright 2015-present Open Networking Laboratory
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

(function () {
    'use strict';

    var urlSuffix = '/rs/logout';

    angular.module('cordMast', [])
        .controller('CordMastCtrl',
        ['$log','$scope', '$resource', '$location', '$window',
            function ($log, $scope, $resource, $location, $window) {
                var LogoutData, resource;

                $scope.logout = function () {
                    $log.debug('Logging out...');
                    LogoutData = $resource($scope.shared.url + urlSuffix);
                    resource = LogoutData.get({},
                        function () {
                            $location.path('/login');
                            $window.location.href = $location.absUrl();
                            $log.debug('Resource received:', resource);
                        });
                };
            }])

        .directive('mast', function () {
            return {
                restrict: 'E',
                templateUrl: 'app/fw/mast/mast.html'
            };
        });
}());
