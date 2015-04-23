/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mleko.brzdac.crawler.filescanner;

import it.sauronsoftware.ftp4j.FTPAbortedException;
import it.sauronsoftware.ftp4j.FTPClient;
import it.sauronsoftware.ftp4j.FTPDataTransferException;
import it.sauronsoftware.ftp4j.FTPException;
import it.sauronsoftware.ftp4j.FTPFile;
import it.sauronsoftware.ftp4j.FTPIllegalReplyException;
import it.sauronsoftware.ftp4j.FTPListParseException;
import it.sauronsoftware.ftp4j.FTPListParser;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import mleko.brzdac.crawler.Utils;
import mleko.brzdac.crawler.db.Database;
import mleko.brzdac.crawler.db.dao.DirectoryDAO;
import mleko.brzdac.crawler.db.dao.HostDAO;
import mleko.brzdac.crawler.exceptions.WTFException;
import mleko.brzdac.crawler.ftp.ExtendedUnixListParser;
import mleko.brzdac.crawler.ftp.MLSDListParser;
import mleko.brzdac.crawler.pojo.Directory;
import mleko.brzdac.crawler.pojo.File;

/**
 *
 * @author mleko
 */
public class FileScanner {

	private final String host;
	private final long longIp;

	private final FTPClient client;
	private final LinkedList<Directory> directoryToScanStack;

	{
		client = new FTPClient();
		client.setCharset(null);
		client.getConnector().setConnectionTimeout(5);
		client.getConnector().setReadTimeout(15);

//		client.addCommunicationListener(new FTPCommunicationListener() {
//
//			@Override
//			public void sent(String statement) {
//				Logger.getLogger("ftp.sent").log(Level.FINEST, "SND: {0}", new Object[]{statement});
//			}
//
//			@Override
//			public void received(String statement) {
//				Logger.getLogger("ftp.received").log(Level.FINEST, "RCV: {0}", new Object[]{statement});
//			}
//		});
		directoryToScanStack = new LinkedList<>();

		client.addListParser(new MLSDListParser());
		client.addListParser(new ExtendedUnixListParser());

		for (FTPListParser p : client.getListParsers()) {
			if (p.getClass() == it.sauronsoftware.ftp4j.listparsers.MLSDListParser.class) {
				client.removeListParser(p);
			}
		}
	}

	public FileScanner(String host) {
		this.host = host;
		longIp = Utils.ip2long(host);
	}

	public boolean scan() throws WTFException {

		Changes changes = null;
		Logger.getLogger(FileScanner.class.getName()).log(Level.FINE, "Start file scan of host:{0}", new Object[]{host});

		try {
			String[] welcomeMessage = client.connect(host);
			if (welcomeMessage.length > 0 && (welcomeMessage[0].contains("CesarFTP") || welcomeMessage[0].contains("GuildFTPd"))) {
				client.setCharset("ISO-8859-2");
			}

			/* Try to log onto guest account, if fail, try annonymous login */
			try {
				client.login("guest", "a");
			} catch (FTPException | FTPIllegalReplyException | IOException | IllegalStateException e) {
				client.login("anonymous", "brzdac@krol.me");
			}

			client.setPassive(true);

			directoryToScanStack.push(new Directory("/"));
			changes = recursiveScan();

			client.disconnect(true);
		} catch (IOException | FTPException | IllegalStateException | FTPIllegalReplyException ex) {
			Logger.getLogger(FileScanner.class.getName()).log(Level.INFO, null, ex);
		}
		Logger.getLogger(FileScanner.class.getName()).log(Level.FINE, "Finished file scan of host:{0}", new Object[]{host});
		if (changes != null) {
			try {
				Logger.getLogger(FileScanner.class.getName()).log(Level.FINER, "Applying changes of host:{0} to db", new Object[]{host});
				Database.addHostIfNotExist(longIp);
				HostDAO.applyFileChanges(longIp, changes);
				HostDAO.UpdateShareSize(longIp);
				HostDAO.UpdateFileScanDate(longIp);
				Logger.getLogger(FileScanner.class.getName()).log(Level.FINER, "Changes aplied to host:{0}", new Object[]{host});
			} catch (SQLException ex) {
				Logger.getLogger(FileScanner.class.getName()).log(Level.SEVERE, null, ex);
				return false;
			}
		}
		return true;
	}

