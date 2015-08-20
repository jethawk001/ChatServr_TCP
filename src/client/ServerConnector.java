package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ServerConnector implements Runnable
{
	private ClientGUI guiHandle;
	private Socket socket;
	protected BufferedReader reader;
	protected PrintWriter writer;

	ServerConnector(ClientGUI guiHdl)
	{
		guiHandle = guiHdl;
	}

	public PrintWriter getWriter()
	{
		return writer;
	}

	public Socket init(String serverAddress, int serverPort) throws Exception
	{
		socket = new Socket(serverAddress, serverPort);

		if (socket.isConnected())
		{
			// connection successful
		} else
		{
			if (socket != null)
				socket.close();

			throw (new Exception("Couldn't connect to [" + serverAddress + ":" + serverPort + "]"));
		}

		reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		writer = new PrintWriter(socket.getOutputStream());

		return socket;
	}
	
	public boolean isConnected()
	{
		return socket.isConnected();
	}
	
	public void shutdown()
	{
		Client.getLogger().info("Shutting down client connection to server.");
		try
		{
			socket.close();

			if(guiHandle != null)
				guiHandle.handleServerStateChange(false);
		} catch (IOException e)
		{
			Client.getLogger().severe(e.getStackTrace().toString());
		}
	}

	@Override
	public void run()
	{
		try
		{
			while (!socket.isClosed() && socket.isConnected())
			{
				String line = reader.readLine();
				
				if(line == null)
					break; // server closed connection
				
				guiHandle.getOutTextArea().append(line + "\n");
				
			}
		} catch (IOException e)
		{
			// server could have closed connection
			if (socket != null && socket.isClosed())
				Client.getLogger().warning("Server closed connection..");
			else
				Client.getLogger().severe(e.getMessage());
		} catch (Exception e)
		{
			Client.getLogger().severe(e.getStackTrace().toString());
		} finally
		{
			shutdown();
		}
	}
}
