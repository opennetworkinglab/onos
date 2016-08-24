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
 ONOS GUI -- Remote -- General Purpose Functions
 */
(function () {
    'use strict';

    var uiContext = '/onos/ui/',
        rsSuffix = uiContext + 'rs/',
        wsSuffix = uiContext + 'websock/';

    angular.module('onosRemote')
        .factory('UrlFnService', ['$location', function ($loc) {

            function matchSecure(protocol) {
                var p = $loc.protocol(),
                    secure = (p === 'https' || p === 'wss');
                return secure ? protocol + 's' : protocol;
            }

            function urlBase(protocol, port, host) {
                // A little bit of funky here. It is possible that ONOS sits
                // behind a proxy and has an app prefix, e.g.
                //      http://host:port/my/app/onos/ui...
                // This bit of regex grabs everything after the host:port and
                // before the uiContext (/onos/ui/) and uses that as an app
                // prefix by inserting it back into the WS URL.
                // If no prefix, then no insert.

                var match = $loc.absUrl().match('.*//[^/]+/(.+)' + uiContext),
                    appPrefix = match ? '/' + match[1] : '';

                return matchSecure(protocol) + '://' +
                    (host || $loc.host()) + ':' +
                    (port || $loc.port()) + appPrefix;
            }

            function httpPrefix(suffix) {
                return urlBase('http') + suffix;
            }

            function wsPrefix(suffix, wsport, host) {
                return urlBase('ws', wsport, host) + suffix;
            }

            function rsUrl(path) {
                return httpPrefix(rsSuffix) + path;
            }

            function wsUrl(path, wsport, host) {
                return wsPrefix(wsSuffix, wsport, host) + path;
            }

            return {
                rsUrl: rsUrl,
                wsUrl: wsUrl
            };
        }]);

}());
