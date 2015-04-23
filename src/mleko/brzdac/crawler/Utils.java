/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mleko.brzdac.crawler;

/**
 *
 * @author mleko
 */
public class Utils {

	public static long ip2long(String ip) throws NumberFormatException, IllegalArgumentException {
		String[] parts = ip.split("\\.");
		long result = 0;
		int power = 0;
		if (parts.length != 4) {
			throw new IllegalArgumentException("Invalid IP format");
		}
		for (int i = parts.length; i > 0; i--) {
			int parseInt = Integer.parseInt(parts[i - 1]);
			if (parseInt < 0 || parseInt > 255) {
				throw new IllegalArgumentException("Invalid IP address");
			}
			result += Math.pow(256, power++) * parseInt;
		}
		return result;
	}

	public static String long2ip(long ip) {
		String result = "";
		while (ip > 0) {
			if (!"".equals(result)) {
				result = "." + result;
			}
			result = ip % 256 + result;
			ip = (long) Math.floor(ip / 256);
		}
		return result;
	}

}
