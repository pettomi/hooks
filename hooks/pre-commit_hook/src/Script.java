import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilePermission;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Scanner;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

public class Script {

	private static final int ERROR_LOCK_EXIST = 0;
	String gold_repos_url = null;
	String admin_user = null;
	String admin_pwd = null;
	String current_front_repos_url = null;
	String svn_path_os = null;
	String gold_repo_name = null;
	String working_directory;
	String workspace_gold;
	String workspace_front;
	String current_repo_name;
	Path temp;
	Path logs;
	String current_front_repos;
	String txn;
	PrintWriter out;
	File log;
	File lock;
	String separator;
	File working_directory_file;
	String daemon_port;
	String lock_queries_path;
	String root;
	String access_control_rules_path;
	String lock_rules;

	public Script(String[] args) throws IOException {

		/* EZ ITT A WORKSPACE!!! */
		separator = String.valueOf(File.separatorChar);
		System.out.println(separator);
		temp = Files.createTempDirectory("mondo");
		workspace_gold = temp.toAbsolutePath().toString() + separator + "workspace_gold" + separator;
		workspace_front = temp.toAbsolutePath().toString() + separator + "workspace_front" + separator;
		working_directory = System.getProperty("user.dir") + separator;
		working_directory = new File(working_directory).getParentFile().toString() + separator;
		current_front_repos = args[0];
		String[] split = current_front_repos.split(separator + separator); // \\\\
																			// ez
																			// volt
																			// itt
		current_repo_name = split[split.length - 1];
		System.out.println(current_repo_name);
		txn = args[1];
		root = args[2];
		Properties prop = new Properties();
		InputStream input = new FileInputStream(working_directory + "config" + separator + "config.properties");

		// load a properties file
		prop.load(input);

		// get the property value and print it out
		gold_repos_url = FilenameUtils.separatorsToSystem(prop.getProperty("gold_repos_url"));
		admin_user = prop.getProperty("admin_user");
		admin_pwd = prop.getProperty("admin_pwd");
		current_front_repos_url = FilenameUtils.separatorsToSystem(prop.getProperty("url") + current_front_repos);
		svn_path_os = FilenameUtils.separatorsToSystem(prop.getProperty("svn_path_os"));
		gold_repo_name = FilenameUtils.separatorsToSystem(prop.getProperty("gold_repo_name"));
		daemon_port = FilenameUtils.separatorsToSystem(prop.getProperty("daemon_port"));
		access_control_rules_path = FilenameUtils
				.separatorsToSystem(prop.getProperty("PATH_TO_ACCESS_CONTROL_RULES_FROM_REPOSITORY_ROOT"));
		lock_queries_path = FilenameUtils
				.separatorsToSystem(prop.getProperty("PATH_TO_ACCESS_CONTROL_AND_LOCK_QUERIES_FROM_REPOSITORY_ROOT"));
		lock_rules = FilenameUtils.separatorsToSystem(prop.getProperty("PATH_TO_LOCK_RULES_FROM_REPOSITORY_ROOT"));

		input.close();

		log = new File(working_directory + "log" + separator + current_repo_name + "_front_log.txt");
		if (!log.exists())
			log.createNewFile();
		out = new PrintWriter(log);

		System.out.println(working_directory);
	}

