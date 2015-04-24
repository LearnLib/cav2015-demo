package de.learnlib.cav2015.learnviz;

import java.io.IOException;
import java.util.List;

import net.automatalib.words.Word;

final class QueriesShowable extends Showable {

	private final List<? extends Word<?>> queries;
	public QueriesShowable(String title, List<? extends Word<?>> queries) {
		super(title);
		this.queries = queries;
	}
	@Override
	public void writeHTML(Appendable a) throws IOException {
		for (Word<?> q : queries) {
			a.append(q.isEmpty() ? "&epsilon;" : q.toString()).append("<br>").append('\n');
		}
	}
	
	
}
