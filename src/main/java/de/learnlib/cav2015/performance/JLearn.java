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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import net.automatalib.words.Alphabet;
import net.automatalib.words.Word;
import de.learnlib.algorithms.dhc.mealy.MealyDHC;
import de.learnlib.algorithms.discriminationtree.mealy.DTLearnerMealy;
import de.learnlib.algorithms.lstargeneric.ce.ObservationTableCEXHandlers;
import de.learnlib.algorithms.lstargeneric.closing.ClosingStrategies;
import de.learnlib.algorithms.lstargeneric.mealy.ClassicLStarMealy;
import de.learnlib.algorithms.malerpnueli.MalerPnueliMealy;
import de.learnlib.algorithms.rivestschapire.RivestSchapireMealy;
import de.learnlib.api.LearningAlgorithm.MealyLearner;
import de.learnlib.api.MembershipOracle;
import de.learnlib.counterexamples.GlobalSuffixFinders;
import de.learnlib.counterexamples.LocalSuffixFinders;
import de.learnlib.jlearn.JLearnAngluinMealy;
import de.learnlib.jlearn.JLearnDHCMealy;
import de.learnlib.jlearn.JLearnObservationPackMealy;
import de.learnlib.jlearn.JLearnSplitterCreator;
import de.learnlib.mealy.MealyUtil;
import de.ls5.jlearn.algorithms.dhc.DHC;

public class JLearn {
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
		
		public abstract <I,O> MealyLearner<I,O> createLearnLibLearner(Alphabet<I> alphabet, MembershipOracle<I, Word<O>> oracle);
		public abstract <I,O> MealyLearner<I,O> createJLearnLearner(Alphabet<I> alphabet, MembershipOracle<I, Word<O>> oracle);
	}
	
	private static final Map<String,Learner> learners = new HashMap<>();
	
	public static Collection<Learner> getLearners() {
		return Collections.unmodifiableCollection(learners.values());
	}
	
	public static Collection<Learner> getLearners(Collection<? extends String> learnerNames) {
		if (learnerNames == null) {
			return getLearners();
		}
		return learnerNames.stream().distinct()
				.map(learners::get)
				.filter(x -> x != null)
				.collect(Collectors.toList());
	}
	
	
	
	private static <I> List<Word<I>> alphabet2words(Alphabet<I> alphabet) {
		List<Word<I>> result = new ArrayList<>(alphabet.size());
		for (I sym : alphabet) {
			result.add(Word.fromLetter(sym));
		}
		return result;
	}
	
	private static void addLearner(Learner learner) {
		learners.put(learner.getName(), learner);
	}
	
	
	// note: this needs to be a class-level variable, or else it will be
	// garbage collected and the settings will be reverted
	private static final Logger dhcLogger;
	
	static {
		dhcLogger = Logger.getLogger(DHC.class.getName());
		dhcLogger.setLevel(Level.OFF);
		
		addLearner(new Learner("angluin-classic", 0) {
			@Override
			public <I, O> MealyLearner<I, O> createLearnLibLearner(
					Alphabet<I> alphabet, MembershipOracle<I, Word<O>> oracle) {
				return MealyUtil.wrapSymbolLearner(new ClassicLStarMealy<>(alphabet, MealyUtil.wrapWordOracle(oracle), Collections.emptyList(),
						ObservationTableCEXHandlers.CLASSIC_LSTAR, ClosingStrategies.CLOSE_FIRST));
			}
			@Override
			public <I, O> MealyLearner<I, O> createJLearnLearner(
					Alphabet<I> alphabet, MembershipOracle<I, Word<O>> oracle) {
				return new JLearnAngluinMealy<>(alphabet, oracle);
			}
		});
		addLearner(new Learner("maler-pnueli", 1) {
			@Override
			public <I, O> MealyLearner<I, O> createLearnLibLearner(
					Alphabet<I> alphabet, MembershipOracle<I, Word<O>> oracle) {
				return new MalerPnueliMealy<>(alphabet, oracle, alphabet2words(alphabet), ClosingStrategies.CLOSE_FIRST);
			}
			@Override
			public <I, O> MealyLearner<I, O> createJLearnLearner(
					Alphabet<I> alphabet, MembershipOracle<I, Word<O>> oracle) {
				return new JLearnAngluinMealy<>(alphabet, oracle, JLearnSplitterCreator.MALER_STYLE);
			}
		});
		addLearner(new Learner("rivest-schapire", 2) {
			@Override
			public <I, O> MealyLearner<I, O> createLearnLibLearner(
					Alphabet<I> alphabet, MembershipOracle<I, Word<O>> oracle) {
				return new RivestSchapireMealy<>(alphabet, oracle, alphabet2words(alphabet), ClosingStrategies.CLOSE_FIRST);
			}
			@Override
			public <I, O> MealyLearner<I, O> createJLearnLearner(
					Alphabet<I> alphabet, MembershipOracle<I, Word<O>> oracle) {
				return new JLearnAngluinMealy<>(alphabet, oracle, JLearnSplitterCreator.RIVEST_STYLE);
			}
		});
		addLearner(new Learner("dt", 4) {
			@Override
			public <I, O> MealyLearner<I, O> createLearnLibLearner(
					Alphabet<I> alphabet, MembershipOracle<I, Word<O>> oracle) {
				return new DTLearnerMealy<>(alphabet, oracle, LocalSuffixFinders.RIVEST_SCHAPIRE, true);
			}
			@Override
			public <I, O> MealyLearner<I, O> createJLearnLearner(
					Alphabet<I> alphabet, MembershipOracle<I, Word<O>> oracle) {
				return new JLearnObservationPackMealy<>(alphabet, oracle, JLearnSplitterCreator.RIVEST_STYLE);
			}
		});
		addLearner(new Learner("dhc", 3) {
			@Override
			public <I, O> MealyLearner<I, O> createLearnLibLearner(
					Alphabet<I> alphabet, MembershipOracle<I, Word<O>> oracle) {
				return new MealyDHC<>(alphabet, oracle, GlobalSuffixFinders.RIVEST_SCHAPIRE, alphabet2words(alphabet));
			}
			@Override
			public <I, O> MealyLearner<I, O> createJLearnLearner(
					Alphabet<I> alphabet, MembershipOracle<I, Word<O>> oracle) {
				return new JLearnDHCMealy<>(alphabet, oracle, JLearnSplitterCreator.RIVEST_STYLE);
			}
		});
	}
}
