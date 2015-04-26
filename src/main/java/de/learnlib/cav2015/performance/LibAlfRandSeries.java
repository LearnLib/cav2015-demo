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

import net.automatalib.automata.fsa.DFA;
import net.automatalib.words.Alphabet;

import com.misberner.clitools.api.CLITool;

import de.learnlib.api.LearningAlgorithm.DFALearner;
import de.learnlib.api.MembershipOracle;
import de.learnlib.cache.dfa.DFACaches;
import de.learnlib.examples.LearningExample.DFALearningExample;
import de.learnlib.examples.dfa.ExampleRandomDFA;
import de.learnlib.libalf.LibalfActiveDFALearner;
import de.learnlib.oracles.CounterOracle.DFACounterOracle;
import de.learnlib.oracles.SimulatorOracle;

public class LibAlfRandSeries implements CLITool {
	
	public String getName() {
		return "libalf-randseries";
	}
	
	public String getDescription() {
		return
			"Compares the performance of LearnLib and LibAlf on a series of randomly " +
			"generated automata.";
	}
	
	public static <I> void runExample(String exampleName, DFALearningExample<I> example,
			Collection<? extends LibAlf.Learner> learners, PrintWriter out) {
		
		DFA<?,I> target = example.getReferenceAutomaton();
		Alphabet<I> alphabet = example.getAlphabet();
	
		System.out.println("Running example " + exampleName + "(" + alphabet.size() + "/" + target.size() + ")");
		
		MembershipOracle.DFAMembershipOracle<I> directOracle
			= new SimulatorOracle.DFASimulatorOracle<>(target);
		
		out.print(target.size());
		out.print(' ');
		
		for (LibAlf.Learner learnerPair : learners) {
			String learnerName = learnerPair.getName();

			System.out.print(learnerName + " ... ");
			System.out.flush();

			System.gc();
			// LearnLib
			{
				DFACounterOracle<I> directCounterOracle = new DFACounterOracle<I>(directOracle, "MQs");
				
				MembershipOracle.DFAMembershipOracle<I> cacheOracle
					= DFACaches.createTreeCache(alphabet, directCounterOracle);
				
				DFALearner<I> learnlibLearner = learnerPair.createLearnLibLearner(alphabet, cacheOracle);
				long learnLibMs = Util.runLearner(example, learnlibLearner);
				System.out.print(learnLibMs + "ms (" + directCounterOracle.getCount() + "MQs)");
				System.out.flush();
				
				out.print(learnLibMs);
				out.print(' ');
				
				// For GC
				learnlibLearner = null;
				cacheOracle = null;
				directCounterOracle = null;
			}
			System.gc();
			
			// Libalf
			{
				DFACounterOracle<I> libalfCounterOracle = new DFACounterOracle<I>(directOracle, "MQs");
				
				LibalfActiveDFALearner<I> libalfLearner = learnerPair.createLibalfLearner(alphabet, libalfCounterOracle);
				long libalfMs = Util.runLearner(example, libalfLearner);
				System.out.println(" / " + libalfMs + "ms (" + libalfCounterOracle.getCount() + " MQs)");
					
				out.print(libalfMs);
				out.print(' ');
				
				libalfCounterOracle = null;
				libalfLearner.dispose();
				libalfLearner = null;
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
			"    -u|-upper <num>     Set the upper state count  limit (exclusive,\n" +
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
		
		List<LibAlf.Learner> learners = new ArrayList<>(LibAlf.getLearners());
		Collections.sort(learners);
		
		for (int alphabetSize : alphabetSizes) {
			System.out.println("Running series for alphabet size " + alphabetSize);
			File outFile = new File(outputDir, "libalf-randseries-" + alphabetSize + ".dat");
			System.out.println("Writing results to file " + outFile);
			try (PrintWriter pw = new PrintWriter(outFile)) {
				for (int numStates = low; numStates < high; numStates += step) {
					DFALearningExample<Integer> example = new ExampleRandomDFA(alphabetSize, numStates);
					runExample("random-" + alphabetSize + "-" + numStates, example, learners, pw);
				}
			}
		}
		
		return true;
	}
}
