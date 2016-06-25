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
package org.onosproject.isis.controller;

import java.util.Map;

/**
 * Representation of an ISIS LSP bin which is part of LSP aging process.
 */
public interface IsisLspBin {

    /**
     * Returns all the LSPs in the bin.
     *
     * @return all LSPs in the bin
     */
    Map<String, LspWrapper> listOfLsp();

    /**
     * Adds LSP to bin for aging.
     *
     * @param lspKey     key to add the LSP
     * @param lspWrapper LSP wrapper instance
     */
    void addIsisLsp(String lspKey, LspWrapper lspWrapper);

    /**
     * Removes LSP from bin.
     *
     * @param lspKey     LSP key
     * @param lspWrapper LSP wrapper instance
     */
    void removeIsisLsp(String lspKey, LspWrapper lspWrapper);
}