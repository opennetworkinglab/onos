/*
 * Copyright (C) 2013 EBay Software Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Ashwin Raveendran, Madhu Venugopal
 */
package org.onosproject.ovsdb.lib.message;

public class MonitorSelect {

    boolean initial;
    boolean insert;
    boolean delete;
    boolean modify;

    public MonitorSelect(boolean initial, boolean insert, boolean delete, boolean modify) {
        this.initial = initial;
        this.insert = insert;
        this.delete = delete;
        this.modify = modify;
    }

    public MonitorSelect() {
    }

    public boolean isInitial() {
        return initial;
    }

    public void setInitial(boolean initial) {
        this.initial = initial;
    }

    public boolean isInsert() {
        return insert;
    }

    public void setInsert(boolean insert) {
        this.insert = insert;
    }

    public boolean isDelete() {
        return delete;
    }

    public void setDelete(boolean delete) {
        this.delete = delete;
    }

    public boolean isModify() {
        return modify;
    }

    public void setModify(boolean modify) {
        this.modify = modify;
    }
}
