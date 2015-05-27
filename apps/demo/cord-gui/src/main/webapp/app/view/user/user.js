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

    var bundleUrl = 'http://localhost:8080/rs/bundle',
        userUrl = 'http://localhost:8080/rs/users',
        family = 'family',
        url_filter = 'url_filter';

    angular.module('cordUser', [])
        .controller('CordUserCtrl', ['$log', '$scope', '$resource',
            function ($log, $scope, $resource) {
                var BundleData, bundleResource, UserData, userResource;
                $scope.page = 'user';
                $scope.isFamily = false;
                $scope.newLevels = {};

                BundleData = $resource(bundleUrl);
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

                UserData = $resource(userUrl);
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

            $log.debug('Cord User Ctrl has been created.');
        }])
        .directive('editUser', [function () {
            return {
                link: function (scope, elem) {

                }
            };
        }]);

}());
