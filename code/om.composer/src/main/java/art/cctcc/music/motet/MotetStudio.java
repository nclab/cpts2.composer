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
package art.cctcc.music.motet;

import static art.cctcc.music.Parameters.CF_DIR;
import art.cctcc.music.cpt.framework.CptComposer;
import art.cctcc.music.cpt.framework.CptEvolution;
import art.cctcc.music.cpt.graphs.x.CptMusicSpace;
import art.cctcc.music.cpt.model.CptCantusFirmus;
import art.cctcc.music.cpt.model.CptCounterpoint;
import art.cctcc.music.motet.framework.MotetComposer;
import art.cctcc.music.motet.model.Motet;
import static art.cctcc.music.motet.model.enums.MotetComposerType.*;
import static art.cctcc.music.motet.model.enums.ProgressiveFactor.*;
import art.cctcc.music.motet.model.enums.SectionType;
import static art.cctcc.music.motet.model.enums.SectionType.CPT_BASS;
import art.cctcc.music.utils.Settings;
import art.cctcc.music.utils.io.CFXMLReader;
import static art.cctcc.music.utils.io.CFXMLReader.getCptMusicSpaceFromCFComposer;
import art.cctcc.music.utils.io.CFXMLWriter;
import art.cctcc.music.utils.musicxml.MotetScore;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import static art.cctcc.music.Parameters.SETTINGS_FILENAME;
import art.cctcc.music.cpt.graphs.y_cpt.CptPitchSpace;
import art.cctcc.music.cpt.graphs.y_cpt.CptPitchSpaceChromatic;
import art.cctcc.music.utils.Tools;

/**
 *
 * @author Jonathan Chang, Chun-yien <ccy@musicapoetica.org>
 */
public class MotetStudio {

  private final Settings settings;
  private final Path project_folder;
  private final List<MotetComposer> motet_composers;
  private final List<Entry<CptCantusFirmus[], CptMusicSpace>> cf_lists;
  private final CptPitchSpace cps;

  public MotetStudio(Path project_folder)
          throws FileNotFoundException, IOException {

    var settings_path = new File(project_folder.toFile(), SETTINGS_FILENAME);
    var props = new Properties();
    try (var in = new FileInputStream(settings_path)) {
      props.loadFromXML(in);
    }
    this.settings = new Settings(props);
    this.project_folder = project_folder;
    this.cf_lists = loadCFLists();
    this.cps = settings.chromatic ? CptPitchSpaceChromatic.getInstance() : null;
    this.motet_composers = initMotetComposers();
  }

  private List<Entry<CptCantusFirmus[], CptMusicSpace>> loadCFLists() {

    var model_path = new File(CF_DIR, settings.cf_project_folder);
    var model_path_dir_list = model_path.listFiles(File::isDirectory);
    System.out.printf("Model folders in %s = %d\n",
            settings.cf_project_folder, model_path_dir_list.length);
    return settings.cf_model_folder.isBlank()
            ? Arrays.stream(model_path_dir_list)
                    .sorted()
                    .map(this::loadCFList)
                    .collect(Collectors.toList())
            : List.of(loadCFList(new File(model_path, settings.cf_model_folder)));
  }

  private Entry<CptCantusFirmus[], CptMusicSpace> loadCFList(File model) {

    System.out.println("Loading cf list from " + model);
    var fileList = new File(model, "cantus_firmus")
            .listFiles((dir, name) -> name.endsWith(".xml"));
    return Map.entry(
            Arrays.stream(fileList)
                    .sorted(Comparator.comparing(file -> Integer.valueOf(file.getName().split("[_.]")[2])))
                    .map(CFXMLReader::getCptCantusFirmusFromXML)
                    .filter(cf -> !settings.check_cf || Tools.checkCF(cf, cps) == null)
                    .sorted(Comparator.comparing(CF_SCORE))
                    .toArray(CptCantusFirmus[]::new),
            getCptMusicSpaceFromCFComposer(new File(model, "composer.xml"), false)
    );
  }

  public void saveCFLists() throws IOException {

    for (int i = 0; i < this.motet_composers.size(); i++) {
      var composer = this.motet_composers.get(i);
      var cf_list = this.cf_lists.get(settings.cf_model_folder == null ? i : 0);
      var output_path = project_folder.resolve(composer.getId());
      output_path.toFile().mkdirs();
      System.out.printf("%s --> %s\n", composer.getId(), output_path);
      var writeOut = Arrays.stream(cf_list.getKey())
              .sorted(Comparator.comparing(CF_SCORE))
              .map(cf -> String.format("%s, %f, \"%s\"", cf.getId(), CF_SCORE.apply(cf), cf.getMelody()))
              .collect(Collectors.joining("\n"));
      Files.writeString(output_path.resolve("cf_list.csv"), writeOut);
    }
  }

  private List<MotetComposer> initMotetComposers() {

    return IntStream.range(0, settings.composer_no)
            .mapToObj(i -> new MotetComposer("Composer-" + i, this.cf_lists.get(settings.cf_model_folder == null ? i : 0).getValue()))
            .peek(composer -> {
              System.out.println("Initializing " + composer);
              if (settings.test) {
                composer.setType(TEST);
              }
              composer.setParallel(settings.parallel);
              composer.setChromatic(settings.chromatic);
              composer.init_graphs();
              composer.getGraph().setFraction_mode(settings.fraction_mode);
            })
            .collect(Collectors.toList());
  }

