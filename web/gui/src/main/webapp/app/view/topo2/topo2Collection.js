/*
 * Copyright 2016-present Open Networking Foundation
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

        var opts = options || {};

        this.models = [];
        this._reset();

        this.initialize.apply(this, arguments);

        if (opts.comparator) {
            this.comparator = opts.comparator;
        }

        if (models) {
            this.add(models);
        }
    }

    Collection.prototype = {
        model: Model,
        initialize: function () {},
        addModel: function (data) {
            if (Object.getPrototypeOf(data) !== Object.prototype) {
                this.models.push(data);
                data.collection = this;
                this._byId[data.get('id')] = data;
                return data;
            }

            if (this._byId[data.id]) {
                return this._byId[data.id];
            }

            var CollectionModel = this.model;
            var model = new CollectionModel(data, this);
            model.collection = this;

            this.models.push(model);
            this._byId[data.id] = model;

            return model;
        },
        add: function (data) {
            var _this = this;

            if (angular.isArray(data)) {
                data.forEach(function (d) {
                    _this.addModel(d);
                });
            } else {
                return this.addModel(data);
            }
        },
        remove: function (model) {
            var index = _.indexOf(this.models, model);
            this.models.splice(index, 1);
        },
        get: function (id) {

            if (!id) {
                return null;
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
        filter: function (comparator) {
            return _.filter(this.models, comparator);
        },
        empty: function () {
            _.map(this.models, function (m) {
                m.remove();
            });
            this._reset();
        },
        _reset: function () {
            this._byId = [];
            this.models = [];
        },
        toJSON: function (options) {
            return this.models.map(function (model) {
                return model.toJSON(options);
            });
        },
    };

    angular.module('ovTopo2')
        .factory('Topo2Collection',
        ['Topo2Model', 'FnService',
            function (_Model_, fn) {
                Collection.extend = fn.extend;
                Model = _Model_;
                return Collection;
            },
        ]);

})();
