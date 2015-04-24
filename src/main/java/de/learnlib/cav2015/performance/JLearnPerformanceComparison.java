package de.learnlib.cav2015.performance;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.automatalib.automata.transout.MealyMachine;
import net.automatalib.util.automata.Automata;
import net.automatalib.words.Alphabet;
import net.automatalib.words.Word;
import net.automatalib.words.impl.Alphabets;

import com.misberner.clitools.api.CLITool;

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

public class JLearnPerformanceComparison implements CLITool {
	
	@Override
	public String getName() {
		return "perfcomp-jlearn";
	}
	
	@Override
	public String getDescription() {
		return "Performance comparison with the old LearnLib (JLearn)";
	}

	private static final Map<String,MealyLearningExample<?,?>> mealyExamples;
		
	static {
		Alphabet<Integer> a5 = Alphabets.integers(0, 4);
		Alphabet<Integer> a10 = Alphabets.integers(0, 9);
		Alphabet<Integer> a20 = Alphabets.integers(0, 19);
		mealyExamples = new LinkedHashMap<>();
		mealyExamples.put("random-5-100", new ExampleRandomMealy<>(a5, 100, false, true));
		mealyExamples.put("random-5-500", new ExampleRandomMealy<>(a5, 500, false, true));
		mealyExamples.put("random-10-100", new ExampleRandomMealy<>(a10, 100, false, true));
		mealyExamples.put("random-10-500", new ExampleRandomMealy<>(a10, 500, false, true));
		mealyExamples.put("random-20-100", new ExampleRandomMealy<>(a20, 100, false, true));
		mealyExamples.put("random-20-500", new ExampleRandomMealy<>(a20, 500, false, true));
		mealyExamples.put("peterson2", DFA2Mealy.ex2mealy(DFABenchmarks.loadPeterson2()));
		mealyExamples.put("keylock-5-64", DFA2Mealy.ex2mealy(new ExampleKeylock(64, false, 4)));
		mealyExamples.put("keylockc-5-64", DFA2Mealy.ex2mealy(new ExampleKeylock(64, true, 4)));
		mealyExamples.put("keylock-10-64", DFA2Mealy.ex2mealy(new ExampleKeylock(64, false, 9)));
		mealyExamples.put("keylockc-10-64", DFA2Mealy.ex2mealy(new ExampleKeylock(64, true, 9)));
		mealyExamples.put("keylock-5-128", DFA2Mealy.ex2mealy(new ExampleKeylock(128, false, 4)));
		mealyExamples.put("keylockc-5-128", DFA2Mealy.ex2mealy(new ExampleKeylock(128, true, 4)));
		mealyExamples.put("keylock-10-128", DFA2Mealy.ex2mealy(new ExampleKeylock(128, false, 9)));
		mealyExamples.put("keylockc-10-128", DFA2Mealy.ex2mealy(new ExampleKeylock(128, true, 9)));
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
		out.println("# " + exampleName + ", inputs: " + alphabet.size() + ", states: " + target.size());
		
		MembershipOracle.MealyMembershipOracle<I,O> directOracle
			= new SimulatorOracle.MealySimulatorOracle<>(target);
		
		int learnerId = 0;
		for (NamedLearnerPair learnerPair : learners) {
			String learnerName = learnerPair.getName();
			
			long totalTimeLearnLib = 0L;
			long totalQueriesLearnlib = 0L;
			long totalTimeJLearn = 0L;
			long totalQueriesJLearn = 0L;
			
			for (int i = 0; i < 10; i++) {			
				System.out.print(learnerName + " #" + i + " ... ");
				System.out.flush();

				MealyCounterOracle<I,O> directCounterOracle = new MealyCounterOracle<>(directOracle, "MQs");
				
				MembershipOracle.MealyMembershipOracle<I,O> cacheOracle
					= MealyCaches.createTreeCache(alphabet, directCounterOracle);
				

				MealyCounterOracle<I,O> jlearnCounterOracle = new MealyCounterOracle<>(directOracle, "MQs");
				MembershipOracle.MealyMembershipOracle<I, O> jlearnCacheOracle
					= MealyCaches.createTreeCache(alphabet, jlearnCounterOracle);
				
				
				System.gc();
				MealyLearner<I,O> learnlibLearner = learnerPair.createLearnLibLearner(alphabet, cacheOracle);
				long learnLibMs = runLearner(example, learnlibLearner);
				System.out.print(learnLibMs + "ms (" + directCounterOracle.getCount() + "MQs)");
				System.out.flush();
				System.gc();
				totalTimeLearnLib += learnLibMs;
				totalQueriesLearnlib += directCounterOracle.getCount();
				
				MealyLearner<I,O> jlearnLearner = learnerPair.createJLearnLearner(alphabet, jlearnCacheOracle);
				long jlearnMs = runLearner(example, jlearnLearner);
				System.out.println(" / " + jlearnMs + "ms (" + jlearnCounterOracle.getCount() + " MQs)");
				totalTimeJLearn += jlearnMs;
				totalQueriesJLearn += jlearnCounterOracle.getCount();
			}
			
			out.printf("%s %d %d %d %d %d\n", learnerName, learnerId, totalTimeLearnLib / 10L, totalQueriesLearnlib / 10L, totalTimeJLearn / 10L, totalQueriesJLearn / 10L);
			out.flush();
			learnerId++;
		}
		
		
	}
	
	public boolean runMain(String[] args) throws IOException {
		
		File f = new File("results-jlearn");
		f.mkdirs();
		for (Map.Entry<String,MealyLearningExample<?,?>> example : mealyExamples.entrySet()) {
			String name = example.getKey();
			File datFile = new File(f, name + ".dat");
			try (PrintWriter pw = new PrintWriter(datFile)) {
				runExample(name, example.getValue(), pw);
				System.out.println();
			}
		}
		return true;
	}

}
