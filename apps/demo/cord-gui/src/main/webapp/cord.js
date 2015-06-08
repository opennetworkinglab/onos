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

    var modules = [
            'ngRoute',
            'ngResource',
            'ngAnimate',
            'cordMast',
            'cordFoot',
            'cordNav'
        ],
        viewIds = [
            'login',
            'home',
            'user',
            'bundle'
        ],
        viewDependencies = [],
        dependencies;

    function capitalize(word) {
        return word ? word[0].toUpperCase() + word.slice(1) : word;
    }

    viewIds.forEach(function (id) {
        if (id) {
            viewDependencies.push('cord' + capitalize(id));
        }
    });

    dependencies = modules.concat(viewDependencies);

    angular.module('cordGui', dependencies)
        .config(['$routeProvider', function ($routeProvider) {
            $routeProvider
                .otherwise({
                    redirectTo: '/login'
                });

            function viewCtrlName(vid) {
                return 'Cord' + capitalize(vid) + 'Ctrl';
            }

            function viewTemplateUrl(vid) {
                return 'app/view/' + vid + '/' + vid + '.html';
            }

            viewIds.forEach(function (vid) {
                if (vid) {
                    $routeProvider.when('/' + vid, {
                        controller: viewCtrlName(vid),
                        controllerAs: 'ctrl',
                        templateUrl: viewTemplateUrl(vid)
                    });
                }
            });
        }])
        .controller('CordCtrl', ['$scope', '$location',
            function ($scope, $location) {
                $scope.shared = {
                    url: 'http://' + $location.host() + ':' + $location.port(),
                    userActivity: {}
                };
                $scope.page = {};
            }]);
}());
