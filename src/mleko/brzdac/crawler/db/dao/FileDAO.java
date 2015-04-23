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
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import mleko.brzdac.crawler.Utils;
import mleko.brzdac.crawler.db.Database;
import mleko.brzdac.crawler.db.DatabaseConnection;
import mleko.brzdac.crawler.exceptions.WTFException;
import mleko.brzdac.crawler.pojo.Directory;
import mleko.brzdac.crawler.pojo.File;

/**
 *
 * @author mleko
 */
public class FileDAO {

	public static void removeHostFiles(long longIp, HashMap<Directory, ArrayList<File>> dirContent) throws WTFException {
		try {
			PreparedStatement deleteHostFileStatement = DatabaseConnection.getInstance().prepareStatement("DELETE FROM host_file WHERE host = ? AND directory_id = ? AND file_id = ?");
			deleteHostFileStatement.setLong(1, longIp);
			for (Map.Entry<Directory, ArrayList<File>> entry : dirContent.entrySet()) {

				Directory directory = entry.getKey();
				ArrayList<File> fileList = entry.getValue();
				int directoryId = DirectoryDAO.getDirectoryId(directory);
				deleteHostFileStatement.setInt(2, directoryId);

				for (File file : fileList) {
					int fileId = getFileId(file);
					deleteHostFileStatement.setInt(3, fileId);
					deleteHostFileStatement.execute();
				}

			}
		} catch (SQLException ex) {
			Logger.getLogger(FileDAO.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	private static int getFileId(File f) throws SQLException, WTFException {
		if (f.id > 0) {
			return f.id;
		}
		DatabaseConnection.getInstance().setAutoCommit(false);
		try {
			PreparedStatement prepareStatement = DatabaseConnection.getInstance().prepareStatement("SELECT id FROM file WHERE name = ?");
			prepareStatement.setString(1, f.name);

			ResultSet executeQuery = prepareStatement.executeQuery();
			if (executeQuery.next()) {
				return executeQuery.getInt(1);
			}
			int extension_int = 0;
			try {
				if (f.name.lastIndexOf(".") >= 0 && f.name.length() - f.name.lastIndexOf(".") < 5) {
					extension_int = Integer.parseInt(f.name.substring(f.name.lastIndexOf(".") + 1), 36);
				}
			} catch (NumberFormatException e) {
				Logger.getLogger(Database.class.getName()).log(Level.INFO, "Failed to parse extension of file {0}", new Object[]{f.name});
			}
			PreparedStatement prepareStatement1 = DatabaseConnection.getInstance().prepareStatement("INSERT INTO file(name, extension_int) VALUES(?,?)");
			prepareStatement1.setString(1, f.name);
			prepareStatement1.setInt(2, extension_int);
			prepareStatement1.execute();
			ResultSet generatedKeys = prepareStatement1.getGeneratedKeys();
			if (generatedKeys.next()) {
				return generatedKeys.getInt(1);
			}
		} finally {
			if (!DatabaseConnection.getInstance().getAutoCommit()) {
				DatabaseConnection.getInstance().setAutoCommit(true);
			}
		}
		throw new WTFException();
	}

	static void addHostFiles(long longIp, HashMap<Directory, ArrayList<File>> newFiles) throws WTFException {
		try {
			PreparedStatement insertHostFileStatement = DatabaseConnection.getInstance().prepareStatement("INSERT host_file(host,directory_id,file_id,size) VALUES(?,?,?,?)");
			insertHostFileStatement.setLong(1, longIp);
			for (Map.Entry<Directory, ArrayList<File>> entry : newFiles.entrySet()) {
				Directory directory = entry.getKey();
				ArrayList<File> fileList = entry.getValue();
				Logger.getLogger(FileDAO.class.getName()).log(Level.FINEST, "Add directory [{0}] content to host {1}", new Object[]{directory.getPath(), Utils.long2ip(longIp)});
				int directoryId = DirectoryDAO.getDirectoryId(directory);
				insertHostFileStatement.setInt(2, directoryId);

				for (File file : fileList) {
					int fileId = getFileId(file);
					insertHostFileStatement.setInt(3, fileId);
					insertHostFileStatement.setLong(4, file.size);
					insertHostFileStatement.execute();
				}

			}
		} catch (SQLException ex) {
			Logger.getLogger(FileDAO.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

}
