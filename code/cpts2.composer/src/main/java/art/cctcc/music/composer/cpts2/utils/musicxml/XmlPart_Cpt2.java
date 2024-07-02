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
package art.cctcc.music.composer.cpts2.utils.musicxml;

import art.cctcc.music.cpt.model.CptMelody;
import art.cctcc.music.utils.musicxml.XmlClef;
import art.cctcc.music.utils.musicxml.XmlPart_Motet;

/**
 *
 * @author Jonathan Chang, Chun-yien <ccy@musicapoetica.org>
 */
public class XmlPart_Cpt2 extends XmlPart_Motet {

  public XmlPart_Cpt2(String id, String part_name, String part_abbreviation, XmlClef clef, CptMelody... melodies) {

    super(id, part_name, part_abbreviation, clef, melodies);
  }

}
