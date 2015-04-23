/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mleko.brzdac.crawler;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import mleko.brzdac.crawler.communication.TelnetServer;
import mleko.brzdac.crawler.db.Database;
import mleko.brzdac.crawler.worker.ServerScanner;

/**
 *
 * @author mleko
 */
public class BrzdacCrawler {

	/**
	 * @param args the command line arguments
	 */
	public static void main(String[] args) {
		try {
			if (args.length > 0) {
				Config.setConfigurationFile(args[0]);
			} else {
				Config.setConfigurationFile("./config.ini");
			}

			Logger.getLogger("").setLevel(Level.ALL);

			FileHandler fileHandler = new FileHandler("./info.log.xml", true);
			fileHandler.setLevel(Level.INFO);
			Logger.getLogger("").addHandler(fileHandler);

			ServerScanner serverScanner = new ServerScanner();
			new Thread(serverScanner, "ServerScannerThread").start();

			TelnetServer telnetServer = new TelnetServer(serverScanner);
			Logger.getLogger("").addHandler(telnetServer);
			new Thread(telnetServer, "CommunicationServer").start();

			Database.ClearLongInactiveHosts();

		} catch (IllegalStateException | IOException | SecurityException ex) {
			Logger.getLogger(BrzdacCrawler.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

}

class StdOutHandler extends Handler {

	SimpleFormatter f = new SimpleFormatter();

	@Override
	public void publish(LogRecord record) {
		System.out.print(f.format(record));
	}

	@Override
	public void flush() {
		System.out.flush();
	}

	@Override
	public void close() throws SecurityException {
		System.out.flush();
	}
}
