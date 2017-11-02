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

public class Main {

	public static void main(String[] args) throws IOException, InterruptedException {
	
		Script sc=new Script(args);
		sc.Run();
	}
}
