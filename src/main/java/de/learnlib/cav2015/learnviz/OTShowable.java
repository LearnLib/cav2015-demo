/* Copyright (C) 2015 TU Dortmund
 * This file is part of LearnLib, http://www.learnlib.de/.
 * 
 * LearnLib is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License version 3.0 as published by the Free Software Foundation.
 * 
 * LearnLib is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with LearnLib; if not, see
 * <http://www.gnu.de/documents/lgpl.en.html>.
 */
package de.learnlib.cav2015.learnviz;

import java.io.IOException;

import net.automatalib.words.Word;

import com.google.common.base.Function;

import de.learnlib.algorithms.features.observationtable.ObservationTable;
import de.learnlib.algorithms.features.observationtable.writer.ObservationTableHTMLWriter;

final class OTShowable<I,D> extends Showable {
	
	public static final Function<Boolean,String> BOOL_01 = new Function<Boolean,String>() {
		@Override
		public String apply(Boolean input) {
			return input.booleanValue() ? "1" : "0";
		}
	};
	
	private static final class WordPrinter<I> implements Function<Word<? extends I>,String> {
		@Override
		public String apply(Word<? extends I> input) {
			if (input.isEmpty()) {
				return "&epsilon;";
			}
			return input.toString();
		}
	};
	
	
	private final ObservationTable<I, D> ot;
	private final Function<? super D, ? extends String> outWriter;
	
	public OTShowable(String title, ObservationTable<I, D> ot, Function<? super D, ? extends String> outWriter) {
		super(title);
		this.ot = ot;
		this.outWriter = outWriter;
	}

	@Override
	public void writeHTML(Appendable a) throws IOException {
		ObservationTableHTMLWriter<I, D> writer = new ObservationTableHTMLWriter<I,D>(new WordPrinter<I>(), outWriter);
		writer.write(ot, a);
	}

}
