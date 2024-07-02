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

import java.util.Objects;
import tech.metacontext.ocnhfa.composer.cf.model.MusicThread;
import tech.metacontext.ocnhfa.composer.cf.model.enums.Pitch;
import tech.metacontext.ocnhfa.composer.cf.model.y.PitchMove;

/**
 *
 * @author Jonathan Chang, Chun-yien <ccy@musicapoetica.org>
 */
public class StyleSelector_3 {

    public static boolean test(MusicThread t) {

        var intervals = t.getCf().getHistory().stream()
                .filter(move -> Objects.nonNull(move.getSelected().getFrom()))
                .map(PitchMove::getSelected)
                .mapToInt(Pitch::diff)
                .toArray();
        int i = 0, accumulate = 0;
        do {
            if (i >= intervals.length * 2 / 3 || intervals[i] < -2 || intervals[i] > 3) {
                return false;
            }
            accumulate += intervals[i++] - 1;
        } while (accumulate + 1 < 7);
        return true;
    }
}
