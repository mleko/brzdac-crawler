/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mleko.brzdac.crawler.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import mleko.brzdac.crawler.Utils;

/**
 *
 * @author mleko
 */
public class Database {

	public static void addHostIfNotExist(long ip) throws SQLException {
		PreparedStatement select = DatabaseConnection.getInstance().prepareStatement("SELECT COUNT(1) FROM host WHERE host = ?");
		select.setLong(1, ip);
		ResultSet executeQuery = select.executeQuery();
		executeQuery.next();
		if (executeQuery.getInt(1) == 0) {
			PreparedStatement createStatement = DatabaseConnection.getInstance().prepareStatement(
					"INSERT INTO host(host) VALUES (?)"
			);
			createStatement.setLong(1, ip);
			createStatement.execute();
		}
	}

	public static void ClearLongInactiveHosts() {
		try {
			Logger.getLogger(Database.class.getName()).log(Level.INFO, "Clearing long inactive hosts");
			PreparedStatement deleteHostStatement = DatabaseConnection.getInstance().prepareStatement("DELETE FROM host WHERE host = ?");
			PreparedStatement deleteHostFilesStatement = DatabaseConnection.getInstance().prepareStatement("DELETE FROM host_file WHERE host = ?");
			PreparedStatement deleteHostUptimeStatement = DatabaseConnection.getInstance().prepareStatement("DELETE FROM host_uptime WHERE host = ?");

			PreparedStatement prepareStatement = DatabaseConnection.getInstance().prepareStatement("SELECT host FROM host WHERE DATE_SUB(NOW(), INTERVAL 30 DAY) > last_activity_date");
			ResultSet executeQuery = prepareStatement.executeQuery();

			int counter = 0;
			while (executeQuery.next()) {
				Logger.getLogger(Database.class.getName()).log(Level.FINEST, "Deleting old host : {0}", Utils.long2ip(executeQuery.getInt(1)));
				deleteHostUptimeStatement.setInt(1, executeQuery.getInt(1));
				deleteHostUptimeStatement.execute();
				deleteHostFilesStatement.setInt(1, executeQuery.getInt(1));
				deleteHostFilesStatement.execute();
				deleteHostStatement.setInt(1, executeQuery.getInt(1));
				deleteHostStatement.execute();
				counter++;
				Logger.getLogger(Database.class.getName()).log(Level.FINER, "Deleted old host : {0}", Utils.long2ip(executeQuery.getInt(1)));
			}
			Logger.getLogger(Database.class.getName()).log(Level.INFO, "Deleted {0} host(s)", counter);
		} catch (SQLException ex) {
			Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

}
