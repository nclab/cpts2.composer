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
package art.cctcc.music.composer.cpts2.graphs.y_cpt2_cf;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.Getter;
import lombok.Setter;

import static tech.metacontext.ocnhfa.antsomg.impl.StandardParameters.getRandom;
import tech.metacontext.ocnhfa.antsomg.impl.StandardGraph;
import tech.metacontext.ocnhfa.composer.cf.utils.Pair;
import static art.cctcc.music.utils.CptCalculator.matrix_power;
import art.cctcc.music.cpt.ex.EmptyGraphException;
import art.cctcc.music.cpt.ex.ImmatureCptCfPitchSpaceException;
import art.cctcc.music.cpt.graphs.y_cpt.CptPitchMove;
import art.cctcc.music.cpt.graphs.y_cpt.CptPitchNode;
import art.cctcc.music.cpt.graphs.y_cpt.CptPitchPath;
import art.cctcc.music.cpt.graphs.y_cpt.CptPitchSpace;
import art.cctcc.music.cpt.graphs.y_cpt.CptPitchSpaceChromatic;
import art.cctcc.music.cpt.model.CptCadence;
import art.cctcc.music.cpt.model.CptCantusFirmus;
import art.cctcc.music.cpt.model.enums.CptContrapuntalMotion;
import art.cctcc.music.cpt.model.enums.CptEcclesiasticalMode;
import art.cctcc.music.cpt.model.enums.CptPitch;
import static art.cctcc.music.cpt.model.enums.IntervalQuality.*;

import static art.cctcc.music.composer.cpts2.graphs.y_cpt2_cf.Cpt2CfGraphMode.*;
import static art.cctcc.music.composer.cpts2.utils.Constants.*;
import static art.cctcc.music.composer.cpts2.utils.Cpt2Calculator.pitchSpaceToMatrix;
import static art.cctcc.music.composer.cpts2.model.Cpt2MeasurePortion.*;
import art.cctcc.music.composer.cpts2.model.Cpt2Locus;
import art.cctcc.music.composer.cpts2.ex.InvalidLocusException;

/**
 *
 * @author Jonathan Chang, Chun-yien <ccy@musicapoetica.org>
 */
public class Cpt2CfPitchSpace extends StandardGraph<CptPitchPath, CptPitchNode> {

  @Getter private CptCantusFirmus cf;
  @Getter private boolean treble;

  @Getter @Setter private boolean beginWithRest;
  @Getter @Setter private boolean wholeNoteCadence;
  @Getter @Setter private CptPitchSpace y_cpt;

  private Map<Cpt2Locus, Set<CptPitchPath>> loci;
  private List<CptCadence> cadences;
  private boolean dissonant;
  private Cpt2CfGraphMode mode;

  public Cpt2CfPitchSpace(
          CptCantusFirmus cf, boolean treble,
          boolean beginWithRest, boolean wholeNoteCadence) {

    this(ALPHA_Y, BETA_Y, cf, treble, beginWithRest, wholeNoteCadence);
  }

  public Cpt2CfPitchSpace(double alpha, double beta,
          CptCantusFirmus cf, boolean treble,
          boolean beginWithRest, boolean wholeNoteCadence) {

    super(alpha, beta);

    this.cf = cf;
    this.treble = treble;
    this.beginWithRest = beginWithRest;
    this.wholeNoteCadence = wholeNoteCadence;

    this.y_cpt = new CptPitchSpaceChromatic(); // getInstance() will return a shared instance
    this.y_cpt.init_graph(); // using constructor requires manually initialization.

    this.cadences = this.cf.getMode().getCadences().stream()
            .filter(c -> !isVoiceOverlappingAtLocus(treble, c.getFormula().getLast(), cf.length() - 1))
            .collect(Collectors.toList());
  }

