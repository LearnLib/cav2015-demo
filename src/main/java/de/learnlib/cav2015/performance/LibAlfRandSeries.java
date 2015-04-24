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
import de.learnlib.algorithms.kv.dfa.KearnsVaziraniDFA;
import de.learnlib.algorithms.lstargeneric.ce.ObservationTableCEXHandlers;
import de.learnlib.algorithms.lstargeneric.closing.ClosingStrategies;
import de.learnlib.algorithms.lstargeneric.dfa.ExtensibleLStarDFA;
import de.learnlib.algorithms.nlstar.NLStarLearner;
import de.learnlib.api.LearningAlgorithm;
import de.learnlib.api.LearningAlgorithm.DFALearner;
import de.learnlib.api.MembershipOracle;
import de.learnlib.cache.dfa.DFACaches;
import de.learnlib.examples.LearningExample.DFALearningExample;
import de.learnlib.examples.dfa.DFABenchmarks;
import de.learnlib.examples.dfa.ExampleKeylock;
import de.learnlib.examples.dfa.ExampleRandomDFA;
import de.learnlib.libalf.LibalfAngluinColDFA;
import de.learnlib.libalf.LibalfAngluinSimpleDFA;
import de.learnlib.libalf.LibalfKVDFA;
import de.learnlib.libalf.LibalfNLStar;
import de.learnlib.libalf.LibalfRSDFA;
import de.learnlib.oracles.CounterOracle.DFACounterOracle;
import de.learnlib.oracles.DefaultQuery;
import de.learnlib.oracles.SimulatorOracle;

public class LibAlfRandSeries {
	
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
		dfaExamples.put("random-5-100", new ExampleRandomDFA(5, 100));
		dfaExamples.put("random-5-500", new ExampleRandomDFA(5, 500));
		dfaExamples.put("random-10-100", new ExampleRandomDFA(10, 100));
		dfaExamples.put("random-10-500", new ExampleRandomDFA(10, 500));
		dfaExamples.put("random-20-100", new ExampleRandomDFA(20, 100));
		dfaExamples.put("random-20-500", new ExampleRandomDFA(20, 500));
		dfaExamples.put("peterson2", DFABenchmarks.loadPeterson2());
		//dfaExamples.put("peterson3", DFABenchmarks.loadPeterson3());
		dfaExamples.put("keylock-5-64", new ExampleKeylock(64, false, 4));
		dfaExamples.put("keylockc-5-64", new ExampleKeylock(64, true, 4));
		dfaExamples.put("keylock-10-64", new ExampleKeylock(64, false, 9));
		dfaExamples.put("keylockc-10-64", new ExampleKeylock(64, true, 9));
		dfaExamples.put("keylock-5-128", new ExampleKeylock(128, false, 4));
		dfaExamples.put("keylockc-5-128", new ExampleKeylock(128, true, 4));
		dfaExamples.put("keylock-10-128", new ExampleKeylock(128, false, 9));
		dfaExamples.put("keylockc-10-128", new ExampleKeylock(128, true, 9));
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
		
		out.print(target.size());
		out.print(' ');
		
		for (NamedLearnerPair learnerPair : learners) {
			String learnerName = learnerPair.getName();

			System.out.print(learnerName + " ... ");
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
			
			out.print(learnLibMs);
			out.print(' ');
			DFALearner<I> libalfLearner = learnerPair.createLibalfLearner(alphabet, libalfCounterOracle);
			long libalfMs = -1;
			if (libalfLearner != null) {
				libalfMs = runLearner(example, libalfLearner);
			}
			System.out.println(" / " + libalfMs + "ms (" + libalfCounterOracle.getCount() + " MQs)");
				
			out.print(libalfMs);
			out.print(' ');
		}
		
		out.println();
		out.flush();
	}
	
	public static void main(String[] args) throws IOException {
		int[] alphabetSizes = { 2, 10, 100 };
		for (int alphabetSize : alphabetSizes) {
			try (PrintWriter pw = new PrintWriter(new File("comp-" + alphabetSize + ".dat"))) {
				for (int numStates = 10; numStates < 1000; numStates += 10) {
					DFALearningExample<Integer> example = new ExampleRandomDFA(alphabetSize, numStates);
					runExample("random-" + alphabetSize + "-" + numStates, example, pw);
				}
			}
		}
	}
}
