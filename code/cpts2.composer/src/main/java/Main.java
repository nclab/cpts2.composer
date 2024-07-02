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
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import org.dom4j.DocumentHelper;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

import tech.metacontext.ocnhfa.antsomg.impl.StandardParameters;
import tech.metacontext.ocnhfa.antsomg.impl.StandardGraph;
import art.cctcc.music.cpt.model.CptCantusFirmus;
import art.cctcc.music.utils.JeppesenCF;
import art.cctcc.music.utils.io.CFXMLReader;

import art.cctcc.music.composer.cpts2.framework.Cpt2Composer;
import art.cctcc.music.composer.cpts2.framework.Cpt2Thread;
import art.cctcc.music.composer.cpts2.graphs.y_cpt2_cf.Cpt2CfGraphMode;
import art.cctcc.music.composer.cpts2.utils.Tools;
import static art.cctcc.music.composer.cpts2.utils.Constants.*;
import static art.cctcc.music.composer.cpts2.utils.Tools.getIndexOfFile;

/**
 *
 * @author Jonathan Chang, Chun-yien <ccy@musicapoetica.org>
 */
public class Main {

  private static boolean beginWithRest;
  private static boolean wholeNoteCadence;
  private static Cpt2CfGraphMode y_mode;
  private static String cf_source;

  public static void main(String[] args) throws URISyntaxException {

    if (args.length > 0 && args[0].toLowerCase().equals("help")) {
      System.out.println(
              """
              ========================
              command-line args:
              ========================
              [0]    - [mode] CON | CHR | DIS
              [1]    - [options] BWR | WNC | BWRWNC(-)
              [2]    - [cf] JEP | DEF
              [3...] - (int) cf numbers
                * JEP: cf numbers(1-22)
                * DEF: [3] composer(0-4), [4...] cf numbers(31, 51, 41, 48, 36)

              ========================
              explaination
              ========================
              [mode]
                CON: Conventional Rules
                CHR: Chromatic style by replacing graph y-cpt
                DIS: Dissonant style twist by adjusting rules
              [options]
                BWR: Begin with rest
                WNC: Whole note cadence (vs. half note)
              [cf]
                JEP: Jeppesen cantus firmus
                DEF: Default cantus firmus generated by previous work

              Ex:
              Run with default settings (con - jep 1)
              mvn exec:java@main

              Run with default cf no. 2, 3, 4, and 5 from composer 1 in chromatic-style-twisted rules with options BWR and WNC.
              mvn exec:java@main -Dexec.args="chr - def 1 2 3 4 5"

              Run with Jeppesen cf no. 22 in dissonant-style-twisted rules with options WNC.
              mvn exec:java@main -Dexec.args="dis wnc jep 22"
              """);
      System.exit(0);
    }

    StandardParameters.initialization(Instant.now().getEpochSecond());

    y_mode = args.length > 0 ? Arrays.stream(Cpt2CfGraphMode.values())
            .filter(m -> m.abbr.equals(args[0].toUpperCase()))
            .findAny().orElse(Cpt2CfGraphMode.CONVENTIONAL)
            : Cpt2CfGraphMode.CONVENTIONAL;
    System.out.println("Y_MODE = " + y_mode);

    var options = args.length > 1 ? args[1].toLowerCase() : "bwrwnc";
    beginWithRest = "-".equals(options) || options.contains("bwr");
    wholeNoteCadence = "-".equals(options) || options.contains("wnc");
    System.out.println("BEGIN_WITH_REST = " + beginWithRest);
    System.out.println("WHOLE_NOTE_CADENCE = " + wholeNoteCadence);

    cf_source = args.length > 2 ? args[2].toLowerCase() : "jep";
    if (!List.of("def", "jep").contains(cf_source)) {
      System.out.println("Unrecognized cf source: " + cf_source);
      System.exit(-1);
    }
    System.out.println("CF: " + cf_source);

    var numbers = args.length > 3 ? Arrays.stream(args)
            .skip(cf_source.equals("def") ? 4 : 3)
            .mapToInt(Integer::valueOf)
            .distinct()
            .filter(i -> i > 0)
            .filter(i -> cf_source.equals("jep") && i <= 22 || cf_source.equals("def") && i <= 100)
            .toArray() : new int[]{1};
    System.out.println("Number(s): " + Arrays.toString(numbers));

    switch (cf_source) {
      case "def" -> compose(args.length > 3 ? args[3] : "0", numbers);
      case "jep" -> compose(numbers);
    }
  }