  public void modifyBaseGraph(Cpt2CfGraphMode mode) {

    this.mode = mode;
    switch (mode) {
      case DISSONANT:
        this.dissonant = true;
      case CHROMATIC:
        y_cpt.getEdges().removeIf(path -> path.absDiff() == 0);
        y_cpt.getEdges().stream()
                .filter(path -> !path.getTo().getPitch().getAccidental().isEmpty())
                .forEach(path -> path.setCost(path.getCost() / 2.0));
        y_cpt.getEdges().stream()
                .filter(CptContrapuntalMotion::leap)
                .forEach(path -> path.setCost(path.getCost() * 2.0));
        break;
      case CONVENTIONAL:
        y_cpt.getEdges().removeIf(path -> path.absDiff() == 0);
        y_cpt.getEdges().removeIf(path -> !path.getFrom().getPitch().getAccidental().isEmpty()
                && !path.getFrom().toString().startsWith("Bf"));
        y_cpt.getEdges().removeIf(path -> !path.getTo().getPitch().getAccidental().isEmpty()
                && !path.getTo().toString().startsWith("Bf"));
        y_cpt.getEdges().stream()
                .filter(path -> path.getTo().toString().startsWith("Bf") || CptContrapuntalMotion.leap(path))
                .forEach(path -> path.setCost(path.getCost() * 2.0));
    }
  }

