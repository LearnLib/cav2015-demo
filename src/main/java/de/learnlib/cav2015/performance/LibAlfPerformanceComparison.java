package de.learnlib.cav2015.performance;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.automatalib.automata.fsa.DFA;
import net.automatalib.util.automata.Automata;
import net.automatalib.words.Alphabet;
import net.automatalib.words.Word;
import de.learnlib.acex.analyzers.AcexAnalyzers;
import de.learnlib.algorithms.lstargeneric.ce.ObservationTableCEXHandlers;
import de.learnlib.algorithms.lstargeneric.closing.ClosingStrategies;
import de.learnlib.algorithms.lstargeneric.dfa.ExtensibleLStarDFA;
import de.learnlib.api.LearningAlgorithm;
import de.learnlib.api.LearningAlgorithm.DFALearner;
import de.learnlib.api.MembershipOracle;
import de.learnlib.cache.dfa.DFACaches;
import de.learnlib.examples.LearningExample.DFALearningExample;
import de.learnlib.examples.dfa.DFABenchmarks;
import de.learnlib.examples.dfa.ExampleKeylock;
import de.learnlib.examples.dfa.ExampleRandomDFA;
import de.learnlib.oracles.CounterOracle.DFACounterOracle;
import de.learnlib.oracles.DefaultQuery;
import de.learnlib.oracles.SimulatorOracle;

public class LibAlfPerformanceComparison {
	
	public static abstract class NamedLearnerPair {
		private final String name;
		
		public NamedLearnerPair(String name) {
			this.name = name;
		}
		
		public String getName() {
			return name;
		}
		
		public abstract <I> DFALearner<I> createLearnLibLearner(Alphabet<I> alphabet, MembershipOracle<I, Boolean> oracle);
		public abstract <I> DFALearner<I> createLibalfLearner(Alphabet<I> alphabet, MembershipOracle<I, Boolean> oracle);
	}
	
	private static final Map<String,DFALearningExample<?>> dfaExamples;
	private static final List<NamedLearnerPair> learners;
	
	
	static {
		dfaExamples = new LinkedHashMap<>();
		//dfaExamples.put("pots2", DFABenchmarks.loadPots2());
		//dfaExamples.put("pots3", DFABenchmarks.loadPots3());
		int[] alphabetSizes = { 5, 10, 20 };
		int[] stateCounts = { 100, 500 };
		for (int alphabetSize : alphabetSizes) {
			for (int stateCount : stateCounts) {
				dfaExamples.put("random-" + alphabetSize + "-" + stateCount, new ExampleRandomDFA(alphabetSize, stateCount));
			}
		}
		dfaExamples.put("peterson2", DFABenchmarks.loadPeterson2());
		//dfaExamples.put("peterson3", DFABenchmarks.loadPeterson3());
		alphabetSizes = new int[]{ 5, 10 };
		stateCounts = new int[]{64, 128};
		for (int alphabetSize : alphabetSizes) {
			for (int stateCount : stateCounts) {
				dfaExamples.put("keylock-" + alphabetSize + "-" + stateCount, new ExampleKeylock(stateCount, false, alphabetSize - 1));
				dfaExamples.put("keylockc-" + alphabetSize + "-" + stateCount, new ExampleKeylock(stateCount, true, alphabetSize - 1));
			}
		}

//		dfaExamples.put("keylock-10-256", new ExampleKeylock(256, false, 9));
//		dfaExamples.put("keylockc-10-256", new ExampleKeylock(256, true, 9));
		
		learners = new ArrayList<>();
		learners.add(new NamedLearnerPair("angluin-classic") {
			@Override
			public <I> DFALearner<I> createLearnLibLearner(
					Alphabet<I> alphabet, MembershipOracle<I, Boolean> oracle) {
				return new ExtensibleLStarDFA<>(alphabet, oracle, Collections.emptyList(),
						ObservationTableCEXHandlers.CLASSIC_LSTAR,
						ClosingStrategies.CLOSE_FIRST);
			}

			@Override
			public <I> DFALearner<I> createLibalfLearner(Alphabet<I> alphabet,
					MembershipOracle<I, Boolean> oracle) {
				return new LibalfAngluinSimpleDFA<>(alphabet, oracle);
			}
		});
		learners.add(new NamedLearnerPair("angluin-col") {
			@Override
			public <I> DFALearner<I> createLearnLibLearner(
					Alphabet<I> alphabet, MembershipOracle<I, Boolean> oracle) {
				return new ExtensibleLStarDFA<>(alphabet, oracle, Collections.emptyList(),
						ObservationTableCEXHandlers.MALER_PNUELI,
						ClosingStrategies.CLOSE_FIRST);
			}

			@Override
			public <I> DFALearner<I> createLibalfLearner(Alphabet<I> alphabet,
					MembershipOracle<I, Boolean> oracle) {
				return new LibalfAngluinColDFA<>(alphabet, oracle);
			}
		});
		learners.add(new NamedLearnerPair("rivest-schapire") {
			@Override
			public <I> DFALearner<I> createLearnLibLearner(
					Alphabet<I> alphabet, MembershipOracle<I, Boolean> oracle) {
				return new ExtensibleLStarDFA<>(alphabet, oracle, Collections.emptyList(),
						ObservationTableCEXHandlers.RIVEST_SCHAPIRE,
						ClosingStrategies.CLOSE_FIRST);
			}

			@Override
			public <I> DFALearner<I> createLibalfLearner(Alphabet<I> alphabet,
					MembershipOracle<I, Boolean> oracle) {
				return new LibalfRSDFA<>(alphabet, oracle);
			}
		});
//		learners.add(new NamedLearnerPair("nlstar") {
//			@Override
//			public <I> DFALearner<I> createLearnLibLearner(
//					Alphabet<I> alphabet, MembershipOracle<I, Boolean> oracle) {
//				return new NLStarLearner<>(alphabet, oracle).asDFALearner();
//			}
//
//			@Override
//			public <I> DFALearner<I> createLibalfLearner(Alphabet<I> alphabet,
//					MembershipOracle<I, Boolean> oracle) {
//				return new LibalfNLStar<>(alphabet, oracle).asDFALearner();
//			}
//		});
		learners.add(new NamedLearnerPair("kearns-vazirani") {
			@Override
			public <I> DFALearner<I> createLearnLibLearner(
					Alphabet<I> alphabet, MembershipOracle<I, Boolean> oracle) {
				return new KearnsVaziraniDFA<>(alphabet, oracle, true, AcexAnalyzers.LINEAR_FWD);
			}

			@Override
			public <I> DFALearner<I> createLibalfLearner(Alphabet<I> alphabet,
					MembershipOracle<I, Boolean> oracle) {
				return new LibalfKVDFA<>(alphabet, oracle, false);
			}
		});
		learners.add(new NamedLearnerPair("kearns-vazirani-binsearch") {
			@Override
			public <I> DFALearner<I> createLearnLibLearner(
					Alphabet<I> alphabet, MembershipOracle<I, Boolean> oracle) {
				return new KearnsVaziraniDFA<>(alphabet, oracle, true, AcexAnalyzers.BINARY_SEARCH);
			}

			@Override
			public <I> DFALearner<I> createLibalfLearner(Alphabet<I> alphabet,
					MembershipOracle<I, Boolean> oracle) {
				return new LibalfKVDFA<>(alphabet, oracle, true);
			}
		});
	}
	
