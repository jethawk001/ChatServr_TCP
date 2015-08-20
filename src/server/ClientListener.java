package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class ClientListener implements Runnable
{
	private static ClientListener instance = null;

	// Initialization and working
	private boolean initDone = false;
	private volatile boolean shutdown = false;

	// Variables
	private int portNumber = -1;

	public static ClientListener instance()
	{
		if (instance == null)
			instance = new ClientListener();

		return instance;
	}

	public boolean initialize(int portNumber)
	{
		if (portNumber <= 0)
			return false;

		this.shutdown = false;
		this.portNumber = portNumber;
		this.initDone = true;

		return true;
	}

	public void shutdown()
	{
		// tell the client handler to say bye to all clients/users
		try
		{
			ClientHandler.shutDownAllClients();
		} catch (IOException e)
		{
			Server.getLogger().severe(e.getStackTrace().toString());
		}
		
		this.shutdown = true;
	}
	
	private void idle()
	{
		Server.idle();
	}

	@Override
	public void run()
	{
		if (!initDone)
		{
			Server.getLogger().severe("Cannot start clientListener without initialzing...");
			return;
		}

		ServerSocket listener = null;
		Socket socket = null;

		// Open socket for listening to new clients
		try
		{
			Server.getLogger().info("Listening on socket number [" + portNumber + "]");
			listener = new ServerSocket(portNumber);
		
			// had to do this so that we can detect a shutdown within a second 
			// and the while loop can read the new value of shutdown else we are just stuck 
			// trying to accept infinitely.
			listener.setSoTimeout(1000);

			while (!shutdown)
			{
				try
				{
					socket = listener.accept();

					// just for safety - not sure if this will occur
					if (socket != null && !socket.isClosed() && socket.isConnected())
					{	
						Server.getLogger().info("Received client connection [" + socket.getRemoteSocketAddress() + "]");
						
						// TODO: Create a new client with this accept and let the client class
						// take over.
						ClientHandler handler = new ClientHandler(socket);
						Server.submitThread(new Thread(handler));
						
						socket = null;
					}

				} catch (SocketTimeoutException e)
				{
					// we just woke up on a timeout
					// go back to the while
					idle();
				}
			}

		} catch (IOException e)
		{
			e.printStackTrace();
		} finally
		{
			try
			{
				if (socket != null)
					socket.close();

				if (listener != null)
					listener.close();

			} catch (IOException e)
			{
				Server.getLogger().severe(e.getStackTrace().toString());
			}
		}

	}
}
