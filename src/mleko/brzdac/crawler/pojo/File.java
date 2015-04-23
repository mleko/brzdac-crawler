/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mleko.brzdac.crawler.pojo;

/**
 *
 * @author mleko
 */
public class File {

	public int id;
	public String name;
	public long size;

	public File(String name, long size) {
		this.name = name;
		this.size = size;
		this.id = 0;
	}

	public File(int id, String name, long size) {
		this.id = id;
		this.name = name;
		this.size = size;
	}

	@Override
	public boolean equals(Object obj) {
		if (this.getClass() != obj.getClass()) {
			return false;
		}
		File file = (File) obj;
		if (this.name == null ? file.name != null : !this.name.equalsIgnoreCase(file.name)) {
			return false;
		}
		return this.size == file.size;
	}

	@Override
	public int hashCode() {
		int hash = 7;
		hash = 61 * hash + (this.name != null ? this.name.toLowerCase().hashCode() : 0);
		hash = 61 * hash + (int) (this.size ^ (this.size >>> 32));
		return hash;
	}

}
