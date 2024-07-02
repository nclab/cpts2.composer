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


import static tech.metacontext.ocnhfa.composer.cf.model.enums.MusicThought.*;
import art.cctcc.music.cpt.graphs.x.CptMusicSpace;

import static art.cctcc.music.composer.cpts2.utils.Constants.*;

/**
 *
 * @author Jonathan Chang, Chun-yien <ccy@musicapoetica.org>
 */
public class Cpt2MusicSpace extends CptMusicSpace {

  public Cpt2MusicSpace() {

    super(ALPHA_X, BETA_X);
  }

  @Override
  public void init_graph() {

    super.init_graph();
    // start_directional
    this.queryByVertex(START).stream()
            .filter(path -> path.getTo().equals(DIRECTIONAL))
            .forEach(path -> path.setCost(path.getCost() / 2.0));
    // complemental_shortterm
    this.queryByVertex(COMPLEMENTAL).stream()
            .filter(path -> path.getTo().equals(SHORTTERM))
            .forEach(path -> path.setCost(path.getCost() * 2.0));
  }
}
