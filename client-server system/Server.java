package finalproject.server;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.text.BadLocationException;

import finalproject.db.DBInterface;
import finalproject.entities.Person;

public class Server extends JFrame implements Runnable {

	public static final int DEFAULT_PORT = 8001;
	private static final int FRAME_WIDTH = 600;
	private static final int FRAME_HEIGHT = 800;
	final int AREA_ROWS = 10;
	final int AREA_COLUMNS = 40;
	
	private JTextArea textArea;
	private JLabel dbName;
	private String fileName;
	private String header;
	
	//DB variables
	private Connection conn;
	private PreparedStatement query;
	private PreparedStatement insertData;
	

	public Server() throws IOException, SQLException {
		this(DEFAULT_PORT, "server.db");
	}
	
	public Server(String dbFile) throws IOException, SQLException {
		this(DEFAULT_PORT, dbFile);
	}

	public Server(int port, String dbFile) throws IOException, SQLException {

		this.setSize(Server.FRAME_WIDTH, Server.FRAME_HEIGHT);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.createMenuBar();
		this.createPanel();
		this.fileName = dbFile;
		Thread s = new Thread(this);
		s.start();

	}
	
	private void createMenuBar() {
		JMenuBar menuBar = new JMenuBar();
		menuBar.add(createFileMenu());
		setJMenuBar(menuBar);
	}
	
	private JMenu createFileMenu()
	{
		JMenu menu = new JMenu("File");
		menu.add(createFileExitItem());
		return menu;
	}
	
	private JMenuItem createFileExitItem(){
		JMenuItem item = new JMenuItem("Exit");
		item.addActionListener(e->System.exit(0));
		return item;
	}
	private void createPanel() {
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		
		JPanel controlPanel = new JPanel();
		controlPanel.setLayout(new GridLayout(2,1));
		
		JPanel subPanel1 = new JPanel();
		JLabel label = new JLabel("DB: ");
		dbName = new JLabel("<None>");
		subPanel1.add(label);
		subPanel1.add(dbName);
		
		JPanel subPanel2 = new JPanel();
		JButton queryButton = new JButton("Query DB");
		queryButton.addActionListener(new QueryButtonListener());
		subPanel2.add(queryButton);
		
		controlPanel.add(subPanel1);
		controlPanel.add(subPanel2);
	
		textArea = new JTextArea();
		textArea.setEditable(false);
		textArea.setWrapStyleWord(true);
		JScrollPane scroller = new JScrollPane(textArea);
		
		panel.add(controlPanel, BorderLayout.NORTH);
		panel.add(scroller, BorderLayout.CENTER);
		this.add(panel);
	}
	
	class QueryButtonListener implements ActionListener{
		
		public void actionPerformed(ActionEvent e) {
			   try {
				   
				  ResultSet rset = query.executeQuery(); 
				  ResultSetMetaData rsmd = rset.getMetaData();
				  int numColumns = rsmd.getColumnCount();
				  String rowData = "";
				  while(rset.next()) {
					  
					  for(int i=1;i<=numColumns;i++) {
						  Object o = rset.getObject(i);
						  rowData += o.toString() + "\t";
					  }
					  rowData += "\n";
				  }
				  textArea.append("DB Result:\n"+header+rowData);
					try {
						textArea.setCaretPosition(textArea.getLineStartOffset(textArea.getLineCount()-1));
					} catch (BadLocationException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
			   }
			   catch(SQLException ex) {
				   ex.printStackTrace();

			   }
			   
		   }

	}
	
