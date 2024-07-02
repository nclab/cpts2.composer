/*
 * Copyright 2020 Jonathan Chang, Chun-yien <ccy@musicapoetica.org>.
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
package tech.metacontext.ocnhfa.composer.cf.ec;

import java.util.Map;
import tech.metacontext.ocnhfa.antsomg.impl.StandardGraph;
import static tech.metacontext.ocnhfa.antsomg.impl.StandardParameters.initialization;
import tech.metacontext.ocnhfa.composer.cf.ec.function.Evaluator;
import tech.metacontext.ocnhfa.composer.cf.ec.function.Mutator;
import tech.metacontext.ocnhfa.composer.cf.ec.function.Recombinator;
import tech.metacontext.ocnhfa.composer.cf.model.MusicThread;
import static tech.metacontext.ocnhfa.composer.cf.model.Parameters.*;
import tech.metacontext.ocnhfa.composer.cf.model.enums.ComposerType;
import static tech.metacontext.ocnhfa.composer.cf.model.enums.ComposerType.*;
import tech.metacontext.ocnhfa.composer.cf.utils.io.musicxml.Clef;

/**
 * EC entry of cf.composer.
 *
 * <pre>
 * EC Version of Cantus Firmus Composer for Species Counterpoint
 *
 * TYPE=COMPOSE | COMPOSE_STATIC
 * SEED=random seed (long)
 * THREAD_NUMBER=number of threads (int)
 * TARGET_SIZE=selected target length (int)
 * FRACTION_MODE=Power | Coefficient | Power_Multiply
 * CLEF=Treble | Bass | Soprano | Tenor | Alto (auto select by range if not specified)
 * SAVE=Specify if save score (boolean, false by default)
 *
 * PROJECT=specified project folder as preset (String)
 * FOLDER=a prefix for sub-folders in the specified project folder as preset (String)
 * </pre>
 *
 * @author Jonathan Chang, Chun-yien <ccy@musicapoetica.org>
 */
public class ECMain {

  /*
     * TYPE=DEVELOP_STANDARD
     * MODE=Dorian 
     * CLEF=Alto 
     * SAVE=true 
     * COMPOSER_NUMBER=10 
     * SEED=1573885717 
     * FRACTION_MODE=Power_Multiply
   */
  static Map<String, String> params;
  static Evaluator<MusicThread> eval_function
          = t -> (double) t.getCf().getMelody().size();
  static Recombinator<MusicThread> crossover
          = pair -> pair.e1();
  static Mutator<MusicThread> mutation
          = t -> t;

  public static void main(String[] args) {

    var crossover_rate = 0.75;
    var mutation_rate = 0.5;

    params = argsToParam(args);

    if (params.isEmpty()) {
      System.out.printf("""
                    EC Version of Cantus Firmus Composer for Species Counterpoint
                    %s
                    TYPE=COMPOSE | COMPOSE_STATIC
                    SEED=(Long) random seed | RANDOM | DEFAULT_SEED*
                    THREAD_NUMBER=(Integer) number of threads 
                    TARGET_SIZE=(Integer) selected target size 
                    FRACTION_MODE=Power | Coefficient | Power_Multiply
                    CLEF=Treble | Bass | Soprano | Tenor | Alto (auto select by range if not specified)
                    SAVE=(Boolean) Specify if save score, FALSE by default)
                    %s
                    When TASK=COMPOSE | COMPOSE_STATIC
                    PROJECT=(String) specified project folder as preset 
                    FOLDER=(String) prefix for sub-folders in the specified project folder as preset
                    """, LINE, LINE);
      System.exit(0);
    }
    System.out.println(LINE);

    var type = getParam(params, "TYPE", COMPOSE_STATIC, ComposerType::valueOfIgnoreCase);

    if (type != COMPOSE && type != COMPOSE_STATIC) {
      System.out.println("Only PRESET and PRESET_STATIC types allowed in EC Version.");
      System.exit(-1);
    }
    try {
      initialization(getParam(params, "SEED", DEFAULT_SEED, value -> switch (value) {
        case "RANDOM" ->
          System.currentTimeMillis() / 1000;
        default ->
          Long.valueOf(value);
      }));
    } catch (Exception e) {
      if (!getParam(params, "TEST", false, value -> true)) {
        throw e;
      }
    }
    var thread_number = getParam(params, "THREAD_NUMBER",
            DEFAULT_COMPOSE_THREAD_NUMBER, Integer::valueOf);

    var target_size = getParam(params, "TARGET_SIZE",
            DEFAULT_TARGET_SIZE, Integer::valueOf);

    var fraction_mode = getParam(params, "FRACTION_MODE",
            DEFAULT_FRACTION_MODE, StandardGraph.FractionMode::valueOf);

    var clef = getParam(params, "CLEF", null, Clef::valueOf);

    var save = getParam(params, "SAVE", false, Boolean::valueOf);

    var generation = getParam(params, "GENERATION",
            DEFAULT_GENERATION, Integer::valueOf);

    var threshold = getParam(params, "THRESHOLD",
            null, Double::valueOf);

    var ec_studio = new ECStudio(type, threshold, generation,
            eval_function, crossover, mutation,
            crossover_rate, mutation_rate);

    var project = getParam(params, "PROJECT", DEMO_STANDARD, String::valueOf);
    var folder = getParam(params, "FOLDER", null, String::valueOf);
    var style = getParam(params, "STYLE", null, Integer::valueOf);

    ec_studio.setThread_number(thread_number)
            .setTarget_size(target_size)
            .setFraction_mode(fraction_mode)
            .setModel(project, folder, style);

    ec_studio.run();

    if (save) {
      ec_studio.saveScore(clef);
    }
  }
}
