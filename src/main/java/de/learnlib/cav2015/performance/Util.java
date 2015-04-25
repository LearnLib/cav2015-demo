package de.learnlib.cav2015.performance;

import net.automatalib.automata.UniversalDeterministicAutomaton;
import net.automatalib.automata.concepts.Output;
import net.automatalib.util.automata.Automata;
import net.automatalib.words.Alphabet;
import net.automatalib.words.Word;
import de.learnlib.api.LearningAlgorithm;
import de.learnlib.examples.LearningExample;
import de.learnlib.oracles.DefaultQuery;

public class Util {

	public Util() {
		// TODO Auto-generated constructor stub
	}
	
	public static int[] parseIntArray(String intsStr) {
		String[] intStrs = intsStr.split("\\s*,\\s*");
		int[] ints = new int[intStrs.length];
		for (int i = 0; i < intStrs.length; i++) {
			ints[i] = Integer.parseInt(intStrs[i]);
		}
		return ints;
	}

	public static <I,D,A extends UniversalDeterministicAutomaton<?, I, ?, ?, ?> & Output<I, D>>
	long runLearner(
			LearningExample<I, D, ? extends A> example, 
			LearningAlgorithm<? extends A, I, D> learner) {
		A target = example.getReferenceAutomaton();
		Alphabet<I> alphabet = example.getAlphabet();
		
		long learnerNanos = 0L;
		long start = System.nanoTime();
		learner.startLearning();
		learnerNanos += System.nanoTime() - start;
		
		A hyp = learner.getHypothesisModel();
		
		Word<I> ceWord;
		
		while ((ceWord = Automata.findSeparatingWord(target, hyp, alphabet)) != null) {
			DefaultQuery<I, D> ceQuery = new DefaultQuery<>(ceWord, target.computeOutput(ceWord));
			start = System.nanoTime();
			learner.refineHypothesis(ceQuery);
			learnerNanos += System.nanoTime() - start;
			hyp = learner.getHypothesisModel();
		}
		
		return learnerNanos / 1000000L;
	}
}
