package de.learnlib.cav2015.learnviz;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.automatalib.words.Word;
import de.learnlib.api.MembershipOracle;
import de.learnlib.api.Query;

final class RecordingOracle<I, D> implements MembershipOracle<I, D> {
	
	private final MembershipOracle<I, D> delegate;
	
	private List<Word<I>> newQueries = new ArrayList<>();
	
	public RecordingOracle(MembershipOracle<I, D> delegate) {
		this.delegate = delegate;
	}
	
	
	public List<Word<I>> fetchNewQueries() {
		List<Word<I>> result = newQueries;
		
		newQueries = new ArrayList<>();
		
		return result;
	}


	@Override
	public void processQueries(Collection<? extends Query<I, D>> queries) {
		for (Query<I,D> qry : queries) {
			newQueries.add(qry.getInput());
		}
		delegate.processQueries(queries);
	}
	
}
