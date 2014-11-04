/*
 * Copyright 2014 Open Networking Laboratory
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
 Geometry library - based on work by Mike Bostock.
 */

(function() {

    if (typeof geo == 'undefined') {
        geo = {};
    }

    var tolerance = 1e-10;

    function eq(a, b) {
        return (Math.abs(a - b) < tolerance);
    }

    function gt(a, b) {
        return (a - b > -tolerance);
    }

    function lt(a, b) {
        return gt(b, a);
    }

    geo.eq = eq;
    geo.gt = gt;
    geo.lt = lt;

    geo.LineSegment = function(x1, y1, x2, y2) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;

        // Ax + By = C
        this.a = y2 - y1;
        this.b = x1 - x2;
        this.c = x1 * this.a + y1 * this.b;

        if (eq(this.a, 0) && eq(this.b, 0)) {
            throw new Error(
                'Cannot construct a LineSegment with two equal endpoints.');
        }
    };

    geo.LineSegment.prototype.intersect = function(that) {
        var d = (this.x1 - this.x2) * (that.y1 - that.y2) -
            (this.y1 - this.y2) * (that.x1 - that.x2);

        if (eq(d, 0)) {
            // The two lines are parallel or very close.
            return {
                x : NaN,
                y : NaN
            };
        }

        var t1  = this.x1 * this.y2 - this.y1 * this.x2,
            t2  = that.x1 * that.y2 - that.y1 * that.x2,
            x   = (t1 * (that.x1 - that.x2) - t2 * (this.x1 - this.x2)) / d,
            y   = (t1 * (that.y1 - that.y2) - t2 * (this.y1 - this.y2)) / d,
            in1 = (gt(x, Math.min(this.x1, this.x2)) && lt(x, Math.max(this.x1, this.x2)) &&
                gt(y, Math.min(this.y1, this.y2)) && lt(y, Math.max(this.y1, this.y2))),
            in2 = (gt(x, Math.min(that.x1, that.x2)) && lt(x, Math.max(that.x1, that.x2)) &&
                gt(y, Math.min(that.y1, that.y2)) && lt(y, Math.max(that.y1, that.y2)));

        return {
            x   : x,
            y   : y,
            in1 : in1,
            in2 : in2
        };
    };

    geo.LineSegment.prototype.x = function(y) {
        // x = (C - By) / a;
        if (this.a) {
            return (this.c - this.b * y) / this.a;
        } else {
            // a == 0 -> horizontal line
            return NaN;
        }
    };

    geo.LineSegment.prototype.y = function(x) {
        // y = (C - Ax) / b;
        if (this.b) {
            return (this.c - this.a * x) / this.b;
        } else {
            // b == 0 -> vertical line
            return NaN;
        }
    };

    geo.LineSegment.prototype.length = function() {
        return Math.sqrt(
                (this.y2 - this.y1) * (this.y2 - this.y1) +
                (this.x2 - this.x1) * (this.x2 - this.x1));
    };

    geo.LineSegment.prototype.offset = function(x, y) {
        return new geo.LineSegment(
                this.x1 + x, this.y1 + y,
                this.x2 + x, this.y2 + y);
    };

})();
