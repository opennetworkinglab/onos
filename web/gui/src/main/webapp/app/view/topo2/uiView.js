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
 ONOS GUI -- Base UIView class.
 A base class for UIViews to extend from
 */

(function () {
    'use strict';

    function View(options) {
        if (options && options.el) {
            this.el = options.el;
            this.$el = angular.element(this.el);
        }

        this.initialize.apply(this, arguments);
    }

    angular.module('ovTopo2')
    .factory('Topo2UIView', [
        'FnService',
        function (fn) {

            _.extend(View.prototype, {
                el: null,
                empty: function () {
                    if (this.$el) {
                        this.$el.empty();
                    }
                },
                destroy: function () {
                    // TODO: Unbind Events
                    this.empty();
                    return this;
                }
            });

            View.extend = fn.extend;
            return View;
        }
    ]);

})();
