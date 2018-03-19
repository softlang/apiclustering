package org.softlang.dscor.utils;

import java.io.File;

public class Dots {

	public static final String DOT_EXE = "C:\\Program Files (x86)\\Graphviz2.38\\bin\\dot.exe";

	public static void compile(File in, File out) {

		try {
			Runtime rt = Runtime.getRuntime();

			String[] args = { DOT_EXE, "-T" + "png", in.getAbsolutePath(), "-o", out.getAbsolutePath() };
			Process p = rt.exec(args);

			p.waitFor();

		} catch (java.io.IOException ioe) {
			System.err.println("Error:    in I/O processing of out in dir " + out + "\n");
			System.err.println("       or in calling external command");
			ioe.printStackTrace();
		} catch (java.lang.InterruptedException ie) {
			System.err.println("Error: the execution of the external program was interrupted");
			ie.printStackTrace();
		}

	}
}