	public static <I> long runLearner(DFALearningExample<I> example, LearningAlgorithm<DFA<?,I>, I, Boolean> learner) {
		DFA<?,I> target = example.getReferenceAutomaton();
		Alphabet<I> alphabet = example.getAlphabet();
		
		long learnerNanos = 0L;
		long start = System.nanoTime();
		learner.startLearning();
		learnerNanos += System.nanoTime() - start;
		
		DFA<?,I> hyp = learner.getHypothesisModel();
		
		Word<I> ceWord;
		
		while ((ceWord = Automata.findSeparatingWord(target, hyp, alphabet)) != null) {
			DefaultQuery<I, Boolean> ceQuery = new DefaultQuery<>(ceWord, target.computeOutput(ceWord));
			start = System.nanoTime();
			learner.refineHypothesis(ceQuery);
			learnerNanos += System.nanoTime() - start;
			hyp = learner.getHypothesisModel();
		}
		
		return learnerNanos / 1000000L;
	}
	
	public static <I> void runExample(String exampleName, DFALearningExample<I> example, PrintWriter out) {
		
		DFA<?,I> target = example.getReferenceAutomaton();
		Alphabet<I> alphabet = example.getAlphabet();
	
		System.out.println("Running example " + exampleName + "(" + alphabet.size() + "/" + target.size() + ")");
		
		MembershipOracle.DFAMembershipOracle<I> directOracle
			= new SimulatorOracle.DFASimulatorOracle<>(target);
		
		int learnerId = 0;
		for (NamedLearnerPair learnerPair : learners) {
			String learnerName = learnerPair.getName();
			
			long totalTimeLearnlib = 0L;
			long totalTimeLibalf = 0L;
			long totalQueriesLearnlib = 0L;
			long totalQueriesLibalf = 0L;
			
			for (int i = 0; i < 10; i++) {			
				System.out.print(learnerName + " #" + i + " ... ");
				System.out.flush();

				DFACounterOracle<I> directCounterOracle = new DFACounterOracle<I>(directOracle, "MQs");
				
				MembershipOracle.DFAMembershipOracle<I> cacheOracle
					= DFACaches.createTreeCache(alphabet, directCounterOracle);
				

				DFACounterOracle<I> libalfCounterOracle = new DFACounterOracle<I>(directOracle, "MQs");
				
				
				System.gc();
				DFALearner<I> learnlibLearner = learnerPair.createLearnLibLearner(alphabet, cacheOracle);
				long learnLibMs = runLearner(example, learnlibLearner);
				System.out.print(learnLibMs + "ms (" + directCounterOracle.getCount() + "MQs)");
				System.out.flush();
				System.gc();
				totalTimeLearnlib += learnLibMs;
				totalQueriesLearnlib += directCounterOracle.getCount();
				
				DFALearner<I> libalfLearner = learnerPair.createLibalfLearner(alphabet, libalfCounterOracle);
				long libalfMs = -1;
				if (libalfLearner != null) {
					libalfMs = runLearner(example, libalfLearner);
				}
				System.out.println(" / " + libalfMs + "ms (" + libalfCounterOracle.getCount() + " MQs)");
				totalTimeLibalf += libalfMs;
				totalQueriesLibalf += libalfCounterOracle.getCount();
				
			}
			
			out.printf("%s %d %d %d %d %d\n", learnerName, learnerId, totalTimeLearnlib / 10L, totalQueriesLearnlib / 10L, totalTimeLibalf / 10L, totalQueriesLibalf / 10L);
			out.flush();
			learnerId++;
		}
		
		
	}
	
	public static void main(String[] args) throws IOException {
		File f = new File("results-libalf");
		f.mkdirs();
		for (Map.Entry<String,DFALearningExample<?>> example : dfaExamples.entrySet()) {
			String name = example.getKey();
			File resultsFile = new File(f, name + ".dat");
			try (PrintWriter pw = new PrintWriter(resultsFile)) { 
				runExample(example.getKey(), example.getValue(), pw);
				System.out.println();
			}
		}
	}
}
