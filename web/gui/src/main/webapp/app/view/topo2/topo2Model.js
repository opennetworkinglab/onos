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
        this.set(attrs, { silent: true });
        this.initialize.apply(this, arguments);
    }

    Model.prototype = {

        initialize: function () {},

        onChange: function (property, value, options) {},

        get: function (attr) {
            return this.attributes[attr];
        },

        set: function(key, val, options) {

            if (!key) {
                return this;
            }

            var attributes;
            if (typeof key === 'object') {
                attributes = key;
                options = val;
            } else {
                (attributes = {})[key] = val;
            }

            options || (options = {});

            var unset = options.unset,
                silent = options.silent,
                changes = [],
                changing   = this._changing;

            this._changing = true;

            if (!changing) {

                // NOTE: angular.copy causes issues in chrome
                this._previousAttributes = Object.create(Object.getPrototypeOf(this.attributes));
                this.changed = {};
            }

            var current = this.attributes,
                changed = this.changed,
                previous = this._previousAttributes;

            angular.forEach(attributes, function (attribute, index) {

                val = attribute;

                if (!angular.equals(current[index], val)) {
                    changes.push(index);
                }

                if (!angular.equals(previous[index], val)) {
                    changed[index] = val;
                } else {
                    delete changed[index];
                }

                unset ? delete current[index] : current[index] = val;
            });

            // Trigger all relevant attribute changes.
            if (!silent) {
                if (changes.length) {
                    this._pending = options;
                }
                for (var i = 0; i < changes.length; i++) {
                    this.onChange(changes[i], this, current[changes[i]], options);
                }
            }

            this._changing = false;
            return this;
        },
        toJSON: function(options) {
            return angular.copy(this.attributes)
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
