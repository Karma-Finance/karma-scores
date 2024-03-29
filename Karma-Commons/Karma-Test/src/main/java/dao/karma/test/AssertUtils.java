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

package dao.karma.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.function.Executable;

public class AssertUtils {

  public static <T extends Throwable> void assertThrowsMessage (Class<T> expectedType, Executable executable, String message) {
    assertEquals(
      message,
      assertThrows(expectedType, executable).getMessage());
  }

  public static <T extends Throwable> void assertThrowsMessage (Executable executable, String message) {
    assertThrowsMessage(AssertionError.class, executable, message);
  }
}
