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

/*
 ONOS GUI -- General Purpose Angular directives.
 */

(function () {
    'use strict';

    angular.module('onosApp')

        // Create a resize directive, that we can apply to elements
        // so that they can respond to window resize events.
        .directive('resize', ['$window', 'FnService', function ($window, fs) {
            return {
                scope: {
                    offsetHeight: '@',
                    offsetWidth: '@',
                    notifier: '&'
                },
                link: function (scope, element) {
                    var elem = d3.select(element[0]);
                    scope.$watchCollection(function () {
                        return {
                            h: $window.innerHeight,
                            w: $window.innerWidth
                        };
                    }, function () {
                        var offH = scope.offsetHeight || 0,
                            offW = scope.offsetWidth || 0,
                            wsz = fs.windowSize(offH, offW);

                        elem.style({
                            height: wsz.height + 'px',
                            width: wsz.width + 'px'
                        });

                        if (fs.isF(scope.notifier)) {
                            scope.notifier();
                        }
                    });

                    angular.element($window).bind('resize', function () {
                        scope.$apply();
                    });
                }
            };
        }])

        .directive('ngRepeatComplete', [function () {
            return function (scope) {
                if (scope.$last) {
                    scope.$emit('ngRepeatComplete');
                    scope.$broadcast('ngRepeatComplete');
                }
            };
        }]);
}());
