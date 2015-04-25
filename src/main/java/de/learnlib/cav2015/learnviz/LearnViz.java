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
package de.learnlib.cav2015.learnviz;

import java.awt.Desktop;
import java.io.File;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.swing.JOptionPane;

import net.automatalib.automata.FiniteAlphabetAutomaton;
import net.automatalib.automata.UniversalDeterministicAutomaton;
import net.automatalib.automata.concepts.SuffixOutput;
import net.automatalib.automata.fsa.DFA;
import net.automatalib.automata.transout.MealyMachine;
import net.automatalib.graphs.concepts.GraphViewable;
import net.automatalib.graphs.dot.EmptyDOTHelper;
import net.automatalib.serialization.taf.parser.PrintStreamDiagnosticListener;
import net.automatalib.serialization.taf.parser.TAFParseException;
import net.automatalib.serialization.taf.parser.TAFParser;
import net.automatalib.util.automata.Automata;
import net.automatalib.words.Alphabet;
import net.automatalib.words.Word;
import net.automatalib.words.WordBuilder;

import com.google.common.base.Objects;
import com.misberner.clitools.api.CLITool;

import de.learnlib.algorithms.discriminationtree.dfa.DTLearnerDFA;
import de.learnlib.algorithms.discriminationtree.dfa.DTLearnerDFABuilder;
import de.learnlib.algorithms.discriminationtree.mealy.DTLearnerMealy;
import de.learnlib.algorithms.discriminationtree.mealy.DTLearnerMealyBuilder;
import de.learnlib.algorithms.lstargeneric.ce.ObservationTableCEXHandlers;
import de.learnlib.algorithms.lstargeneric.dfa.ExtensibleLStarDFA;
import de.learnlib.algorithms.lstargeneric.dfa.ExtensibleLStarDFABuilder;
import de.learnlib.algorithms.lstargeneric.mealy.ExtensibleLStarMealy;
import de.learnlib.algorithms.lstargeneric.mealy.ExtensibleLStarMealyBuilder;
import de.learnlib.algorithms.ttt.base.BaseTTTLearner;
import de.learnlib.algorithms.ttt.base.BaseTTTLearner.Splitter;
import de.learnlib.algorithms.ttt.base.DTNode;
import de.learnlib.algorithms.ttt.base.TTTEventListener;
import de.learnlib.algorithms.ttt.base.TTTHypothesis.TTTEdge;
import de.learnlib.algorithms.ttt.base.TTTState;
import de.learnlib.algorithms.ttt.base.TTTTransition;
import de.learnlib.algorithms.ttt.dfa.TTTLearnerDFA;
import de.learnlib.algorithms.ttt.dfa.TTTLearnerDFABuilder;
import de.learnlib.algorithms.ttt.mealy.TTTLearnerMealy;
import de.learnlib.algorithms.ttt.mealy.TTTLearnerMealyBuilder;
import de.learnlib.api.LearningAlgorithm;
import de.learnlib.api.LearningAlgorithm.DFALearner;
import de.learnlib.api.LearningAlgorithm.MealyLearner;
import de.learnlib.api.MembershipOracle;
import de.learnlib.oracles.DefaultQuery;
import de.learnlib.oracles.SimulatorOracle;

public class LearnViz implements CLITool {
	
	private abstract class Algo {
		@SuppressWarnings("unchecked")
		public <I> void learn(FiniteAlphabetAutomaton<?, I, ?> target) {
			show(new GraphShowable<>("Target system", target.graphView()));
			JOptionPane.showMessageDialog(null, "Updates will be displayed in your browser. Press OK to start the learning process");
			if (target instanceof MealyMachine) {
				learnMealy((MealyMachine<?,I,?,?>) target, target.getInputAlphabet());
			}
			else if (target instanceof DFA) {
				learnDFA((DFA<?,I>) target, target.getInputAlphabet());
			}
			else {
				System.err.println("Unsupported target system type!");
			}
		}
		
		protected <I,D> RecordingOracle<I, D> createOracle(SuffixOutput<I, D> out) {
			return new RecordingOracle<>(new SimulatorOracle<>(out));
		}
		
