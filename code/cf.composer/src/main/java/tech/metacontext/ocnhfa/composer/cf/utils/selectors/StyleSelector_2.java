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

/**
 *
 * @author Jonathan Chang, Chun-yien <ccy@musicapoetica.org>
 */
public class StyleSelector_2 {

    public static boolean test(MusicThread t) {

        var intervals = t.getCf().getHistory().stream()
                .filter(move -> Objects.nonNull(move.getSelected().getFrom()))
                .mapToInt(move -> move.getSelected().getInterval())
                .toArray();
        return Arrays.stream(intervals).anyMatch(i -> i == 8)
                && Arrays.stream(intervals).filter(i -> i > 3 && i != 8).count() >= 2;
    }
}
