/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mleko.brzdac.crawler.filescanner;

import java.util.ArrayList;
import java.util.HashMap;
import mleko.brzdac.crawler.pojo.Directory;
import mleko.brzdac.crawler.pojo.File;

/**
 *
 * @author mleko
 */
public class Changes {

	public ArrayList<Directory> removedDirectories;
	public HashMap<Directory, ArrayList<File>> removedFiles;
	public HashMap<Directory, ArrayList<File>> newFiles;

	{
		removedDirectories = new ArrayList<Directory>();
		removedFiles = new HashMap<Directory, ArrayList<File>>();
		newFiles = new HashMap<Directory, ArrayList<File>>();
	}

	public Changes() {
	}

	boolean addFile(Directory path, File file) {
		if (!newFiles.containsKey(path)) {
			newFiles.put(path, new ArrayList<File>());
		}
		return newFiles.get(path).add(file);
	}

	boolean removeFile(Directory path, File file) {
		if (!removedFiles.containsKey(path)) {
			removedFiles.put(path, new ArrayList<File>());
		}
		return removedFiles.get(path).add(file);
	}

	boolean removeDirectory(Directory path) {
		return removedDirectories.add(path);
	}

	boolean removeFiles(Directory path, ArrayList<File> files) {
		if (!removedFiles.containsKey(path)) {
			removedFiles.put(path, new ArrayList<File>());
		}
		return removedFiles.get(path).addAll(files);
	}

	boolean removeDirectories(ArrayList<Directory> directories) {
		return removedDirectories.addAll(directories);
	}
}