		protected <I,O> void showMealyLearnerState(
				int round,
				Alphabet<I> alphabet,
				LearningAlgorithm.MealyLearner<I, O> learner,
				MealyMachine<?, I, ?, O> hypothesis,
				RecordingOracle<I, Word<O>> oracle) {
			String roundName = (round != -1) ? "round " + round : "final";
			show(new GraphShowable<>("Hypothesis (" + roundName + ")", hypothesis.transitionGraphView(alphabet)),
					new QueriesShowable("New queries", oracle.fetchNewQueries()));
		}
		protected <I> void showDFALearnerState(
				int round,
				Alphabet<I> alphabet,
				LearningAlgorithm.DFALearner<I> learner,
				DFA<?, I> hypothesis,
				RecordingOracle<I, Boolean> oracle) {
			show(new GraphShowable<>("Hypothesis (round " + round + ")", hypothesis.transitionGraphView(alphabet)),
					new QueriesShowable("New queries", oracle.fetchNewQueries()));
		}
		
		private <I> Word<I> parseWord(String str, Alphabet<I> alphabet) {
			String[] symbols = str.split("\\s+");
			WordBuilder<I> wb = new WordBuilder<>(symbols.length);
			for (String sym : symbols) {
				Optional<I> s = alphabet.stream().filter(i -> sym.equals(i.toString())).findFirst();
				if (!s.isPresent()) {
					JOptionPane.showMessageDialog(null, "Invalid symbol '" + sym + "'!");
					return null;
				}
				wb.add(s.get());
			}
			return wb.toWord();
		}
		
		protected <I,
			A1 extends UniversalDeterministicAutomaton<?,I,?,?,?> & SuffixOutput<I, ?>,
			A2 extends UniversalDeterministicAutomaton<?,I,?,?,?> & SuffixOutput<I, ?>>
		Word<I> findCounterexample(A1 hypothesis,
				A2 target,
				Alphabet<I> alphabet) {
			Word<I> sepWord = Automata.findSeparatingWord(target, hypothesis, alphabet);
			if (sepWord == null) {
				return null;
			}
			if (interactive) {
				Word<I> ceWord = null;
				String ceString = null;
				do {
					ceString = JOptionPane.showInputDialog("Enter counterexample (symbols separated by spaces)", (ceString == null) ? "" : ceString);
					if (ceString == null) {
						int ret = JOptionPane.showConfirmDialog(null, "The hypothesis is not yet equivalent to the target system. A sample counterexample is " + sepWord + ". Terminate anyway?",
								"Terminate learning?",
								JOptionPane.YES_NO_OPTION);
						if (ret == JOptionPane.YES_OPTION) {
							return null;
						}
						continue;
					}
					ceWord = parseWord(ceString, alphabet);
					if (ceWord == null) {
						continue;
					}
					if (Objects.equal(hypothesis.computeOutput(ceWord), target.computeOutput(ceWord))) {
						JOptionPane.showMessageDialog(null, "Word '" + ceWord + "' is not a counterexample!");
						ceWord = null;
						continue;
					}
				} while (ceWord == null);

				return ceWord;
			}
			return sepWord;
		}
				
		protected <I,O> void learnMealy(MealyMachine<?, I, ?, O> target, Alphabet<I> alphabet) {
			RecordingOracle<I, Word<O>> oracle = createOracle(target);
			
			LearningAlgorithm.MealyLearner<I, O> learner
				= createMealyLearner(oracle, alphabet);
			
			learner.startLearning();
			
			MealyMachine<?,I,?,O> hyp = learner.getHypothesisModel();
						
			int round = 0;
			
			while (Automata.findSeparatingWord(hyp, target, alphabet) != null) {
				showMealyLearnerState(round, alphabet, learner, hyp, oracle);
				
				Word<I> ceWord = findCounterexample(hyp, target, alphabet);
				if (ceWord == null) {
					return;
				}
				
				if (!interactive) {
					JOptionPane.showMessageDialog(null, "Finished round " + round + ", counterexample: " + ceWord);
				}
				
				DefaultQuery<I, Word<O>> ce = new DefaultQuery<I, Word<O>>(ceWord, target.computeOutput(ceWord));
				learner.refineHypothesis(ce);
				hyp = learner.getHypothesisModel();
				
				round++;
			}
			
			showMealyLearnerState(-1, alphabet, learner, hyp, oracle);
		}
		
