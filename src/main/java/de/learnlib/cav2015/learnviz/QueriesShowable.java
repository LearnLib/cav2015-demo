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
import java.util.List;

import net.automatalib.words.Word;

final class QueriesShowable extends Showable {

	private final List<? extends Word<?>> queries;
	public QueriesShowable(String title, List<? extends Word<?>> queries) {
		super(title);
		this.queries = queries;
	}
	@Override
	public void writeHTML(Appendable a) throws IOException {
		for (Word<?> q : queries) {
			a.append(q.isEmpty() ? "&epsilon;" : q.toString()).append("<br>").append('\n');
		}
	}
	
	
}
