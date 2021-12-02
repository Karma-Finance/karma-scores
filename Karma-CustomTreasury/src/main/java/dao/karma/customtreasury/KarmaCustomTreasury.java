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

package dao.karma.customtreasury;

import score.annotation.External;

public class KarmaCustomTreasury {

    // ================================================
    // Consts
    // ================================================
    // Contract class name
    public static final String NAME = "KarmaCustomTreasury";

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
    public KarmaCustomTreasury(
    ) {
        this.name = "KarmaDAO Custom Treasury";
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