		protected <I> void learnDFA(DFA<?, I> target, Alphabet<I> alphabet) {
			RecordingOracle<I, Boolean> oracle = createOracle(target);
			
			LearningAlgorithm.DFALearner<I> learner
				= createDFALearner(oracle, alphabet);
			
			learner.startLearning();
			
			DFA<?,I> hyp = learner.getHypothesisModel();
						
			int round = 0;
			
			while (Automata.findSeparatingWord(target, hyp, alphabet) != null) {
				showDFALearnerState(round, alphabet, learner, hyp, oracle);
				
				Word<I> ceWord = findCounterexample(hyp, target, alphabet);
				if (ceWord == null) {
					return;
				}
				
				if (!interactive) {
					JOptionPane.showMessageDialog(null, "Finished round " + round + ", counterexample: " + ceWord);
				}
				
				DefaultQuery<I, Boolean> ce = new DefaultQuery<>(ceWord, target.computeOutput(ceWord));
				learner.refineHypothesis(ce);
				hyp = learner.getHypothesisModel();
				
				round++;
			}
			
			showDFALearnerState(-1, alphabet, learner, hyp, oracle);
		}
		
		
		abstract <I,O> LearningAlgorithm.MealyLearner<I, O> createMealyLearner(
				MembershipOracle<I, Word<O>> oracle,
				Alphabet<I> alphabet);
		abstract <I> LearningAlgorithm.DFALearner<I> createDFALearner(
				MembershipOracle<I, Boolean> oracle,
				Alphabet<I> alphabet);
	}
	
	private class AlgoLStar extends Algo {
		@Override
		<I, O> MealyLearner<I, O> createMealyLearner(
				MembershipOracle<I, Word<O>> oracle, Alphabet<I> alphabet) {
			return new ExtensibleLStarMealyBuilder<I,O>()
					.withOracle(oracle)
					.withAlphabet(alphabet)
					.create();
		}

		@Override
		<I> DFALearner<I> createDFALearner(MembershipOracle<I, Boolean> oracle,
				Alphabet<I> alphabet) {
			return new ExtensibleLStarDFABuilder<I>()
					.withOracle(oracle)
					.withAlphabet(alphabet)
					.create();
		}

		@Override
		protected <I, O> void showMealyLearnerState(int round,
				Alphabet<I> alphabet, MealyLearner<I, O> learner,
				MealyMachine<?, I, ?, O> hypothesis,
				RecordingOracle<I, Word<O>> oracle) {
			ExtensibleLStarMealy<I,O> lstar = (ExtensibleLStarMealy<I,O>) learner;
			String roundName = (round != -1) ? "round " + round : "final";
			show(new GraphShowable<>("Hypothesis (" + roundName + ")", ((GraphViewable) hypothesis).graphView()),
					new OTShowable<>("Observation table (" + roundName + ")", lstar.getObservationTable(), Object::toString),
					new QueriesShowable("New queries", oracle.fetchNewQueries()));
		}

		@Override
		protected <I> void showDFALearnerState(int round, Alphabet<I> alphabet,
				DFALearner<I> learner, DFA<?, I> hypothesis,
				RecordingOracle<I, Boolean> oracle) {
			ExtensibleLStarDFA<I> lstar = (ExtensibleLStarDFA<I>) learner;
			String roundName = (round != -1) ? "round " + round : "final";
			show(new GraphShowable<>("Hypothesis (" + roundName + ")", ((GraphViewable) hypothesis).graphView()),
					new OTShowable<>("Observation table (" + roundName + ")", lstar.getObservationTable(), OTShowable.BOOL_01),
					new QueriesShowable("New queries", oracle.fetchNewQueries()));
		}
	}
	
	
	private class AlgoRS extends AlgoLStar {
		@Override
		<I, O> MealyLearner<I, O> createMealyLearner(
				MembershipOracle<I, Word<O>> oracle, Alphabet<I> alphabet) {
			return new ExtensibleLStarMealyBuilder<I,O>()
					.withOracle(oracle)
					.withAlphabet(alphabet)
					.withCexHandler(ObservationTableCEXHandlers.RIVEST_SCHAPIRE)
					.create();
		}

