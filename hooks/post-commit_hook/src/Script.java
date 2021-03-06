import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Properties;

import org.apache.commons.io.FileUtils;

public class Script {

	String url;
	String gold_repos_url;
	String admin_user;
	String admin_pwd;
	String gold_repo ;
	String txn;  
	String svn_path_os;
	String gold_repo_name;
	String working_directory;
	ArrayList<String> repos;
	File log;
	PrintWriter out;
	File lock;

	public Script(String[] args) throws IOException {
		
		gold_repo=args[0];
		txn= args[1].split("-")[0];
		working_directory=System.getProperty("user.dir") + "/";
		repos=new ArrayList<String>();
		
		// properties beolvasása
		Properties prop = new Properties();
		InputStream input = new FileInputStream("config.properties");

		// load a properties file
		prop.load(input);

		// get the property value
		url = prop.getProperty("url");
		admin_user = prop.getProperty("admin_user");
		admin_pwd = prop.getProperty("admin_pwd");
		svn_path_os = prop.getProperty("svn_path_os");
		gold_repo_name = prop.getProperty("gold_repo_name");
		gold_repos_url = url + gold_repo_name;
		
		// log
		log = new File("G:\\"+ gold_repo_name+ "_front_log.txt");
		if(!log.exists())
			log.createNewFile();
		out = new PrintWriter(log);	

	}

	public void Run() throws IOException, InterruptedException {

		
		out.println(gold_repo);
		out.println(txn);

		
		out.println("1. Workspace gold és front directory létrehozása");
		Path temp = Files.createTempDirectory("mondo");
		String workspace_gold = temp.toAbsolutePath().toString() + "/workspace_gold/";
		String workspace_front = temp.toAbsolutePath().toString() + "/workspace_front/";

		File workspace_gold_directory = new File(workspace_gold);
		workspace_gold_directory.mkdir();
		File workspace_front_directory = new File(workspace_front);
		workspace_front_directory.mkdir();

		
		out.println("2. Lekérjük a front felhasználó változtatásait. Változtatások: ");
		String get_changes = "svnlook changed -r " + txn + " " + gold_repo;
		ArrayList<String> changes = cmd(get_changes);
		System.out.println(changes.toString());
		System.out.println(get_changes);
		out.println(changes.toString());

		
		out.println("3. Megnézzük ki commitolt: ");
		String get_front_user = "svnlook author -r " + txn + " " + gold_repo;
		String front_user = cmd(get_front_user).get(0);
		System.out.println(front_user);
		out.println(front_user);

		
		out.println("4. Lekérjük a commit üzenetet. Üzenet: ");
		String get_commit_message = "svnlook log -r " + txn + " " + gold_repo;
		String commit_message = cmd(get_commit_message).get(0);
		System.out.println(commit_message);
		out.println(commit_message);

		
		out.println("5. Kiolvassuk a config2.properties file-ból a cluster-hez tartozó repókat");
		BufferedReader r = new BufferedReader(new FileReader(working_directory + "config2.properties"));
		String line = r.readLine();
		while (line != null) {
			repos.add(line);
			line = r.readLine();
		}
		r.close();

		checklock();
		
		out.println(
				"6. Iterálunk végig a változtatásokon. Berakjuk a Gold working directoryba a front user által létrehozott változtatásokat.");
		for (String change : changes) {

			if (change.equals(""))
				break;

			String file = change.substring(4);
			System.out.println(file);
			File new_file = new File(workspace_gold + file);

			if (change.startsWith("A") || change.startsWith("U") || change.startsWith("UU")) {

				if (file.endsWith("/")) {
					new_file.mkdirs();
				} else {
					new_file.getParentFile().mkdirs();
					new_file.createNewFile();
					String copy = "svnlook cat -r " + txn + " " + gold_repo + " " + file + " > " + workspace_gold
							+ file;
					cmd(copy);
				}

			}
		}

		
		out.println(
				"7. Végigiterálunk a cluster-ben levő repókon és egyesével átmásoljuk a Gold working directory változtatásait a Front working directory-kba");
		int i = 1;
		for (String frontrepo : repos) {

			String frontrepo_url = url + frontrepo;

			
			out.println("7." + i + " Lehúzzuk a " + frontrepo + "-t ");

			String svncheckout = "svn checkout " + frontrepo_url + " -q  --username " + admin_user + " --password "
					+ admin_pwd + " " + "--quiet --non-interactive";
			System.out.println(cmd("cd " + workspace_front + " && " + svncheckout));
			System.out.println(svncheckout);

			for (String change : changes) {
				if (change.equals(""))
					break;
				String file = change.substring(4);
				System.out.println(file);
				File new_file = null;

				if (change.startsWith("D")) {
					new_file = new File(workspace_front + frontrepo + "/" + file);
					if (new_file.exists()) {
						new_file.delete();
						cmd("cd " + workspace_front + frontrepo + " && " + "svn delete " + file);
					}
				}
			}

			
			out.println(
					"7." + i + ".1 Átmásoljuk a Gold Working directory tartalmát a Front Working directoryba (jelenlegi front repó: "
							+ frontrepo + " )");
			File front = new File(workspace_front + frontrepo);
			File gold = new File(workspace_gold);
			FileUtils.copyDirectory(gold, front);

			
			out.println("7." + i++ + ".2 Addoljuk és Commitoljuk a változtatásokat a megfelelő front repóba");
			String svn_add = " svn add --force * --auto-props --parents --depth infinity -q";
			out.println(cmd("cd " + workspace_front + frontrepo + " && " + svn_add));
			String svn_commit = "svn commit -m \"" + commit_message + "\" --username " + admin_user + " --password "
					+ admin_pwd + " --quiet --non-interactive";
			out.println(cmd("cd " + workspace_front + frontrepo + " && " + svn_commit));

		}
		out.println("8. Töröljük a working directoryk tartalmát");
		FileUtils.deleteQuietly(temp.toFile());

		out.println("9. Unlockoljuk a file-okat");
		out.println("10. Kiléptem: " + System.currentTimeMillis());
	//	unlock(changes);
		lock.deleteOnExit();
		out.flush();
		out.close();
	}
	
