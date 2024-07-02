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
package art.cctcc.music.utils;

import art.cctcc.music.cpt.model.CptCantusFirmus;
import art.cctcc.music.cpt.model.enums.CptPitch;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import tech.metacontext.ocnhfa.composer.cf.model.enums.EcclesiasticalMode;

/**
 *
 * @author Jonathan Chang, Chun-yien <ccy@musicapoetica.org>
 */
public class JeppesenCF {

  private static JeppesenCF instance;

  private static List<CFDataRow> data;

  public static JeppesenCF getInstance() {

    return instance == null ? new JeppesenCF() : instance;
  }

  public static String raw
          = """
          Dorian, 1, D4, F4, E4, D4, G4, F4, A4, G4, F4, E4, D4
          Dorian, 2, D4, A4, G4, F4, E4, D4, F4, E4, D4
          Dorian, 3, D4, F4, G4, A4, G4, F4, E4, D4
          Dorian, 4, D4, A4, Bf4, A4, G4, F4, E4, D4
          Dorian, 5, D3, E3, D3, A3, G3, F3, E3, D3
          Phrygian, 6, E4, C4, D4, C4, A3, A4, G4, E4, F4, E4
          Phrygian, 7, E4, A3, A4, G4, F4, E4, D4, F4, E4
          Phrygian, 8, E4, D4, E4, F4, G4, A4, D4, F4, E4
          Phrygian, 9, E4, A3, B3, C4, D4, E4, F4, D4, G4, F4, E4
          Phrygian, 10, E4, D4, G4, F4, C4, D4, E4, A3, E4
          Mixolydian, 11, G3, D4, C4, A3, B3, C4, B3, A3, G3
          Mixolydian, 12, G3, E3, F3, G3, A3, G3, C4, B3, A3, G3
          Mixolydian, 13, G3, D4, B3, E4, D4, C4, B3, A3, G3
          Mixolydian, 14, G4, C4, D4, E4, F4, E4, A4, G4
          Aeolian, 15, A3, F4, E4, C4, D4, C4, B3, A3
          Aeolian, 16, A3, B3, G3, A3, B3, A3, D4, C4, B3, A3
          Aeolian, 17, A3, E4, B3, D4, C4, B3, A3
          Aeolian, 18, A3, A4, G4, E4, F4, E4, D4, C4, B3, A3
          Ionian, 19, C4, E4, F4, G4, E4, A4, G4, E4, F4, E4, D4, C4
          Ionian, 20, C4, G4, G3, A3, B3, C4, D4, E4, F4, E4, D4, C4
          Ionian, 21, C4, E4, D4, E4, F4, G4, A4, D4, E4, D4, C4
          Ionian, 22, C4, G3, A3, B3, C4, D4, E4, D4, C4""";

  private JeppesenCF() {

    try {
//      var path = Path.of(ClassLoader.getSystemResource("/JeppesenCF.csv").toURI());
//      data = Files.readAllLines(path).stream()
      data = raw.lines()
              .map(line -> line.split(" *, *"))
              .map(CFDataRow::new)
              .collect(Collectors.toList());
    } catch (Exception ex) {
      Logger.getLogger(JeppesenCF.class.getName()).log(Level.SEVERE, null, ex);
    }
  }

  public List<CFDataRow> getData() {

    return data;
  }

  public List<CFDataRow> getData(EcclesiasticalMode mode) {

    return data.stream()
            .filter(row -> row.mode().equals(mode))
            .toList();
  }

  /**
   * <pre>
   * Dorian
   * 1  = [D4, F4, E4, D4, G4, F4, A4, G4, F4, E4, D4]
   * 2  = [D4, A4, G4, F4, E4, D4, F4, E4, D4]
   * 3  = [D4, F4, G4, A4, G4, F4, E4, D4]
   * 4  = [D4, A4, Bf4, A4, G4, F4, E4, D4]
   * 5  = [D3, E3, D3, A3, G3, F3, E3, D3]
   * Phrygian
   * 6  = [E4, C4, D4, C4, A3, A4, G4, E4, F4, E4]
   * 7  = [E4, A3, A4, G4, F4, E4, D4, F4, E4]
   * 8  = [E4, D4, E4, F4, G4, A4, D4, F4, E4]
   * 9  = [E4, A3, B3, C4, D4, E4, F4, D4, G4, F4, E4]
   * 10 = [E4, D4, G4, F4, C4, D4, E4, A3, E4]
   * Mixolydian
   * 11 = [G3, D4, C4, A3, B3, C4, B3, A3, G3]
   * 12 = [G3, E3, F3, G3, A3, G3, C4, B3, A3, G3]
   * 13 = [G3, D4, B3, E4, D4, C4, B3, A3, G3]
   * 14 = [G4, C4, D4, E4, F4, E4, A4, G4]
   * Aeolian
   * 16 = [A3, B3, G3, A3, B3, A3, D4, C4, B3, A3]
   * 17 = [A3, E4, B3, D4, C4, B3, A3]
   * 18 = [A3, A4, G4, E4, F4, E4, D4, C4, B3, A3]
   * 15 = [A3, F4, E4, C4, D4, C4, B3, A3]
   * Ionian
   * 19 = [C4, E4, F4, G4, E4, A4, G4, E4, F4, E4, D4, C4]
   * 20 = [C4, G4, G3, A3, B3, C4, D4, E4, F4, E4, D4, C4]
   * 21 = [C4, E4, D4, E4, F4, G4, A4, D4, E4, D4, C4]
   * 22 = [C4, G3, A3, B3, C4, D4, E4, D4, C4]
   * </pre>
   *
   * @param number
   * @return Cantus firmus specified.
   */
  public CptCantusFirmus getCFByNumber(int number) {

    return data.stream()
            .filter(row -> row.number() == number)
            .map(row -> new CptCantusFirmus("Jeppesen_" + number, row.melody()))
            .findAny().orElse(null);
  }
}

record CFDataRow(EcclesiasticalMode mode, int number, List<CptPitch> melody) {

  public CFDataRow(String[] row) {

    this(EcclesiasticalMode.valueOf(row[0]),
            Integer.valueOf(row[1]),
            Arrays.stream(row, 2, row.length)
                    .map(CptPitch::valueOf)
                    .collect(Collectors.toList()));
  }
}