		@Override
		<I> DFALearner<I> createDFALearner(MembershipOracle<I, Boolean> oracle,
				Alphabet<I> alphabet) {
			return new ExtensibleLStarDFABuilder<I>()
					.withOracle(oracle)
					.withAlphabet(alphabet)
					.withCexHandler(ObservationTableCEXHandlers.RIVEST_SCHAPIRE)
					.create();
		}
	}
	
	private class AlgoDT extends Algo {
		@Override
		<I, O> MealyLearner<I, O> createMealyLearner(
				MembershipOracle<I, Word<O>> oracle, Alphabet<I> alphabet) {
			return new DTLearnerMealyBuilder<I,O>()
					.withOracle(oracle)
					.withAlphabet(alphabet)
					.create();
		}

		@Override
		<I> DFALearner<I> createDFALearner(MembershipOracle<I, Boolean> oracle,
				Alphabet<I> alphabet) {
			return new DTLearnerDFABuilder<I>()
					.withOracle(oracle)
					.withAlphabet(alphabet)
					.create();
		}

		@Override
		protected <I, O> void showMealyLearnerState(int round,
				Alphabet<I> alphabet, MealyLearner<I, O> learner,
				MealyMachine<?, I, ?, O> hypothesis,
				RecordingOracle<I, Word<O>> oracle) {
			DTLearnerMealy<I,O> dt = (DTLearnerMealy<I,O>) learner;
			String roundName = (round != -1) ? "round " + round : "final";
			show(new GraphShowable<>("Hypothesis (" + roundName + ")", dt.getHypothesisDS().graphView(), dt.getHypothesisDOTHelper()),
					new GraphShowable<>("Discrimination tree (" + roundName + ")", dt.dtGraphView()),
					new QueriesShowable("New queries", oracle.fetchNewQueries()));
		}

		@Override
		protected <I> void showDFALearnerState(int round, Alphabet<I> alphabet,
				DFALearner<I> learner, DFA<?, I> hypothesis,
				RecordingOracle<I, Boolean> oracle) {
			DTLearnerDFA<I> dt = (DTLearnerDFA<I>) learner;
			String roundName = (round != -1) ? "round " + round : "final";
			show(new GraphShowable<>("Hypothesis (" + roundName + ")", dt.getHypothesisDS().graphView(), dt.getHypothesisDOTHelper()),
					new GraphShowable<>("Discrimination tree (" + roundName + ")", dt.dtGraphView()),
					new QueriesShowable("New queries", oracle.fetchNewQueries()));
		}
	}
	
	private class AlgoTTT extends Algo {
		@Override
		<I, O> MealyLearner<I, O> createMealyLearner(
				MembershipOracle<I, Word<O>> oracle, Alphabet<I> alphabet) {
			TTTLearnerMealy<I,O> learner = new TTTLearnerMealyBuilder<I,O>()
					.withOracle(oracle)
					.withAlphabet(alphabet)
					.create();
			learner.addEventListener(new EventListener<>(alphabet, learner));
			return learner;
		}

		@Override
		<I> DFALearner<I> createDFALearner(MembershipOracle<I, Boolean> oracle,
				Alphabet<I> alphabet) {
			TTTLearnerDFA<I> learner = new TTTLearnerDFABuilder<I>()
					.withOracle(oracle)
					.withAlphabet(alphabet)
					.create();
			learner.addEventListener(new EventListener<>(alphabet, learner));
			return learner;
		}