	public static ArrayList<String> findAll(BufferedReader r) throws IOException {
		String line = null;
		ArrayList<String> result= new ArrayList<String>();
		for(int i=0; i<4; i++) {
			line = r.readLine();
		}
		while(!line.equals("")){
			line=r.readLine();
			result.add(line);
		}
		return  result;
	}
	
	public  ArrayList<String> cmd(String command) throws IOException{
		ArrayList<String> result= new ArrayList<String>();
		try{
		ProcessBuilder builder = new ProcessBuilder("cmd.exe");
		builder.redirectErrorStream(true);
		Process p = builder.start();
		BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
		BufferedWriter w = new BufferedWriter(new OutputStreamWriter(p.getOutputStream()));	
		w.write(command);
		w.newLine();
		w.flush();
		result=findAll(r);
		}catch(Exception e){
			
		}finally{
		}
		
		return result;
	}
	
	public void unlock(ArrayList<String> changes) throws IOException {
		
		int i=1;
		for (String change : changes) {
			if (change.equals(""))
				break;
			String file = change.substring(4);
			System.out.println(file);
//			File new_file = new File(workspace_front + file);
//			System.out.println(workspace_front + file);
			
			if (change.startsWith("A") || change.startsWith("U") || change.startsWith("UU")) {
					String unlock =  "svn unlock " + gold_repos_url+ "/" +file;
					System.out.println(unlock);
					out.println((cmd(unlock)));
			}
		}
	}
	private void checklock() throws IOException {
		lock=new File( "G:\\lock.properties");
		if(lock.exists()){
			Properties prop = new Properties();
			InputStream input = new FileInputStream(lock);
			prop.load(input);
			String commiter_repo = prop.getProperty("repo");
			out.println("5.1 Létezik a lock file. A commitoló repó:" + commiter_repo);
			for (String repo : repos) {	
				System.out.println(repo);
				if(commiter_repo.contains(repo.split("/")[0])){;
					repos.remove(repo);
				}
			}
			input.close();
		}
		lock.createNewFile();
		PrintWriter pw = new PrintWriter(lock);
		pw.println("repo=\"" + gold_repo + "\"");

		pw.flush();
		pw.close();
		
	}
	
}
