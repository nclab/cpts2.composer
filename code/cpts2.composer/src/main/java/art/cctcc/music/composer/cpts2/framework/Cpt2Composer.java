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
package art.cctcc.music.composer.cpts2.framework;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.Collections;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.io.IOException;
import java.nio.file.Path;
import lombok.Getter;
import lombok.Setter;

import tech.metacontext.ocnhfa.antsomg.model.AntsOMGSystem;
import tech.metacontext.ocnhfa.antsomg.model.Graph;
import tech.metacontext.ocnhfa.antsomg.impl.StandardGraph;
import tech.metacontext.ocnhfa.antsomg.impl.StandardGraph.FractionMode;
import art.cctcc.music.cpt.ex.ImmatureCptCfPitchSpaceException;
import art.cctcc.music.cpt.graphs.y_cpt.CptPitchPath;
import art.cctcc.music.cpt.model.CptCantusFirmus;
import art.cctcc.music.cpt.model.enums.CptTask;
import art.cctcc.music.cpt.model.enums.CptContrapuntalMotion;
import static art.cctcc.music.cpt.model.enums.CptTask.*;

import art.cctcc.music.composer.cpts2.graphs.x.Cpt2MusicSpace;
import art.cctcc.music.composer.cpts2.graphs.y_cpt2_cf.Cpt2CfPitchSpace;
import art.cctcc.music.composer.cpts2.graphs.y_cpt2_cf.Cpt2CfGraphMode;
import art.cctcc.music.composer.cpts2.model.Cpt2Locus;
import art.cctcc.music.composer.cpts2.utils.Tools;
import art.cctcc.music.composer.cpts2.utils.musicxml.Cpt2Score;
import static art.cctcc.music.composer.cpts2.model.Cpt2MeasurePortion.ARSIS;
import static art.cctcc.music.composer.cpts2.utils.Constants.*;

/**
 *
 *
 * @author Jonathan Chang, Chun-yien <ccy@musicapoetica.org>
 */
public class Cpt2Composer implements AntsOMGSystem<Cpt2Thread> {

  @Getter private final String id;
  @Getter private final CptCantusFirmus cf;
  @Getter private final boolean treble;

  private CptTask task;
  private int population;
  private double x_pheromone_deposit;
  private double y_pheromone_deposit;
  private double x_pheromone_evaporate_rate;
  private double y_pheromone_evaporate_rate;
  private final int rounds = 4;

  @Getter @Setter private Cpt2CfGraphMode y_mode;

  private Map<String, Graph> graphs;
  private List<Cpt2Thread> threads;

  @Getter private final boolean beginWithRest;
  @Getter private final boolean wholeNoteCadence;
  @Getter private double[][][] chancePerPathNode;

  @Setter private StandardGraph.FractionMode fraction_mode;

  public static Cpt2Composer getInstance(String id, CptCantusFirmus cf, boolean isTreble,
          boolean beginWithRest, boolean wholeNoteCadence) {

    return new Cpt2Composer(id, cf, isTreble, beginWithRest, wholeNoteCadence);
  }

  private Cpt2Composer(String id, CptCantusFirmus cf, boolean isTreble,
          boolean beginWithRest, boolean wholeNoteCadence) {

    System.out.printf("*** Creating Cpt2Composer [%s] ...\n", id);
    System.out.println("cf = " + cf);
    System.out.println("isTreble = " + isTreble);
    System.out.println("Is begin with rest = " + beginWithRest);
    System.out.println("Is whole note cadence = " + wholeNoteCadence);
    this.id = id;
    this.cf = cf;
    this.treble = isTreble;
    this.beginWithRest = beginWithRest;
    this.wholeNoteCadence = wholeNoteCadence;
  }

  public void developPrimary(Cpt2CfGraphMode Y_MODE) {

    this.y_mode = Y_MODE;
    this.init_graphs();
    this.setTask(DEVELOP_PRIMARY);
    this.navigate();
  }