		@Override
		protected <I, O> void showMealyLearnerState(int round,
				Alphabet<I> alphabet, MealyLearner<I, O> learner,
				MealyMachine<?, I, ?, O> hypothesis,
				RecordingOracle<I, Word<O>> oracle) {
			TTTLearnerMealy<I,O> ttt = (TTTLearnerMealy<I,O>) learner;
			String roundName = (round != -1) ? "round " + round : "final";
			show(new GraphShowable<>("Hypothesis (" + roundName + ")", ttt.getHypothesisDS().graphView(), ttt.getHypothesisDOTHelper()),
					new GraphShowable<>("Discrimination tree (" + roundName + ")", ttt.dtGraphView()),
					new QueriesShowable("New queries", oracle.fetchNewQueries()));
		}

		@Override
		protected <I> void showDFALearnerState(int round, Alphabet<I> alphabet,
				DFALearner<I> learner, DFA<?, I> hypothesis,
				RecordingOracle<I, Boolean> oracle) {
			TTTLearnerDFA<I> ttt = (TTTLearnerDFA<I>) learner;
			String roundName = (round != -1) ? "round " + round : "final";
			show(new GraphShowable<>("Hypothesis (" + roundName + ")", ttt.getHypothesisDS().graphView(), ttt.getHypothesisDOTHelper()),
					new GraphShowable<>("Discrimination tree (" + roundName + ")", ttt.dtGraphView()),
					new QueriesShowable("New queries", oracle.fetchNewQueries()));
		}
	}
	
	private boolean interactive;
	private Algo algo = null;
	private final Map<String, Algo> algorithms = new HashMap<>();
	private String tafFile = null;
	
	public LearnViz() {
		algorithms.put("lstar", new AlgoLStar());
		algorithms.put("rs", new AlgoRS());
		algorithms.put("dt", new AlgoDT());
		algorithms.put("ttt", new AlgoTTT());
	}
	
	@Override
	public String getName() {
		return "learnviz";
	}
	
	@Override
	public String getDescription() {
		return "Visualize a learning process";
	}
	
	private void printUsage() {
		System.err.println(getName() + " - " + getDescription());
		System.err.println("Usage:");
		System.err.printf(" %s -h|-help\n   print this message and exit.\n", getName());
		System.err.printf(" %s [<options>] <taf-file>\n   learn the automaton in the given TAF file", getName());
		System.err.println("Possible options:");
		System.err.println(" -i|-interactive\n   allow the user to enter counterexamples manually");
		System.err.println(" -a|-algo <algorithm>\n   set the learning algorithm (lstar [default], rs, dt, ttt)");
	}
	
	private void parseOptions(String[] args) {
		int i = 0;
		String algoName = "lstar";
		while (i < args.length) {
			String arg = args[i++];
			if (arg.charAt(0) == '-') {
				switch (arg.substring(1)) {
				case "h": case "help":
					printUsage();
					System.exit(0);
				case "i": case "interactive":
					interactive = true;
					break;
				case "a": case "algo":
					if (i >= args.length) {
						System.err.println("Fatal: expecting algorithm name after " + arg);
						System.exit(1);
					}
					algoName = args[i++];
					break;
				default:
					System.err.println("Fatal: unknown option " + arg);
					System.exit(1);
				}
			}
			else {
				if (tafFile != null) {
					System.err.println("Fatal: expecting exactly one input TAF file");
					System.exit(1);
				}
				tafFile = arg;
			}
		}
		algo = algorithms.get(algoName);
		if (algo == null) {
			System.err.println("Fatal: unknown algorithm '" + algoName + "'");
			System.exit(1);
		}
		if (tafFile == null) {
			System.err.println("Fatal: no input TAF file specified");
			System.exit(1);
		}
	}
	
	@Override
	public boolean runMain(String[] args) {
		parseOptions(args);
		try {
			FiniteAlphabetAutomaton<?, ?, ?> automaton = TAFParser.parseAny(new File(tafFile), PrintStreamDiagnosticListener.getStderrDiagnosticListener());
			algo.learn(automaton);
			return true;
		}
		catch (TAFParseException ex) {
			System.err.println("Fatal: could not parse TAF file: " + ex.getMessage());
			return false;
		}
	}
	
	
	public static class EventListener<I,D> implements TTTEventListener<I, D> {
		
		private final Alphabet<I> alphabet;
		private final BaseTTTLearner<?,I,D> learner;
		
