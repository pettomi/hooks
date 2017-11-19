import java.io.BufferedReader;

import java.io.File;

import java.io.IOException;

import java.io.InputStreamReader;

import java.io.PrintWriter;
import java.util.ArrayList;


public class Main {
	 static File log;
	 static	PrintWriter	out;
	
	public static void main(String[] args) throws Exception {
		
		
		String separator = String.valueOf(File.separatorChar);
//		String working_directory2 = Main.class.getProtectionDomain().getCodeSource().getLocation().toString().substring(5);
//		String working_directory = new File(working_directory2).getParentFile().getParentFile().toString() + separator;
		
		String working_directory=args[3];
		
		log = new File(working_directory + "log" + separator  + "gold_start_log.txt");
		if (!log.exists())
			log.createNewFile();
		out = new PrintWriter(log);
		out.println(working_directory);
		out.println(cmd("java -jar post-commit.jar " +args[0] + " "+ args[1]+" "+args[2], working_directory+"hooks"));
		out.flush();
		out.close();
	}
	
	public static ArrayList<String> cmd(String command, String where_to) throws IOException {
		ArrayList<String> result = new ArrayList<String>();
		String s;
		try {
			File where= new File(where_to);
			Process p = Runtime.getRuntime().exec(command,null, where);
//			ProcessBuilder pb = new ProcessBuilder(command);
//			if(where.exists())
//				out.println("létezik");
//			pb.directory(where);
//			Process p= pb.start();
			BufferedReader error = new BufferedReader(
			        new InputStreamReader(p.getErrorStream()));
			while ((s = error.readLine()) != null)
				out.println(s);
		        
		    BufferedReader br = new BufferedReader(
		        new InputStreamReader(p.getInputStream()));
			
		    while ((s = br.readLine()) != null)
		        result.add(s);
		    p.waitFor();
		    out.println ("exit: " + p.exitValue());
		    p.destroy();
		} catch (Exception e) {
			out.println(e.getMessage());
		} finally {
		}
		return result;
	}

}