  @Override
  public final void init_graph() throws ImmatureCptCfPitchSpaceException {

    loci = new HashMap<>();
    var starts = cf.getMode().getTerminals(this.treble).stream()
            .filter(pitch -> !isVoiceOverlappingAtLocus(this.treble, pitch, 0))
            .filter(pitch -> !isTooFarApart(pitch, 0))
            .map(pitch -> CptPitchPath.of(null, pitch))
            .collect(Collectors.toSet());
    for (int i = 0; i < cf.length() - 3; i++) {
      var locus = new Cpt2Locus(i, i == 0 && beginWithRest ? REST : ARSIS);
      if (locus.portion().equals(REST)) {
        loci.put(locus.getNext(), starts);
        var set_arsis = starts.stream()
                .flatMap(path -> y_cpt.queryByVertex(path.getTo()).stream())
                .filter(path -> dissonant ^ isConsonanceAtLocus(path.getTo(), locus.bar() + 1))
                .filter(path -> !isForbiddenMotion(path, locus.bar() + 1))
                .filter(path -> !isVoiceOverlappingAtLocus(this.treble, path.getTo(), locus.bar()))
                .filter(path -> !isVoiceCrossing(this.treble, path, locus.getNextBar()))
                .filter(path -> !isTooFarApart(path.getTo(), 0))
                .map(CptPitchPath::new)
                .collect(Collectors.toSet());
        loci.put(locus.getNextBar(), set_arsis);
      } else {
        if (i == 0)
          loci.put(locus, starts);
        this.generateBarEdges(locus);
      }
    }
    //cadence
    var locus = new Cpt2Locus(cf.length() - 3, ARSIS);
    if (wholeNoteCadence) {
      final var set_arsis = new HashSet<CptPitchPath>();
      final var set_finalis = new HashSet<CptPitchPath>();
      var set_thesis = loci.get(locus).stream()
              .flatMap(path -> y_cpt.queryByVertex(path.getTo()).stream())
              .map(CptPitchPath::new)
              .collect(Collectors.toSet());
      final var locus_thesis = locus.getNext(); //thesis of cf.length()-3
      loci.put(locus_thesis, set_thesis);
      loci.get(locus_thesis).forEach(path -> {
        var node = path.getTo();
        this.cadences.stream()
                .map(ca -> new Pair<>(ca.getPathToCadence(node), ca.getPitchPath()))
                .filter(p -> p.e1() != null)
                .map(p -> Map.entry(p, CptPitchPath.of(path.getFrom(), p.e1().getFrom())))
                // e: key - cadence, value - previous path
                .filter(e -> e.getValue().melodicFeasible())
                .filter(e -> !hasStylisticDepartures(e.getValue(), locus_thesis))
                .filter(e -> !isForbiddenMotion(e.getKey().e1(), locus_thesis.getNext().bar())) // for dis
                .filter(e -> !isForbiddenMotion(e.getKey().e2(), locus_thesis.getNext().bar() + 1))
                .filter(e -> !isTooFarApart(e.getKey().e2().getTo(), locus_thesis.getNext().bar() + 1))
                .forEach(e -> {
                  set_arsis.add(e.getKey().e1());
                  set_finalis.add(e.getKey().e2());
                  if (cf.getMode().equals(CptEcclesiasticalMode.Aeolian)) {
                    path.setTo(e.getKey().e1().getFrom());
                  }
                });
      });
      loci.put(locus.getNextBar(), set_arsis);
      loci.put(locus.getNextBar().getNextBar(), set_finalis);
    } else { //!wholeNoteCadence
      this.generateBarEdges(locus);
      final var locus_arsis = locus.getNextBar(); //cf.length()-2      
      final var set_arsis = loci.get(locus.getNext()).stream()
              .flatMap(path -> y_cpt.queryByVertex(path.getTo()).stream())
              .filter(path -> !hasStylisticDepartures(path, locus_arsis))
              .map(CptPitchPath::new)
              .collect(Collectors.toSet());
      loci.put(locus_arsis, set_arsis);
      final var set_thesis = new HashSet<CptPitchPath>();
      final var set_finalis = new HashSet<CptPitchPath>();
      loci.get(locus_arsis).forEach(path -> {
        var node = path.getTo();
        this.cadences.stream()
                .map(ca -> new Pair<>(ca.getPathToCadence(node), ca.getPitchPath()))
                .filter(p -> p.e1() != null)
                .map(p -> Map.entry(p, CptPitchPath.of(path.getFrom(), p.e1().getFrom())))
                // e: key - cadence, value - previous path
                .filter(e -> e.getValue().melodicFeasible())
                .filter(e -> !hasStylisticDepartures(e.getValue(), locus_arsis))
                .filter(e -> !hasStylisticDepartures(e.getKey().e1(), locus_arsis.getNext()))
                .filter(e -> !isForbiddenMotion(e.getKey().e2(), locus_arsis.bar() + 1))
                .filter(e -> !isTooFarApart(e.getKey().e2().getTo(), locus_arsis.bar() + 1))
                .forEach(e -> {
                  set_thesis.add(e.getKey().e1());
                  set_finalis.add(e.getKey().e2());
                  if (cf.getMode().equals(CptEcclesiasticalMode.Aeolian)) {
                    path.setTo(e.getKey().e1().getFrom());
                  }
                });
      });
      loci.put(locus_arsis.getNext(), set_thesis);
      loci.put(locus_arsis.getNextBar(), set_finalis);
    }
    
    if (this.getBar() == cf.length()) {
      loci.keySet().stream()
              .sorted(Comparator.reverseOrder())
              .skip(2)
              .forEach(loc -> {
                try {
                  loci.get(loc).removeIf(path -> this.queryByVertex(loc.getNext(), path.getTo()).isEmpty());
                } catch (Exception ex) {
                  System.out.println("loc = " + loc);
                  System.out.println("loci.get(loc) = " + loci.get(loc));
                  System.exit(-1);
                }
                loci.getOrDefault(loc.getNext(), Set.of())
                        .removeIf(p -> this.queryByDestination(loc, p.getFrom()).isEmpty());
              });
      if (List.of(FractionMode.Power, FractionMode.Power_Multiply).contains(this.getFraction_mode()))
        this.getEdges().forEach(path -> path.addPheromoneDeposit(alpha));
    }
  }

  public BigDecimal cpt_count() {

    if (this.getBar() < this.cf.length()) {
      return BigDecimal.ZERO;
    }
    var matrix = matrix_power(pitchSpaceToMatrix(this), this.loci.size() - 1);
    return matrix.toCount();
  }

