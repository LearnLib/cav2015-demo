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
package de.learnlib.cav2015.tafview;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import net.automatalib.automata.FiniteAlphabetAutomaton;
import net.automatalib.serialization.taf.parser.PrintStreamDiagnosticListener;
import net.automatalib.serialization.taf.parser.TAFParseException;
import net.automatalib.serialization.taf.parser.TAFParser;
import net.automatalib.visualization.Visualization;

import com.misberner.clitools.api.CLITool;

public class TAFView implements CLITool {

	@Override
	public String getName() {
		return "tafview";
	}

	@Override
	public String getDescription() {
		return "Visualize TAF files";
	}
	
	private void visualize(InputStream in) {
		try {
			FiniteAlphabetAutomaton<?, ?, ?> automaton = TAFParser.parseAny(in, PrintStreamDiagnosticListener.getStderrDiagnosticListener());
			Visualization.visualizeGraph(automaton.graphView(), false);
		}
		catch (TAFParseException ex) {
			System.err.println("Could not parse TAF: " + ex.getMessage());
		}
	}

	@Override
	public boolean runMain(String[] args) throws Exception {
		if (args.length == 0) {
			visualize(System.in);
		}
		for (String file : args) {
			try (InputStream is = new BufferedInputStream(new FileInputStream(new File(file)))) {
				visualize(is);
			}
			catch (FileNotFoundException ex) {
				System.err.println("File " + file + " not found: " + ex.getMessage());
			}
			catch (IOException ex) {
				System.err.println("Error reading file " + file + ": " + ex.getMessage());
			}
		}
		return true;
	}


}
