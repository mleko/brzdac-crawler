/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mleko.brzdac.crawler.worker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import mleko.brzdac.crawler.Utils;
import mleko.brzdac.crawler.exceptions.WTFException;
import mleko.brzdac.crawler.filescanner.FileScanner;
import mleko.brzdac.crawler.pojo.Host;

/**
 *
 * @author mleko
 */
class FileScannerTask implements Runnable {

	private boolean run = true;
	private final Queue<Host> hosts;
	private final List<Host> lockedHosts;

	{
		hosts = new ConcurrentLinkedQueue<>();
		lockedHosts = Collections.synchronizedList(new ArrayList<Host>());
	}

	public void stop() {
		synchronized (this) {
			if (run) {
				run = false;
				this.notify();
			}
		}
	}

	@Override
	public void run() {
		Logger.getLogger(FileScannerTask.class.getName()).log(Level.INFO, "FileScannerTask Start");
		run = true;
		while (run) {
			Host host = getHostToScan();
			if (null == host) {
				try {
					Logger.getLogger(FileScannerTask.class.getName()).log(Level.FINE, "No host to file scan, wait");
					synchronized (this) {
						this.wait();
					}
				} catch (InterruptedException ex) {
				}
				continue;
			}
			try {
				new FileScanner(Utils.long2ip(host.longIp)).scan();
			} catch (WTFException ex) {
				Logger.getLogger(FileScannerTask.class.getName()).log(Level.SEVERE, null, ex);
			} finally {
				synchronized (this) {
					lockedHosts.remove(host);
				}
			}
		}
	}

	private Host getHostToScan() {
		synchronized (this) {
			Host host = hosts.poll();
			if (null != host) {
				lockedHosts.add(host);
			}
			return host;
		}
	}

	/**
	 * Returns <tt>true</tt> if host is added or already in queue
	 *
	 * @param host
	 * @return
	 */
	public boolean addHostToScan(Host host) {
		synchronized (this) {
			if (!hosts.contains(host) && !lockedHosts.contains(host)) {
				boolean added = hosts.offer(host);
				if (added) {
					Logger.getLogger(FileScannerTask.class.getName()).log(Level.INFO, "Host {0} added to file scan queue", Utils.long2ip(host.longIp));
					synchronized (this) {
						this.notify();
					}
				}
				return added;
			}
			return false;
		}
	}

	public Host[] getHostsQueuedToScan() {
		Host[] a = new Host[0];
		return hosts.toArray(a);
	}

}
