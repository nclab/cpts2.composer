/*
 * Copyright 2019 Jonathan Chang, Chun-yien <ccy@musicapoetica.org>.
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
package tech.metacontext.ocnhfa.composer.cf.utils.selectors;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import tech.metacontext.ocnhfa.composer.cf.model.MusicThread;
import tech.metacontext.ocnhfa.composer.cf.model.enums.Pitch;
import tech.metacontext.ocnhfa.composer.cf.model.y.PitchNode;

/**
 *
 * @author Jonathan Chang, Chun-yien <ccy@musicapoetica.org>
 */
public class StyleSelector_0 {

    public static boolean test(MusicThread t) {

        var ord_list = t.getCf().getMelody().stream()
                .map(PitchNode::getPitch)
                .mapToInt(Pitch::ordinal)
                .toArray();
        int counter1 = 0, counter2 = 0;
        do {
            var s = Arrays.stream(ord_list)
                    .mapToObj(String::valueOf)
                    .collect(Collectors.joining(" "));
            counter1 += IntStream.range(0, s.length() - 8)
                    .mapToObj(i -> s.substring(i))
                    .filter(sub -> sub.startsWith("0 1 2 3 4"))
                    .count();
            counter2 += IntStream.range(0, s.length() - 8)
                    .mapToObj(i -> s.substring(i))
                    .filter(sub -> sub.startsWith("4 3 2 1 0"))
                    .count();
            if (counter1 >= 1 && counter2 >= 1) {
                return true;
            }
            ord_list = Arrays.stream(ord_list).map(Math::decrementExact).toArray();
        } while (Arrays.stream(ord_list).max().getAsInt() >= 4);
        return false;
    }
}
