package de.learnlib.cav2015.performance;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.automatalib.automata.transout.MealyMachine;
import net.automatalib.util.automata.Automata;
import net.automatalib.words.Alphabet;
import net.automatalib.words.Word;
import net.automatalib.words.impl.Alphabets;
import de.learnlib.algorithms.dhc.mealy.MealyDHC;
import de.learnlib.algorithms.discriminationtree.mealy.DTLearnerMealy;
import de.learnlib.algorithms.lstargeneric.ce.ObservationTableCEXHandlers;
import de.learnlib.algorithms.lstargeneric.closing.ClosingStrategies;
import de.learnlib.algorithms.lstargeneric.mealy.ClassicLStarMealy;
import de.learnlib.algorithms.malerpnueli.MalerPnueliMealy;
import de.learnlib.algorithms.rivestschapire.RivestSchapireMealy;
import de.learnlib.api.LearningAlgorithm;
import de.learnlib.api.LearningAlgorithm.MealyLearner;
import de.learnlib.api.MembershipOracle;
import de.learnlib.api.Query;
import de.learnlib.cache.mealy.MealyCaches;
import de.learnlib.counterexamples.GlobalSuffixFinders;
import de.learnlib.counterexamples.LocalSuffixFinders;
import de.learnlib.examples.LearningExample.MealyLearningExample;
import de.learnlib.examples.dfa.DFABenchmarks;
import de.learnlib.examples.dfa.ExampleKeylock;
import de.learnlib.examples.mealy.ExampleRandomMealy;
import de.learnlib.jlearn.JLearnAngluinMealy;
import de.learnlib.jlearn.JLearnDHCMealy;
import de.learnlib.jlearn.JLearnObservationPackMealy;
import de.learnlib.jlearn.JLearnSplitterCreator;
import de.learnlib.mealy.MealyUtil;
import de.learnlib.oracles.CounterOracle.MealyCounterOracle;
import de.learnlib.oracles.DefaultQuery;
import de.learnlib.oracles.SimulatorOracle;

public class JLearnRandSeries {
	
	public static abstract class NamedLearnerPair {
		private final String name;
		
		public NamedLearnerPair(String name) {
			this.name = name;
		}
		
		public String getName() {
			return name;
		}
		
		public abstract <I,O> MealyLearner<I,O> createLearnLibLearner(Alphabet<I> alphabet, MembershipOracle<I, Word<O>> oracle);
		public abstract <I,O> MealyLearner<I,O> createJLearnLearner(Alphabet<I> alphabet, MembershipOracle<I, Word<O>> oracle);
	}
	
	public static class SeqOracle<I,D> implements MembershipOracle<I, D> {
		private final MembershipOracle<I, D> delegate;
		
		public SeqOracle(MembershipOracle<I, D> delegate) {
			this.delegate = delegate;
		}

		@Override
		public void processQueries(Collection<? extends Query<I, D>> queries) {
			List<Query<I,D>> queryList = new ArrayList<>(1);
			queryList.add(null);
			for (Query<I,D> q : queries) {
				queryList.set(0, q);
				delegate.processQueries(queryList);
			}
		}
	}
	
	
	public static <I,O> long runLearner(MealyLearningExample<I,O> example, LearningAlgorithm<MealyMachine<?,I,?,O>, I, Word<O>> learner) {
		MealyMachine<?,I,?,O> target = example.getReferenceAutomaton();
		Alphabet<I> alphabet = example.getAlphabet();
		
		long learnerNanos = 0L;
		long start = System.nanoTime();
		learner.startLearning();
		learnerNanos += System.nanoTime() - start;
		
		MealyMachine<?,I,?,O> hyp = learner.getHypothesisModel();
		
		Word<I> ceWord;
		
		while ((ceWord = Automata.findSeparatingWord(target, hyp, alphabet)) != null) {
			DefaultQuery<I, Word<O>> ceQuery = new DefaultQuery<>(ceWord, target.computeOutput(ceWord));
			start = System.nanoTime();
			learner.refineHypothesis(ceQuery);
			learnerNanos += System.nanoTime() - start;
			hyp = learner.getHypothesisModel();
		}
		
		return learnerNanos / 1000000L;
	}
	
	public static <I,O> void runExample(String exampleName, MealyLearningExample<I,O> example, PrintWriter out) {
		
		MealyMachine<?,I,?,O> target = example.getReferenceAutomaton();
		Alphabet<I> alphabet = example.getAlphabet();
	
		System.out.println("Running example " + exampleName + "(" + alphabet.size() + "/" + target.size() + ")");
		
		MembershipOracle.MealyMembershipOracle<I,O> directOracle
			= new SimulatorOracle.MealySimulatorOracle<>(target);
		
		out.print(target.size());
		out.print(' ');
		
		for (NamedLearnerPair learnerPair : learners) {
			String learnerName = learnerPair.getName();

			System.out.print(learnerName + " ... ");
			System.out.flush();

			MealyCounterOracle<I,O> directCounterOracle = new MealyCounterOracle<>(directOracle, "MQs");
				
//			MembershipOracle.MealyMembershipOracle<I,O> cacheOracle
//				= MealyCaches.createTreeCache(alphabet, directCounterOracle);
			
			MealyCounterOracle<I,O> jlearnCounterOracle = new MealyCounterOracle<>(directOracle, "MQs");
			
//			MembershipOracle.MealyMembershipOracle<I,O> jlearnCacheOracle
//				= MealyCaches.createTreeCache(alphabet, jlearnCounterOracle);
				
				
			System.gc();
			MealyLearner<I,O> learnlibLearner = learnerPair.createLearnLibLearner(alphabet, directCounterOracle);
			long learnLibMs = runLearner(example, learnlibLearner);
			System.out.print(learnLibMs + "ms (" + directCounterOracle.getCount() + "MQs)");
			System.out.flush();
			System.gc();
			
			out.print(learnLibMs);
			out.print(' ');
			MealyLearner<I,O> libalfLearner = learnerPair.createJLearnLearner(alphabet, jlearnCounterOracle);
			long libalfMs = -1;
			if (libalfLearner != null) {
				libalfMs = runLearner(example, libalfLearner);
			}
			System.out.println(" / " + libalfMs + "ms (" + jlearnCounterOracle.getCount() + " MQs)");
				
			out.print(libalfMs);
			out.print(' ');
		}
		
		out.println();
		out.flush();
	}
	
	private static int[] parseInts(String intsStr) {
		String[] intStrs = intsStr.split("\\s*,\\s*");
		int[] ints = new int[intStrs.length];
	}
	
	
	public boolean runMain(String[] args) throws IOException {
		int[] alphabetSizes = { 2, 10, 100 };
		for (int alphabetSize : alphabetSizes) {
			try (PrintWriter pw = new PrintWriter(new File("jlearn-comp-nocache-rerun-" + alphabetSize + ".dat"))) {
				int[] stateCounts = { 680, 250, 340, 520, 600 };
				for (int numStates : stateCounts) {
					MealyLearningExample<Integer,Boolean> example = new ExampleRandomMealy<>(Alphabets.integers(0, alphabetSize - 1), numStates, false, true);
					runExample("random-" + alphabetSize + "-" + numStates, example, pw);
				}
			}
		}
	}
}
