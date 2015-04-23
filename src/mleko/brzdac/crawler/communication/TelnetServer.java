/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mleko.brzdac.crawler.communication;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import mleko.brzdac.crawler.worker.ServerScanner;

/**
 *
 * @author mleko
 */
public class TelnetServer extends Handler implements Runnable {

	private boolean run;
	private final ServerScanner managedScanner;
	private final List<ClientHandler> clientHandlers;

	{
		clientHandlers = Collections.synchronizedList(new ArrayList<ClientHandler>());
	}

	public TelnetServer(ServerScanner serverScanner) {
		this.managedScanner = serverScanner;
	}

	@Override
	public void run() {
		run = true;
		try {
			ServerSocket serverSocket = new ServerSocket(666);
			while (run) {
				Socket accept = serverSocket.accept();
				ClientHandler clientHandler = new ClientHandler(accept, managedScanner);
				clientHandlers.add(clientHandler);
				new Thread(clientHandler).start();
			}
		} catch (IOException ex) {
			Logger.getLogger(TelnetServer.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	public void stop() {
		this.run = false;
		for (ClientHandler clientHandler : clientHandlers) {
			clientHandler.stop();
		}
	}

	@Override
	public void publish(LogRecord record) {
		for (ClientHandler clientHandler : clientHandlers) {
			clientHandler.handleLog(record);
		}
	}

	@Override
	public void flush() {
	}

	@Override
	public void close() throws SecurityException {
	}

}
