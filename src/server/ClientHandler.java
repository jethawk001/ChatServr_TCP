package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

// Once server accept a client connection
// this will handle the client from here
// i.e. communicating with the client for 
// user name and then messaging from one client
// to the other...

public class ClientHandler implements Runnable
{
	static ConcurrentHashMap<String, ClientHandler> clientLoginNameMap = new ConcurrentHashMap<String, ClientHandler>();

	// Client specific
	Socket socket;
	int localPortConnection; // store the client handle using the local assigned port
	String clientLoginName;
	BufferedReader reader;
	PrintWriter writer;

	public ClientHandler(Socket socket)
	{
		this.socket = socket;
		this.localPortConnection = socket.getLocalPort();
	}
	
	private void getUserName() throws IOException
	{
		// Let the user know all the people logged in
		writer.println("Existing users logged in...");
		for (Entry<String, ClientHandler> s : clientLoginNameMap.entrySet())
		{
			writer.println(s.getKey());
		}
		writer.flush();


		// ask user for his/her login name
		String loginName = "";

		// Ensure that the client provides a valid login name to start a
		// chat
		while (loginName.trim() == "" || 
				clientLoginNameMap.putIfAbsent(loginName, this) != null /* no one else used the login name */ )
		{
			writer.flush();
			writer.print("Enter your chat user name (not taken by anyone):");
			writer.flush();
			loginName = reader.readLine();
		}

		// At this point client has a valid login name
		// now add him to the map
		this.clientLoginName = loginName;
		
		writer.print(loginName + " you can start chatting now.");
		writer.flush();
	}
	
	private static void announceUserState(String userName, boolean added)
	{
		// At this point announce to all users that user has logged in
		Server.getLogger().info("[" + userName + "] " + ( added ? "connected" : "disconnected" ));
		
		for (Entry<String, ClientHandler> s : clientLoginNameMap.entrySet())
		{
			ClientHandler handle = s.getValue();
			
			handle.writer.println("[" + userName + "] " + ( added ? "connected" : "disconnected" ));
			handle.writer.flush();
		}
	}

	private static boolean sendChatMessage(String fromUser, String toUser, String message)
	{
		ClientHandler toUserHandle = clientLoginNameMap.get(toUser);
		if(toUserHandle == null)
		{
			Server.getLogger().info("Message from [" + fromUser + "] to [" + toUser + "] failed as other user is not present." );
			return false;
		}
		
		toUserHandle.writer.println("Message received from: [" + fromUser + "] : [" + message + "]");
		toUserHandle.writer.flush();
		return true;
	}
	
	private void giveInstructions()
	{
		writer.println("Enter message in format username/chat message");
		writer.println("Type bye to quit");
		writer.flush();
	}
	
	public static void shutDownAllClients() throws IOException
	{
		Server.getLogger().info("Shutting down all clients.");
		for (Entry<String, ClientHandler> s : clientLoginNameMap.entrySet())
		{
			ClientHandler handle = s.getValue();
			
			handle.writer.println("Server shutting down - GoodBye");
			handle.writer.flush();
			handle.socket.close();
		}
		
		clientLoginNameMap.clear();
	}

	@Override
	public void run()
	{
		try
		{
			reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			writer = new PrintWriter(socket.getOutputStream());

			writer.println("Welcome to chat server..");
			writer.flush();

			getUserName();
			
			announceUserState(this.clientLoginName, true);
			
			giveInstructions();
			
			while(!socket.isClosed())
			{
				// wait for user to enter the chat messages
				// Format expected user name/chat message
				String chatInput = reader.readLine();
				
				if(chatInput == null) // comes on client closing connection
				{	
					socket.close();
					
					// clean user from maps
					clientLoginNameMap.remove(clientLoginName);
					announceUserState(clientLoginName, false);

					continue;
				}
				
				String[] chatInputSplit = chatInput.split("/", 2); // want to split only @ first "/"
				if(chatInputSplit.length == 1)
				{
					if(chatInputSplit[0].trim().equals("bye"))
					{
						writer.println("GoodBye !");
						writer.flush();
						socket.close();
						
						// clean user from maps
						clientLoginNameMap.remove(clientLoginName);
						
						announceUserState(clientLoginName, false);
					}
				}
				else if(chatInputSplit.length == 2)
				{
					if(clientLoginNameMap.containsKey(chatInputSplit[0]))
					{
						// other side user is present in map as of now
						// try sending him a message
						boolean success = sendChatMessage(clientLoginName, chatInputSplit[0], chatInputSplit[1]);
						if (!success)
							writer.println("Message not sent to [" + chatInputSplit[0] + "]");
						
						writer.flush();
					}
					else
					{
						// other side user not present or just disconnected
						writer.println(chatInputSplit[0] + " is either disconnected or not valid");
						writer.flush();
					}
				}
			}

		} catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
