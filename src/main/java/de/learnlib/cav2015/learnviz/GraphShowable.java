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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

import net.automatalib.commons.dotutil.DOT;
import net.automatalib.graphs.Graph;
import net.automatalib.graphs.dot.GraphDOTHelper;
import net.automatalib.util.graphs.dot.GraphDOT;

final class GraphShowable<N,E> extends Showable {
	
	private final Graph<N,E> graph;
	private final GraphDOTHelper<N, ? super E>[] additionalHelpers;
	
	@SafeVarargs
	public GraphShowable(String title, Graph<N,E> graph, GraphDOTHelper<N, ? super E> ...additionalHelpers) {
		super(title);
		this.graph = graph;
		this.additionalHelpers = additionalHelpers;
	}


	@Override
	public void writeHTML(Appendable a) throws IOException {
		File dotFile = File.createTempFile("learnlib", ".dot");
		try (Writer w = new BufferedWriter(new FileWriter(dotFile))) {
			GraphDOT.write(graph, w, additionalHelpers);
		}
		File pngFile = File.createTempFile("learnlib", ".png");
		try (Reader r = new BufferedReader(new FileReader(dotFile))) {
			DOT.runDOT(r, "png", pngFile);
		}
		String uri = pngFile.toURI().toString();
		a.append("<a href=\"" + uri + "\" target=\"_blank\"><img src=\"" + uri + "\"></a>");
	}
	
}
