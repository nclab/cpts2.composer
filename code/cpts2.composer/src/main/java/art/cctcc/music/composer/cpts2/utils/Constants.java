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
package art.cctcc.music.composer.cpts2.utils;

import java.nio.file.Path;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;

import art.cctcc.music.composer.cpts2.framework.Cpt2Composer;
import art.cctcc.music.composer.cpts2.graphs.y_cpt2_cf.Cpt2CfGraphMode;
import static art.cctcc.music.composer.cpts2.graphs.y_cpt2_cf.Cpt2CfGraphMode.CONVENTIONAL;

/**
 *
 * @author Jonathan Chang, Chun-yien <ccy@musicapoetica.org>
 */
public class Constants {

  public static final double ALPHA_X = 1.0;
  public static final double BETA_X = 1.0;
  public static final double ALPHA_Y = 2.0;
  public static final double BETA_Y = 1.0;
  public static final double PHEROMONE_DEPOSIT_UNIT = 0.0002;
  public static final double X_EXPLORE_CHANCE = 0.1;
  public static final double Y_EXPLORE_CHANCE = 0.2;
  public static final double EVAPORATE_RATE = 0.05;
  public static final int EVAPORATE_FREQUENCY = 5;
  public static final int CPT_COMPOSING_POPULATION = 100;

  public static final int CPT2_RANGE_RESTRICTION_CON = 16; // composite major 3rd
  public static final int CPT2_RANGE_RESTRICTION_CHR = 19; // composite perfect 5th
  public static final int CPT2_RANGE_RESTRICTION_DIS = 24; // composite perfect octave

  public static final Path DIR_PROJECT_OUTPUT
          = Path.of(System.getProperty("user.dir"), "projects");

  public static final String CF_DIR
          = "/GENERATE_CF_02-18-58-824635800";
  public static final String CF_COMPOSER_PREFIX
          = "Composer-EC_COMPOSE_STATIC_02-18-58-858638_";

  public static Path CF_PATH;

  static {
    try {
      CF_PATH = Path.of(Constants.class.getResource(CF_DIR).toURI());
    } catch (URISyntaxException ex) {
      Logger.getLogger(Constants.class.getName()).log(Level.SEVERE, null, ex);
    }
  }

  public static double DOUBLE_DELTA = 1e-15;

  public static Path getStandardOutputFolder(String id,
          Cpt2Composer composer) {

    var folder = DIR_PROJECT_OUTPUT
            .resolve(id
                    + "_" + composer.getCf().getId()
                    + (composer.getY_mode().equals(CONVENTIONAL) ? "" : "_" + composer.getY_mode().abbr)
                    + (composer.isBeginWithRest() ? "_BWR" : "")
                    + (composer.isWholeNoteCadence() ? "_WNC" : ""));
    folder.toFile().mkdirs();
    return folder;
  }

  public static int getCpt2RangeRestriction(Cpt2CfGraphMode y_mode) {

    return switch (y_mode) {
      case CONVENTIONAL ->
        CPT2_RANGE_RESTRICTION_CON;
      case CHROMATIC ->
        CPT2_RANGE_RESTRICTION_CHR;
      case DISSONANT ->
        CPT2_RANGE_RESTRICTION_DIS;
    };
  }
}