	public void Run() throws IOException, InterruptedException {

		// log
		try {
			out.println(current_front_repos);
			out.println(txn);

			checklock();

			out.println(working_directory);
			out.println("1.  Making workspace gold directory");
			File workspace_gold_directory = new File(workspace_gold);
			workspace_gold_directory.mkdir();

			out.println("2. Making workspace front directory");
			File workspace_front_directory = new File(workspace_front);
			workspace_front_directory.mkdir();

			out.println("3. Megnézzük ki commitolt: ");
			String get_front_user = "svnlook author -t " + txn + " " + current_front_repos;
			String front_user = cmd(get_front_user).get(0);
			out.println(front_user);

			out.println("4. Lehúzzuk a Gold repot");
			String svncheckout = "svn checkout " + gold_repos_url + " -q  --username " + admin_user + " --password "
					+ admin_pwd + " " + "--quiet --non-interactive";
			System.out.println(cmd("cd " + workspace_gold + " && " + svncheckout));
			System.out.println(svncheckout);

			out.println("5. Lekérjük a front felhasználó változtatásait. Változtatások: ");
			String get_changes = "svnlook changed -t " + txn + " " + current_front_repos;
			ArrayList<String> changes = cmd(get_changes);
			System.out.println(changes.toString());
			System.out.println(get_changes);
			out.println(changes.toString());

			out.println("6. Lekérjük a commit üzenetet. Üzenet: ");
			String get_commit_message = "svnlook log -t " + txn + " " + current_front_repos;
			String commit_message = cmd(get_commit_message).get(0);
			System.out.println(commit_message);
			out.println(commit_message);

			out.println("7 Lekérjük a lockokat.");
			String get_locks = "cd " + workspace_gold + gold_repo_name + " && svn status -u";
			ArrayList<String> statuses = cmd(get_locks);

			for (String status : statuses) {
				System.out.println(status);
				if (status.equals(""))
					break;

				if (status.contains("   O")) {
					out.println("7.2 Találtunk lockot kilépünk.");
					out.flush();
					out.close();
					System.exit(0);
				}
			}

			File f1 = new File(workspace_gold + access_control_rules_path);
			File f2 = new File(workspace_gold + lock_queries_path);
			File f3 = new File(workspace_gold + lock_rules);

			out.println("8. Nem találtunk lockot. Iterálunk végig a változtatásokon");
			for (String change : changes) {
				if (change.equals(""))
					break;
				String file = change.substring(4);
				System.out.println(file);
				File new_file = new File(workspace_front + file);
				System.out.println(workspace_front + file);

				if (change.startsWith("A") || change.startsWith("U") || change.startsWith("UU")) {

					if (file.endsWith(separator)) {
						new_file.mkdirs();
					} else {
						new_file.getParentFile().mkdirs();
						new_file.createNewFile();

						String lock = "cd " + workspace_gold + gold_repo_name + " && svn lock " + file;
						// System.out.println(cmd(lock));

						String copy = "svnlook cat -t " + txn + " " + current_front_repos + " " + file + " > "
								+ workspace_front + file;
						out.println(cmd(copy));

						if (FilenameUtils.getExtension(change).equals("wtspec4m") && f1.exists() && f2.exists()) {
							String lens;
							if (f3.exists()) {
								out.println(
										"8.1 Rules és Queries léteznek és a fájltípus megegyezik a wtspec4m-mel. Végrehajtjuk a lencse transzformációkat");
								lens = "cd " + working_directory + "invoker" + separator + " && java -jar invoker.jar "
										+ front_user + " " + workspace_gold + file + " " + workspace_front + file
										+ " -performPutBack " + working_directory + " salt_" + current_repo_name
										+ " seed_" + current_repo_name + " mondo " + workspace_gold
										+ access_control_rules_path + " " + workspace_gold + " " + lock_queries_path
										+ workspace_gold + lock_rules + " " + root;
							} else {
								out.println(
										"8.1 Rules és Queries léteznek és a fájltípus megegyezik a wtspec4m-mel. Végrehajtjuk a lencse transzformációkat");
								lens = "cd " + working_directory + "invoker" + separator
										+ " && java -jar invoker.jar " + front_user + " " + workspace_gold + file + " "
										+ workspace_front + file + " -performPutBack " + working_directory + " salt_"
										+ current_repo_name + " seed_" + current_repo_name + " mondo " + workspace_gold
										+ access_control_rules_path + " " + workspace_gold + " " + lock_queries_path
										+ " " + root;

							}
							out.println("8.2 lencséket hajtottuk végre:");
							out.println(lens);
							out.println(cmd(lens));
						}
					}

				}

				if (change.startsWith("D")) {
					new_file = new File(workspace_gold + gold_repo_name + separator + file);
					if (new_file.exists()) {
						new_file.delete();
						cmd("cd " + workspace_gold + gold_repo_name + " && " + "svn delete " + file);
					}
				}
			}

			out.println("9. Átmásoljuk a front working directory tartalmát a Gold working directoryba");
			File front = new File(workspace_front);
			File gold = new File(workspace_gold + gold_repo_name);
			FileUtils.copyDirectory(front, gold);

			out.println("10. Addoljuk és commitoljuk a Gold working directory tartalmát");
			String svn_add = "svn add --force * --auto-props --parents --depth infinity -q";
			out.println(cmd("cd " + workspace_gold + gold_repo_name + " && " + svn_add));

			String svn_commit = "svn commit -m \"" + commit_message + "\" --username " + admin_user + " --password "
					+ admin_pwd + " --quiet --non-interactive";
			out.println(cmd("cd " + workspace_gold + gold_repo_name + " && " + svn_commit));

			out.println("11. Töröljük a working directoryk tartalmát");
			FileUtils.deleteQuietly(temp.toFile());
			out.flush();
			out.close();
		} catch (Exception e) {
			e.printStackTrace();
			out.flush();
			out.close();
			FileUtils.deleteDirectory(temp.toFile());
			lock.delete();
		}
	}

	private void checklock() throws IOException {
		lock = new File(working_directory + "lock" + separator + "lock.properties");
		if (lock.exists()) {
			out.println("Található lock fájl. Pre-commit kihagyása, mert Goldról jött a commit");
			out.flush();
			out.close();
			System.exit(ERROR_LOCK_EXIST);
		}
		lock.createNewFile();
		PrintWriter pw = new PrintWriter(lock);
		pw.println("repo=\"" + current_front_repos + "\"");

		pw.flush();
		pw.close();

	}

	public ArrayList<String> findAll(BufferedReader r) throws IOException {
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
	}

	public ArrayList<String> cmd(String command) throws IOException {
		ArrayList<String> result = new ArrayList<String>();
		try {
			ProcessBuilder builder = new ProcessBuilder("bin/sh");
			builder.redirectErrorStream(true);
			Process p = builder.start();
			BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
			BufferedWriter w = new BufferedWriter(new OutputStreamWriter(p.getOutputStream()));
			w.write(command);
			w.newLine();
			w.flush();
			result = findAll(r);
		} catch (Exception e) {

		} finally {
		}
		System.out.println(result);
		return result;
	}
}
