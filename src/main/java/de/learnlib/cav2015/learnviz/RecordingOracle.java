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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.automatalib.words.Word;
import de.learnlib.api.MembershipOracle;
import de.learnlib.api.Query;

final class RecordingOracle<I, D> implements MembershipOracle<I, D> {
	
	private final MembershipOracle<I, D> delegate;
	
	private List<Word<I>> newQueries = new ArrayList<>();
	
	public RecordingOracle(MembershipOracle<I, D> delegate) {
		this.delegate = delegate;
	}
	
	
	public List<Word<I>> fetchNewQueries() {
		List<Word<I>> result = newQueries;
		
		newQueries = new ArrayList<>();
		
		return result;
	}


	@Override
	public void processQueries(Collection<? extends Query<I, D>> queries) {
		for (Query<I,D> qry : queries) {
			newQueries.add(qry.getInput());
		}
		delegate.processQueries(queries);
	}
	
}
