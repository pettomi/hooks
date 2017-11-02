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
		startSecondJVM(args[0], args[1]);
		log = new File("G:\\post-commit-start-log.txt");
		if(!log.exists())
			log.createNewFile();
		out = new PrintWriter(log);	
		out.println("kileptem: " + System.currentTimeMillis());
		out.flush();
		out.close();
	}
	
	public static void startSecondJVM(String s1, String s2) throws Exception {
		String separator = System.getProperty("file.separator");
		String classpath = System.getProperty("java.class.path");
		String path = System.getProperty("java.home") + separator + "bin" + separator + "java";
		
//		out.println(cmd("java -jar post-commit.jar "+ s1 + " " + s2));
		cmd("java -jar post-commit.jar "+ s1 + " " + s2);

	}
	
/*	public static ArrayList<String> findAll(BufferedReader r) throws IOException {
		String line = null;
		ArrayList<String> result = new ArrayList<String>();
		for (int i = 0; i < 4; i++) {
			line = r.readLine();
		}
		while (!line.equals("")) {
			line = r.readLine();
			result.add(line);
		}
		return result;
	}*/

	public static void cmd(String command) throws IOException {
		ArrayList<String> result = new ArrayList<String>();
		try {
			ProcessBuilder builder = new ProcessBuilder("cmd.exe");
			builder.redirectErrorStream(true);
			Process p = builder.start();
			BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
			r.readLine();
			BufferedWriter w = new BufferedWriter(new OutputStreamWriter(p.getOutputStream()));
			w.write(command);
			w.newLine();
			w.flush();
//			result = findAll(r);
		} catch (Exception e) {

		} finally {
		}

//		return result;
	}

}