  public void developSchemePlanners() {

    this.motet_composers.stream()
            .peek(c -> System.out.printf("Developing scheme planner of %s ...\n", c.getId()))
            .forEach(motet_composer -> {
              motet_composer.init_population();
              do {
                motet_composer.navigate();
              } while (!motet_composer.isAimAchieved());
            });
  }

  public void planSchemes() {

    this.motet_composers.stream()
            .peek(c -> System.out.printf("Planning schemes of %s ...\n", c.getId()))
            .forEach(c -> c.plan(settings.motet_no_per_composer));
  }

  public void saveSchemes() throws IOException {

    for (int i = 0; i < settings.composer_no; i++) {
      var composer = motet_composers.get(i);
      var output_path = project_folder.resolve(composer.getId());
      Files.writeString(output_path.resolve("planner.graphviz"),
              composer.getGraph().asGraphviz());
      var motet_list_path = output_path.resolve("motet_list.csv");
      var listOut = "Motet Title, Sections" + System.lineSeparator()
              + composer.getMotets().stream()
                      .map(motet -> motet.getTitle() + ", " + motet.getScheme().size() + ", " + motet.getSchemeString())
                      .collect(Collectors.joining(System.lineSeparator()));
      Files.writeString(motet_list_path, listOut);
    }
  }

  public void composeMotets() {

    for (int i = 0; i < this.motet_composers.size(); i++) {
      var composer = this.motet_composers.get(i);
      System.out.println("** " + composer + " **");
      var cf_list = cf_lists.get(settings.cf_model_folder.isEmpty() ? i : 0).getKey();
      composer.compose(Arrays.asList(cf_list));
    }
  }

  public void save() {

    this.motet_composers.forEach(this::saveComposer);
  }

  private void saveComposer(MotetComposer composer) {

    var output_path = project_folder.resolve(composer.getId());
    output_path.toFile().mkdirs();
    System.out.printf("%s --> %s\n", composer.getId(), output_path);

    try {
      var graph_x_path = output_path.resolve("graph_x");
      graph_x_path.toFile().mkdirs();
      for (var entry : composer.getX().entrySet()) {
        var graph_x = entry.getValue();
        Files.writeString(graph_x_path.resolve(entry.getKey() + ".xml"),
                graph_x.asXML());
        Files.writeString(graph_x_path.resolve(entry.getKey() + ".graphviz"),
                graph_x.asGraphviz());
      }

      for (int j = 0; j < settings.motet_no_per_composer; j++) {
        var motet = composer.getMotets().get(j);
        var motet_score = new MotetScore(motet.getTitle(),
                settings.project_name + "\n" + composer.getId(),
                motet.getComposition());
        var score_path = output_path.resolve(String.format("motet_%d.musicxml", j + 1));
        motet_score.writeMusicXML(score_path.toFile());

        var motet_data_folder = output_path.resolve(motet.getId());
        saveMotetData(motet, motet_data_folder);
      }
    } catch (IOException ex) {
      Logger.getLogger(MotetStudio.class
              .getName()).log(Level.SEVERE, null, ex);
    }
  }

  private void saveMotetData(Motet motet, Path motet_data_folder)
          throws IOException {

    motet_data_folder.toFile().mkdirs();

    for (int i = 0; i < motet.getScheme().size(); i++) {
      var section = motet.getComposition().get(i);
      var folder = motet_data_folder.resolve(i + "_" + section.getKey().name());
      folder.toFile().mkdirs();
      var sectionType = section.getKey();
      switch (sectionType) {
        case CF ->
          saveMotetCF(folder, (CptCantusFirmus) section.getValue(),
                  (CptComposer[]) motet.getData().get(i));
        case CPT_TREBLE, CPT_BASS ->
          saveMotetCPT(folder, sectionType, (CptCounterpoint) section.getValue(),
                  (CptEvolution) motet.getData().get(i));
      }
    }
  }

  private void saveMotetCF(Path folder, CptCantusFirmus cf, CptComposer[] data)
          throws IOException {

    CFXMLWriter.saveCantusFirmus(cf, folder.toFile(), "cf");

    Files.writeString(folder.resolve("composer_treble.xml"), data[0].asXML());
    Files.writeString(folder.resolve("treble.x.graphviz"), data[0].getX().asGraphviz());
    Files.writeString(folder.resolve("treble.y.graphviz"), data[0].getY().asGraphviz());

    Files.writeString(folder.resolve("composer_bass.xml"), data[0].asXML());
    Files.writeString(folder.resolve("bass.x.graphviz"), data[1].getX().asGraphviz());
    Files.writeString(folder.resolve("bass.y.graphviz"), data[1].getY().asGraphviz());
  }

  private void saveMotetCPT(Path folder, SectionType type, CptCounterpoint cpt, CptEvolution data)
          throws IOException {

    CFXMLWriter.saveCounterpoint(cpt, type, folder.toFile(), "cpt");
    var evals = data.getEvals();
    Files.writeString(folder.resolve("ec_data.csv"),
            IntStream.range(0, evals.size())
                    .mapToObj(k -> k + ", " + evals.get(k))
                    .collect(Collectors.joining(System.lineSeparator())));
    Files.writeString(folder.resolve("report.txt"), data.getReport());
  }

}
