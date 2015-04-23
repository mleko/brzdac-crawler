/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mleko.brzdac.crawler.pojo;

import java.util.Date;

/**
 *
 * @author mleko
 */
public class Host {

	public long longIp;
	public Date lastScanAttempt;
	public Date lastFileScan;

	public Host(long longIp, Date lastScanAttempt, Date lastFileScan) {
		this.longIp = longIp;
		this.lastScanAttempt = lastScanAttempt;
		this.lastFileScan = lastFileScan;
	}

	@Override
	public int hashCode() {
		int hash = 5;
		hash = 43 * hash + (int) (this.longIp ^ (this.longIp >>> 32));
		return hash;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final Host other = (Host) obj;
		return this.longIp == other.longIp;
	}

}
