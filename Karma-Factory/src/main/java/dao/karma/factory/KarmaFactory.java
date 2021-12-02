/*
 * Copyright 2021 KarmaDAO
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

package dao.karma.factory;

import score.annotation.External;

public class KarmaFactory {

    // ================================================
    // Consts
    // ================================================
    // Contract class name
    public static final String NAME = "KarmaFactory";

    // Contract name
    private final String name;

    // ================================================
    // DB Variables
    // ================================================

    // ================================================
    // Event Logs
    // ================================================

    // ================================================
    // Methods
    // ================================================
    /**
     *  Contract constructor
     * 
     */
    public KarmaFactory(
    ) {
        this.name = "KarmaDAO Factory";
    }

    // ================================================
    // Checks
    // ================================================

    // ================================================
    // Public variable getters
    // ================================================
    /**
     * Get the contract name
     */
    @External(readonly = true)
    public String name() {
        return this.name;
    }
}
