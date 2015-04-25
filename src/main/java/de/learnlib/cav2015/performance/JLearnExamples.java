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

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.LongSummaryStatistics;
import java.util.Map;

import net.automatalib.automata.transout.MealyMachine;
import net.automatalib.util.automata.Automata;
import net.automatalib.words.Alphabet;
import net.automatalib.words.Word;
import net.automatalib.words.impl.Alphabets;

import com.misberner.clitools.api.CLITool;

import de.learnlib.api.LearningAlgorithm;
import de.learnlib.api.LearningAlgorithm.MealyLearner;
import de.learnlib.api.MembershipOracle;
import de.learnlib.cache.mealy.MealyCaches;
import de.learnlib.examples.LearningExample.MealyLearningExample;
import de.learnlib.examples.dfa.DFABenchmarks;
import de.learnlib.examples.dfa.ExampleKeylock;
import de.learnlib.examples.mealy.ExampleRandomMealy;
import de.learnlib.oracles.CounterOracle.MealyCounterOracle;
import de.learnlib.oracles.DefaultQuery;
import de.learnlib.oracles.SimulatorOracle;

public class JLearnExamples implements CLITool {
	
	@Override
	public String getName() {
		return "jlearn-examples";
	}
	
	@Override
	public String getDescription() {
		return "Compares the performance of LearnLib and JLearn on a series of examples";
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
	

	public static <I,O> void runExample(String exampleName, MealyLearningExample<I,O> example,
			Collection<? extends JLearn.Learner> learners, int repeatCount, PrintWriter out) {
		
		MealyMachine<?,I,?,O> target = example.getReferenceAutomaton();
		Alphabet<I> alphabet = example.getAlphabet();
	
		System.out.println("Running example " + exampleName + "(" + alphabet.size() + "/" + target.size() + ")");
		out.println("# " + exampleName + ", inputs: " + alphabet.size() + ", states: " + target.size());
		
		MembershipOracle.MealyMembershipOracle<I,O> directOracle
			= new SimulatorOracle.MealySimulatorOracle<>(target);
		
		for (JLearn.Learner learnerPair : learners) {
			String learnerName = learnerPair.getName();
			int learnerId = learnerPair.getId();

			LongSummaryStatistics timeLearnlib = new LongSummaryStatistics();
			LongSummaryStatistics queriesLearnlib = new LongSummaryStatistics();
			LongSummaryStatistics timeJLearn = new LongSummaryStatistics();
			LongSummaryStatistics queriesJLearn = new LongSummaryStatistics();

			for (int i = 0; i < repeatCount; i++) {
				System.out.print(learnerName + " #" + i + " ... ");
				System.out.flush();

				System.gc();

				// LearnLib
				{
					MealyCounterOracle<I, O> directCounterOracle = new MealyCounterOracle<>(
							directOracle, "MQs");

					MembershipOracle.MealyMembershipOracle<I, O> cacheOracle = MealyCaches
							.createTreeCache(alphabet, directCounterOracle);

					MealyLearner<I, O> learnlibLearner = learnerPair
							.createLearnLibLearner(alphabet, cacheOracle);
					long learnLibMs = runLearner(example, learnlibLearner);
					System.out.print(learnLibMs + "ms ("
							+ directCounterOracle.getCount() + "MQs)");
					System.out.flush();
					timeLearnlib.accept(learnLibMs);
					queriesLearnlib.accept(directCounterOracle.getCount());

					learnlibLearner = null;
					cacheOracle = null;
					directCounterOracle = null;
				}
				System.gc();

				// JLearn
				{
					MealyCounterOracle<I, O> jlearnCounterOracle = new MealyCounterOracle<>(
							directOracle, "MQs");

					MembershipOracle.MealyMembershipOracle<I, O> jlearnCacheOracle = MealyCaches
							.createTreeCache(alphabet, jlearnCounterOracle);

					MealyLearner<I, O> jlearnLearner = learnerPair
							.createJLearnLearner(alphabet, jlearnCacheOracle);
					long jlearnMs = runLearner(example, jlearnLearner);
					System.out.println(" / " + jlearnMs + "ms ("
							+ jlearnCounterOracle.getCount() + " MQs)");
					timeJLearn.accept(jlearnMs);
					queriesJLearn.accept(jlearnCounterOracle.getCount());

					jlearnLearner = null;
					jlearnCacheOracle = null;
					jlearnCounterOracle = null;
				}

				System.gc();
			}

			out.println(String.format(Locale.ENGLISH, "%s %d %f %f %f %f",
					learnerName, learnerId, timeLearnlib.getAverage(),
					queriesLearnlib.getAverage(), timeJLearn.getAverage(),
					queriesJLearn.getAverage()));

			out.flush();
			learnerId++;
		}
	}
	
	
	private String outputDirName = null;
	private int repeatCount = 10;
	
	private static final String USAGE_MSG =
			"Usage: \n" +
			" {0} -h|-help\n" +
			"   Prints this message and exits.\n" +
			" {0} [<options...>] <output-dir>\n" +
			"   Runs LearnLib and LibAlf algorithms on a set of examples.\n" +
			"   Options can be any of:\n" +
			"    -n|-repeat <num>    Sets the repeat count for each example (default: 10)";
	
	private void printUsage() {
		System.err.println(MessageFormat.format(USAGE_MSG, getName()));
		System.exit(0);
	}
	
	private boolean parseOptions(String[] args) {
		int i = 0;
		while (i < args.length) {
			String arg = args[i++];
			if (arg.length() == 0) {
				continue;
			}
			if (arg.charAt(0) == '-') {
				switch (arg.substring(1)) {
				case "h": case "help":
					printUsage();
				case "n": case "repeat":
					if (i == args.length) {
						System.err.println("Error: expected argument to option " + arg);
						return false;
					}
					String nstr = args[i++];
					try {
						this.repeatCount = Integer.parseInt(nstr);
					}
					catch (NumberFormatException ex) {
						System.err.println("Error: expected numeric argument to option " + arg);
						return false;
					}
					break;
				default:
					System.err.println("Unknown option " + arg);
					return false;
				}
			}
			else {
				if (outputDirName != null) {
					System.err.println("You need to specify exactly ONE output directory");
					return false;
				}
				this.outputDirName = arg;
			}
		}
		
		return true;
	}
	
	
	@Override
	public boolean runMain(String[] args) throws IOException {
		if (!parseOptions(args)) {
			return false;
		}
		if (outputDirName == null) {
			System.err.println("Error: no output directory specified");
			printUsage();
			return false;
		}
		
		File outputDir = new File(outputDirName);
		outputDir.mkdirs();
		if (!outputDir.isDirectory()) {
			System.err.println("Error: could not create output directory " + outputDir.getAbsolutePath());
			return false;
		}
		
		Collection<JLearn.Learner> learners = JLearn.getLearners();
		
		for (Map.Entry<String,MealyLearningExample<?,?>> example : mealyExamples.entrySet()) {
			String name = example.getKey();
			File resultsFile = new File(outputDir, name + ".dat");
			try (PrintWriter pw = new PrintWriter(resultsFile)) { 
				runExample(example.getKey(), example.getValue(), learners, repeatCount, pw);
				System.out.println();
			}
		}
		
		return true;
	}

}
