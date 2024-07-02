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
package art.cctcc.music.composer.cpts2.graphs.x;

import java.util.function.Predicate;

import tech.metacontext.ocnhfa.antsomg.impl.StandardMove;
import tech.metacontext.ocnhfa.composer.cf.model.x.MusicNode;
import tech.metacontext.ocnhfa.composer.cf.model.x.MusicPath;
import art.cctcc.music.cpt.graphs.x.CptMusicMove;
import art.cctcc.music.cpt.graphs.y_cpt.CptPitchPath;
import art.cctcc.music.cpt.model.enums.CptPitch;

import art.cctcc.music.composer.cpts2.framework.Cpt2Thread;

/**
 *
 * @author Jonathan Chang, Chun-yien <ccy@musicapoetica.org>
 */
public class Cpt2MusicMove extends CptMusicMove {

  public Cpt2MusicMove(StandardMove<MusicPath> move0, StandardMove<MusicPath> move1) {

    super(move0, move1);
  }

  public Cpt2MusicMove(MusicNode move0_To, MusicNode move1_To) {

    super(move0_To, move1_To);
  }

  public Predicate<CptPitchPath> getPredicate(Cpt2Thread thread) {

    return path -> thread.getRoute().isEmpty() ? switch (this.getMusicThought()) {
      case Directional_Conjunct ->
        Math.abs(CptPitch.diatonicDiff(path)) == 1;
      case Directional_Disjunct ->
        Math.abs(CptPitch.diatonicDiff(path)) > 1;
      default ->
        true;
    } : switch (this.getMusicThought()) {
      case Directional_Conjunct ->
        (thread.lastPitchDirection() > 0 && CptPitch.diatonicDiff(path) == 1)
        || (thread.lastPitchDirection() < 0 && CptPitch.diatonicDiff(path) == -1)
        || (thread.lastPitchDirection() == 0 && Math.abs(CptPitch.diatonicDiff(path)) == 1);
      case Directional_Disjunct ->
        (thread.lastPitchDirection() > 0 && CptPitch.diatonicDiff(path) > 1)
        || (thread.lastPitchDirection() < 0 && CptPitch.diatonicDiff(path) < -1)
        || (thread.lastPitchDirection() == 0 && Math.abs(CptPitch.diatonicDiff(path)) > 1);
      case Complemental_LongTerm ->
        (thread.lastPitchLevel() > 0 && CptPitch.diff(path) < 0)
        || (thread.lastPitchLevel() < 0 && CptPitch.diff(path) > 0)
        || thread.lastPitchDirection() == 0;
      case Complemental_ShortTerm ->
        (thread.lastPitchDirection() > 0 && CptPitch.diff(path) < 0)
        || (thread.lastPitchDirection() < 0 && CptPitch.diff(path) > 0)
        || thread.lastPitchDirection() == 0;
      case NULL ->
        true;
    };
  }
}
