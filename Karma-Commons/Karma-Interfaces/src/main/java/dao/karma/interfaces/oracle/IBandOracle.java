/*
 * Copyright 2021 Karma
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

package dao.karma.interfaces.oracle;

import java.util.Map;
import score.Address;
import score.Context;

public class IBandOracle {
  @SuppressWarnings("unchecked")
  public static Map<String, ?> get_reference_data (Address address, String base, String quote) {
    return (Map<String, ?>) Context.call(address, "get_reference_data", base, quote);
  }
}