  public void developSecondary(Cpt2CfGraphMode Y_MODE, Cpt2MusicSpace x) {

    this.y_mode = Y_MODE;
    this.init_graph_y();
    this.setX(x);
    this.setTask(DEVELOP_SECONDARY);
    this.navigate();
  }

  public void compose() {

    this.setTask(CptTask.COMPOSE);
    this.navigate();
  }

  public void save(Path folder) throws IOException {

    var subfolder = folder.resolve(treble ? "treble" : "bass");
    System.out.println("Output folder = " + folder);
    for (int i = 0; i < this.threads.size(); i++) {
      var t = this.threads.get(i);
      var filename = "S2Counterpoint_" + (i + 1);
      System.out.println("Writing score to " + filename);
      new Cpt2Score("No." + (i + 1) + " in " + t.getCpt().getCf().getMode(), id, t)
              .writeMusicXML(subfolder, filename + ".musicxml");
    }
  }

  @Override
  public void init_graphs() {

    init_graph_y();
    this.graphs.put("x", new Cpt2MusicSpace());
    this.getX().init_graph();
  }

  public void init_graph_y() {

    if (this.graphs == null)
      this.graphs = new HashMap<>();

    var y = new Cpt2CfPitchSpace(this.cf, this.treble,
            this.beginWithRest, this.wholeNoteCadence);
    y.modifyBaseGraph(y_mode);
    try {
      y.init_graph();
    } catch (ImmatureCptCfPitchSpaceException ex) {
      System.out.println(ex);
      System.out.println(ex.graph);
      System.exit(-1);
    }
    this.setY(y);
  }

  @Override
  public void init_population() {

    if (this.threads == null)
      this.threads = new ArrayList<>();
    var shortage = population - this.threads.size();
    this.threads.addAll(Stream.generate(this::create_individual)
            .limit(shortage)
            .toList());
  }

  public Cpt2Thread create_individual() {

    return new Cpt2Thread(
            "Cpt-" + this.cf.getId(),
            this.cf, this.getY().getStart(),
            this.treble, this.beginWithRest, this.wholeNoteCadence);
  }

  @Override
  public void navigate() {

    init_population();
    for (int i = 0; i < this.threads.size(); i++) {
      this.threads.get(i).develop(this.getX(), this.getY(),
              this.x_pheromone_deposit, this.y_pheromone_deposit);
      if (this.task == COMPOSE)
        continue;
      if (i + 1 % EVAPORATE_FREQUENCY == 0)
        this.evaporate();
      chancePerPathNode[i + 1] = Tools.getChancePerPathNode(this.getY());
    }
    if (this.task == COMPOSE) {
      var before = this.threads.size();
      var msg = "Thread size = " + before;
      var threads_distinct = new LinkedHashSet<>(this.threads);
      if (threads_distinct.size() < this.threads.size()) {
        this.threads.clear();
        this.threads.addAll(threads_distinct);
        before = threads_distinct.size();
        msg += ", after distinct = " + before;
      }
      this.threads.removeIf(this::hasStylisticDepartures);
      if (this.threads.size() < before)
        msg += ", after removing departures = " + this.threads.size();
      if (msg.contains(","))
        System.out.println(msg);
    }
    if (!this.isAimAchieved())
      this.navigate();
  }

  public boolean hasStylisticDepartures(Cpt2Thread thread) {

    var cpt = thread.getCpt();
    if (cpt.pitchRange() > getCpt2RangeRestriction(this.getY_mode()))
      return true;
    for (int bar = 1; bar < cpt.getCf().length(); bar++) {
      var cf_path = CptPitchPath.of(
              cpt.getCf().getNote(bar - 1),
              cpt.getCf().getNote(bar));
      var locus_arsis = new Cpt2Locus(bar, ARSIS);
      var cpt_path_arsis = CptPitchPath.of(
              cpt.getNote(locus_arsis.getPreviousBar()),
              cpt.getNote(locus_arsis));
      if ((!beginWithRest || bar > 1)
              && (wholeNoteCadence || bar < cpt.getCf().length() - 1)
              && CptContrapuntalMotion.isForbiddenParallel(cf_path, cpt_path_arsis))
        return true;

      if (wholeNoteCadence && bar == cpt.getCf().length() - 1)
        continue;
      var cpt_path_thesis = CptPitchPath.of(
              cpt.getNote(locus_arsis.getPrevious()),
              cpt.getNote(locus_arsis));
      if (CptContrapuntalMotion.isForbiddenHiddenParallel(cf_path, cpt_path_thesis))
        return true;
    }
    return false;
  }

