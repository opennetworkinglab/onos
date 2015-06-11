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

    var urlSuffix = '/rs/dashboard';

    function randomDate(start, end) {
        return new Date(
            start.getTime() + Math.random() * (end.getTime() - start.getTime())
        );
    }

    angular.module('cordHome', [])
        .controller('CordHomeCtrl', ['$log', '$scope', '$resource', '$filter',
            function ($log, $scope, $resource, $filter) {
                var DashboardData, resource;
                $scope.page.curr = 'dashboard';

                DashboardData = $resource($scope.shared.url + urlSuffix);
                resource = DashboardData.get({},
                    // success
                    function () {
                        $scope.bundle_name = resource.bundle_name;
                        $scope.bundle_desc = resource.bundle_desc;
                        $scope.users = resource.users;

                        if ($.isEmptyObject($scope.shared.userActivity)) {
                            $scope.users.forEach(function (user) {
                                var date = randomDate(new Date(2015, 0, 1),
                                    new Date());

                                $scope.shared.userActivity[user.id] =
                                    $filter('date')(date, 'mediumTime');
                            });
                        }
                    },
                    // error
                    function () {
                        $log.error('Problem with resource', resource);
                    });
                $log.debug('Resource received:', resource);

                $log.debug('Cord Home Ctrl has been created.');
        }]);
}());