		public EventListener(Alphabet<I> alphabet, BaseTTTLearner<?,I,D> learner) {
			this.alphabet = alphabet;
			this.learner = learner;
		}
		@Override
		public void preFinalizeDiscriminator(
				final DTNode<I, D> blockRoot,
				final Splitter<I, D> splitter) {
			final I symbol = alphabet.getSymbol(splitter.symbolIdx);
			
			show(new GraphShowable<>("Hypothesis", learner.getHypothesisDS().graphView(),
					learner.getHypothesisDOTHelper(),
					new EmptyDOTHelper<TTTState<I,D>, TTTEdge<I, D>>() {
						@Override
						public boolean getNodeProperties(
								TTTState<I, D> node,
								Map<String, String> properties) {
							if (node == splitter.state1 || node == splitter.state2) {
								properties.put(NodeAttrs.COLOR, "blue");
							}
							return true;
						}

						@Override
						public boolean getEdgeProperties(
								TTTState<I, D> src,
								TTTEdge<I, D> edge,
								TTTState<I, D> tgt,
								Map<String, String> properties) {
							if ((src == splitter.state1 || src == splitter.state2) && edge.transition.getInput().equals(symbol)) {
								properties.put(EdgeAttrs.COLOR, "blue");
							}
							return true;
						}
			}),
			new GraphShowable<>("Discrimination tree", learner.getDiscriminationTree().graphView(),
					new EmptyDOTHelper<DTNode<I, D>, DTNode<I,D>>() {
						@Override
						public boolean getNodeProperties(
								DTNode<I, D> node,
								Map<String, String> properties) {
							if (node == blockRoot) {
								properties.put(NodeAttrs.COLOR, "red");
							}
							else if (node == splitter.succSeparator) {
								properties.put(NodeAttrs.COLOR, "green");
							}
							else if (node.isLeaf()) {
								TTTState<I,D> state = node.getState();
								if (state == splitter.state1 || state == splitter.state2) {
									properties.put(NodeAttrs.COLOR, "blue");
								}
							}
							return true;
						}
			}));
			if (splitter.succSeparator != null) {
				JOptionPane.showMessageDialog(null, String.format("'%s'-successor of states %s and %s is "
						+ "separated by final discriminator %s.\nUsing %s %s to replace temporary discriminator %s",
						symbol, splitter.state1, splitter.state2, splitter.succSeparator.getDiscriminator(),
						symbol, splitter.discriminator,
						blockRoot.getDiscriminator()));
			}
			else {
				JOptionPane.showMessageDialog(null, String.format("States %s and %s produce differing outputs "
							+ "on %s.\nUsing %s to replace temporary discriminator %s.",
							splitter.state1, splitter.state2, symbol, symbol, blockRoot.getDiscriminator()));
			}
		}

		@Override
		public void postFinalizeDiscriminator(
				DTNode<I, D> blockRoot,
				Splitter<I, D> splitter) {			
		}

