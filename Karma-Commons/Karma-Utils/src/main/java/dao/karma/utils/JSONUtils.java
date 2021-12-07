/*
 * Copyright 2021 ICONation
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

package dao.karma.utils;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import score.Context;
import scorex.io.IOException;
import scorex.io.Reader;
import scorex.io.StringReader;

public class JSONUtils {
  public static byte[] method (String method) {
    return ("{\"method\": \"" + method + "\"}").getBytes();
  }
  
  public static byte[] method (String method, JsonObject params) {
    JsonObject data = Json.object()
        .add("method", method)
        .add("params", params);

    byte[] dataBytes = data.toString().getBytes();

    return dataBytes;
  }

  public static JsonObject parseData(byte[] _data) {

    Reader reader = new StringReader(new String(_data));
    JsonValue input = null;

    try {
        input = Json.parse(reader);
    } catch (IOException e) {
        Context.revert("JSONUtils::parseData: Invalid JSON");
    }

    return input.asObject();
  }
}
