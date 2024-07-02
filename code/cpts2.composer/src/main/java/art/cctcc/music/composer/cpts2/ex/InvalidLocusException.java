/*
 * Copyright 2022 Jonathan Chang, Chun-yien <ccy@musicapoetica.org>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package art.cctcc.music.composer.cpts2.ex;

import art.cctcc.music.composer.cpts2.model.Cpt2Locus;

/**
 *
 * @author Jonathan Chang, Chun-yien <ccy@musicapoetica.org>
 */
public class InvalidLocusException extends RuntimeException {

  /**
   * Creates a new instance of <code>InvalidLocusException</code> without detail
   * message.
   */
  public InvalidLocusException() {
  }

  /**
   * Constructs an instance of <code>InvalidLocusException</code> with the
   * specified detail message.
   *
   * @param msg the detail message.
   */
  public InvalidLocusException(String msg) {

    super(msg);
  }

  public InvalidLocusException(Cpt2Locus locus) {

    super(locus.toString());
  }
}
