package de.learnlib.cav2015.performance;

import net.automatalib.automata.fsa.DFA;
import net.automatalib.automata.transout.impl.compact.CompactMealy;
import net.automatalib.commons.util.mappings.MutableMapping;
import net.automatalib.util.automata.Automata;
import net.automatalib.words.Alphabet;
import de.learnlib.examples.DefaultLearningExample;
import de.learnlib.examples.LearningExample.DFALearningExample;
import de.learnlib.examples.LearningExample.MealyLearningExample;

public class DFA2Mealy {

	public static <S,I> CompactMealy<I, Boolean> dfa2mealy(Alphabet<I> alphabet, DFA<S,I> dfa) {
		int numStates = dfa.size();
		CompactMealy<I,Boolean> result = new CompactMealy<>(alphabet, numStates + 1);
		
		MutableMapping<S, Integer> stateMap = dfa.createStaticStateMapping();
		
		for (S state : dfa) {
			int resState = result.addIntState();
			stateMap.put(state, resState);
		}
		
		S init = dfa.getInitialState();
		int resInit = stateMap.get(init);
		result.setInitialState(resInit);
		
		int sink = -1;
		
		for (S state : dfa) {
			int resState = stateMap.get(state);
			for (I sym : alphabet) {
				S target = dfa.getSuccessor(state, sym);
				int resTarget;
				boolean resOut;
				if (target == null) {
					if (sink == -1) {
						sink = result.addIntState();
						for (int i = 0; i < alphabet.size(); i++) {
							result.setTransition(sink, i, sink, false);
						}
					}
					resTarget = sink;
					resOut = false;
				}
				else {
					resTarget = stateMap.get(target);
					resOut = dfa.isAccepting(target);
				}
				result.setTransition(resState, sym, resTarget, resOut);
			}
		}
		
		Automata.invasiveMinimize(result, alphabet);
		
		return result;
	}

	public static <I> MealyLearningExample<I, Boolean> ex2mealy(DFALearningExample<I> example) {
		DFA<?,I> dfa = example.getReferenceAutomaton();
		Alphabet<I> alphabet = example.getAlphabet();
		
		CompactMealy<I,Boolean> mealy = dfa2mealy(alphabet, dfa);
		return new DefaultLearningExample.DefaultMealyLearningExample<>(mealy);
	}
}
