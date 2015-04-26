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

import net.automatalib.automata.fsa.DFA;
import net.automatalib.words.Alphabet;

import com.misberner.clitools.api.CLITool;

import de.learnlib.api.LearningAlgorithm.DFALearner;
import de.learnlib.api.MembershipOracle;
import de.learnlib.cache.dfa.DFACaches;
import de.learnlib.examples.LearningExample.DFALearningExample;
import de.learnlib.examples.dfa.DFABenchmarks;
import de.learnlib.examples.dfa.ExampleKeylock;
import de.learnlib.examples.dfa.ExampleRandomDFA;
import de.learnlib.libalf.LibalfActiveDFALearner;
import de.learnlib.oracles.CounterOracle.DFACounterOracle;
import de.learnlib.oracles.SimulatorOracle;

public class LibAlfExamples implements CLITool {
	
	
	private static final Map<String,DFALearningExample<?>> dfaExamples;
	
	static {
		dfaExamples = new LinkedHashMap<>();
	
		int[] alphabetSizes = { 5, 10, 20 };
		int[] stateCounts = { 100, 500 };
		for (int alphabetSize : alphabetSizes) {
			for (int stateCount : stateCounts) {
				dfaExamples.put("random-" + alphabetSize + "-" + stateCount, new ExampleRandomDFA(alphabetSize, stateCount));
			}
		}
		dfaExamples.put("peterson2", DFABenchmarks.loadPeterson2());
		alphabetSizes = new int[]{ 5, 10 };
		stateCounts = new int[]{64, 128};
		for (int alphabetSize : alphabetSizes) {
			for (int stateCount : stateCounts) {
				dfaExamples.put("keylock-" + alphabetSize + "-" + stateCount, new ExampleKeylock(stateCount, false, alphabetSize - 1));
				dfaExamples.put("keylockc-" + alphabetSize + "-" + stateCount, new ExampleKeylock(stateCount, true, alphabetSize - 1));
			}
		}
	}
	
	@Override
	public String getName() {
		return "libalf-examples";
	}
	
	@Override
	public String getDescription() {
		return "Compares the performance of LearnLib and LibAlf on a series of examples";
	}
	
	public static <I> void runExample(String exampleName, DFALearningExample<I> example,
			Collection<? extends LibAlf.Learner> learners, int repeatCount, PrintWriter out) {
		DFA<?,I> target = example.getReferenceAutomaton();
		Alphabet<I> alphabet = example.getAlphabet();
	
		System.out.println("Running example " + exampleName + "(" + alphabet.size() + "/" + target.size() + ")");
		
		MembershipOracle.DFAMembershipOracle<I> directOracle
			= new SimulatorOracle.DFASimulatorOracle<>(target);

		for (LibAlf.Learner learnerPair : learners) {
			String learnerName = learnerPair.getName();
			int learnerId = learnerPair.getId();

			LongSummaryStatistics timeLearnlib = new LongSummaryStatistics();
			LongSummaryStatistics timeLibalf = new LongSummaryStatistics();
			LongSummaryStatistics queriesLearnlib = new LongSummaryStatistics();
			LongSummaryStatistics queriesLibalf = new LongSummaryStatistics();

			for (int i = 0; i < repeatCount; i++) {
				System.out.print(learnerName + " #" + i + " ... ");
				System.out.flush();
				System.gc();

				// LearnLib
				{
					DFACounterOracle<I> directCounterOracle = new DFACounterOracle<I>(
							directOracle, "MQs");

					MembershipOracle.DFAMembershipOracle<I> cacheOracle = DFACaches
							.createTreeCache(alphabet, directCounterOracle);

					DFALearner<I> learnlibLearner = learnerPair
							.createLearnLibLearner(alphabet, cacheOracle);
					long learnLibMs = Util.runLearner(example, learnlibLearner);
					System.out.print(learnLibMs + "ms ("
							+ directCounterOracle.getCount() + "MQs)");
					System.out.flush();

					timeLearnlib.accept(learnLibMs);
					queriesLearnlib.accept(directCounterOracle.getCount());

					directCounterOracle = null;
					cacheOracle = null;
					learnlibLearner = null;
				}
				System.gc();

				// LibAlf
				{
					DFACounterOracle<I> libalfCounterOracle = new DFACounterOracle<I>(
							directOracle, "MQs");

					LibalfActiveDFALearner<I> libalfLearner = learnerPair
							.createLibalfLearner(alphabet, libalfCounterOracle);
					long libalfMs = Util.runLearner(example, libalfLearner);
					System.out.println(" / " + libalfMs + "ms ("
							+ libalfCounterOracle.getCount() + " MQs)");
					timeLibalf.accept(libalfMs);
					queriesLibalf.accept(libalfCounterOracle.getCount());

					libalfCounterOracle = null;
					libalfLearner.dispose();
					libalfLearner = null;
				}
				System.gc();
			}

			out.println(String.format(Locale.ENGLISH, "%s %d %f %f %f %f",
					learnerName, learnerId, timeLearnlib.getAverage(),
					queriesLearnlib.getAverage(), timeLibalf.getAverage(),
					queriesLibalf.getAverage()));
			out.flush();
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
		
		Collection<LibAlf.Learner> learners = LibAlf.getLearners();
		
		for (Map.Entry<String,DFALearningExample<?>> example : dfaExamples.entrySet()) {
			String name = example.getKey();
			File resultsFile = new File(outputDir, "libalf-examples-" + name + ".dat");
			try (PrintWriter pw = new PrintWriter(resultsFile)) { 
				runExample(example.getKey(), example.getValue(), learners, repeatCount, pw);
				System.out.println();
			}
		}
		
		return true;
	}
}
