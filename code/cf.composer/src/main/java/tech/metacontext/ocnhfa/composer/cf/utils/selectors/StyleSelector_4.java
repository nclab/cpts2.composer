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
import java.util.Objects;
import tech.metacontext.ocnhfa.composer.cf.model.MusicThread;
import tech.metacontext.ocnhfa.composer.cf.model.enums.Pitch;
import tech.metacontext.ocnhfa.composer.cf.model.y.PitchMove;

/**
 *
 * @author Jonathan Chang, Chun-yien <ccy@musicapoetica.org>
 */
public class StyleSelector_4 {

    public static boolean test(MusicThread t) {

        var intervals = t.getCf().getHistory().stream()
                .filter(move -> Objects.nonNull(move.getSelected().getFrom()))
                .limit(3)
                .map(PitchMove::getSelected)
                .mapToInt(Pitch::diff)
                .toArray();
        return Arrays.stream(intervals).allMatch(i -> Math.abs(i) < 5)
                && Arrays.stream(intervals).anyMatch(i -> Math.abs(i) > 2)
                && intervals[0] * intervals[1] < 0 && intervals[0] * intervals[2] > 0;
    }
}
