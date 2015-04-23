/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mleko.brzdac.crawler.ftp;

import it.sauronsoftware.ftp4j.FTPFile;
import it.sauronsoftware.ftp4j.FTPListParseException;
import it.sauronsoftware.ftp4j.FTPListParser;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;
import java.util.StringTokenizer;

/**
 *
 * @author mleko
 */
public class MLSDListParser implements FTPListParser {

	/**
	 * Date format 1 for MLSD date facts (supports millis).
	 */
	private static final DateFormat MLSD_DATE_FORMAT_1 = new SimpleDateFormat("yyyyMMddhhmmss.SSS Z");

	/**
	 * Date format 2 for MLSD date facts (doesn't support millis).
	 */
	private static final DateFormat MLSD_DATE_FORMAT_2 = new SimpleDateFormat("yyyyMMddhhmmss Z");

	@Override
	public FTPFile[] parse(String[] lines) throws FTPListParseException {
		ArrayList<FTPFile> list = new ArrayList<FTPFile>();
		for (String line : lines) {
			FTPFile file = parseLine(line);
			if (file != null) {
				list.add(file);
			}
		}
		int size = list.size();
		FTPFile[] ret = new FTPFile[size];
		for (int i = 0; i < size; i++) {
			ret[i] = (FTPFile) list.get(i);
		}
		return ret;
	}

	private FTPFile parseLine(String line) throws FTPListParseException {
		// Divides facts and name.

		line = replaceEntities(line);
		if (!line.contains("; ")) {
			throw new FTPListParseException(line);
		}
		// Extracts the file name.
		String name = line.substring(line.indexOf("; ") + 2).trim();
		line = line.substring(0, line.indexOf("; "));

		ArrayList<String> list = new ArrayList<String>();

		StringTokenizer st = new StringTokenizer(line, ";");
		while (st.hasMoreElements()) {
			String aux = st.nextToken().trim();
			if (aux.length() > 0) {
				list.add(aux);
			}
		}

		// Parses the facts.
		Properties facts = new Properties();
		for (String aux : list) {
			int sep = aux.indexOf('=');
			if (sep == -1) {
				throw new FTPListParseException(line);
			}
			String key = aux.substring(0, sep).trim();
			String value = aux.substring(sep + 1, aux.length()).trim();
			if (key.length() == 0) {
				throw new FTPListParseException(line);
			}
			facts.setProperty(key, value);
		}
		// Type.
		int type;
		String typeString = facts.getProperty("type");
		if (typeString == null) {
			throw new FTPListParseException();
		} else if ("file".equalsIgnoreCase(typeString)) {
			type = FTPFile.TYPE_FILE;
		} else if ("dir".equalsIgnoreCase(typeString)) {
			type = FTPFile.TYPE_DIRECTORY;
		} else if ("cdir".equalsIgnoreCase(typeString)) {
			// Current directory. Skips...
			return null;
		} else if ("pdir".equalsIgnoreCase(typeString)) {
			// Parent directory. Skips...
			return null;
		} else {
			// Unknown... (link?)... Skips...
			return null;
		}
		// Last modification date.
		Date modifiedDate = null;
		String modifyString = facts.getProperty("modify");
		if (modifyString != null) {
			modifyString += " +0000";
			try {
				synchronized (MLSD_DATE_FORMAT_1) {
					modifiedDate = MLSD_DATE_FORMAT_1.parse(modifyString);
				}
			} catch (ParseException e1) {
				try {
					synchronized (MLSD_DATE_FORMAT_2) {
						modifiedDate = MLSD_DATE_FORMAT_2.parse(modifyString);
					}
				} catch (ParseException e2) {

				}
			}
		}
		// Size.
		long size = 0;
		String sizeString = facts.getProperty("size");
		if (sizeString != null) {
			if (sizeString.length() == 0) {
				size = 0;
			} else {
				try {
					size = Long.parseLong(sizeString);
				} catch (NumberFormatException e) {

				}
			}
			if (size < 0) {
				size = 0;
			}
		}
		// Done!
		FTPFile ret = new FTPFile();
		ret.setType(type);
		ret.setModifiedDate(modifiedDate);
		ret.setSize(size);
		ret.setName(name);
		return ret;
	}

	private String replaceEntities(String line) {
		return line.replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&apos;", "'");
	}

}
