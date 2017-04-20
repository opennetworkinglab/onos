/*
 * Copyright 2017-present Open Networking Laboratory
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
 ONOS GUI -- Topology Region Navigation Service
 */

(function () {

    var $loc, wss;
    var instance;

    var RegionNavigationService = function () {
        this.listeners = {};
        instance = this;
    };

    RegionNavigationService.prototype = {

        addListener: function (event, sub) {
            if (!this.listeners[event]) {
                this.listeners[event] = [];
            }
            this.listeners[event].push(sub);
        },
        removeListener: function (sub) {
            // TODO: This will be needed when we re-implement the overlays
            // An overlay might want to add a listener when activated and will
            // need to remove the listener when deactivated.
        },
        notifyListeners: function (event, payload) {
            _.each(this.listeners[event], function (cb) {
                cb(payload);
            });
        },

        navigateToRegion: function (id) {
            $loc.search('regionId', id);
            wss.sendEvent('topo2navRegion', {
                rid: id
            });
            this.notifyListeners('region:navigation-start', id);
        },
        navigateToRegionComplete: function () {
            this.notifyListeners('region:navigation-complete');
        },

        destory: function () {
            this.listeners = {};
        }
    };

    angular.module('ovTopo2')
        .factory('Topo2RegionNavigationService', [
            '$location', 'WebSocketService',
            function (_$loc_, _wss_) {

                $loc = _$loc_;
                wss = _wss_;

                return instance || new RegionNavigationService();
            }
        ]);
})();