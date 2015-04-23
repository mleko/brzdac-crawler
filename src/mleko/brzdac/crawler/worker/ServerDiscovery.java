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
import java.util.Calendar;
import java.util.logging.Level;
import java.util.logging.Logger;
import mleko.brzdac.crawler.Utils;
import mleko.brzdac.crawler.db.dao.HostDAO;
import mleko.brzdac.crawler.pojo.Host;

/**
 *
 * @author mleko
 */
class ServerDiscovery implements Runnable {

	private boolean run = false;
	private long runTill = 0;
	private long nextIpLong;
	private final long startIpLong;
	private final long endIpLong;
	private final FileScannerTask fileScanner;

	static public final int DiscoveryDelay;

	static {
		DiscoveryDelay = 60 * 60 * 5;
	}

	{
		startIpLong = Utils.ip2long("10.1.0.0");
		endIpLong = Utils.ip2long("10.12.255.255");
		nextIpLong = startIpLong;
	}

	public ServerDiscovery(FileScannerTask fileScanner) {
		this.fileScanner = fileScanner;
	}

	public void setTimer(long time) {
		runTill = Calendar.getInstance().getTime().getTime() + time;
	}

	@Override
	public void run() {
		Logger.getLogger(ServerDiscovery.class.getName()).log(Level.INFO, "Start server discovery");
		run = true;
		while (run && runTill > Calendar.getInstance().getTime().getTime()) {
			Host host = HostDAO.fetchHostToDiscovery(DiscoveryDelay);
			if (null == host) {
				host = new Host(nextIpLong++, null, null);
				if (nextIpLong > endIpLong) {
					nextIpLong = startIpLong;
				}
			}
			checkHostActivity(host);
		}
		Logger.getLogger(ServerDiscovery.class.getName()).log(Level.INFO, "End server discovery");
	}

	public synchronized void stop() {
		run = false;
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
				Logger.getLogger(ServerDiscovery.class.getName()).log(Level.FINEST, null, e);
			}
			HostDAO.discoveryStatus(host.longIp, true);
			Logger.getLogger(ServerDiscovery.class.getName()).log(Level.FINER, "Host {0} Active", Utils.long2ip(host.longIp));

			if (host.lastFileScan == null || Calendar.getInstance().getTime().getTime() - host.lastFileScan.getTime() > ServerScanner.FileScanDelay * 1000) {
				addHostToFileScanQueue(host);
			}
		} catch (IllegalStateException | IOException | FTPIllegalReplyException | FTPException e) {
			HostDAO.discoveryStatus(host.longIp, false);
			Logger.getLogger(ServerDiscovery.class.getName()).log(Level.FINER, "Host {0} InActive", Utils.long2ip(host.longIp));
		}

	}

	private void addHostToFileScanQueue(Host host) {
		fileScanner.addHostToScan(host);
	}

}
