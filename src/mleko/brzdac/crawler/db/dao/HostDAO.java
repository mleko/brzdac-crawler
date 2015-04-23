/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mleko.brzdac.crawler.db.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import mleko.brzdac.crawler.db.DatabaseConnection;
import mleko.brzdac.crawler.exceptions.WTFException;
import mleko.brzdac.crawler.filescanner.Changes;
import mleko.brzdac.crawler.pojo.Host;

/**
 *
 * @author mleko
 */
public class HostDAO {

	public static ArrayList<Host> fetchHostsToActivityScan(int ScanDelay) {
		ArrayList<Host> hosts = new ArrayList<Host>();
		try {
			PreparedStatement selectStatement = DatabaseConnection.getInstance().prepareStatement(
					"SELECT host, last_scan_attempt, last_file_scan_date FROM host "
					+ "WHERE "
					+ "(DATE_ADD(last_scan_attempt, INTERVAL ? SECOND) < ? OR last_scan_attempt IS NULL) "
					+ "AND shared > 0"
			);
			long now = Calendar.getInstance().getTime().getTime();
			selectStatement.setInt(1, ScanDelay);
			selectStatement.setTimestamp(2, new java.sql.Timestamp(now));
			ResultSet rows = selectStatement.executeQuery();
			while (rows.next()) {
				hosts.add(new Host(rows.getInt(1), rows.getTimestamp(2), rows.getTimestamp(3)));
			}

		} catch (SQLException ex) {
			Logger.getLogger(HostDAO.class.getName()).log(Level.SEVERE, null, ex);
		}
		return hosts;
	}

	public static Date fetchOldestActivityScanDate() {
		try {
			PreparedStatement selectDateStatement = DatabaseConnection.getInstance().prepareStatement("SELECT MIN(last_scan_attempt) FROM host WHERE shared > 0");
			ResultSet dateResultRow = selectDateStatement.executeQuery();
			if (dateResultRow.next()) {
				return dateResultRow.getTimestamp(1);
			}
		} catch (SQLException ex) {
			Logger.getLogger(HostDAO.class.getName()).log(Level.SEVERE, null, ex);
		}
		return Calendar.getInstance().getTime();
	}

