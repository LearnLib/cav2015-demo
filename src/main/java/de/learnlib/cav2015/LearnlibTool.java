package de.learnlib.cav2015;

import com.misberner.clitools.CLIToolDispatcher;

public class LearnlibTool {

	public static void main(String[] args) throws Exception {
		CLIToolDispatcher dispatcher = new CLIToolDispatcher();
		dispatcher.addClassRegexInclude("de\\.learnlib\\.cav2015\\..*");
		dispatcher.run(args);
	}

}
