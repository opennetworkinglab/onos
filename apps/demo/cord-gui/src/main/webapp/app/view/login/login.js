/*
 * Copyright 2015 Open Networking Laboratory
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
    var urlSuffix = '/rs/login';

    angular.module('cordLogin', [])
        .controller('CordLoginCtrl',
        ['$log', '$scope', '$resource', '$location', '$window',
            function ($log, $scope, $resource, $location, $window) {
                var LoginData, resource;
                $scope.page.curr = 'login';

                function getResource(email) {
                    LoginData = $resource($scope.shared.url + urlSuffix + '/' + email);
                    resource = LoginData.get({},
                        function () {
                            $location.url('/home');
                            $window.location.href = $location.absUrl();
                        });
                }

                $scope.login = function () {
                    if ($scope.email && $scope.password) {
                        getResource($scope.email);
                        $scope.shared.login = $scope.email;
                    }
                };

                $log.debug('Cord Login Ctrl has been created.');
        }]);
}());
