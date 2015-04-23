/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mleko.brzdac.crawler.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import mleko.brzdac.crawler.Config;

/**
 *
 * @author mleko
 */
public class DatabaseConnection {

	private static Connection connection;

	private DatabaseConnection() {

	}

	static {
		try {
			Class.forName("com.mysql.jdbc.Driver");
			connection = DriverManager.getConnection(
					"jdbc:mysql://" + Config.getInstance().get("database", "host") + "/" + Config.getInstance().get("database", "dbname") + "?"
					+ "user=" + Config.getInstance().get("database", "user") + "&password=" + Config.getInstance().get("database", "password"));

		} catch (ClassNotFoundException | SQLException ex) {
			Logger.getLogger(DatabaseConnection.class.getName()).log(Level.SEVERE, null, ex);
			System.exit(1);
		}
	}

	public static synchronized Connection getInstance() {
		return connection;
	}

}
