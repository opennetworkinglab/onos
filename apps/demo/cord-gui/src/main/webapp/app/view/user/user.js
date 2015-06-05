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

    var bundleUrlSuffix = '/rs/bundle',
        userUrlSuffix = '/rs/users',
        family = 'family',
        url_filter = 'url_filter';

    angular.module('cordUser', [])
        .controller('CordUserCtrl', ['$log', '$scope', '$resource', '$timeout',
            function ($log, $scope, $resource, $timeout) {
                var BundleData, bundleResource;
                $scope.page.curr = 'user';
                $scope.isFamily = false;
                $scope.newLevels = {};
                $scope.showCheck = false;

                // === Get data functions ---

                BundleData = $resource($scope.shared.url + bundleUrlSuffix);
                bundleResource = BundleData.get({},
                    // success
                    function () {
                        var result;
                        $scope.isFamily = (bundleResource.bundle.id === family);
                        if ($scope.isFamily) {
                            result = $.grep(
                                bundleResource.bundle.functions,
                                function (elem) {
                                    if (elem.id === url_filter) { return true; }
                                }
                            );
                            $scope.levels = result[0].params.levels;
                        }
                    },
                    // error
                    function () {
                        $log.error('Problem with resource', bundleResource);
                    }
                );

                function getUsers(url) {
                    var UserData, userResource;
                    UserData = $resource(url);
                    userResource = UserData.get({},
                        // success
                        function () {
                            $scope.users = userResource.users;
                        },
                        // error
                        function () {
                            $log.error('Problem with resource', userResource);
                        }
                    );
                }

                getUsers($scope.shared.url + userUrlSuffix);

                // === Form functions ---

                function levelUrl(id, level) {
                    return $scope.shared.url +
                        userUrlSuffix + '/' + id + '/apply/url_filter/level/' + level;
                }

                $scope.applyChanges = function (changeLevels) {
                    var requests = [];

                    if ($scope.users) {
                        $.each($scope.users, function (index, user) {
                            var id = user.id,
                                level = user.profile.url_filter.level;
                            if ($scope.newLevels[id] !== level) {
                                requests.push(levelUrl(id, $scope.newLevels[id]));
                            }
                        });

                        $.each(requests, function (index, req) {
                            getUsers(req);
                        });
                    }
                    changeLevels.$setPristine();
                    $scope.showCheck = true;
                    $timeout(function () {
                        $scope.showCheck = false;
                    }, 3000);
                };

                $scope.cancelChanges = function (changeLevels) {
                    if ($scope.users) {
                        $.each($scope.users, function (index, user) {
                            $scope.newLevels[user.id] = user.profile.url_filter.level;
                        });
                    }
                    changeLevels.$setPristine();
                    $scope.showCheck = false;
                };

            $log.debug('Cord User Ctrl has been created.');
        }]);

}());
