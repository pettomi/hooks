import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;

public class Main {

	static File log;
	static PrintWriter	out;
	public static void main(String[] args) throws Exception {
		startSecondJVM(args[0], args[1],args[2]);
	}
	
	public static void startSecondJVM(String s1, String s2, String s3) throws Exception {
		cmd("java -jar post-commit.jar "+ s1 + " " + s2 + " " + s3);
	}
	
	public static ArrayList<String> cmd(String command) throws IOException {
		ArrayList<String> result = new ArrayList<String>();
		String s;
		try {
			Process p = Runtime.getRuntime().exec(command);
		    BufferedReader br = new BufferedReader(
		        new InputStreamReader(p.getInputStream()));
		    while ((s = br.readLine()) != null)
		        result.add(s);
		    p.waitFor();
		    System.out.println ("exit: " + p.exitValue());
		    p.destroy();
		} catch (Exception e) {

		} finally {
		}
		return result;
	}

}