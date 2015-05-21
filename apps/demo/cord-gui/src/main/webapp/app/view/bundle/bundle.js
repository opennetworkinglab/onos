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

    var $log, $resource;

    var url = 'http://localhost:8080/rs/bundle';

    var basic = 'basic',
        family = 'family',
        current,
        avCb;

    angular.module('cordBundle', [])
        .controller('CordBundleCtrl', ['$log', '$scope', '$resource',
            function (_$log_, $scope, _$resource_) {
                var BundleData, resource;
                $scope.page = 'bundle';
                $log = _$log_;
                $resource = _$resource_;

                BundleData = $resource(url);
                resource = BundleData.get({},
                    // success
                    function () {
                        current = resource.bundle.id;
                        $scope.name = resource.bundle.name;
                        $scope.desc = resource.bundle.desc;
                        $scope.funcs = resource.bundle.functions;
                        avCb(resource);
                    },
                    // error
                    function () {
                        $log.error('Problem with resource', resource);
                    });

                $log.debug('Cord Bundle Ctrl has been created.');
        }])

        .controller('CordAvailable', ['$scope',
            function ($scope) {
                avCb = function (resource) {
                    $scope.id = (current === basic) ? family : basic;
                    $scope.bundles = resource.bundles;

                    $scope.bundles.forEach(function (bundle) {
                        if (bundle.id === $scope.id) {
                            $scope.available = bundle;
                        }
                    });
                };

                $log.debug('Cord Available Ctrl has been created.');
        }])

        .directive('bundleAvailable', function () {
            return {
                templateUrl: 'app/view/bundle/available.html'
            };
        });
}());
