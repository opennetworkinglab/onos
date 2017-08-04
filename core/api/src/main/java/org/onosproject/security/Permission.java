/*
 * Copyright 2015-present Open Networking Foundation
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

package org.onosproject.security;

import com.google.common.annotations.Beta;

@Beta
public class Permission {

    protected String classname;
    protected String name;
    protected String actions;

    public Permission(String classname, String name, String actions) {
        this.classname = classname;
        this.name = name;
        if (actions == null) {
            this.actions = "";
        } else {
            this.actions = actions;
        }
    }

    public Permission(String classname, String name) {
        this.classname = classname;
        this.name = name;
        this.actions = "";
    }

    public String getClassName() {
       return classname;
    }

    public String getName() {
        return name;
    }

    public String getActions() {
        return actions;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public boolean equals(Object thatPerm) {
        if (this == thatPerm) {
            return true;
        }

        if (!(thatPerm instanceof Permission)) {
            return false;
        }

        Permission that = (Permission) thatPerm;
        return (this.classname.equals(that.classname)) && (this.name.equals(that.name))
                && (this.actions.equals(that.actions));
    }

    @Override
    public String toString() {
        return String.format("(%s, %s, %s)", classname, name, actions);
    }
}