  @Override
  public void addEdges(CptPitchPath... paths) {

    var locus = loci.keySet().stream().sorted(Comparator.reverseOrder()).findFirst().orElse(null);
    if (locus != null)
      loci.put(locus.getNext(), Set.of(paths));
  }

  private void generateBarEdges(Cpt2Locus locus) {

    var set_thesis = loci.get(locus).stream()
            .flatMap(path -> y_cpt.queryByVertex(path.getTo()).stream())
            .filter(path -> !isVoiceCrossing(treble, path, locus.getNext()))
            .map(CptPitchPath::new)
            .collect(Collectors.toSet());
    if (this.mode != CONVENTIONAL)
      set_thesis.stream()
              .filter(path -> !isConsonanceAtLocus(path.getTo(), locus.bar()))
              .forEach(path -> path.setCost(path.getCost() / 2.0));
    loci.put(locus.getNext(), set_thesis);
    generateArsisEdges(locus.getNextBar());
  }

  private void generateArsisEdges(Cpt2Locus locus) {

    if (!locus.portion().equals(ARSIS))
      throw new InvalidLocusException(locus);
    var set_previous = loci.get(locus.getPrevious());
    var set_arsis = set_previous.stream()
            .flatMap(path -> y_cpt.queryByVertex(path.getTo()).stream())
            .filter(path -> !hasStylisticDepartures(path, locus))
            .map(CptPitchPath::new)
            .collect(Collectors.toSet());
    loci.put(locus, set_arsis);
    loci.replace(
            locus.getPrevious(),
            set_previous.stream()
                    .filter(path -> set_arsis.stream().map(CptPitchPath::getFrom).toList().contains(path.getTo()))
                    .collect(Collectors.toSet()));
  }

  public CptPitchMove getMove(Cpt2Locus locus, CptPitchNode current, double explore_chance) {

    var paths = this.queryByVertex(locus, current);
    var fractions = new ArrayList<Double>();
    var sum = paths.stream()
            .mapToDouble(this::getFraction)
            .peek(fractions::add)
            .sum();
    var r = new AtomicReference<Double>(getRandom().nextDouble() * sum);
    var isExploring = getRandom().nextDouble() < explore_chance;
    try {
      var selected = isExploring || paths.size() == 1
              ? paths.get(getRandom().nextInt(paths.size()))
              : IntStream.range(0, paths.size())
                      .filter(i -> r.getAndSet(r.get() - fractions.get(i)) < fractions.get(i))
                      .mapToObj(paths::get)
                      .findFirst().get();
      return new CptPitchMove(isExploring, paths, selected);
    } catch (Exception ex) {
      System.out.println("Exception: getMove() failed to select path.");
      System.out.println("locus = " + locus);
      System.out.println("current = " + current);
      System.out.println("paths = " + paths);
      System.out.println("ex = " + ex);
    }
    return null;
  }

  public CptPitchMove move(CptPitchMove move, double pheromone_deposit) {

    move.getSelected().addPheromoneDeposit(pheromone_deposit);
    return move;
  }

  /**
   * Find CptPitchPaths that come from the same pitch.
   *
   * @param locus locus, must greater than 0.
   * @param pitch specified origin CptPitchNode.
   * @return List of qualified CptPitchPath.
   */
  public List<CptPitchPath> queryByVertex(Cpt2Locus locus, CptPitchNode pitch) {

    return loci.get(locus).stream()
            .filter(path -> Objects.equals(path.getFrom(), pitch))
            .collect(Collectors.toList());
  }

  /**
   * Find CptPitchPaths that go to the specified pitch.
   *
   * @param locus locus.
   * @param pitch specified destination CptPitchNode.
   * @return List of qualified CptPitchPath.
   */
  public List<CptPitchPath> queryByDestination(Cpt2Locus locus, CptPitchNode pitch) {

    return loci.get(locus).stream()
            .filter(path -> path.getTo().equals(pitch))
            .collect(Collectors.toList());
  }

