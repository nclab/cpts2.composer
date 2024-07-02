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

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import tech.metacontext.ocnhfa.composer.cf.model.MusicThread;

/**
 *
 * @author Jonathan Chang, Chun-yien <ccy@musicapoetica.org>
 */
public class StyleSelector {

    public static Predicate<MusicThread> get(int number) {

        return (thread) -> switch (number) {
            case 0->StyleSelector_0.test(thread);
            case 1->StyleSelector_1.test(thread);
            case 2->StyleSelector_2.test(thread);
            case 3->StyleSelector_3.test(thread);
            case 4->StyleSelector_4.test(thread);
            default->true;
        };
    }
    
    public static String totalReport(List<MusicThread> threads){
        
        return IntStream.rangeClosed(0, 4)
                .mapToObj(StyleSelector::get)
                .map(selector -> threads.stream().filter(selector).count())
                .map(counter -> String.format("%3d(%5.2f%%)",
                        counter, 100.0 * counter / threads.size()))
                .collect(Collectors.joining(" "));
    }
}