  @Override
  public void evaporate() {

    if (this.task != COMPOSE) {
      this.getY().getEdges().forEach(p -> p.evaporate(y_pheromone_evaporate_rate));
      if (this.task != DEVELOP_SECONDARY)
        this.getX().getEdges().forEach(p -> p.evaporate(x_pheromone_evaporate_rate));
    }
  }

  @Override
  public boolean isAimAchieved() {

    return this.threads.size() >= this.population
            && this.threads.stream().allMatch(Cpt2Thread::isCompleted);
  }

  @Override
  public List<Cpt2Thread> getAnts() {

    return Collections.unmodifiableList(this.threads);
  }

  public void setTask(CptTask task) {

    this.threads = null;
    this.task = task;
    switch (task) {
      case DEVELOP_PRIMARY -> {
        population = (int) Math.pow(10, rounds - 1);
        x_pheromone_deposit = PHEROMONE_DEPOSIT_UNIT * this.getX().getEdges().size();
        y_pheromone_deposit = PHEROMONE_DEPOSIT_UNIT * this.getY().getEdges().size();
        x_pheromone_evaporate_rate = EVAPORATE_RATE;
        y_pheromone_evaporate_rate = EVAPORATE_RATE;
        if (List.of(FractionMode.Power, FractionMode.Power_Multiply).contains(this.fraction_mode)) {
          this.getX().getEdges().forEach(path -> path.addPheromoneDeposit(x_pheromone_deposit));
          this.getY().getEdges().forEach(path -> path.addPheromoneDeposit(y_pheromone_deposit));
        }
        chancePerPathNode = new double[population + 1][][];
        chancePerPathNode[0] = Tools.getChancePerPathNode(this.getY());
      }
      case DEVELOP_SECONDARY -> {
        population = (int) Math.pow(10, rounds - 1);
        x_pheromone_deposit = 0.0;
        y_pheromone_deposit = PHEROMONE_DEPOSIT_UNIT * this.getY().getEdges().size();
        x_pheromone_evaporate_rate = 0.0;
        y_pheromone_evaporate_rate = EVAPORATE_RATE;
        if (List.of(FractionMode.Power, FractionMode.Power_Multiply).contains(this.fraction_mode)) {
          this.getY().getEdges().forEach(path -> path.addPheromoneDeposit(y_pheromone_deposit));
        }
        chancePerPathNode = new double[population + 1][][];
        chancePerPathNode[0] = Tools.getChancePerPathNode(this.getY());
      }
      case COMPOSE -> {
        population = CPT_COMPOSING_POPULATION;
        x_pheromone_deposit = 0.0;
        y_pheromone_deposit = 0.0;
        x_pheromone_evaporate_rate = 0.0;
        y_pheromone_evaporate_rate = 0.0;
      }
    }
  }

  @Override
  public Map<String, Graph> getGraphs() {

    return Collections.unmodifiableMap(this.graphs);
  }

  public Cpt2MusicSpace getX() {

    return (Cpt2MusicSpace) this.graphs.get("x");
  }

  public void setX(Cpt2MusicSpace x) {

    this.graphs.put("x", x);
  }

  public Cpt2CfPitchSpace getY() {

    return (Cpt2CfPitchSpace) this.graphs.get("y");
  }

  public void setY(Cpt2CfPitchSpace y) {

    this.graphs.put("y", y);
  }

}