  public int getBar() {

    return (int) loci.entrySet().stream()
            .filter(e -> !e.getValue().isEmpty())
            .map(Entry::getKey)
            .map(Cpt2Locus::bar)
            .distinct()
            .count();
  }

  @Override
  public String asGraphviz() {

    var incompleted = this.getBar() < cf.length();

    var cluster = IntStream.range(0, this.getBar())
            .mapToObj(bar -> String.format("""
                    \tsubgraph cluster_locus_%d {
                    %s
                    \t\tlabel = \"%d [%s]\";
                    \t\tcolor=red
                    \t}""",
            bar,
            loci.keySet().stream().filter(l -> l.bar() == bar).sorted()
                    .map(locus -> String.format("""                                        
                    \t\tsubgraph cluster_%s {
                    \t\t\tnode [style=filled];
                    %s
                    \t\t\tlabel = \"%s\";
                    \t\t\tcolor=blue
                    \t\t}""",
                    locus.toString(),
                    loci.get(locus).stream()
                            .map(p -> String.format("\t\t\t%s_%s[label=\"%s\"];", p.getTo().getPitch().name(), locus.toString(), p.getTo().getPitch().name()))
                            .distinct()
                            .sorted()
                            .collect(Collectors.joining("\n")),
                    locus.portion())).collect(Collectors.joining("\n")),
            bar, cf.getNote(bar)))
            .collect(Collectors.joining("\n"));
    var body = loci.keySet().stream().filter(l -> !loci.get(l).isEmpty()).sorted().map(
            locus -> loci.get(locus).stream().filter(path -> path.getFrom() != null).sorted(Comparator.comparing(CptPitchPath::toString)).map(
                    path -> isBlank()
                            ? String.format("\t%s -> %s [ label=<c=%.1f>, penwidth=0.5 ];",
                                    path.getFrom().getName() + "_" + (this.wholeNoteCadence && locus.bar() > cf.length() - 2 ? locus.getPreviousBar() : locus.getPrevious()).toString(),
                                    path.getTo().getName() + "_" + locus.toString(),
                                    path.getCost())
                            : String.format("\t%s -> %s [ label=<c=%.1f, pher=%.2f>, penwidth=0.5 ];",
                                    path.getFrom().getName() + "_" + (this.wholeNoteCadence && locus.bar() > cf.length() - 2 ? locus.getPreviousBar() : locus.getPrevious()).toString(),
                                    path.getTo().getName() + "_" + locus.toString(),
                                    path.getCost(),
                                    path.getPheromoneTrail()))
                    .collect(Collectors.joining("\n")))
            .collect(Collectors.joining("\n"));
    return String.format("""
                    digraph %s {
                    \t%s
                    \trankdir=LR;
                    \tnode [shape=circle];
                    %s
                    %s
                    }""",
            this.getClass().getSimpleName(),
            String.format("// cantus firmus = %s", this.cf.getMelody().toString())
            + (incompleted ? String.format("\n\t// Incompleted Graph (cf.length = %d).", cf.length()) : ""),
            cluster, body);
  }

