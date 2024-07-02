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

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import java.util.function.Predicate;
import org.dom4j.DocumentHelper;
import lombok.Getter;

import tech.metacontext.ocnhfa.composer.cf.ex.UnexpectedMusicNodeException;
import tech.metacontext.ocnhfa.composer.cf.model.enums.MusicThought;
import art.cctcc.music.cpt.framework.CptThread;
import art.cctcc.music.cpt.framework.CptTrace;
import art.cctcc.music.cpt.model.CptCantusFirmus;
import art.cctcc.music.cpt.graphs.y_cpt.*;

import art.cctcc.music.composer.cpts2.graphs.x.*;
import art.cctcc.music.composer.cpts2.graphs.y_cpt2_cf.Cpt2CfPitchSpace;
import art.cctcc.music.composer.cpts2.model.Cpt2Counterpoint;
import art.cctcc.music.composer.cpts2.model.Cpt2Locus;
import art.cctcc.music.composer.cpts2.utils.Tools;
import static art.cctcc.music.composer.cpts2.utils.Constants.*;

/**
 *
 * @author Jonathan Chang, Chun-yien <ccy@musicapoetica.org>
 */
public class Cpt2Thread extends CptThread {

  @Getter private Cpt2Counterpoint cpt;

  /**
   * The chance the ant strays away from the path determined by pheromone
   *
   * @param cpt_id
   * @param cf
   * @param entry
   * @param treble
   * @param beginWithRest
   * @param wholeNoteCadence
   */
  public Cpt2Thread(String cpt_id, CptCantusFirmus cf, CptPitchNode entry,
          boolean treble, boolean beginWithRest, boolean wholeNoteCadence) {

    super(cf, entry);
    setup(cpt_id, cf, treble, beginWithRest, wholeNoteCadence);

    if (beginWithRest)
      this.cpt.addNote(CptPitchNode.getEmptyNode());
    this.cpt.addNote(entry);
  }

  /**
   *
   * @param cpt_id
   * @param cf
   * @param melody
   * @param treble
   * @param beginWithRest
   * @param wholeNoteCadence
   */
  public Cpt2Thread(String cpt_id, CptCantusFirmus cf, List<CptPitchNode> melody,
          boolean treble, boolean beginWithRest, boolean wholeNoteCadence) {

    super(cf, List.of());
    setup(cpt_id, cf, treble, beginWithRest, wholeNoteCadence);

    melody.stream().forEach(this.cpt::addNote);
  }

  private void setup(String cpt_id, CptCantusFirmus cf,
          boolean treble, boolean beginWithRest, boolean wholeNoteCadence) {

    this.cpt = new Cpt2Counterpoint(cpt_id, cf);
    this.cpt.setTreble(treble);
    this.cpt.setBeginWithRest(beginWithRest);
    this.cpt.setWholeNoteCadence(wholeNoteCadence);
  }

  @Override
  public CptPitchPath lastPitchPath() {

    return this.getRoute() == null
            ? CptPitchPath.of(this.cpt.getNote(this.cpt.length() - 2), this.cpt.getMelody().getLast())
            : this.getCurrentTrace().getY().getSelected();
  }

  public void develop(Cpt2MusicSpace x, Cpt2CfPitchSpace y,
          double x_pheromone_deposit, double y_pheromone_deposit) {

    if (!this.isCompleted()) {
      y.getSortedLoci(1).forEach(locus -> {
        var x_move = nav_x(x, x_pheromone_deposit);
        var y_move = nav_y(locus, y, x_move.getPredicate(this), y_pheromone_deposit);
        super.setCurrentTrace(new CptTrace(x_move, y_move));
      });
      this.setCompleted(true);
    }
  }

  public Cpt2MusicMove nav_x(Cpt2MusicSpace x, double x_pheromone_deposit) {

    Cpt2MusicMove x_move;
    if (cpt.length() >= 2 && cpt.getMelody().getLast().getName().matches("$[BF].^")) {
      x_move = new Cpt2MusicMove(MusicThought.DIRECTIONAL, MusicThought.CONJUNCT);
    } else if (cpt.length() >= 2 && this.lastPitchPath().absDiff() > 5) {
      x_move = new Cpt2MusicMove(MusicThought.COMPLEMENTAL, MusicThought.SHORTTERM);
    } else {
      var move0 = x.getMove(x.getStart(), X_EXPLORE_CHANCE);
      var move1 = x.getMove(move0.getSelected().getTo(), X_EXPLORE_CHANCE);
      x_move = new Cpt2MusicMove(move0, move1);
      if (Objects.isNull(x_move.getMusicThought())) {
        throw new UnexpectedMusicNodeException(
                x_move.getMoves()[0].getSelected().getTo(),
                x_move.getMoves()[1].getSelected().getTo());
      }
    }
    x.move(x_move, x_pheromone_deposit);
    return x_move;
  }