  public static void compose(String composer, int... numbers) {

    System.out.println("Composer: " + composer);
    var xml_files_path = CF_PATH.resolve(CF_COMPOSER_PREFIX + composer)
            .resolve("cantus_firmus");
    System.out.println("xml_files_path = " + xml_files_path);
    var xml_files = xml_files_path.toFile().listFiles(file -> file.getName().endsWith(".xml"));
    var indices = Arrays.stream(numbers).boxed().toList();
    var cf_list = Arrays.stream(xml_files)
            .sorted(Comparator.comparing(getIndexOfFile::apply))
            .map(CFXMLReader::getCptCantusFirmusFromXML)
            .filter(Tools.CFFilter)
            .toList();
    var cfs = cf_list.stream()
            .filter(cf -> numbers.length == 0 || indices.contains(cf_list.indexOf(cf) + 1))
            .peek(cf -> System.out.printf("Fetching %s (%d) ...\n", cf.getId(), cf_list.indexOf(cf) + 1))
            .toList();
    compose(cfs);
  }

  public static void compose(int... jepNumber) {

    var cfs = Arrays.stream(jepNumber)
            .mapToObj(JeppesenCF.getInstance()::getCFByNumber)
            .toList();
    compose(cfs);
  }

  public static void compose(List<CptCantusFirmus> cfs) {

    var id = Instant.now().getEpochSecond();

    cfs.stream().forEach(cf -> {
      try {
        var c_treble = Cpt2Composer.getInstance(
                String.format("%s on %s (%s)", id, cf.getId().replace("_", " "), "treble"),
                cf, true, beginWithRest, wholeNoteCadence);
        c_treble.setFraction_mode(StandardGraph.FractionMode.Power_Multiply);
        c_treble.developPrimary(y_mode);
        var folder = getStandardOutputFolder("" + id, c_treble);

        saveSettings(folder.resolve("settings.xml"), id, cf);

        c_treble.compose();
        c_treble.save(folder);

        var c_bass = Cpt2Composer.getInstance(
                String.format("%s on %s (%s)", id, cf.getId().replace("_", " "), "bass"),
                cf, false, beginWithRest, wholeNoteCadence);
        c_bass.setFraction_mode(StandardGraph.FractionMode.Power_Multiply);
        c_bass.developSecondary(y_mode, c_treble.getX());
        c_bass.compose();
        c_bass.save(folder);
      } catch (Exception ex) {
        Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        System.exit(-1);
      }
    });
    System.out.println("All writings finished.");
  }

  private static void saveSettings(Path path, long id, CptCantusFirmus cf) {

    var doc = DocumentHelper.createDocument();
    var root = doc.addElement("cpts2.composer")
            .addAttribute("id", "" + id);
    root.addElement("timestamp")
            .addText(Instant.ofEpochSecond(id).atZone(ZoneId.systemDefault()).toString());
    root.addElement("y_mode")
            .addText(y_mode.toString());
    root.addElement("BWR")
            .addText("" + beginWithRest);
    root.addElement("WNC")
            .addText("" + wholeNoteCadence);
    root.addElement("cf_source")
            .addText(cf_source);
    root.addElement("cantus_firmus")
            .addAttribute("id", cf.getId())
            .addText(cf.getMelody().toString());
    var format = OutputFormat.createPrettyPrint();
    format.setExpandEmptyElements(true);
    try (var fw = new FileWriter(path.toFile())) {
      var writer = new XMLWriter(fw, format);
      writer.write(doc);
    } catch (IOException ex) {
      Logger.getLogger(Cpt2Thread.class.getName()).log(Level.SEVERE, null, ex);
    }
  }
}