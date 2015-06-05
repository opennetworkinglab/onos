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
        .controller('CordLoginCtrl', ['$log', '$scope', '$resource',
            function ($log, $scope, $resource) {
                var LoginData, resource;
                $scope.page.curr = 'login';

                $scope.login = function () {
                    var email;
                    if (!$scope.email) {
                        email = 'mom@user.org';
                    } else {
                        email = $scope.email
                    }
                    LoginData = $resource($scope.shared.url + urlSuffix + '/' + email);
                    resource = LoginData.get();
                };

                $log.debug('Cord Login Ctrl has been created.');
        }]);
}());