	private Changes recursiveScan() throws WTFException {
		Changes changes = new Changes();
		try {
			/* load current directory list */
			ArrayList<Directory> directoryList = DirectoryDAO.readCurrentDirectories(longIp);
			if (null == directoryList) {
				return null;
			}

			/* until there are dirs to scan */
			while (directoryToScanStack.size() > 0) {
				Directory directory = directoryToScanStack.pop();
				Logger.getLogger(FileScanner.class.getName()).log(Level.FINEST, "Scan directory {0}", new Object[]{directory.getPath()});

				/* if scanned dir exists, remove it from list.
				 * Dirs left after scan will be deleted from db - they dont exist anymore on host
				 */
				if (directoryList.contains(directory)) {
					directoryList.remove(directory);
				}

				try {
					client.changeDirectory(directory.getPath());
				} catch (FTPException e) {
					Logger.getLogger(FileScanner.class.getName()).log(Level.FINE, "Failed to change directory to {0}, ignoring", new Object[]{directory.getPath()});
					continue;
				}

				FTPFile[] list = null;
				for (int i = 0;; i++) {
					try {
						list = client.list();
						break;
					} catch (SocketTimeoutException | FTPException | FTPDataTransferException e) {
						if (i < 3) {
							Logger.getLogger(FileScanner.class.getName()).log(Level.FINE, "FTPTimeout, retrying");
						} else {
							throw e;
						}
					}
				}
				Set<String> fileSet = new HashSet<>();
				ArrayList<File> hostDirContent = DirectoryDAO.getDirectoryContent(longIp, directory);
				for (FTPFile hostFile : list) {
					//ignore non unique fileNames
					if (!fileSet.add(hostFile.getName())) {
						continue;
					}
					if (hostFile.getType() == FTPFile.TYPE_DIRECTORY) {
						if (hostFile.getName().length() > 444 || hostFile.getName().equalsIgnoreCase(".") || hostFile.getName().equalsIgnoreCase("..")) {
							continue;
						}
						directoryToScanStack.addFirst(new Directory(directory.getPath() + hostFile.getName()));
					} else if (hostFile.getType() == FTPFile.TYPE_FILE) {
						if (hostFile.getName().length() > 144) {
							continue;
						}
						File file = new File(hostFile.getName(), hostFile.getSize());

						if (!hostDirContent.contains(file)) {
							changes.addFile(directory, file);
						} else {
							hostDirContent.remove(file);
						}
					}
				}
				if (!hostDirContent.isEmpty()) {
					changes.removeFiles(directory, hostDirContent);
				}
			}
			if (!directoryList.isEmpty()) {
				changes.removeDirectories(directoryList);
			}

		} catch (IllegalStateException | IOException | FTPIllegalReplyException | FTPException | FTPDataTransferException | FTPAbortedException ex) /*Multicatch*/ {
			Logger.getLogger(FileScanner.class.getName()).log(Level.INFO, ex.getMessage(), ex);
			return null;
		} catch (FTPListParseException ex) {
			Logger.getLogger(FileScanner.class.getName()).log(Level.WARNING, "Failed to scan host {0} with message: {1}", new Object[]{host, ex.getMessage()});
			return null;
		}

		for (Map.Entry<Directory, ArrayList<File>> en : changes.removedFiles.entrySet()) {
			Directory rDirectory = en.getKey();
			ArrayList<File> object1 = en.getValue();
			Logger.getLogger(FileScanner.class.getName()).log(Level.FINER, "Removed files from dir:{0}", rDirectory.getPath());
			for (File file : object1) {
				Logger.getLogger(FileScanner.class.getName()).log(Level.FINEST, "- {0} - {1}", new Object[]{file.name, file.size});
			}

		}
		for (Map.Entry<Directory, ArrayList<File>> en : changes.newFiles.entrySet()) {
			Directory aDirectory = en.getKey();
			ArrayList<File> object1 = en.getValue();
			Logger.getLogger(FileScanner.class.getName()).log(Level.FINER, "Added files to dir:{0}", aDirectory.getPath());
			for (File file : object1) {
				Logger.getLogger(FileScanner.class.getName()).log(Level.FINEST, "+ {0} - {1}", new Object[]{file.name, file.size});
			}

		}

		return changes;
	}
}