  private boolean hasStylisticDepartures(CptPitchPath path, Cpt2Locus locus) {

    var consonanceAtLocus = locus.portion().equals(ARSIS)
            ? (dissonant ^ isConsonanceAtLocus(path.getTo(), locus.bar()))
            : true;
    var tooFarApart = isTooFarApart(path.getTo(), locus.bar());
    var forbiddenMotion = locus.portion().equals(ARSIS) ? isForbiddenMotion(path, locus.bar()) : false;
    var voiceOverlappingAtLocus = isVoiceOverlappingAtLocus(this.treble, path.getTo(), locus.bar());
    var voiceCrossing = isVoiceCrossing(this.treble, path, locus);
    var unisonAtLocus = locus.portion().equals(ARSIS) || !CptContrapuntalMotion.leap(path) ? isUnisonAtLocus(path.getTo(), locus.bar()) : false;
    var conjuntAfterUnison = locus.portion().equals(ARSIS) && isUnisonAtLocus(path.getFrom(), locus.bar() - 1)
            ? !CptContrapuntalMotion.leap(path) : true;
    var invalidDissonanceAtThesis = isInvalidDissonanceAtThesis(path, locus);

    var isExposingCrossBarDissonantOctave = !dissonant && locus.portion().equals(ARSIS)
            ? isExposingCrossBarDissonantOctave(path, locus) : false;

    var result = !consonanceAtLocus || tooFarApart || forbiddenMotion
            || voiceOverlappingAtLocus || voiceCrossing
            || unisonAtLocus || !conjuntAfterUnison || invalidDissonanceAtThesis
            || isExposingCrossBarDissonantOctave;
    return result;
  }

  public boolean isExposingCrossBarDissonantOctave(CptPitchPath path, Cpt2Locus locus) {

    if (locus.getPrevious() == null || locus.portion().equals(THESIS))
      return false;
    var cf_path = CptPitchPath.of(cf.getNote(locus.bar() - 1), cf.getNote(locus.bar()));
    var path1 = CptPitchPath.of(path.getFrom(), cf_path.getTo());
    var path2 = CptPitchPath.of(cf_path.getFrom(), path.getTo());
    var degree1 = Math.abs(CptPitch.diatonicDiff(path1));
    var degree2 = Math.abs(CptPitch.diatonicDiff(path2));
    var quality1 = CptPitch.quality(path1);
    var quality2 = CptPitch.quality(path2);
    return degree1 % 7 == 0 && List.of(Augmented, Diminished).contains(quality1)
            || degree2 % 7 == 0 && List.of(Augmented, Diminished).contains(quality2);
  }

  public boolean isConsonanceAtLocus(CptPitchNode pitch, int locus) {

    var pitch2 = cf.getNote(locus);
    var degree = Math.abs(CptPitch.diatonicDiff(pitch, pitch2));
    var quality = CptPitch.quality(pitch, pitch2);
    return switch (degree % 7) {
      case 0, 4: yield quality.equals(Perfect);
      case 2, 5: yield List.of(Major, Minor).contains(quality);
      default: yield false;
    };
  }

  public boolean isTooFarApart(CptPitchNode pitch, int locus) {

    var pitch2 = cf.getNote(locus);
    var degree = Math.abs(CptPitch.diatonicDiff(pitch, pitch2));
    return degree > 14;
  }

  public boolean isForbiddenMotion(CptPitchPath path, int bar) {

    // Check only if locus portion is ARSIS.
    var cf_path = CptPitchPath.of(cf.getNote(bar - 1), cf.getNote(bar));

    var isForbiddenHiddenParallel
            = CptContrapuntalMotion.isForbiddenHiddenParallel(path, cf_path);
    var isForbiddenParallel
            = CptContrapuntalMotion.isForbiddenParallel(path, cf_path);
    var isForbiddenLeap
            = CptContrapuntalMotion.isForbiddenLeap(path, cf_path);

    var result = isForbiddenHiddenParallel || isForbiddenParallel
            || isForbiddenLeap;

    return result;
  }

  public boolean isVoiceOverlappingAtLocus(boolean treble, CptPitchNode pitch, int locus) {

    var diff = CptPitch.diff(cf.getNote(locus), pitch);
    return diff != 0 && treble ^ diff > 0;
  }

  public boolean isVoiceCrossing(boolean treble, CptPitchPath path, Cpt2Locus locus) {

    if (locus.getPrevious() == null)
      return false;
    return isVoiceOverlappingAtLocus(treble, path.getFrom(), locus.bar())
            || isVoiceOverlappingAtLocus(treble, path.getTo(), locus.getPrevious().bar());
  }

