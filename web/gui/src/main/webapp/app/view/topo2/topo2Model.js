/*
 * Copyright 2016-present Open Networking Laboratory
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
 ONOS GUI -- Topology Force Module.
 Visualization of the topology in an SVG layer, using a D3 Force Layout.
 */

(function () {
    'use strict';

    function Model(attributes) {

        var attrs = attributes || {};
        this.attributes = {};

        attrs = angular.extend({}, attrs);
        this.set(attrs);
    }

    Model.prototype = {

        get: function (attr) {
            return this.attributes[attr];
        },

        set: function(data) {
            angular.extend(this.attributes, data);
        },
    };


    Model.extend = function (protoProps, staticProps) {

        var parent = this;
        var child;

        child = function () {
            return parent.apply(this, arguments);
        };

        angular.extend(child, parent, staticProps);

        // Set the prototype chain to inherit from `parent`, without calling
        // `parent`'s constructor function and add the prototype properties.
        child.prototype = angular.extend({}, parent.prototype, protoProps);
        child.prototype.constructor = child;

        // Set a convenience property in case the parent's prototype is needed
        // later.
        child.__super__ = parent.prototype;

        return child;
    };

    angular.module('ovTopo2')
        .factory('Topo2Model',
        [
            function () {
                return Model;
            }
        ]);

})();