  public CptPitchMove nav_y(Cpt2Locus locus, Cpt2CfPitchSpace y,
          Predicate<CptPitchPath> predicate_x, double y_pheromone_deposit) {

    final var current = getCurrentTrace().getY().getSelected().getTo();
    var y_move = Stream.generate(() -> y.getMove(locus, current, Y_EXPLORE_CHANCE))
            .filter(move -> predicate_x.test(move.getSelected())
            || y.queryByVertex(locus, current).stream().filter(predicate_x).count() == 0)
            .findFirst()
            .orElseThrow();
    y.move(y_move, y_pheromone_deposit);
    var notes = y.queryByVertex(locus, current).stream()
            .filter(predicate_x)
            .map(CptPitchPath::getTo)
            .toList();
    return y_move;
  }

  public String asXML(Cpt2MusicSpace x, Cpt2CfPitchSpace y) {

    var doc = DocumentHelper.createDocument();
    var root = doc.addElement(this.getClass().getSimpleName());
    var cf = this.getCpt().getCf();
    root.addElement("mode")
            .addText(this.getCpt().getCf().getMode().name());
    root.addElement(cf.getClass().getSimpleName())
            .addText(cf.getMelody().toString());
    root.addElement("cf_length")
            .addText("" + this.getCpt().getCf().length());
    root.addElement(this.getCpt().getClass().getSimpleName())
            .addText(this.getCpt().getMelody().toString().replace("null", "Rest"));
    root.addElement("beginWithRest")
            .addText("" + this.getCpt().isBeginWithRest());
    root.addElement("wholeNoteCadence")
            .addText("" + this.getCpt().isWholeNoteCadence());
    var route = root.addElement("cpt2_route")
            .addAttribute("length", "" + this.getCpt().length());

    this.getRoute().stream()
            .filter(tr -> tr.getX() != null && tr.getY().getSelected().getFrom() != null)
            .forEach(tr -> {
              var locus = y.getSortedLoci().get(this.getRoute().indexOf(tr));
              var trace = route.addElement(tr.getClass().getSimpleName())
                      .addAttribute("locus", locus.toString());

              var cptMusicMove = tr.getX();
              var music_move = trace.addElement(cptMusicMove.getClass().getSimpleName());
              music_move.addElement(cptMusicMove.getMusicThought().getClass().getSimpleName())
                      .addText(cptMusicMove.getMusicThought().name());
              var x_moves = music_move.addElement("MusicPaths");
              Stream.of(0, 1)
                      .forEach(i -> {
                        var selected = cptMusicMove.getMoves()[i].getSelected();
                        var x_move = x_moves.addElement("MusicPath")
                                .addAttribute("move", "" + i);
                        x_move.addElement("from")
                                .addText(selected.getFrom().getName());
                        x_move.addElement("to")
                                .addText(selected.getTo().getName());
                        x_move.addElement("chance")
                                .addText("" + x.getChance(x.queryByVertex(selected.getFrom()), selected));
                        x_move.addElement("exploit")
                                .addText("" + !cptMusicMove.getMoves()[i].isExploring());
                      });

              var cptPitchMove = tr.getY();
              var pitch_move = trace.addElement(cptPitchMove.getClass().getSimpleName());
              var selected = pitch_move.addElement("selected");
              selected.addElement("from")
                      .addText(cptPitchMove.getSelected().getFrom().getName());
              selected.addElement("to")
                      .addText(cptPitchMove.getSelected().getTo().getName());
              selected.addElement("chance")
                      .addText("" + y.getChance(cptPitchMove.getSelected(), locus));
              selected.addElement("exploit")
                      .addText("" + !cptPitchMove.isExploring());
              var y_moves = pitch_move.addElement("CptPitchPaths");
              cptPitchMove.getPheromoneRecords().entrySet().stream()
                      .forEach(e -> {
                        var y_move = y_moves.addElement("CptPitchPath");
                        y_move.addElement("from")
                                .addText(e.getKey().getFrom().getName());
                        y_move.addElement("to")
                                .addText(e.getKey().getTo().getName());
                        y_move.addElement("chance")
                                .addText("" + y.getChance(e.getKey(), locus));
                      });
            });
    return Tools.getXMLPrettyPrint(doc, false);
  }

  @Override
  public int hashCode() {
    int hash = 3;
    hash = 73 * hash + Objects.hashCode(this.cpt.getMelody().toString());
    hash = 73 * hash + Objects.hashCode(this.cpt.getCf().getMelody().toString());
    return hash;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (obj instanceof final Cpt2Thread other)
      return Objects.equals(
              this.cpt.getMelody().toString(),
              other.getCpt().getMelody().toString()
      ) && Objects.equals(
              this.cpt.getCf().getMelody().toString(),
              other.getCpt().getCf().getMelody().toString());
    else
      return false;
  }
}
