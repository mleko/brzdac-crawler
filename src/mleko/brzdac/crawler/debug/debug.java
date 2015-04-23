/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mleko.brzdac.crawler.debug;

import it.sauronsoftware.ftp4j.FTPClient;
import it.sauronsoftware.ftp4j.FTPDataTransferException;
import it.sauronsoftware.ftp4j.FTPException;
import it.sauronsoftware.ftp4j.FTPFile;
import it.sauronsoftware.ftp4j.FTPIllegalReplyException;
import it.sauronsoftware.ftp4j.FTPListParseException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.SortedMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import mleko.brzdac.crawler.ftp.ExtendedUnixListParser;

/**
 *
 * @author mleko
 */
public class debug {

	public static void main(String[] args) throws Exception {
		Long.parseLong("0");
//
	}

	public static void CheckParsers() throws FTPListParseException {
		String s = "drwxrw-rw- 1 root  root        0 Dec 13  2012 Avatar\n"
				+ "drwxrw-rw- 1 root  root        0 Oct 31  2012 Battleship\n"
				+ "drwxrw-rw- 1 root  root        0 Dec 11  2012 Bourne trylogia\n"
				+ "drwxrw-rw- 1 root  root        0 Dec 13  2012 Braveheart\n"
				+ "drwxrw-rw- 1 root  root        0 Jun 22 11:07 Człowiek w żelaznej masce (1998)\n"
				+ "drwxrw-rw- 1 root  root        0 Nov 12  2012 Die Hard 1-4\n"
				+ "drwxrw-rw- 1 root  root        0 Dec 13  2012 Dyktator\n"
				+ "drwxrw-rw- 1 root  root        0 Dec 13  2012 Gangster\n"
				+ "drwxrw-rw- 1 root  root        0 Apr 28 20:58 Gladiator\n"
				+ "drwxrw-rw- 1 root  root        0 Aug 28 21:04 ILUZJA.EXTENDED.RERIP.720p-SPARKS\n"
				+ "drwxrw-rw- 1 root  root        0 Dec 18  2012 Jurrasic Park 1-3\n"
				+ "drwxrw-rw- 1 root  root        0 Jun 22 11:14 Maska (1994)\n"
				+ "drwxrw-rw- 1 root  root        0 Mar 28 18:45 Matrix Trylogy [720p]\n"
				+ "drwxrw-rw- 1 root  root        0 Dec 13  2012 Rytuał\n"
				+ "drwxrw-rw- 1 root  root        0 Dec 28  2012 Star Trek\n"
				+ "drwxrw-rw- 1 root  root        0 Aug 31 20:14 Star Wars Saga\n"
				+ "drwxrw-rw- 1 root  root        0 Dec 13  2012 The Avengers\n"
				+ "drwxrw-rw- 1 root  root        0 Dec 13  2012 The Dark Knight Rises\n"
				+ "drwxrw-rw- 1 root  root        0 Mar  7 21:15 The Hobbit An Unexpected Journey 2012 BluRay 720p DTS x264-3Li\n"
				+ "drwxrw-rw- 1 root  root        0 Dec 13  2012 The.Amazing.Spider-Man.2012.720p\n"
				+ "drwxrw-rw- 1 root  root        0 Dec 13  2012 Transformers\n"
				+ "-rwxrw-rw- 1 root  root -277911841 Oct  3  2012 TT3D.Closer.to.the.Edge.2011.PL.720p.BluRay.X264-SLiSU.mkv\n"
				+ "drwxrw-rw- 1 root  root        0 Mar 28 18:39 Wróg u bram";
		ExtendedUnixListParser extendedUnixListParser = new ExtendedUnixListParser();
		FTPFile[] parse = extendedUnixListParser.parse(s.split("\n"));
	}

	public static void CheckHost() throws Exception {
		FTPClient ftpClient = new FTPClient();
		ftpClient.connect("10.4.1.0");
		ftpClient.addListParser(new ExtendedUnixListParser());
		ftpClient.login("anonymous", "debug@brzdac.net");
		FTPFile[] list = ftpClient.list();
	}
}

/*
 type=file;modify=20090315204446;size=734580736; Underworld 2; Ewolucja (2006) [DVDRip.XviD].avi
 */
