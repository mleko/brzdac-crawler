/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mleko.brzdac.crawler.pojo;

/**
 *
 * @author mleko
 */
public class Directory {

	private String path;

	public Directory(String path) {
		if (!path.endsWith("/")) {
			path = path + "/";
		}
		this.path = path;
	}

	@Override
	public int hashCode() {
		int hash = 5;
		hash = 97 * hash + (this.getPath() != null ? this.getPath().toLowerCase().hashCode() : 0);
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
		final Directory other = (Directory) obj;

		return !((this.path == null) ? (other.path != null) : !this.path.equalsIgnoreCase(other.path));
	}

	/**
	 * @return the path
	 */
	public String getPath() {
		return path;
	}

}