	public static void updateActivityState(long longIp, boolean active) {
		try {
			long now = Calendar.getInstance().getTime().getTime();
			PreparedStatement insertUptimeRecordStatement = DatabaseConnection.getInstance().prepareStatement("INSERT INTO host_uptime(host,active,timestamp) VALUES(?,?,?)");
			insertUptimeRecordStatement.setLong(1, longIp);
			insertUptimeRecordStatement.setBoolean(2, active);
			insertUptimeRecordStatement.setTimestamp(3, new java.sql.Timestamp(now));
			insertUptimeRecordStatement.execute();
			if (active) {
				PreparedStatement updateStatement = DatabaseConnection.getInstance().prepareStatement(
						"UPDATE host SET active = 1, last_scan_attempt = ?, last_activity_date = ? WHERE host = ?"
				);
				updateStatement.setTimestamp(1, new java.sql.Timestamp(now));
				updateStatement.setTimestamp(2, new java.sql.Timestamp(now));
				updateStatement.setLong(3, longIp);
				updateStatement.execute();

			} else {
				PreparedStatement updateStatement = DatabaseConnection.getInstance().prepareStatement(
						"UPDATE host SET active = 0, last_scan_attempt = ? WHERE host = ?"
				);
				updateStatement.setTimestamp(1, new java.sql.Timestamp(now));
				updateStatement.setLong(2, longIp);
				updateStatement.execute();
			}
		} catch (SQLException ex) {
			Logger.getLogger(HostDAO.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	public static void UpdateFileScanDate(long longIp) {
		try {

			PreparedStatement updateStatement = DatabaseConnection.getInstance().prepareStatement(
					"UPDATE host SET last_file_scan_date = ? WHERE host = ?"
			);
			long now = Calendar.getInstance().getTime().getTime();
			updateStatement.setTimestamp(1, new java.sql.Timestamp(now));
			updateStatement.setLong(2, longIp);
			updateStatement.execute();

		} catch (SQLException ex) {
			Logger.getLogger(HostDAO.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	public static void UpdateShareSize(long longIp) {
		try {
			PreparedStatement selectStatement = DatabaseConnection.getInstance().prepareStatement(
					"SELECT SUM(size) FROM host_file WHERE host = ?"
			);
			selectStatement.setLong(1, longIp);
			ResultSet result = selectStatement.executeQuery();
			if (!result.first()) {
				return;
			}
			long shares = result.getLong(1);
			PreparedStatement updateStatement = DatabaseConnection.getInstance().prepareStatement("UPDATE host SET shared = ? WHERE host = ?");
			updateStatement.setLong(1, shares);
			updateStatement.setLong(2, longIp);
			updateStatement.execute();
		} catch (SQLException ex) {
			Logger.getLogger(HostDAO.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	public static void applyFileChanges(long longIp, Changes changes) throws WTFException {
		if (!changes.removedDirectories.isEmpty()) {
			DirectoryDAO.removeDirectories(longIp, changes.removedDirectories);
		}
		if (!changes.removedFiles.isEmpty()) {
			FileDAO.removeHostFiles(longIp, changes.removedFiles);
		}
		if (!changes.newFiles.isEmpty()) {
			FileDAO.addHostFiles(longIp, changes.newFiles);
		}
	}

	public static Host fetchHostToDiscovery(int ScanDelay) {
		try {
			PreparedStatement selectStatement = DatabaseConnection.getInstance().prepareStatement(
					"SELECT host, last_scan_attempt, last_file_scan_date FROM host "
					+ "WHERE "
					+ "(DATE_ADD(last_scan_attempt, INTERVAL ? SECOND) < ? OR last_scan_attempt IS NULL) "
					+ "AND shared = 0 LIMIT 1"
			);
			long now = Calendar.getInstance().getTime().getTime();
			selectStatement.setInt(1, ScanDelay);
			selectStatement.setTimestamp(2, new java.sql.Timestamp(now));
			ResultSet rows = selectStatement.executeQuery();
			if (rows.first()) {
				return new Host(rows.getInt(1), rows.getTimestamp(2), rows.getTimestamp(3));
			} else {
				return null;
			}

		} catch (SQLException ex) {
			Logger.getLogger(HostDAO.class.getName()).log(Level.SEVERE, null, ex);
		}
		return null;
	}

	public static void discoveryStatus(long longIp, boolean active) {
		try {
			long now = Calendar.getInstance().getTime().getTime();
			if (active) {
				PreparedStatement discoverStatement = DatabaseConnection.getInstance().prepareStatement(
						"INSERT INTO host(host,active,last_scan_attempt, last_activity_date) "
						+ "VALUES "
						+ "(?,1,?,?) "
						+ "ON DUPLICATE KEY UPDATE "
						+ "active = 1, last_scan_attempt = VALUES(last_scan_attempt), last_activity_date = VALUES(last_activity_date)"
				);
				discoverStatement.setLong(1, longIp);
				discoverStatement.setTimestamp(2, new java.sql.Timestamp(now));
				discoverStatement.setTimestamp(3, new java.sql.Timestamp(now));
				discoverStatement.execute();
			} else {
				PreparedStatement updateStatement = DatabaseConnection.getInstance().prepareStatement(
						"UPDATE host SET last_scan_attempt = ?, active = 0 WHERE host = ?");
				updateStatement.setTimestamp(1, new java.sql.Timestamp(now));
				updateStatement.setLong(2, longIp);
				updateStatement.execute();
			}

		} catch (SQLException ex) {
			Logger.getLogger(HostDAO.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

}
