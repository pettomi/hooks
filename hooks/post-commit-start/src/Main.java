import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

public class Main {

	static File log;
	static PrintWriter	out;
	public static void main(String[] args) throws Exception {
		startSecondJVM(args[0], args[1],args[2]);
	}
	
	public static void startSecondJVM(String s1, String s2, String s3) throws Exception {
		cmd("java -jar post-commit.jar "+ s1 + " " + s2 + " " + s3);
	}
	
	public static void cmd(String command) throws IOException {
		try {
			ProcessBuilder builder = new ProcessBuilder("bin/sh");
			builder.redirectErrorStream(true);
			Process p = builder.start();
			BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
			r.readLine();
			BufferedWriter w = new BufferedWriter(new OutputStreamWriter(p.getOutputStream()));
			w.write(command);
			w.newLine();
			w.flush();
		} catch (Exception e) {

		} finally {
		}
	}

}