		@Override
		public void ensureConsistency(final TTTState<I, D> state,
				final DTNode<I,D> dtNode, final D realOutcome) {
			show(new GraphShowable<>("Hypothesis", learner.getHypothesisDS().graphView(),
					learner.getHypothesisDOTHelper(),
					new EmptyDOTHelper<TTTState<I,D>, TTTEdge<I, D>>() {
						@Override
						public boolean getNodeProperties(
								TTTState<I, D> node,
								Map<String, String> properties) {
							if (node == state) {
								properties.put(NodeAttrs.COLOR, "red");
							}
							return true;
						}
			}),
			new GraphShowable<>("Discrimination tree", learner.getDiscriminationTree().graphView(),
					new EmptyDOTHelper<DTNode<I, D>, DTNode<I,D>>() {
						@Override
						public boolean getNodeProperties(
								DTNode<I, D> node,
								Map<String, String> properties) {
							if (node == dtNode) {
								properties.put(NodeAttrs.COLOR, "blue");
							}
							else if (node.isLeaf() && node.getState() == state) {
								properties.put(NodeAttrs.COLOR, "red");
							}
							return true;
						}

						@Override
						public boolean getEdgeProperties(
								DTNode<I, D> src,
								DTNode<I, D> edge,
								DTNode<I, D> tgt,
								Map<String, String> properties) {
							if (src == dtNode && edge.getParentEdgeLabel().equals(realOutcome)) {
								properties.put(EdgeAttrs.COLOR, "red");
							}
							return true;
						}
						
			}));
			JOptionPane.showMessageDialog(null, String.format("Instable hypothesis:\nState %s (access sequence %s) predicts wrong "
					+ "output for suffix %s\nReal output: %s (according to discrimination tree).\nUsing "
					+ "%s%s as counterexample.",
					state, state.getAccessSequence(), dtNode.getDiscriminator(), realOutcome, state.getAccessSequence(), dtNode.getDiscriminator()));
		}
		@Override
		public void preSplit(final TTTTransition<I, D> transition,
				final Word<I> tempDiscriminator) {
			show(new GraphShowable<>("Hypothesis", learner.getHypothesisDS().graphView(),
					learner.getHypothesisDOTHelper(),
					new EmptyDOTHelper<TTTState<I,D>, TTTEdge<I, D>>() {
						@Override
						public boolean getNodeProperties(
								TTTState<I, D> node,
								Map<String, String> properties) {
							if (node == transition.getTarget()) {
								properties.put(NodeAttrs.COLOR, "red");
							}
							return true;
						}

						@Override
						public boolean getEdgeProperties(
								TTTState<I, D> src,
								TTTEdge<I, D> edge,
								TTTState<I, D> tgt,
								Map<String, String> properties) {
							if (edge.transition == transition) {
								properties.put(NodeAttrs.COLOR, "blue");
							}
							return true;
						}
						
			}),
			new GraphShowable<>("Discrimination tree", learner.getDiscriminationTree().graphView(),
					new EmptyDOTHelper<DTNode<I, D>, DTNode<I,D>>() {
						@Override
						public boolean getNodeProperties(
								DTNode<I, D> node,
								Map<String, String> properties) {
							if (node.isLeaf() && node.getState() == transition.getTarget()) {
								properties.put(NodeAttrs.COLOR, "red");
							}
							return true;
						}
						
			}));
			JOptionPane.showMessageDialog(null, String.format("MQ([%s] %s) != MQ([%s] %s %s).\nSplitting state "
					+ "%s, new state with access sequence %s,\nand using %s as temporary discriminator",
					transition.getAccessSequence(), tempDiscriminator, transition.getSource().getAccessSequence(),
					transition.getInput(), tempDiscriminator, transition.getTarget(),
					transition.getAccessSequence(), tempDiscriminator));
		}
		@Override
		public void postSplit(TTTTransition<I, D> transition,
				Word<I> tempDiscriminator) {
		}
	}
	
	
	public static void show(Showable... showables) {
		try {
			File htmlFile = File.createTempFile("learnlib", ".html");
			
			try (PrintStream ps = new PrintStream(htmlFile)) {
				ps.println("<html><head><style type=\"text/css\">\n"
			+ "table.learnlib-observationtable { border-width: 1px; border: solid; }\n"
			+ "table.learnlib-observationtable th.suffixes-header { text-align: center; }\n"
			+ "table.learnlib-observationtable th.prefix { vertical-align: top; }\n"
			+ "table.learnlib-observationtable .suffix-column { text-align: left; }\n"
			+ "table.learnlib-observationtable tr { border-width: 1px; border: solid; }\n"
			+ "table.learnlib-observationtable tr.long-prefix { background-color: #dfdfdf; }\n"
			+ "</style></head>\n<body><table cellspacing=\"5px\"><tr>");
				for (Showable s : showables) {
					ps.println("<th>" + s.getTitle() + "</th>");
				}
				ps.println("</tr><tr>");
				for (Showable s : showables) {
					ps.println("<td valign=\"top\">");
					s.writeHTML(ps);
					ps.println("</td>");
				}
				ps.println("</tr></table></body></html>");
			}
			
			Desktop.getDesktop().browse(htmlFile.toURI());
		}
		catch(Exception ex) {
			ex.printStackTrace();
		}
	}

}
