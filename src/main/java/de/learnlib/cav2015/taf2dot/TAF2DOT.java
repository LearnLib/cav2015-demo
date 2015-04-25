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
package de.learnlib.cav2015.taf2dot;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import net.automatalib.automata.FiniteAlphabetAutomaton;
import net.automatalib.serialization.taf.parser.PrintStreamDiagnosticListener;
import net.automatalib.serialization.taf.parser.TAFParseException;
import net.automatalib.serialization.taf.parser.TAFParser;
import net.automatalib.util.graphs.dot.GraphDOT;

import com.misberner.clitools.api.CLITool;

public class TAF2DOT implements CLITool {

	@Override
	public String getName() {
		return "taf2dot";
	}

	@Override
	public String getDescription() {
		return "Convert TAF files to GraphVIZ DOT";
	}

	@Override
	public boolean runMain(String[] args) throws Exception {
		if (args.length > 2) {
			System.err.println("Error: taf2dot requires at most two arguments");
			System.err.println("Usage: taf2dot [<taf-file-in> [<dot-file-out>]]");
			return false;
		}
		InputStream in = System.in;
		OutputStream out = System.out;
		try {
			if (args.length > 0) {
				in = new BufferedInputStream(new FileInputStream(new File(args[0])));
			}
			if (args.length > 1) {
				out = new BufferedOutputStream(new FileOutputStream(new File(args[1])));
			}
			FiniteAlphabetAutomaton<?, ?, ?> automaton = TAFParser.parseAny(in, PrintStreamDiagnosticListener.getStderrDiagnosticListener());
			OutputStreamWriter osw = new OutputStreamWriter(out);
			GraphDOT.write(automaton, osw);
			osw.flush();
		}
		catch (IOException ex) {
			System.err.println("Fatal: " + ex.getMessage());
			return false;
		}
		catch (TAFParseException ex) {
			System.err.println("Fatal error parsing TAF: " + ex.getMessage());
		}
		finally {
			in.close();
			out.close();
		}
		return true;
	}


}
