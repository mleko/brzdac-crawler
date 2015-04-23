/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mleko.brzdac.crawler.db.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import mleko.brzdac.crawler.db.Database;
import mleko.brzdac.crawler.db.DatabaseConnection;
import mleko.brzdac.crawler.exceptions.WTFException;
import mleko.brzdac.crawler.pojo.Directory;
import mleko.brzdac.crawler.pojo.File;

/**
 *
 * @author mleko
 */
public class DirectoryDAO {

	/**
	 *
	 * @param ip
	 * @param directory
	 * @return
	 * @throws mleko.brzdac.crawler.exceptions.WTFException
	 */
	public static ArrayList<File> getDirectoryContent(long ip, Directory directory) throws WTFException {
		try {
			ArrayList<File> fileList = new ArrayList<>();
			int directoryId = getDirectoryId(directory);
			PreparedStatement select = DatabaseConnection.getInstance().prepareStatement("SELECT hf.size, f.name, f.id FROM host_file hf JOIN file f ON hf.file_id=f.id WHERE host = ? AND hf.directory_id = ?");
			select.setLong(1, ip);
			select.setInt(2, directoryId);
			ResultSet executeQuery = select.executeQuery();
			while (executeQuery.next()) {
				File f = new File(executeQuery.getInt("f.id"), executeQuery.getString("f.name"), executeQuery.getLong("hf.size"));
				fileList.add(f);
			}
			return fileList;
		} catch (SQLException ex) {
			Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
		}
		return null;
	}

	public static ArrayList<Directory> readCurrentDirectories(long ip) {
		try {
			ArrayList<Directory> directoryList = new ArrayList<>();
			Connection db = DatabaseConnection.getInstance();
			PreparedStatement statement = db.prepareStatement("SELECT d.path FROM host_file hf JOIN directory d ON d.id=hf.directory_id WHERE host = ? GROUP BY hf.directory_id");
			statement.setLong(1, ip);
			ResultSet result = statement.executeQuery();
			while (result.next()) {
				String directory = result.getString("path");
				directoryList.add(new Directory(directory));
			}
			return directoryList;
		} catch (SQLException ex) {
			Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
		}
		return null;
	}

	protected static int getDirectoryId(Directory path) throws SQLException, WTFException {
		DatabaseConnection.getInstance().setAutoCommit(false);
		try {
			PreparedStatement prepareStatement = DatabaseConnection.getInstance().prepareStatement("SELECT id FROM directory WHERE crc = CRC32(LOWER(?)) AND path = ?");
			prepareStatement.setString(1, path.getPath());
			prepareStatement.setString(2, path.getPath());
			ResultSet executeQuery = prepareStatement.executeQuery();
			if (executeQuery.next()) {
				return executeQuery.getInt(1);
			}
			PreparedStatement prepareStatement1 = DatabaseConnection.getInstance().prepareStatement("INSERT INTO directory(path,crc) VALUES(?,CRC32(LOWER(?)))");
			prepareStatement1.setString(1, path.getPath());
			prepareStatement1.setString(2, path.getPath());
			prepareStatement1.execute();
			DatabaseConnection.getInstance().commit();
			ResultSet generatedKeys = prepareStatement1.getGeneratedKeys();
			if (generatedKeys.next()) {
				return generatedKeys.getInt(1);
			}
		} finally {
			if (!DatabaseConnection.getInstance().getAutoCommit()) {
				DatabaseConnection.getInstance().setAutoCommit(true);
			}
		}
		throw new WTFException("Shoudn't be here");
	}

	public static boolean removeDirectories(long longIp, ArrayList<Directory> directoryList) throws WTFException {
		try {
			PreparedStatement prepareStatement = DatabaseConnection.getInstance().prepareStatement("DELETE FROM host_file WHERE directory_id = ? AND host = ?");
			prepareStatement.setLong(2, longIp);
			for (Directory directory : directoryList) {
				prepareStatement.setInt(1, getDirectoryId(directory));
				prepareStatement.executeUpdate();
			}
			return true;
		} catch (SQLException ex) {
			Logger.getLogger(DirectoryDAO.class.getName()).log(Level.SEVERE, null, ex);
		}
		return false;
	}

}
