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

    var urlSuffix = '/rs/bundle';

    var basic = 'basic',
        family = 'family';

    angular.module('cordBundle', [])
        .controller('CordBundleCtrl', ['$log', '$scope', '$resource',
            function ($log, $scope, $resource) {
                var BundleData, resource,
                    getData;
                $scope.page.curr = 'bundle';
                $scope.show = false;

                getData = function (id) {
                    if (!id) { id = ''; }

                    BundleData = $resource($scope.shared.url + urlSuffix + '/' + id);
                    resource = BundleData.get({},
                        // success
                        function () {
                            var current, availId;
                            current = resource.bundle.id;
                            $scope.name = resource.bundle.name;
                            $scope.desc = resource.bundle.desc;
                            $scope.funcs = resource.bundle.functions;

                            availId = (current === basic) ? family : basic;
                            resource.bundles.forEach(function (bundle) {
                                if (bundle.id === availId) {
                                    $scope.available = bundle;
                                }
                            });
                        },
                        // error
                        function () {
                            $log.error('Problem with resource', resource);
                        });
                };

                getData();

                $scope.changeBundle = function (id) {
                    getData(id);
                };

                $scope.showBundles = function () {
                    $scope.show = !$scope.show;
                };

                $log.debug('Cord Bundle Ctrl has been created.');
            }])

        .directive('bundleAvailable', [function () {
            return {
                templateUrl: 'app/view/bundle/available.html'
            };
        }]);
}());
