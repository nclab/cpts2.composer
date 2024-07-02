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

import java.util.List;
import java.util.Objects;
import static java.util.function.Predicate.not;
import lombok.Getter;
import lombok.Setter;

import art.cctcc.music.cpt.graphs.y_cpt.CptPitchNode;
import art.cctcc.music.cpt.model.CptCantusFirmus;
import art.cctcc.music.cpt.model.CptCounterpoint;
import art.cctcc.music.cpt.model.enums.CptPitch;

import static art.cctcc.music.composer.cpts2.model.Cpt2MeasurePortion.*;

/**
 *
 * @author Jonathan Chang, Chun-yien <ccy@musicapoetica.org>
 */
public class Cpt2Counterpoint extends CptCounterpoint {

  @Getter @Setter private boolean treble;
  @Getter @Setter private boolean beginWithRest;
  @Getter @Setter private boolean wholeNoteCadence;

  public Cpt2Counterpoint(String id, CptCantusFirmus cf) {

    super(cf);
    this.setId(id);
  }

  public Cpt2Counterpoint(String id, List<CptPitchNode> melody,
          boolean treble, boolean beginWithRest, boolean wholeNoteCadence) {

    super(id, melody);
    this.treble = treble;
    this.beginWithRest = beginWithRest;
    this.wholeNoteCadence = wholeNoteCadence;
  }

  public int getIndex(Cpt2Locus locus) {

    var index = locus.bar() * 2;
    if (locus.portion().equals(THESIS))
      index++;
    else if (this.wholeNoteCadence && index == this.getMelody().size())
      index--;
    return index;
  }

  @Override
  public int pitchRange() {

    var summary = this.getMelody().stream()
            .map(CptPitchNode::getPitch)
            .filter(Objects::nonNull)
            .mapToInt(CptPitch::getChromatic_number)
            .summaryStatistics();
    return summary.getMax() - summary.getMin();
  }

  public int getDiffAtLocus(Cpt2Locus locus) {

    return getDiffAtLocus(getIndex(locus));
  }

  public int getDiatonicDiffAtLocus(Cpt2Locus locus) {

    return getDiatonicDiffAtLocus(getIndex(locus));
  }

  public CptPitchNode getNote(Cpt2Locus locus) {

    return getNote(getIndex(locus));
  }

  @Override
  public int length() {

    return (int) this.getMelody().stream()
            .filter(not(CptPitchNode.getEmptyNode()::equals))
            .count();
  }

  @Override
  public CptPitch getMiddle() {

    var summary = this.getMelody().stream()
            .map(CptPitchNode::getPitch)
            .filter(Objects::nonNull)
            .map(CptPitch::getNatural)
            .mapToInt(CptPitch.diatonicValues()::indexOf)
            .summaryStatistics();
    return CptPitch.diatonicValues().get((summary.getMin() + summary.getMax()) / 2);
  }
}
