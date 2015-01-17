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

/*
 ONOS GUI -- Our own Angular directives defined here.

 @author Simon Hunt
 */

(function () {
    'use strict';

    angular.module('onosApp')

        // Create a resize directive, that we can apply to elements
        // so that they can respond to window resize events.
        .directive('resize', ['$window', function ($window) {
            return function (scope, element, attrs) {
                var w = angular.element($window);
                scope.$watch(function () {
                    return {
                        h: window.innerHeight,
                        w: window.innerWidth
                    };
                }, function (newVal, oldVal) {
                    scope.windowHeight = newVal.h;
                    scope.windowWidth = newVal.w;

                    scope.resizeWithOffset = function (offH, offW) {
                        var oh = offH || 0,
                            ow = offW || 0;
                        scope.$eval(attrs.notifier);
                        return {
                            height: (newVal.h - oh) + 'px',
                            width: (newVal.w - ow) + 'px'
                        };
                    };
                }, true);

                w.bind('resize', function () {
                    scope.$apply();
                });
            };
        }])


        // create icon directive, so that we can inject icons into
        // HTML tables etc.
        .directive('icon', ['GlyphService', function (gs) {
            return {
                templateUrl: 'toBeDecided-iconContext.html',
                restrict: 'A',
                link: function (scope, element, attrs) {
                    // TODO: implement this
                    // needs to pull out the parameters for the icon
                    // from the attributes of the element, and use those
                    // as arguments to the IconService.addIcon(...) call.


                }
            };


        }]);
}());
