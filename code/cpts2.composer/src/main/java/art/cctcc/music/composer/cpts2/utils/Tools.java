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

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Comparator;
import java.util.Map;
import java.util.Arrays;
import java.util.function.DoubleUnaryOperator;
import java.util.stream.IntStream;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.dom4j.Document;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

import art.cctcc.music.utils.JeppesenCF;
import art.cctcc.music.cpt.graphs.y_cpt.CptPitchPath;
import art.cctcc.music.cpt.graphs.y_cpt.CptPitchNode;
import art.cctcc.music.cpt.model.CptCantusFirmus;
import art.cctcc.music.cpt.model.enums.CptPitch;

import art.cctcc.music.composer.cpts2.graphs.y_cpt2_cf.*;

/**
 *
 * @author Jonathan Chang, Chun-yien <ccy@musicapoetica.org>
 */
public class Tools {

  public static void main(String[] args) {

    System.out.println("-".repeat(80));
    System.out.println("Jeppesen cantus firmus list");
    System.out.println("-".repeat(80));
    IntStream.rangeClosed(1, 22)
            .mapToObj(i -> Map.entry(i, JeppesenCF.getInstance().getCFByNumber(i)))
            .map(e -> String.format("%2d. %10s (%2d): %s",
            e.getKey(), e.getValue().getMode(), e.getValue().length(), e.getValue().getMelody()))
            .forEach(System.out::println);
  }

  public static double[][] getChancePerPathNode(Cpt2CfPitchSpace y) {

    return y.getSortedLoci(0, 1).stream()
            .map(loc -> {
              var paths = y.getLocus(loc);
              return paths.stream()
                      .sorted(Comparator.comparing(CptPitchPath::toString))
                      .mapToDouble(path -> y.getChance(path, loc))
                      .toArray();
            }).toArray(double[][]::new);
  }

  public static double sumUp2DDoubleArray(double[][] data, DoubleUnaryOperator op) {

    return Arrays.stream(data)
            .flatMapToDouble(Arrays::stream)
            .map(op)
            .sum();
  }

  public static double[][] diff2DDoubleArray(double[][] array1, double[][] array2) {

    var result = new double[array1.length][];
    for (int i = 0; i < array1.length; i++) {
      result[i] = new double[array1[i].length];
      for (int j = 0; j < array1[i].length; j++) {
        result[i][j] = array2[i][j] - array1[i][j];
      }
    }
    return result;
  }

  public static int peakOrValley(CptCantusFirmus cf, boolean peak) {

    var stat = cf.getMelody().stream()
            .map(CptPitchNode::getPitch)
            .mapToInt(CptPitch::getChromatic_number)
            .summaryStatistics();
    return (int) cf.getMelody().stream()
            .map(CptPitchNode::getPitch)
            .map(CptPitch::getChromatic_number)
            .filter(chr -> chr == (peak ? stat.getMax() : stat.getMin()))
            .count();
  }

  public static Predicate<CptCantusFirmus> CFFilter
          = cf -> switch (cf.getMode()) {
    case Dorian -> // 0
      Tools.peakOrValley(cf, true) <= 1 && Tools.peakOrValley(cf, false) <= 3;
    case Phrygian -> // 1
      Tools.peakOrValley(cf, true) <= 3 && Tools.peakOrValley(cf, false) <= 1;
    case Mixolydian -> // 2
      Tools.peakOrValley(cf, true) <= 2 && Tools.peakOrValley(cf, false) <= 2;
    case Aeolian -> // 3
      Tools.peakOrValley(cf, true) <= 1 && Tools.peakOrValley(cf, false) <= 2;
    case Ionian -> // 4
      Tools.peakOrValley(cf, true) <= 3 && Tools.peakOrValley(cf, false) <= 2;
  };

  public static Function<File, Integer> getIndexOfFile
          = file -> Integer.valueOf(file.getName().split("[._]")[2]);

  public static String toMD(List list) {

    return list.toString().replaceAll("[\\[\\]]", "`");
  }

  public static String getXMLPrettyPrint(Document doc, boolean suppressDeclaration) {

    var format = OutputFormat.createPrettyPrint();
    format.setExpandEmptyElements(true);
    format.setSuppressDeclaration(suppressDeclaration);
    var xmlResult = new StringWriter();
    var writer = new XMLWriter(xmlResult, format);
    try {
      writer.write(doc);
    } catch (IOException ex) {
      Logger.getLogger(Tools.class.getName()).log(Level.SEVERE, null, ex);
    }
    return xmlResult.toString();
  }
}
