/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mleko.brzdac.crawler.exceptions;

/**
 *
 * @author mleko
 */
public class WTFException extends Exception {

	public WTFException(String shoudnt_be_here) {
		super(shoudnt_be_here);
	}

	public WTFException() {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

}
