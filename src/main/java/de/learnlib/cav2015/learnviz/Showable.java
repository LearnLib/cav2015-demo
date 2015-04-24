package de.learnlib.cav2015.learnviz;

import java.io.IOException;

abstract class Showable {
	
	private final String title;
	
	public Showable(String title) {
		this.title = title;
	}
	public String getTitle() {
		return title;
	}
	
	public abstract void writeHTML(Appendable a) throws IOException;
}
