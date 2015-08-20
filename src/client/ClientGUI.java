package client;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.TextArea;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.IOException;

public class ClientGUI extends Frame
{
	private String serverAddress;
	private int serverPort = -1;
	private ServerConnector serverConnector;

	private TextArea outText;
	private TextField inText;

	public ClientGUI(String serverAddr, int serverPortNumber) throws Exception
	{
		super("Chat @ [" + serverAddr + ":" + serverPortNumber + "] - connecting..");

		if (serverAddr.trim().equals("") || serverPortNumber <= 0)
			throw (new Exception("serverAddress or serverPortNumber invalid"));

		serverAddress = serverAddr;
		serverPort = serverPortNumber;

		serverConnector = new ServerConnector(this);
		serverConnector.init(serverAddress, serverPort);
	}

	public void initGUI() throws IOException
	{
		setLayout(new BorderLayout());
		add("Center", outText = new TextArea());
		outText.setEditable(false);
		add("South", inText = new TextField());
		pack();
		setVisible(true);
		inText.requestFocus();

		handleServerStateChange(serverConnector.isConnected());

		// inText actions
		inText.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				if ((e.getModifiers() | KeyEvent.VK_ENTER) != 0)
				{
					serverConnector.getWriter().println(inText.getText());
					serverConnector.getWriter().flush();

					outText.append(inText.getText() + "\n");
					inText.setText("");
				}
			}
		});

		this.addWindowListener(new WindowListener()
		{
			@Override
			public void windowOpened(WindowEvent e)
			{
			}

			@Override
			public void windowIconified(WindowEvent e)
			{
			}

			@Override
			public void windowDeiconified(WindowEvent e)
			{
			}

			@Override
			public void windowDeactivated(WindowEvent e)
			{
			}

			@Override
			public void windowClosing(WindowEvent e)
			{
			}

			@Override
			public void windowClosed(WindowEvent e)
			{
				if (serverConnector != null)
					serverConnector.shutdown();
			}

			@Override
			public void windowActivated(WindowEvent e)
			{
			}
		});
	}

	public TextArea getOutTextArea()
	{
		return outText;
	}

	public ServerConnector getServerConnector()
	{
		return serverConnector;
	}

	public void handleServerStateChange(boolean connected)
	{
		setTitle("Chat @ [" + serverAddress + ":" + serverPort + "] - " + (connected ? "connected" : "disconnected"));
	
		if(!connected)
			inText.setEditable(false);
	}
}
