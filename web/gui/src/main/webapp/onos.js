/*
 * Copyright 2014-present Open Networking Laboratory
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
 ONOS GUI -- Main Application Module
 */
(function () {
    'use strict';

    // injected refs
    var $log;

    // define core module dependencies here...
    var coreDependencies = [
        'ngRoute',
        'onosMast',
        'onosNav',
        'onosUtil',
        'onosSvg',
        'onosRemote',
        'onosLayer',
        'onosWidget'
    ];

    // view IDs.. injected via the servlet
    var viewIds = [
        // {INJECTED-VIEW-IDS-START}
        // {INJECTED-VIEW-IDS-END}
        // dummy entry
        ''
    ];

    // secret sauce
    var sauce = [
        '6:69857666',
        '9:826970',
        '22:8069828667',
        '6:698570688669887967',
        '7:6971806889847186',
        '22:8369867682',
        '13:736583',
        '7:667186698384',
        '1:857780888778876787',
        '20:70717066',
        '24:886774868469',
        '17:7487696973687580739078'
        // Add more sauce...
    ];

    var defaultView = 'topo',
        viewDependencies = [];

    viewIds.forEach(function (id) {
        if (id) {
            viewDependencies.push('ov' + cap(id));
        }
    });

    var moduleDependencies = coreDependencies.concat(viewDependencies);

    function saucy(ee, ks) {
        var map = ee.genMap(sauce);
        Object.keys(map).forEach(function (k) {
            ks.addSeq(k, map[k]);
        });
    }

    function cap(s) {
        return s ? s[0].toUpperCase() + s.slice(1) : s;
    }

    angular.module('onosApp', moduleDependencies)

        .controller('OnosCtrl', [
            '$log', '$scope', '$route', '$routeParams', '$location',
            'KeyService', 'ThemeService', 'GlyphService', 'VeilService',
            'PanelService', 'FlashService', 'QuickHelpService', 'EeService',
            'WebSocketService',

            function (_$log_, $scope, $route, $routeParams, $location,
                      ks, ts, gs, vs, ps, flash, qhs, ee, wss) {
                var self = this;
                $log = _$log_;

                self.$route = $route;
                self.$routeParams = $routeParams;
                self.$location = $location;
                self.version = '1.5.0';

                // shared object inherited by all views:
                $scope.onos = {};

                // initialize services...
                ts.init();
                ks.installOn(d3.select('body'));
                ks.bindQhs(qhs);
                gs.init();
                vs.init();
                ps.init();
                saucy(ee, ks);
                flash.initFlash();
                qhs.initQuickHelp();

                wss.createWebSocket({
                    wsport: $location.search().wsport
                });

                $log.log('OnosCtrl has been created');

                $log.debug('route: ', self.$route);
                $log.debug('routeParams: ', self.$routeParams);
                $log.debug('location: ', self.$location);
            }])

        .config(['$routeProvider', function ($routeProvider) {
            // If view ID not provided, route to the default view
            $routeProvider
                .otherwise({
                    redirectTo: '/' + defaultView
                });

            function viewCtrlName(vid) {
                return 'Ov' + cap(vid) + 'Ctrl';
            }

            function viewTemplateUrl(vid) {
                return 'app/view/' + vid + '/' + vid + '.html';
            }

            // Add routes for each defined view.
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

        .directive('detectBrowser', ['$log', 'FnService',
            function ($log, fs) {
                return function (scope) {
                    var body = d3.select('body'),
                        browser = '';
                    if (fs.isChrome()) {
                        browser = 'chrome';
                    } else if (fs.isSafari()) {
                        browser = 'safari';
                    } else if (fs.isFirefox()) {
                        browser = 'firefox';
                    }
                    body.classed(browser, true);
                    scope.onos.browser = browser;

                    if (fs.isMobile()) {
                        body.classed('mobile', true);
                        scope.onos.mobile = true;
                    }

                    $log.debug('Detected browser is', fs.cap(browser));
                };
        }]);
}());
