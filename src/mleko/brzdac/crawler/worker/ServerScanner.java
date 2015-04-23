/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mleko.brzdac.crawler.worker;

import it.sauronsoftware.ftp4j.FTPClient;
import it.sauronsoftware.ftp4j.FTPException;
import it.sauronsoftware.ftp4j.FTPIllegalReplyException;
import java.io.IOException;
import mleko.brzdac.crawler.db.dao.HostDAO;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import mleko.brzdac.crawler.Utils;
import mleko.brzdac.crawler.pojo.Host;

/**
 *
 * @author mleko
 */
public class ServerScanner implements Runnable {

	private boolean run = true;

	static public final int ActivityScanMinDelay,
			ActivityScanDelay,
			FileScanDelay;

	static {
		ActivityScanMinDelay = 60 * 5;
		ActivityScanDelay = 60 * 10;
		FileScanDelay = 60 * 60 * 20;
	}

	private final FileScannerTask fileScanner;
	private final ServerDiscovery serverDiscovery;

	{
		fileScanner = new FileScannerTask();
		serverDiscovery = new ServerDiscovery(fileScanner);
	}

	@Override
	public void run() {
		Logger.getLogger(ServerScanner.class.getName()).log(Level.INFO, "Start server scanner thread");
		run = true;
		Thread fileScannerThread = new Thread(fileScanner, "HostFileScanner");
		fileScannerThread.start();
		Thread discoveryThread = new Thread(serverDiscovery, serverDiscovery.getClass().getName());

		try {
			while (run) {
				ArrayList<Host> hostList = HostDAO.fetchHostsToActivityScan(ActivityScanMinDelay);
				for (Host host : hostList) {
					checkHostActivity(host);
				}
				Date oldestScanDate = HostDAO.fetchOldestActivityScanDate();

				long sleepDuration = (null != oldestScanDate)
						? (oldestScanDate.getTime() + ActivityScanDelay * 1000) - Calendar.getInstance().getTime().getTime()
						: ActivityScanDelay;
				if (sleepDuration > 0) {
					try {
						if (sleepDuration > ActivityScanDelay * 1000) {
							Logger.getLogger(ServerScanner.class.getName()).log(Level.WARNING,
									"sleepDuration strangely long [{0}], reducing to {1}",
									new Object[]{sleepDuration, ActivityScanDelay * 1000}
							);
							sleepDuration = ActivityScanDelay * 1000;
						}
						synchronized (this) {
							Logger.getLogger(ServerScanner.class.getName()).log(Level.INFO, "Sleep {0} milisecond(s)", sleepDuration);
							if (sleepDuration > 2000) {
								serverDiscovery.setTimer((sleepDuration - 2000) / 4);
								discoveryThread = new Thread(serverDiscovery, serverDiscovery.getClass().getName());
								discoveryThread.start();
							}
							this.wait(sleepDuration);
						}
					} catch (InterruptedException ex) {
					} finally {
						serverDiscovery.stop();
						discoveryThread.join();
					}
				}

			}

			fileScannerThread.join();
		} catch (InterruptedException ex) {
		}
	}

	public void stop() {
		synchronized (this) {
			fileScanner.stop();
			serverDiscovery.stop();
			run = false;
			this.notify();
		}
	}

	private void checkHostActivity(Host host) {
		FTPClient client = new FTPClient();
		client.getConnector().setConnectionTimeout(1);

		try {
			String ip = Utils.long2ip(host.longIp);
			client.connect(ip);
			try {
				client.disconnect(false);
			} catch (FTPException | FTPIllegalReplyException | IOException | IllegalStateException e) {
				Logger.getLogger(ServerScanner.class.getName()).log(Level.FINEST, null, e);
			}
			HostDAO.updateActivityState(host.longIp, true);
			Logger.getLogger(ServerScanner.class.getName()).log(Level.FINER, "Host {0} Active", Utils.long2ip(host.longIp));

			if (host.lastFileScan == null || Calendar.getInstance().getTime().getTime() - host.lastFileScan.getTime() > FileScanDelay * 1000) {
				addHostToFileScanQueue(host);
			}
		} catch (FTPException | FTPIllegalReplyException | IOException | IllegalStateException e) {
			HostDAO.updateActivityState(host.longIp, false);
			Logger.getLogger(ServerScanner.class.getName()).log(Level.FINER, "Host {0} InActive", Utils.long2ip(host.longIp));
		}

	}

	public void addHostToFileScanQueue(Host host) {
		fileScanner.addHostToScan(host);
	}

	public Host[] getHostsQueuedToScan() {
		return fileScanner.getHostsQueuedToScan();
	}
}
