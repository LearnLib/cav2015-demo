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
package de.learnlib.cav2015.performance;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import net.automatalib.words.Alphabet;
import de.learnlib.acex.analyzers.AcexAnalyzers;
import de.learnlib.algorithms.kv.dfa.KearnsVaziraniDFA;
import de.learnlib.algorithms.lstargeneric.ce.ObservationTableCEXHandlers;
import de.learnlib.algorithms.lstargeneric.closing.ClosingStrategies;
import de.learnlib.algorithms.lstargeneric.dfa.ExtensibleLStarDFA;
import de.learnlib.api.LearningAlgorithm.DFALearner;
import de.learnlib.api.MembershipOracle;
import de.learnlib.libalf.LibalfActiveDFALearner;
import de.learnlib.libalf.LibalfAngluinColDFA;
import de.learnlib.libalf.LibalfAngluinSimpleDFA;
import de.learnlib.libalf.LibalfKVDFA;
import de.learnlib.libalf.LibalfRSDFA;

public class LibAlf {
	
	public static abstract class Learner implements Comparable<Learner> {
		private final String name;
		private final int id;
		
		public Learner(String name, int id) {
			this.name = name;
			this.id = id;
		}
		
		public String getName() {
			return name;
		}
		
		public int getId() {
			return id;
		}
		
		@Override
		public int compareTo(Learner other) {
			return id - other.id;
		}
		
		public abstract <I> DFALearner<I> createLearnLibLearner(Alphabet<I> alphabet, MembershipOracle<I, Boolean> oracle);
		public abstract <I> LibalfActiveDFALearner<I> createLibalfLearner(Alphabet<I> alphabet, MembershipOracle<I, Boolean> oracle);
	}
	
	private static final Map<String,Learner> learners = new HashMap<>();
	
	private static void addLearner(Learner learner) {
		learners.put(learner.getName(), learner);
	}
	
	public static Collection<Learner> getLearners() {
		return Collections.unmodifiableCollection(learners.values());
	}
	
	public static Collection<Learner> getLearners(Collection<? extends String> names) {
		if (names == null) {
			return getLearners();
		}
		return names.stream().distinct()
				.map(learners::get)
				.filter(x -> x != null)
				.collect(Collectors.toList());
	}
	
	
	static {
		addLearner(new Learner("angluin-classic", 0) {
			@Override
			public <I> DFALearner<I> createLearnLibLearner(
					Alphabet<I> alphabet, MembershipOracle<I, Boolean> oracle) {
				return new ExtensibleLStarDFA<>(alphabet, oracle, Collections.emptyList(),
						ObservationTableCEXHandlers.CLASSIC_LSTAR,
						ClosingStrategies.CLOSE_FIRST);
			}

			@Override
			public <I> LibalfActiveDFALearner<I> createLibalfLearner(Alphabet<I> alphabet,
					MembershipOracle<I, Boolean> oracle) {
				return new LibalfAngluinSimpleDFA<>(alphabet, oracle);
			}
		});
		addLearner(new Learner("angluin-col", 1) {
			@Override
			public <I> DFALearner<I> createLearnLibLearner(
					Alphabet<I> alphabet, MembershipOracle<I, Boolean> oracle) {
				return new ExtensibleLStarDFA<>(alphabet, oracle, Collections.emptyList(),
						ObservationTableCEXHandlers.MALER_PNUELI,
						ClosingStrategies.CLOSE_FIRST);
			}

			@Override
			public <I> LibalfActiveDFALearner<I> createLibalfLearner(Alphabet<I> alphabet,
					MembershipOracle<I, Boolean> oracle) {
				return new LibalfAngluinColDFA<>(alphabet, oracle);
			}
		});
		addLearner(new Learner("rivest-schapire", 2) {
			@Override
			public <I> DFALearner<I> createLearnLibLearner(
					Alphabet<I> alphabet, MembershipOracle<I, Boolean> oracle) {
				return new ExtensibleLStarDFA<>(alphabet, oracle, Collections.emptyList(),
						ObservationTableCEXHandlers.RIVEST_SCHAPIRE,
						ClosingStrategies.CLOSE_FIRST);
			}

			@Override
			public <I> LibalfActiveDFALearner<I> createLibalfLearner(Alphabet<I> alphabet,
					MembershipOracle<I, Boolean> oracle) {
				return new LibalfRSDFA<>(alphabet, oracle);
			}
		});
		addLearner(new Learner("kearns-vazirani", 3) {
			@Override
			public <I> DFALearner<I> createLearnLibLearner(
					Alphabet<I> alphabet, MembershipOracle<I, Boolean> oracle) {
				return new KearnsVaziraniDFA<>(alphabet, oracle, true, AcexAnalyzers.LINEAR_FWD);
			}

			@Override
			public <I> LibalfActiveDFALearner<I> createLibalfLearner(Alphabet<I> alphabet,
					MembershipOracle<I, Boolean> oracle) {
				return new LibalfKVDFA<>(alphabet, oracle, false);
			}
		});
		addLearner(new Learner("kearns-vazirani-binsearch", 4) {
			@Override
			public <I> DFALearner<I> createLearnLibLearner(
					Alphabet<I> alphabet, MembershipOracle<I, Boolean> oracle) {
				return new KearnsVaziraniDFA<>(alphabet, oracle, true, AcexAnalyzers.BINARY_SEARCH);
			}

			@Override
			public <I> LibalfActiveDFALearner<I> createLibalfLearner(Alphabet<I> alphabet,
					MembershipOracle<I, Boolean> oracle) {
				return new LibalfKVDFA<>(alphabet, oracle, true);
			}
		});
	}

}
