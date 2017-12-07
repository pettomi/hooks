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
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

public class Script {

	String url;
	String gold_repos_url;
	String admin_user;
	String admin_pwd;
	String gold_repo;
	String txn;
	String svn_path_os;
	String gold_repo_name;
	String working_directory;
	ArrayList<String> repos;
	ArrayList<String> users;
	File log;
	PrintWriter out;
	File lock;
	String separator;
	String daemon_port;
	String access_control_rules_path;
	String lock_queries_path;
	String root;
	String lock_rules;
	File f1;
	File f2;
	String working_directory2;
	String svn_url_path;

	public Script(String[] args) throws IOException {

		separator = String.valueOf(File.separatorChar);
		gold_repo = args[0];
		txn = args[1].split("-")[0];
		root = args[2];
		working_directory2 = Main.class.getProtectionDomain().getCodeSource().getLocation().toString().substring(5);
		System.out.println(working_directory);
		working_directory = new File(working_directory2).getParentFile().getParentFile().toString() + separator;
		repos = new ArrayList<String>();
		users = new ArrayList<String>();

		// properties beolvasása
		Properties prop = new Properties();
		InputStream input = new FileInputStream(working_directory + "config" + separator + "config.properties");

		// load a properties file
		prop.load(input);

		// get the property value
		url = FilenameUtils.separatorsToSystem(prop.getProperty("url"));
		admin_user = prop.getProperty("admin_user");
		admin_pwd = prop.getProperty("admin_pwd");
		svn_path_os = FilenameUtils.separatorsToSystem(prop.getProperty("svn_path_os"));
		gold_repo_name = FilenameUtils.separatorsToSystem(prop.getProperty("gold_repo_name"));
		access_control_rules_path = FilenameUtils
				.separatorsToSystem(prop.getProperty("PATH_TO_ACCESS_CONTROL_RULES_FROM_REPOSITORY_ROOT"));
		lock_queries_path = FilenameUtils
				.separatorsToSystem(prop.getProperty("PATH_TO_ACCESS_CONTROL_AND_LOCK_QUERIES_FROM_REPOSITORY_ROOT"));
		lock_rules = FilenameUtils.separatorsToSystem(prop.getProperty("PATH_TO_LOCK_RULES_FROM_REPOSITORY_ROOT"));
		svn_url_path = FilenameUtils.separatorsToSystem(prop.getProperty("SVN_URL_PATH"));
		gold_repos_url = url + gold_repo_name;

		// log
		log = new File(working_directory + "log" + separator + gold_repo_name + "_gold_log.txt");
		if (!log.exists())
			log.createNewFile();
		out = new PrintWriter(log);

	}

