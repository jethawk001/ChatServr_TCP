package client;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

//Client App
public class Client
{
	private final static Logger logger = Logger.getLogger(Client.class.getName());
	
	public static Logger getLogger()
	{
		return logger;
	}
	
	public static void main(String[] args)
	{
		if(args.length < 2)
		{
			logger.severe("Usage: client <hostname> <port>");
			System.exit(0);
		}
		
		ExecutorService eS = Executors.newFixedThreadPool(1);
		
		ClientGUI cg;
		try
		{
			cg = new ClientGUI(args[0], Integer.parseInt(args[1]));
			cg.initGUI();

			Thread serverConnectorThread = new Thread(cg.getServerConnector());
			eS.execute(serverConnectorThread);
			
			eS.awaitTermination(1, TimeUnit.DAYS);
			
		} catch (Exception e)
		{
			logger.severe(e.getMessage());
		}
		finally
		{
			logger.info("Shutting down chat client");
			eS.shutdown();
		}
	}
}
