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
package art.cctcc.music.composer.cpts2.model;

import art.cctcc.music.composer.cpts2.ex.InvalidLocusException;
import static art.cctcc.music.composer.cpts2.model.Cpt2MeasurePortion.*;

/**
 *
 * @author Jonathan Chang, Chun-yien <ccy@musicapoetica.org>
 */
public record Cpt2Locus(int bar, Cpt2MeasurePortion portion)
        implements Comparable<Cpt2Locus> {

  public Cpt2Locus getPrevious() {

    if (portion.equals(ARSIS) && bar > 0)
      return new Cpt2Locus(bar - 1, THESIS);
    if (portion.equals(THESIS))
      return new Cpt2Locus(bar, ARSIS);
    return null;
  }

  public Cpt2Locus getNext() {

    if (portion.equals(ARSIS) || portion.equals(REST) && bar == 0)
      return new Cpt2Locus(bar, THESIS);
    if (portion.equals(THESIS))
      return new Cpt2Locus(bar + 1, ARSIS);
    if (portion.equals(REST))
      throw new InvalidLocusException(this);
    return null;
  }

  public Cpt2Locus getNextBar() {

    return new Cpt2Locus(bar + 1, ARSIS);
  }

  public Cpt2Locus getPreviousBar() {

    if (bar == 0)
      throw new InvalidLocusException(this);
    return new Cpt2Locus(bar - 1, ARSIS);
  }

  @Override
  public String toString() {

    return bar + "_" + portion.name();
  }

  @Override
  public int compareTo(Cpt2Locus o) {

    return this.bar == o.bar
            ? this.portion.compareTo(o.portion)
            : Integer.compare(this.bar, o.bar);
  }
}