  public boolean isUnisonAtLocus(CptPitchNode pitch, int bar) {

    return pitch.getPitch().ordinal() == cf.getNote(bar).getPitch().ordinal();
  }

  public boolean isInvalidDissonanceAtThesis(CptPitchPath path, Cpt2Locus locus) {

    if (locus.portion().equals(THESIS) && !isConsonanceAtLocus(path.getTo(), locus.bar()))
      return CptContrapuntalMotion.leap(path);
    if (locus.portion().equals(ARSIS) && !isConsonanceAtLocus(path.getFrom(), locus.bar() - 1))
      return CptContrapuntalMotion.leap(path);
    return false;
  }

  /**
   * Get sorted keys from the loci-paths map with or without given number(s) of
   * trimming.
   *
   * @param trim optional int[] to specify numbers trimmed from head
   * <code>[0]</code> or tail <code>[1]</code>.
   * @return
   */
  public List<Cpt2Locus> getSortedLoci(int... trim) {

    var skip = trim.length > 0 ? trim[0] : 0;
    var limit = this.loci.size() - (trim.length > 1 ? trim[1] : 0);
    return this.loci.keySet().stream().sorted().limit(limit).skip(skip).toList();
  }

  public int getVerticesCount(int... trim) {

    return (int) this.getSortedLoci(trim).stream()
            .mapToLong(loc -> this.getLocus(loc).stream().map(CptPitchPath::getFrom).distinct().count())
            .sum();
  }

  public Set<CptPitchPath> getLocus(Cpt2Locus locus) {

    return loci.get(locus);
  }

  public boolean isEmpty() {

    return Objects.isNull(this.loci) || this.loci.isEmpty()
            || this.loci.values().stream().allMatch(Set::isEmpty);
  }

  @Override
  public CptPitchNode getStart() {

    if (this.isEmpty()) {
      throw new EmptyGraphException();
    }
    var starts = this.loci.get(new Cpt2Locus(0, this.beginWithRest ? THESIS : ARSIS)).stream()
            .map(CptPitchPath::getTo)
            .collect(Collectors.toList());
    var index = getRandom().nextInt(starts.size());
    return starts.get(index);
  }

  @Override
  public List<CptPitchPath> getEdges() {

    return loci.values().stream().flatMap(Set::stream).toList();
  }

  private List<String> checkPassing(CptPitchNode p, Cpt2Locus locus) {

    var result = new ArrayList<String>();
    result.add("checking passing on " + p + ", " + locus);

    var from = this.queryByDestination(locus, p);
    var to = this.queryByVertex(locus.getNext(), p);

    var preserved = from.stream()
            .filter(path -> Math.abs(CptPitch.diatonicDiff(path)) == 1)
            .peek(p1 -> result.add("path_from = " + p1))
            .flatMap(p1 -> to.stream().filter(p2 -> Objects.equals(CptPitch.diatonicDiff(p1), CptPitch.diatonicDiff(p2))).map(p2 -> Map.entry(p1, p2)))
            .peek(e -> result.add("passing found:" + e.toString()))
            .toList();

    this.getLocus(locus)
            .removeIf(path -> {
              if (!path.getTo().equals(p))
                return false;
              var preserving = preserved.stream().anyMatch(e -> e.getKey().equals(path));
              if (!preserving)
                result.add("Removing " + path + " at " + locus + " for not enlisted.");
              return !preserving;
            });
    this.getLocus(locus.getNext()).
            removeIf(path -> {
              if (!path.getFrom().equals(p))
                return false;
              var preserving = preserved.stream().anyMatch(e -> e.getValue().equals(path));
              if (!preserving)
                result.add("Removing " + path + " at " + locus.getNext() + " for not enlisted.");
              return !preserving;
            });

    return result;
  }

  public double getChance(CptPitchPath path, Cpt2Locus locus) {

    return this.getChance(queryByVertex(locus, path.getFrom()), path);
  }
}
