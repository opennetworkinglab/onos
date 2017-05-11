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

    function Model(attributes, collection) {
        this.attributes = {};
        this.set(angular.extend({}, attributes || {}), { silent: true });
        this.collection = collection;
        this.initialize.apply(this, arguments);
    }

    Model.prototype = {

        initialize: function () {},

        onChange: function (property, value, options) {},

        get: function (attr) {
            return this.attributes[attr];
        },

        set: function (key, val, options) {

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

            var opts = options || (options = {});

            var unset = opts.unset,
                silent = opts.silent,
                changes = [],
                changing = this._changing;

            this._changing = true;

            if (!changing) {

                // NOTE: angular.copy causes issues in chrome
                this._previousAttributes = Object.create(
                    Object.getPrototypeOf(this.attributes)
                );
                this.changed = {};
            }

            var current = this.attributes,
                changed = this.changed,
                previous = this._previousAttributes;

            angular.forEach(attributes, function (attribute, index) {

                val = attribute;

                if (!_.isEqual(current[index], val)) {
                    changes.push(index);
                }

                if (!_.isEqual(previous[index], val)) {
                    delete changed[index];
                } else {
                    changed[index] = val;
                }

                if (unset) {
                    delete current[index];
                } else {
                    current[index] = val;
                }
            });

            // Trigger all relevant attribute changes.
            if (!silent) {
                if (changes.length) {
                    this._pending = opts;
                }
                for (var i = 0; i < changes.length; i++) {
                    this.onChange(changes[i], this,
                        current[changes[i]], opts);
                }
            }

            this._changing = false;
            return this;
        },
        toJSON: function (options) {
            return angular.copy(this.attributes);
        },
        remove: function () {
            if (this.collection) {
                this.collection.remove(this);
            }
        }
    };

    angular.module('ovTopo2')
    .factory('Topo2Model', [
        'FnService',
        function (fn) {
            Model.extend = fn.extend;

            return Model;
        }
    ]);
})();