	public void Run() throws IOException, InterruptedException {

		try {
			out.println(gold_repo);
			out.println(txn);

			out.println("1. workspace_gold es workspace_front mappak letrehozasa");
			Path temp = Files.createTempDirectory("mondo");
			String workspace_gold = temp.toAbsolutePath().toString() + separator + "workspace_gold" + separator;
			String workspace_front = temp.toAbsolutePath().toString() + separator + "workspace_front" + separator;

			File workspace_gold_directory = new File(workspace_gold);
			workspace_gold_directory.mkdir();
			File workspace_front_directory = new File(workspace_front);
			workspace_front_directory.mkdir();

			out.println("2. Lekerjuk a front felhasznalo valtoztatasait. Valtoztatasok: ");
			String get_changes = "svnlook changed -r " + txn + " " + gold_repo;
			ArrayList<String> changes = cmd(get_changes);
			System.out.println(changes.toString());
			System.out.println(get_changes);
			out.println(changes.toString());

			out.println("3. Megnezzük ki commitolt: ");
			String get_front_user = "svnlook author -r " + txn + " " + gold_repo;
			String front_user = cmd(get_front_user).get(0);
			System.out.println(front_user);
			out.println(front_user);

			out.println("4. Lekerjuk a commit uzenetet. Uzenet: ");
			String get_commit_message = "svnlook log -r " + txn + " " + gold_repo;
			String commit_message = cmd(get_commit_message).get(0);
			System.out.println(commit_message);
			out.println(commit_message);

			out.println("5.1 Kiolvassuk a config2.properties file-bol a cluster-hez tartozo repository-kat");
			BufferedReader r = new BufferedReader(
					new FileReader(working_directory + "config" + separator + "config2.properties"));
			String line = r.readLine();
			while (line != null) {
				repos.add(line);
				line = r.readLine();
			}
			r.close();

			out.println("5.2 Kiolvassuk a user_list.properties file-bol a cluster-hez tartozo felhasznalokat");
			r = new BufferedReader(new FileReader(working_directory + "config" + separator + "user_list.properties"));
			line = r.readLine();
			while (line != null) {
				users.add(line);
				line = r.readLine();
			}
			r.close();

			checklock();

			out.println(
					"6. Iteralunk vegig a valtoztatasokon. Berakjuk a Gold working directoryba a front user által letrehozott valtoztatasokat.");

			int k = 0;

			for (String change : changes) {

				if (change.equals(""))
					break;

				String file = change.substring(4);
				System.out.println(file);
				File new_file = new File(workspace_gold + file);

				if (change.startsWith("A") || change.startsWith("U") || change.startsWith("UU")) {

					if (file.endsWith(separator)) {
						new_file.mkdirs();
					} else {

						String copy = "svnlook cat -r " + txn + " " + gold_repo + " " + file;
						FileUtils.writeStringToFile(new_file, cmd(copy).get(0), Charset.defaultCharset());

					}

				}
			}

			out.println(
					"7. Vegigiteralunk a cluster-ben levo repokon és egyesevel atmasoljuk a Gold working directory valtoztatasait a Front working directory-kba");
			int i = 1;
			for (String frontrepo : repos) {

				String frontrepo_url = url + svn_url_path + frontrepo;

				out.println("7." + i + " Letoltjuk a " + frontrepo + "-t ");

				String svncheckout = "svn checkout " + frontrepo_url + " -q  --username " + admin_user + " --password "
						+ admin_pwd + " " + "--quiet --non-interactive";
				out.println(svncheckout);
				out.println(cmd(svncheckout, workspace_front));

				/*
				 * File front_repo_file = new File(workspace_front+frontrepo);
				 * front_repo_file.mkdirs();
				 */

				for (String change : changes) {
					if (change.equals(""))
						break;
					String file = change.substring(4);
					System.out.println(file);
					File new_file = null;

					if (change.startsWith("D")) {
						new_file = new File(workspace_front + frontrepo + separator + file);
						if (new_file.exists()) {
							new_file.delete();
							cmd("svn delete " + file, workspace_front + frontrepo);
						}
					}

					if (FilenameUtils.getExtension(file).equals("wtspec4m")) {
						String lens;

						String svn_cat1 = "svnlook cat -r " + txn + " " + gold_repo + " " + access_control_rules_path;
						String svn_cat2 = "svnlook cat -r " + txn + " " + gold_repo + " " + lock_queries_path;

						FileUtils.writeStringToFile(new File(workspace_gold + access_control_rules_path),
								cmd(svn_cat1).get(0), Charset.defaultCharset());
						FileUtils.writeStringToFile(new File(workspace_gold + lock_queries_path), cmd(svn_cat2).get(0),
								Charset.defaultCharset());
						
						f1 = new File(workspace_front+frontrepo + access_control_rules_path);
						f2 = new File(workspace_front+frontrepo + lock_queries_path);

						out.println("access control rules path: " +f1.toString());
						
						if (f1.exists() && f2.exists()) {
							out.println(
									"8.1 Rules és Queries léteznek és a fájltípus megegyezik a wtspec4m-mel. Végrehajtjuk a lencse transzformációkat");
							lens = "java -jar invoker.jar " + front_user + " " + workspace_gold + file + " "
									+ workspace_front + frontrepo + separator + file + " -performPutBack "
									+ working_directory + " salt_" + gold_repo_name + " seed_" + gold_repo_name
									+ "mondo " + workspace_gold + access_control_rules_path + " " + workspace_gold
									+ lock_queries_path + " " + root;
							out.println("8.2 lencséket hajtottuk végre:");
							out.println(lens);
							out.println(cmd(lens, working_directory + "invoker" + separator));
						}
					}
				}
				k++;
				out.println(
						"7." + i + ".1 Atmasoljuk a Gold Working directory tartalmat a Front Working directoryba (jelenlegi front repo: "
								+ frontrepo + " )");
				File front = new File(workspace_front + frontrepo);
				File gold = new File(workspace_gold);
				FileUtils.copyDirectory(gold, front);

				out.println("7." + i++ + ".2 Addoljuk es Commitoljuk a valtoztatasokat a megfelelo front repoba");
				String svn_add = "svn add --force * --auto-props --parents --depth infinity -q";
				String svn_add2 = "svn --force add .";
				out.println(svn_add + " hol:" + workspace_front + frontrepo);
				out.println(cmd(svn_add, workspace_front + frontrepo));
				String svn_commit = "svn commit -m \"" + commit_message + "\" --username " + front_user + " --password "
						+ front_user + " --quiet --non-interactive";
				out.println(svn_commit);
				out.println(cmd(svn_commit, workspace_front + frontrepo));

			}
			out.println("8. Toroljuk a working directoryk tartalmat");
			FileUtils.deleteQuietly(temp.toFile());

			out.println("9. Toroljuk a lock fajlt");
			// out.println("10. Kiléptem: " + System.currentTimeMillis());
			// unlock(changes);
			lock.deleteOnExit();
			out.flush();
			out.close();
		} catch (Exception e) {
			e.printStackTrace();
			lock.deleteOnExit();
			out.println("upsz valami hiba tortent: \n" + e.getMessage() + "\n" + e.getCause());
			out.flush();
			out.close();
		}
	}