	private String getTableName() {
		String name = null;
		try {
			DatabaseMetaData dbmd = conn.getMetaData();
			ResultSet rset = dbmd.getTables(null, null, "%", null);
			name = rset.getObject(3).toString();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return name;
	}
	
	private void setHeader(String tableName) {
		try {
			header = "";
			String linebreak = "";
			query = conn.prepareStatement("SELECT * FROM "+tableName);
			ResultSet rset = query.executeQuery();
			ResultSetMetaData rsmd = rset.getMetaData();
			int numColumns = rsmd.getColumnCount();
					
			for (int i=1;i<=numColumns;i++) {
				Object o = rsmd.getColumnName(i);
				header += o.toString() + "\t";
				for(int j=0;j<o.toString().length();j++) {
					linebreak += "-";
				}
				linebreak += "\t";
				  
			  }
			header += "\n";
			linebreak += "\n";
			
			header = header + linebreak;
	
		   }
		   catch(SQLException e) {
			   e.printStackTrace();
		   }
	}
	public static void main(String[] args) {

		Server sv;
		try {
			sv = new Server("server.db");
			sv.setVisible(true);
		} catch (IOException | SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		//boolean serverStatus = true;
		
		try {
			conn = DriverManager.getConnection("jdbc:sqlite:"+fileName);
			textArea.setText("");
			System.out.println(fileName+" is connected");
			dbName.setText(fileName);
			String tableName = getTableName();
			
			this.setHeader(tableName);
			
			insertData = conn.prepareStatement("INSERT INTO "+tableName+" (first,last,age,city,sent,id) VALUES (?,?,?,?,1,?)");
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try {
			ServerSocket serverSocket = new ServerSocket(8001);
			int threadNo = 1;
			textArea.append("Listening on port "+serverSocket.getLocalPort()+"\n");
			while(true) {
				
				Socket socket = serverSocket.accept();
				
				ThreadHandler r = new ThreadHandler(socket,threadNo);
				Thread t = new Thread(r);
				t.start();
		
				threadNo++;
				
			}
			
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	class ThreadHandler implements Runnable{
		private Socket socket;
		private int threadNo;
		private DataOutputStream outData;
		private ObjectInputStream inObj;
		
		
		public ThreadHandler(Socket socket,int threadNo) {
			this.socket = socket;
			this.threadNo = threadNo;
		}
		
		public void run() {
			
			textArea.append("Starting thread for client "+threadNo+" at "+new Date()+"\n");
			textArea.append("Client "+threadNo+"'s host name is "+socket.getInetAddress().getHostName()+"\n");
			textArea.append("Client "+threadNo+"'s IP Address is "+socket.getInetAddress().getHostAddress()+"\n");
			try {
				textArea.setCaretPosition(textArea.getLineStartOffset(textArea.getLineCount()-1));
			} catch (BadLocationException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
			boolean threadStatus = true;
			
			try {
				outData = new DataOutputStream(socket.getOutputStream());
				inObj = new ObjectInputStream(socket.getInputStream());
			}
			catch(IOException e) {
				e.printStackTrace();
				threadStatus = false;
				//if error occurs that means there is connection problem, 
				//so we will terminate the connection and exit thread
			}
			
			while(true) {
				
				try {
					if(threadStatus) {
				
						Object obj = inObj.readObject();
						Person p = (Person)obj;
						textArea.append("Server get "+p.toString()+"from Client #"+threadNo+"\n");
						try {
							textArea.setCaretPosition(textArea.getLineStartOffset(textArea.getLineCount()-1));
						} catch (BadLocationException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
		
						//insert data to file
						String first = p.getFirst();
						String last = p.getLast();
						int age = p.getAge();
						String city = p.getCity();
						int id = p.getId();
						
						insertData.setString(1, first);
						insertData.setString(2, last);
						insertData.setInt(3, age);
						insertData.setString(4, city);
						insertData.setInt(5, id);
						
						insertData.execute();
						textArea.append("Inserted successfully\n");
						try {
							textArea.setCaretPosition(textArea.getLineStartOffset(textArea.getLineCount()-1));
						} catch (BadLocationException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
						//---finish inserting data---
					
						//send response to client
						outData.writeUTF("Success\n");
						outData.flush();
						
					}
					else {
						//when there is an error in network connection
						
						//terminate connection
						socket.close();
						System.out.println("Connection is closed");
						
						//exit thread
						System.out.println("Exit thread of client #"+threadNo);
						textArea.append("Client #"+threadNo+" IO Error: null, Ending connection\n");
						try {
							textArea.setCaretPosition(textArea.getLineStartOffset(textArea.getLineCount()-1));
						} catch (BadLocationException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
						
						break;
						
					}
				}
				catch(IOException e1) {
					//when the connection error occurs set thread status = false, 
					//enter termination process
					e1.printStackTrace();
					threadStatus = false;
					
				}
				catch (SQLException e2) {
					//if there is a DB error, send the fail message to the client
					//without terminate the connection
					try {
						e2.printStackTrace();
						outData.writeUTF("Failed\n");
						outData.flush();
						System.err.println("Database error: "+e2.getMessage());
					} catch (IOException e3) {
						threadStatus = false;
						e3.printStackTrace();
						System.err.println("IO error: "+e3.getMessage());
					}
				}
				catch(ClassNotFoundException e4) {
					e4.printStackTrace();
					try {
						outData.writeUTF("ClassError\n");
						outData.flush();
						System.err.println("Error: "+e4.getMessage());
					} catch (IOException e5) {
						threadStatus = false;
						e5.printStackTrace();
						System.err.println("IO error: "+e5.getMessage());
					}
				}
			}
			
		}
	}
	
	
	
	
	
	
	
	
	
	
}
