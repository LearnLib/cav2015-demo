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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import net.automatalib.automata.transout.MealyMachine;
import net.automatalib.words.Alphabet;
import net.automatalib.words.Word;
import net.automatalib.words.impl.Alphabets;

import com.misberner.clitools.api.CLITool;

import de.learnlib.api.LearningAlgorithm.MealyLearner;
import de.learnlib.api.MembershipOracle;
import de.learnlib.api.Query;
import de.learnlib.examples.LearningExample.MealyLearningExample;
import de.learnlib.examples.mealy.ExampleRandomMealy;
import de.learnlib.oracles.CounterOracle.MealyCounterOracle;
import de.learnlib.oracles.SimulatorOracle;

public class JLearnRandSeries implements CLITool {
	
	
	@Override
	public String getName() {
		return "jlearn-randseries";
	}
	
	@Override
	public String getDescription() {
		return
				"Compares the performance of LearnLib and JLearn on a series of randomly " +
				"generated automata.";
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
	
	
	
	public static <I,O> void runExample(String exampleName, MealyLearningExample<I,O> example,
			Collection<? extends JLearn.Learner> learners, PrintWriter out) {
		
		MealyMachine<?,I,?,O> target = example.getReferenceAutomaton();
		Alphabet<I> alphabet = example.getAlphabet();
	
		System.out.println("Running example " + exampleName + "(" + alphabet.size() + "/" + target.size() + ")");
		
		MembershipOracle<I,Word<O>> directOracle
			= new SeqOracle<>(new SimulatorOracle.MealySimulatorOracle<>(target));
		
		out.print(target.size());
		out.print(' ');
		
		for (JLearn.Learner learnerPair : learners) {
			String learnerName = learnerPair.getName();

			System.out.print(learnerName + " ... ");
			System.out.flush();

			System.gc();
			// LearnLib
			{
				MealyCounterOracle<I,O> directCounterOracle = new MealyCounterOracle<>(directOracle, "MQs");
	
				MealyLearner<I,O> learnlibLearner = learnerPair.createLearnLibLearner(alphabet,
						directCounterOracle);
				long learnLibMs = Util.runLearner(example, learnlibLearner);
				System.out.print(learnLibMs + "ms (" + directCounterOracle.getCount() + "MQs)");
				System.out.flush();
				
				out.print(learnLibMs);
				out.print(' ');
				
				// For GC
				learnlibLearner = null;
				directCounterOracle = null;
			}
			System.gc();
			
			// Libalf
			{
				MealyCounterOracle<I,O> jlearnCounterOracle = new MealyCounterOracle<>(directOracle, "MQs");
				
				MealyLearner<I,O> jlearnLearner = learnerPair.createJLearnLearner(alphabet, jlearnCounterOracle);
				long jlearnMs = Util.runLearner(example, jlearnLearner);
				System.out.println(" / " + jlearnMs + "ms (" + jlearnCounterOracle.getCount() + " MQs)");
					
				out.print(jlearnMs);
				out.print(' ');
				
				jlearnCounterOracle = null;
				jlearnLearner = null;
			}
			System.gc();
		}
		
		out.println();
		out.flush();
	}
	
	
	
	private int[] alphabetSizes = { 2, 10, 100 };
	private String outputDirName = null;
	private int low = 10;
	private int high = 1000;
	private int step = 10;
	
	private static final String USAGE_MSG =
			"Usage: \n" +
			" {0} -h|-help\n" +
			"   Prints this message and exits.\n" +
			" {0} [<options...>] <output-dir>\n" +
			"   Runs a series of experiments on randomly generated automata.\n" +
			"   Options can be any of:\n" +
			"    -l|-lower <num>     Set the lower state count limit (default: 10)\n" +
			"    -u|-upper <num>     Set the upper state count  limit (exclusive," +
			"                        default: 1000)\n" +
			"    -s|-step <num>      Set the state count stepping (default: 10)\n" +
			"    -k|-alphabet-sizes <sizes>\n" +
			"                        Set the different alphabet sizes as a comma-separated\n" +
			"                        list (default: 2,10,100)";
	
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
				case "l": case "lower":
					if (i == args.length) {
						System.err.println("Error: expected argument to option " + arg);
						return false;
					}
					String lstr = args[i++];
					try {
						this.low = Integer.parseInt(lstr);
					}
					catch (NumberFormatException ex) {
						System.err.println("Error: expected numeric argument to option " + arg);
						return false;
					}
					break;
				case "u": case "upper":
					if (i == args.length) {
						System.err.println("Error: expected argument to option " + arg);
						return false;
					}
					String hstr = args[i++];
					try {
						this.high = Integer.parseInt(hstr);
					}
					catch (NumberFormatException ex) {
						System.err.println("Error: expected numeric argument to option " + arg);
						return false;
					}
					break;
				case "s": case "step":
					if (i == args.length) {
						System.err.println("Error: expected argument to option " + arg);
						return false;
					}
					String sstr = args[i++];
					try {
						this.step = Integer.parseInt(sstr);
					}
					catch (NumberFormatException ex) {
						System.err.println("Error: expected numeric argument to option " + arg);
						return false;
					}
					break;
				case "k": case "alphabet-sizes":
					if (i == args.length) {
						System.err.println("Error: expected argument to option " + arg);
						return false;
					}
					String kstr = args[i++];
					try {
						this.alphabetSizes = Util.parseIntArray(kstr);
					}
					catch (NumberFormatException ex) {
						System.err.println("Error parsing argument to option " + arg + ":");
						System.err.println(ex.getMessage());
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
			System.err.println("Error: No output directory specified");
			printUsage();
			return false;
		}
		
		File outputDir = new File(outputDirName);
		outputDir.mkdirs();
		if (!outputDir.isDirectory()) {
			System.err.println("Could not create output directory " + outputDir.getAbsolutePath());
			return false;
		}
		
		List<JLearn.Learner> learners = new ArrayList<>(JLearn.getLearners());
		Collections.sort(learners);
		
		for (int alphabetSize : alphabetSizes) {
			System.out.println("Running series for alphabet size " + alphabetSize);
			File outFile = new File(outputDir, "jlearn-randseries-" + alphabetSize + ".dat");
			System.out.println("Writing results to file " + outFile);
			try (PrintWriter pw = new PrintWriter(outFile)) {
				for (int numStates = low; numStates < high; numStates += step) {
					MealyLearningExample<Integer,Boolean> example = new ExampleRandomMealy<>(Alphabets.integers(0, alphabetSize - 1), numStates, false, true);;
					runExample("random-" + alphabetSize + "-" + numStates, example, learners, pw);
				}
			}
		}
		
		return true;
	}
}
