/*
 * Copyright 2021 Jonathan Chang, Chun-yien <ccy@musicapoetica.org>.
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
package art.cctcc.music.utils.io;

import art.cctcc.music.cpt.framework.CptComposer;
import art.cctcc.music.cpt.framework.CptEvaluation;
import art.cctcc.music.cpt.framework.CptThread;
import art.cctcc.music.cpt.model.CptCantusFirmus;
import art.cctcc.music.cpt.model.CptCounterpoint;
import art.cctcc.music.motet.model.enums.SectionType;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Comparator;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.IntStream;
import org.dom4j.DocumentHelper;

/**
 *
 * @author Jonathan Chang, Chun-yien <ccy@musicapoetica.org>
 */
public class CFXMLWriter {

  public static File saveCptComposer(CptComposer composer, File parent, String folder) {

    var path = (Objects.isNull(parent)) ? new File(folder) : new File(parent, folder);
    path.mkdirs();

    try (var fw_composer = new FileWriter(new File(path, "composer.xml"));
            var bw_composer = new BufferedWriter(fw_composer);) {
      bw_composer.write(composer.asXML());
    } catch (IOException ex) {
      Logger.getLogger(CFXMLWriter.class.getName()).log(Level.SEVERE, null, ex);
    }

    try (var fw_graph_x = new FileWriter(new File(path, "graph_x.graphviz"));
            var bw_graph_x = new BufferedWriter(fw_graph_x);) {
      bw_graph_x.write(composer.getX().asGraphviz());
    } catch (IOException ex) {
      Logger.getLogger(CFXMLWriter.class.getName()).log(Level.SEVERE, null, ex);
    }

    synchronized (CFXMLWriter.class) {
      try (var fw_graph_y = new FileWriter(new File(path, "graph_y.graphviz"));
              var bw_graph_y = new BufferedWriter(fw_graph_y);) {
        bw_graph_y.write(composer.getY().asGraphviz());
      } catch (IOException ex) {
        Logger.getLogger(CFXMLWriter.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
    return path;
  }

  public static void saveMusicThread(CptThread thread, File file) {

    var doc = DocumentHelper.createDocument();
    var root = doc.addElement("CptMusicThread");
    {
      root.addElement("mode")
              .addText(thread.getCpt().getCf().getMode().name());
      root.addElement("CantusFirmus")
              .addText(thread.getCpt().getCf().toString());
      root.addElement("middle")
              .addText(thread.getCpt().getMiddle().name());

      root.addElement("rating")
              .addText(String.valueOf(CptEvaluation.getInstance(thread).get()));

      var history = root.addElement("history")
              .addAttribute("length", String.valueOf(thread.getCpt().length()));
      {
        IntStream.range(1, thread.getRoute().size()).forEach(i -> {
          var pitch_route = history.addElement("PitchHistory")
                  .addAttribute("move", String.valueOf(i));
          var pitch_history = thread.getRoute().get(i);
          {
            pitch_route.addElement("to")
                    .addText(pitch_history.getDimension("y").getName());
            pitch_route.addElement("MusicThought")
                    .addText(pitch_history.getDimension("x").getName());
            pitch_route.addElement("exploit")
                    .addText(String.valueOf(!pitch_history.getY().isExploring()));
          }
          var routes = pitch_route.addElement("routes");
          {
            pitch_history.getY().getPheromoneRecords().entrySet().stream()
                    .sorted(Entry.comparingByValue(Comparator.reverseOrder()))
                    .forEach(entry -> {
                      var route = routes.addElement("route");
                      route.addElement("to").addText(entry.getKey().getTo().getName());
                      route.addElement("pheromoneTrail").addText(String.valueOf(entry.getValue()));
                    });
          }
        });
      }
    }
    synchronized (CFXMLWriter.class) {
      try (var fw = new FileWriter(file);
              var bw = new BufferedWriter(fw);) {
        bw.write(doc.asXML());
      } catch (IOException ex) {
        Logger.getLogger(CFXMLWriter.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
  }

  public static void saveCantusFirmus(CptCantusFirmus cf, File folder, String filename) {

    var doc = DocumentHelper.createDocument();
    var root = doc.addElement("CptCantusFirmus")
            .addAttribute("id", cf.getId());

    root.addElement("CantusFirmus").addText(cf.getMelody().toString());
    root.addElement("mode").addText(cf.getMode().name());

    synchronized (CFXMLWriter.class) {
      folder.mkdirs();
      var file = (filename == null)
              ? new File(folder, "CptCantusFirmus_" + cf.getId() + ".xml")
              : new File(folder, filename + ".xml");
      try (var fw = new FileWriter(file);
              var bw = new BufferedWriter(fw);) {
        bw.write(doc.asXML());
      } catch (IOException ex) {
        Logger.getLogger(CFXMLWriter.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
  }

  public static void saveCounterpoint(CptCounterpoint cpt, SectionType type,
          File folder, String filename) {

    var doc = DocumentHelper.createDocument();
    var root = doc.addElement("CptCounterpoint")
            .addAttribute("id", cpt.getId());

    root.addElement("Counterpoint").addText(cpt.getMelody().toString());
    root.addElement("SectionType").addText(type.toString());
    root.addElement("CantusFirmus").addText(cpt.getCf().getMelody().toString());
    root.addElement("mode").addText(cpt.getMode().name());

    synchronized (CFXMLWriter.class) {
      folder.mkdirs();
      var file = new File(folder, filename + ".xml");
      try (var fw = new FileWriter(file);
              var bw = new BufferedWriter(fw);) {
        bw.write(doc.asXML());
      } catch (IOException ex) {
        Logger.getLogger(CFXMLWriter.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
  }
}
