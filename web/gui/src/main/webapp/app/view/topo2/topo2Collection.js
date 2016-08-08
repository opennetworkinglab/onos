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
 ONOS GUI -- Topology Collection Module.
 A Data Store that contains model data from the server
 */

(function () {
    'use strict';

    var Model;

    function Collection(models, options) {

        options || (options = {});

        this.models = [];
        this._reset();

        if (options.comparator !== void 0) this.comparator = options.comparator;

        if (models) {
            this.add(models);
        }
    }

    Collection.prototype = {
        model: Model,
        add: function (data) {

            var _this = this;

            if (angular.isArray(data)) {

                data.forEach(function (d) {

                    var model = new _this.model(d);
                    model.collection = _this;

                    _this.models.push(model);
                    _this._byId[d.id] = model;
                });
            }
        },
        get: function (id) {
            if (!id) {
                return void 0;
            }
            return this._byId[id] || null;
        },
        sort: function () {

            var comparator = this.comparator;

            // Check if function
            comparator = comparator.bind(this);
            this.models.sort(comparator);

            return this;
        },
        _reset: function () {
            this._byId = [];
            this.models = [];
        },
        toJSON: function(options) {
            return this.models.map(function(model) { return model.toJSON(options); });
        },
    };

    Collection.extend = function (protoProps, staticProps) {

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
        .factory('Topo2Collection',
        ['Topo2Model',
            function (_Model_) {

                Model = _Model_;
                return Collection;
            }
        ]);

})();
