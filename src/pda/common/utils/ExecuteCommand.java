/**
 * Copyright (C) CIC, TJU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by .
 */
package pda.common.utils;

import org.apache.commons.io.FileUtils;
import pda.common.conf.Constant;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
//import sun.awt.geom.Crossings.EvenOdd;

/**
 * This class is an interface to run script command background
 * 
 * @author Jiajun
 *
 */
public class ExecuteCommand {

	private final static String __name__ = "@ExecuteCommand ";

	/**
	 * execute given commands
	 * 
	 * @param command
	 *            : command to be executed
	 * @return output information when running given command
	 */
	private static String execute(String... command) {
		Process process = null;
		final List<String> results = new ArrayList<String>();
		try {
			ProcessBuilder builder = getProcessBuilder(command);
			builder.redirectErrorStream(true);
			process = builder.start();
			final InputStream inputStream = process.getInputStream();
			
			Thread processReader = new Thread(){
				public void run() {
					BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
					String line;
					try {
						while((line = reader.readLine()) != null) {
							results.add(line + "\n");
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
					try {
						reader.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			};
			
			processReader.start();
			try {
				processReader.join();
				process.waitFor();
			} catch (InterruptedException e) {
				LevelLogger.error(__name__ + "#execute Process interrupted !");
				return "";
			}
		} catch (IOException e) {
			LevelLogger.error(__name__ + "#execute Process output redirect exception !");
		} finally {
			if (process != null) {
				process.destroy();
			}
			process = null;
		}
		
		String result = "";
		for(String s: results) {
			result += s;
		}
		return result;
	}

	/**
	 * execute d4j test command
	 * 
	 * @param command
	 *            : commands to be executed
	 * @throws IOException
	 *             : when the file does not exist for d4j output
	 * @throws InterruptedException
	 *             : when current process is interrupted
	 */
	public static void executeDefects4JTest(String[] command) throws IOException, InterruptedException {

		Utils.deleteFiles(Constant.STR_TMP_INSTR_OUTPUT_FILE);

		File file = new File(Constant.STR_TMP_D4J_OUTPUT_FILE);
		if (!file.exists()) {
			file.getParentFile().mkdirs();
			file.createNewFile();
		}
		executeAndOutputFile(command, Constant.STR_TMP_D4J_OUTPUT_FILE);
	}

	public static List<String> executeCompile(String[] command) throws IOException, InterruptedException {
		Process process = null;
		final List<String> results = new ArrayList<String>();
		try {
			ProcessBuilder builder = getProcessBuilder(command);
			builder.redirectErrorStream(true);
			process = builder.start();
			final InputStream inputStream = process.getInputStream();
			
			Thread processReader = new Thread(){
				public void run() {
					BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
					String line;
					try {
						while((line = reader.readLine()) != null) {
							results.add(line + "\n");
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
					try {
						reader.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			};
			
			processReader.start();
			try {
				processReader.join();
				process.waitFor();
			} catch (InterruptedException e) {
				LevelLogger.error(__name__ + "#execute Process interrupted !");
				return results;
			}
		} catch (IOException e) {
			LevelLogger.error(__name__ + "#execute Process output redirect exception !");
		} finally {
			if (process != null) {
				process.destroy();
			}
			process = null;
		}
		
		return results;
	}
	
	/**
	 * execute outside command when given command and output execution
	 * information to console
	 * 
	 * @param command
	 *            : to be executed
	 * @throws IOException
	 * @throws InterruptedException
	 */
	private static void executeAndOutputConsole(String[] command) throws IOException, InterruptedException {
		Process process = null;
		try {
			ProcessBuilder builder = getProcessBuilder(command);
			builder.redirectErrorStream(true);
			process = builder.start();
			final InputStream inputStream = process.getInputStream();
			
			Thread processReader = new Thread(){
				public void run() {
					BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
					String line;
					try {
						while((line = reader.readLine()) != null) {
							LevelLogger.info(line);
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
					try {
						reader.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			};
			
			processReader.start();
			try {
				processReader.join();
				process.waitFor();
			} catch (InterruptedException e) {
				LevelLogger.error(__name__ + "#execute Process interrupted !");
			}
		} catch (IOException e) {
			LevelLogger.error(__name__ + "#execute Process output redirect exception !");
		} finally {
			if (process != null) {
				process.destroy();
			}
			process = null;
		}
	}

	/**
	 * execute given command and output execution information into file
	 * 
	 * @param command
	 *            : command to be executed
	 * @param outputFile
	 *            : output file path
	 * @return
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static void executeAndOutputFile(String[] command, String outputFile)
			throws IOException, InterruptedException {
		final BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));
		Process process = null;
		final List<String> results = new ArrayList<String>();
		try {
			ProcessBuilder builder = getProcessBuilder(command);
			builder.redirectErrorStream(true);
			process = builder.start();
			final InputStream inputStream = process.getInputStream();
			
			Thread processReader = new Thread(){
				public void run() {
					BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
					String line;
					try {
						while((line = reader.readLine()) != null) {
							writer.write(line + "\n");
							writer.flush();
							results.add(line + "\n");
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
					try {
						reader.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			};
			
			processReader.start();
			try {
				processReader.join();
				process.waitFor();
			} catch (InterruptedException e) {
				LevelLogger.error(__name__ + "#execute Process interrupted !");
			}
			writer.close();
		} catch (IOException e) {
			LevelLogger.error(__name__ + "#execute Process output redirect exception !");
		} finally {
			if (process != null) {
				process.destroy();
			}
			process = null;
		}
	}
	
	private static ProcessBuilder getProcessBuilder(String[] command) { 
		ProcessBuilder builder = new ProcessBuilder(command);
		Map<String, String> evn = builder.environment();
		evn.put("JAVA_HOME", Constant.COMMAND_JAVA_HOME);
		evn.put("PATH", Constant.COMMAND_JAVA_HOME + "/bin:" + evn.get("PATH"));
		evn.put("PATH", "/usr/local/bin:" + evn.get("PATH"));
		return builder;
	}
}