	public static ArrayList<String> findAll(BufferedReader r) throws IOException {
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

	public ArrayList<String> cmd2(String command) throws IOException {
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

		return result;
	}

	public static ArrayList<String> cmd(String command) throws IOException {
		ArrayList<String> result = new ArrayList<String>();
		String s;
		try {
			Process p = Runtime.getRuntime().exec(command);
			BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
			while ((s = br.readLine()) != null)
				result.add(s);
			p.waitFor();
			System.out.println("exit: " + p.exitValue());
			p.destroy();
		} catch (Exception e) {

		} finally {
		}
		return result;
	}

	public void unlock(ArrayList<String> changes) throws IOException {

		int i = 1;
		for (String change : changes) {
			if (change.equals(""))
				break;
			String file = change.substring(4);
			System.out.println(file);
			// File new_file = new File(workspace_front + file);
			// System.out.println(workspace_front + file);

			if (change.startsWith("A") || change.startsWith("U") || change.startsWith("UU")) {
				String unlock = "svn unlock " + gold_repos_url + "/" + file;
				System.out.println(unlock);
				out.println((cmd(unlock)));
			}
		}
	}

	private void checklock() throws Exception {
		lock = new File(working_directory + "lock" + separator + "lock.properties");
		if (lock.exists()) {
			Properties prop = new Properties();
			InputStream input = new FileInputStream(lock);
			prop.load(input);
			String commiter_repo = prop.getProperty("repo");
			input.close();
			out.println("5.1 Letezik a lock file. A commitolo repo:" + commiter_repo);
			String keresettrepo = null;
			for (String repo : repos) {
				out.println(repo);
				if (commiter_repo.contains(repo.subSequence(0, repo.length() - 1))) {
					// repos.remove(repo);
					keresettrepo = repo;
					out.println(repo + "-t kivettuk a listabol");
				}
			}
			repos.remove(keresettrepo);
		} else {
			lock.createNewFile();
			PrintWriter pw = new PrintWriter(lock);
			pw.println("repo=\"" + gold_repo + "\"");

			pw.flush();
			pw.close();
		}
	}

	public ArrayList<String> cmd(String command, String where_to) throws IOException {
		ArrayList<String> result = new ArrayList<String>();
		String s;
		try {
			File where = new File(where_to);
			Process p = Runtime.getRuntime().exec(command, null, where);
			// ProcessBuilder pb = new ProcessBuilder(command);
			// if(where.exists())
			// out.println("létezik");
			// pb.directory(where);
			// Process p= pb.start();
			BufferedReader error = new BufferedReader(new InputStreamReader(p.getErrorStream()));
			while ((s = error.readLine()) != null)
				out.println(s);

			BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));

			while ((s = br.readLine()) != null)
				result.add(s);
			p.waitFor();
			out.println("exit: " + p.exitValue());
			p.destroy();
		} catch (Exception e) {
			out.println(e.getMessage());
		} finally {
		}
		return result;
	}

}
