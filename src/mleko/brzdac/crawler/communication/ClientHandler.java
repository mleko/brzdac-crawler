/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mleko.brzdac.crawler.communication;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.Calendar;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import mleko.brzdac.crawler.Utils;
import mleko.brzdac.crawler.db.Database;
import mleko.brzdac.crawler.pojo.Host;
import mleko.brzdac.crawler.util.AnsiEscapeCodeBuilder;
import mleko.brzdac.crawler.worker.ServerScanner;

/**
 *
 * @author mleko
 */
class ClientHandler implements Runnable {

	private boolean run;

	private final Socket socket;
	private final BufferedWriter writer;
	private final BufferedReader reader;

	ServerScanner managedScanner;

	Level logLevel;
	String logClass;
	SimpleFormatter simpleFormatter;
	boolean colors = false;
	AnsiEscapeCodeBuilder ansiBuilder = new AnsiEscapeCodeBuilder();

	{
		simpleFormatter = new SimpleFormatter();
		logLevel = Level.OFF;
		logClass = "";
	}

	public ClientHandler(Socket accept, ServerScanner scanner) throws IOException {
		socket = accept;
		writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
		reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		managedScanner = scanner;
	}

	@Override
	public void run() {
		if (!socket.isConnected()) {
			return;
		}
		run = true;
		try {
			while (run) {
				String line = reader.readLine();
				if (null == line) {
					break;
				}
				if (line.startsWith("log ")) {
					handleLogCommand(line.substring(4));
				} else if (line.equals("date")) {
					writeToSocket(Calendar.getInstance().getTime().toString() + "\n");
				} else if (line.startsWith("host ")) {
					handleHostCommand(line.substring("host ".length()));
				} else if (line.equals("dbclean")) {
					writeToSocket("Starting DB clean\n");
					Database.ClearLongInactiveHosts();
				} else if (line.equals("color on")) {
					colors = true;
					writeToSocket(ansiBuilder.foregroundColor(AnsiEscapeCodeBuilder.Color.BLUE, true).build() + "Colors enabled\n" + ansiBuilder.reset().build());
				} else if (line.equals("color off")) {
					colors = false;
					writeToSocket("Colors disabled\n");
				} else {
					writeToSocket("Unknown command\nAvailable commands: log, date, dbclean\n");
				}

			}
		} catch (IOException ex) {
			Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
		} finally {
			run = false;
			try {
				reader.close();
			} catch (IOException ex) {
			}
			try {
				writer.close();
			} catch (IOException ex) {
			}
			try {
				socket.close();
			} catch (IOException ex) {
			}
		}
	}

	void stop() {
		synchronized (this) {
			run = false;
			logLevel = Level.OFF;
		}
	}

	void handleLog(LogRecord record) {
		if (Level.OFF != record.getLevel() && record.getLevel().intValue() >= logLevel.intValue() && record.getLoggerName().startsWith(logClass)) {
			String entry = simpleFormatter.format(record);
			String color = null;
			if (colors) {
				int level = record.getLevel().intValue();
				if (level >= Level.SEVERE.intValue()) {
					color = ansiBuilder.foregroundColor(AnsiEscapeCodeBuilder.Color.RED, true).build();
				} else if (level >= Level.WARNING.intValue()) {
					color = ansiBuilder.foregroundColor(AnsiEscapeCodeBuilder.Color.YELLOW, false).build();
				} else if (level >= Level.INFO.intValue()) {
					color = ansiBuilder.foregroundColor(AnsiEscapeCodeBuilder.Color.BLUE, true).build();
				} else if (level <= Level.FINER.intValue()) {
					color = ansiBuilder.foregroundColor(AnsiEscapeCodeBuilder.Color.GREEN, false).build();
				} else if (level <= Level.FINEST.intValue()) {
					color = ansiBuilder.foregroundColor(AnsiEscapeCodeBuilder.Color.GREEN, true).build();
				} else {
					color = ansiBuilder.reset().build();
				}
			}
			entry = color + entry + ansiBuilder.reset().build();
			writeToSocket(entry);
		}
	}

	private void writeToSocket(String message) {
		synchronized (this) {
			try {
				writer.write(message);
				writer.flush();

			} catch (IOException ex) {
				if (!socket.isConnected()) {
					this.stop();
				}
			}
		}
	}

	private void handleLogCommand(String cmd) {

		String param = "";
		String command;
		if (cmd.contains(" ")) {
			command = cmd.substring(0, cmd.indexOf(" ")).toLowerCase();
			param = cmd.substring(cmd.indexOf(" ") + 1);
		} else {
			command = cmd.toLowerCase();
		}

		switch (command) {
			case "level":
				if (!param.equals("")) {
					try {
						Level parse = Level.parse(param);
						logLevel = parse;
					} catch (IllegalArgumentException e) {
					}
				}
				writeToSocket("Current log level: " + logLevel.getName() + " (" + logLevel.intValue() + ")\n");
				break;
			case "class":
				if (!param.equals("")) {
					logClass = param;
				}
				writeToSocket("Current log class: " + logClass + "\n");
				break;
			default:
				writeToSocket("log: Unknown command\nAvailable commands: level, class\n");
				break;
		}
	}

	private void handleHostCommand(String cmd) {
		String param = "";
		String command;
		if (cmd.contains(" ")) {
			command = cmd.substring(0, cmd.indexOf(" ")).toLowerCase();
			param = cmd.substring(cmd.indexOf(" ") + 1);
		} else {
			command = cmd.toLowerCase();
		}

		switch (command) {
			case "scan":
				if (param.length() > 6) {
					try {
						long ip = Utils.ip2long(param);
						managedScanner.addHostToFileScanQueue(new Host(ip, null, null));
					} catch (IllegalArgumentException ex) {
						writeToSocket("Invalid address specified\n");
					}
				}
				Host[] hostsQueuedToScan = managedScanner.getHostsQueuedToScan();
				writeToSocket("Scan queue size: " + hostsQueuedToScan.length + "\n");
				for (Host host : hostsQueuedToScan) {
					writeToSocket(Utils.long2ip(host.longIp) + "\n");
				}
				break;
			case "class":
				if (!param.equals("")) {
					logClass = param;
				}
				writeToSocket("Current log class: " + logClass + "\n");
				break;
			default:
				writeToSocket("host: Unknown command\nAvailable commands: scan\n");
				break;
		}
	}

}
