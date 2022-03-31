package finalproject.client;

import java.util.ArrayList;
import java.sql.*;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import javax.swing.*;

import finalproject.client.ClientInterface.ComboBoxItem;
import finalproject.db.DBInterface;
import finalproject.entities.Person;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

public class ClientInterface extends JFrame {

	private static final long serialVersionUID = 1L;

	public static final int DEFAULT_PORT = 8001;
	
	private static final int FRAME_WIDTH = 600;
	private static final int FRAME_HEIGHT = 400;
	final int AREA_ROWS = 10;
	final int AREA_COLUMNS = 40;
	
	//UI variables
	private JLabel dbName;
	private JLabel connInfo;
	private JTextArea textArea;
	private JComboBox peopleSelect;
	private JFileChooser jFileChooser;
	
	//Database variable
	private PreparedStatement queryDB;
	private PreparedStatement queryPerson;
	private PreparedStatement querySendable;
	private PreparedStatement updateSent;
	private Connection conn;
	
	//Networking variables
	private String host = "localhost";
	Socket socket;
	int port;
	ObjectOutputStream outData;
	InputStreamReader inData;
	
	//other variables
	private String header;
	private boolean firstOpen = false;//check for the first click on open connection button
	
	
	public ClientInterface() {
		this(DEFAULT_PORT);
		
		//create client UI
		this.setSize(FRAME_WIDTH, FRAME_HEIGHT);
		this.createMenuBar();
		this.createPanel();
		this.jFileChooser = new JFileChooser(".");
		
	}
	
	public ClientInterface(int port) {
		this.port = port;
		
	}
	
	private void createMenuBar() {
		JMenuBar menuBar = new JMenuBar();
		menuBar.add(createFileMenu());
		setJMenuBar(menuBar);
	}
	
	private JMenu createFileMenu()
	{
		JMenu menu = new JMenu("File");
		menu.add(createFileOpenItem());
		menu.add(createFileExitItem());
		return menu;
	}
	
	private JMenuItem createFileExitItem(){
		JMenuItem item = new JMenuItem("Exit");
		item.addActionListener(e->System.exit(0));
		return item;
	}
   
	public void createPanel(){
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		panel.add(createControlPanel(),BorderLayout.NORTH);
		textArea = new JTextArea();
		textArea.setEditable(false);
		//JScrollPane scroller = new JScrollPane(textArea);
		//scroller.setPreferredSize(new Dimension(250, 80));
		panel.add(textArea,BorderLayout.CENTER);
		this.add(panel);
	}
	   
	public JPanel createControlPanel(){
		JPanel controlPanel = new JPanel();
		controlPanel.setLayout(new GridLayout(5,1));
		   
		//1st line
		JPanel firstLine = new JPanel();
		JLabel dbLabel = new JLabel("Active DB: ");
		dbName = new JLabel("<None>");
		firstLine.add(dbLabel);
		firstLine.add(dbName);
		//2nd line
		JPanel secondLine = new JPanel();
		JLabel connLabel = new JLabel("Active Connection: ");
		connInfo = new JLabel("<None>");
		secondLine.add(connLabel);
		secondLine.add(connInfo);
		//3rd line
		JPanel comboPanel = new JPanel();
		comboPanel.setSize(20, 5);
		peopleSelect = new JComboBox<String>();
		peopleSelect.addItem("<Empty>");
		comboPanel.add(peopleSelect);
		//4th line
		JPanel connPanel = new JPanel();
		JButton openConn = new JButton("Open Connection");
		JButton closeConn = new JButton("Close Connection");
		openConn.addActionListener(new OpenConnectionListener());
		closeConn.addActionListener(new CloseConnectionLestener());
		connPanel.add(openConn);
		connPanel.add(closeConn);
		//5th line
		JPanel dataPanel = new JPanel();
		JButton send = new JButton("Send Data");
		JButton query = new JButton("Query DB Data");
		query.addActionListener(new QueryButtonListener());
		send.addActionListener(new SendButtonListener());
		dataPanel.add(send);
		dataPanel.add(query);
		
		   
		controlPanel.add(firstLine);
		controlPanel.add(secondLine);
		controlPanel.add(comboPanel);
		controlPanel.add(connPanel);
		controlPanel.add(dataPanel);
		return controlPanel;
	   }
  
   private void fillComboBox() throws SQLException {
	   
	   List<ComboBoxItem> l = getNames();
	   peopleSelect.setModel(new DefaultComboBoxModel(l.toArray()));
	   
   }
   
   private void clearComboBox() {
	   peopleSelect.removeAllItems();
	   peopleSelect.addItem("<Empty>");
	   
   }
   
   private JMenuItem createFileOpenItem() {
	   JMenuItem item = new JMenuItem("Open DB");
	   item.addActionListener(new OpenDBListener());
	   return item;
   }

//-------------------------------------------------------------------------------------------------
   
   //All action listeners
   
   class QueryButtonListener implements ActionListener{
	   
	   public void actionPerformed(ActionEvent e) {
		   try {
			  textArea.setText("");
			  ResultSet rset = queryDB.executeQuery(); 
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
			  textArea.setText(header+rowData);
			  
		   }
		   catch(SQLException ex) {
			   ex.printStackTrace();
		   }
		   catch(NullPointerException ex) {
			   ex.printStackTrace();
			   System.err.println("database not found");
		   }
		   
		   
	   }
   }

   class OpenConnectionListener implements ActionListener{
	   
	   public void actionPerformed(ActionEvent e) {
		   //when the connection has already been opened, click open connection button
		   //does not generate the new connection.
		   try {
			   if(!firstOpen) {
				   socket = new Socket(host,8001);
				   socket.setSoTimeout(100);
				   int addr = socket.getPort();
				   connInfo.setText(host+":"+addr);//**change to that of server
				   System.out.println("connected!!!");
				   
				   //create persistent connection
				   //outData and inData are created only once when connection is established
				   outData = new ObjectOutputStream(socket.getOutputStream());
				   inData = new InputStreamReader(socket.getInputStream());
				   
				   firstOpen = true;
			   }
			   else {
				   if(socket.isClosed()) {
					   socket = new Socket(host,8001);
					   socket.setSoTimeout(100);
					   int addr = socket.getPort();
					   connInfo.setText(host+":"+addr);
					   System.out.println("connected!!!");
					   
					   //create persistent connection
					   //outData and inData are created only once when connection is established
					   outData = new ObjectOutputStream(socket.getOutputStream());
					   inData = new InputStreamReader(socket.getInputStream());
				   }
			   }
			   
		   } 
		   catch (IOException ex1) {
			   ex1.printStackTrace();
			   System.err.println("Server not found");
		   }
		   catch(SecurityException ex2) {
			   ex2.printStackTrace();
		   }
		   catch(NullPointerException ex3) {
			   ex3.printStackTrace();
		   }
		   
	   }
   }
   
   class CloseConnectionLestener implements ActionListener{
	   
	   public void actionPerformed(ActionEvent e) {
		   try {
			   outData.close();
			   inData.close();
			   socket.close();
			   connInfo.setText("<None>");
			   System.out.println("socket is closed!!");
			   
		   } 
		   catch (IOException e1) {
			   e1.printStackTrace();
		   }
		   catch(NullPointerException e2) {
			   e2.printStackTrace();
		   }
		   
	   }
   }
   class SendButtonListener implements ActionListener {

	   public void actionPerformed(ActionEvent e) {
		   
		   	boolean isWritten = true;	
		   	String[] fullName = null;
		   	Integer id = null;
		   	ComboBoxItem personEntry = null;
		 	   	
		   	//check data availability
			try {
				// now, get the person on the object dropdownbox we've selected
				personEntry = (ComboBoxItem) peopleSelect.getSelectedItem();

				// That's tricky which is why I have included the code. the personEntry
				// contains an ID and a name. You want to get a "Person" object out of that
				// which is stored in the database
				fullName = personEntry.name.split(" ", 2);
				id = (Integer) personEntry.id;
				queryPerson.setString(1, fullName[0]);
				queryPerson.setString(2, fullName[1]);
				queryPerson.setInt(3, id);
				
				ResultSet rset = queryPerson.executeQuery();
				String first = (String) rset.getObject(1);
				String last = (String) rset.getObject(2);
				int age = (int) rset.getObject(3);
				String city = (String) rset.getObject(4);

				Person p = new Person(first, last, age, city, id);
				System.out.println(p);

				// Send the person object here over an output stream that you got from the
				// socket.
				outData.writeObject(p);
				outData.flush();
				System.out.println("Object has been sent!!");
				// --finish sending--then wait for a response from server.
				
			} catch(IOException e1) {
				e1.printStackTrace();
				
				isWritten = false;
				try {
					socket.close();
					connInfo.setText("<None>");
					
				} catch (IOException e2) {
					e2.printStackTrace();
					System.err.println("IO error: "+e2.getMessage());
				}
				System.err.println("IO error: "+e1.getMessage());
				
				
			} catch (SQLException e3) {
				e3.printStackTrace();
				isWritten = false;
				System.err.println("database error");
				
			} catch (NullPointerException e4) {
				e4.printStackTrace();
				isWritten = false;
				if(Objects.isNull(personEntry)) {
					System.err.println("no data available: All data sent");
					try {
						if(socket.isClosed()) {
							System.out.println("socket is closed");
						}
						
					} catch (NullPointerException e7) {
						e7.printStackTrace();
						System.err.println("connection not found");
					}
				}
				else {
					System.err.println("connection not found");
				}
				
		
			} catch (ClassCastException e5) {
				e5.printStackTrace();
				isWritten = false;
				System.err.println("database not found");
				//check connection availability when database is not connected
				try {
					if(socket.isClosed()) {
						System.out.println("socket is closed");
					}
					
				} catch (NullPointerException e7) {
					e7.printStackTrace();
					System.err.println("connection not found");
				}
			}		
			//if object or connection is not available, no need to wait for response
			//since no data is sent, and server will not receive any data
			if(isWritten) {
				
				try {

					// responses are going to come over the input as text, and that's tricky,
					// which is why I've done that for you:
					BufferedReader br = new BufferedReader(inData);
					
					String response = br.readLine();
					if (response.contains("Success")) {
						System.out.println("Success");
						// what do you do after we know that the server has successfully
						// received the data and written it to its own database?
						// you will have to write the code for that.
						updateSent.setString(1, fullName[0]);
						updateSent.setString(2, fullName[1]);
						updateSent.setInt(3, id);
						updateSent.execute();
						clearComboBox();
						fillComboBox();

					} else {
						if (response.contains("Failed")) {
							//Server's Database error
							System.err.println("server's DB error");
						}
						if(response.contains("ClassError")) {
							System.err.println("error on sent object");
						}
						System.err.println("Failed");
					}
				} catch (IOException e1) {
					e1.printStackTrace();
					System.err.println("IO error: "+e1.getMessage());
					try {
						socket.close();
						connInfo.setText("<None>");
						System.err.println("connection is closed");
					} catch (IOException e2) {
						e2.printStackTrace();
					}
		
				} catch (SQLException e2) {
					e2.printStackTrace();
					System.err.println("database error");
					
				} catch (NullPointerException e3) {
					e3.printStackTrace();
					System.err.println("connection error");
					try {
						socket.close();
						connInfo.setText("<None>");
					} catch (IOException e4) {
						e4.printStackTrace();
					}
					
				}
			}      
			
		}
		
	}

	/* the "open db" menu item in the client should use this ActionListener */
	   class OpenDBListener implements ActionListener
	      {
	         public void actionPerformed(ActionEvent event)
	         {
	 			int returnVal = jFileChooser.showOpenDialog(getParent());
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					System.out.println("You chose to open this file: " + jFileChooser.getSelectedFile().getAbsolutePath());
					String dbFileName = jFileChooser.getSelectedFile().getAbsolutePath();
					String fileName = dbFileName.substring(dbFileName.lastIndexOf("/")+1);
					try {
						/* now that you have the dbFileName, you should probably connect to the DB */
						/* maybe think about filling the contents of the dropdown box listing names 
						 * and indicating the name of the Active DB
						 */
						connectToDB(fileName);
						dbName.setText(dbFileName.substring(dbFileName.lastIndexOf("/")+1));
						clearComboBox();
						fillComboBox();
						
					} catch (Exception e ) {
						System.err.println("error connection to db: "+ e.getMessage());
						e.printStackTrace();
						dbName.setText("<None>");
						clearComboBox();

					}
					
				}
	         }
	      }
	
//------------------------------------------------------------------------------------------------------------------
	   
	   //other supplementary methods
	   
   private List<ComboBoxItem> getNames() throws SQLException {
	   
	   List<ComboBoxItem> names = new ArrayList<ComboBoxItem>();
	   ResultSet rset = querySendable.executeQuery();
	   
	   //assume that we know the database structure well.
	   while (rset.next()) {
		   int id = (Integer)rset.getObject(6);
		   String name = (String)rset.getObject(1)+ " "+(String)rset.getObject(2);
		   //System.out.println(id);
		   //System.out.println(name);
		   ComboBoxItem item = new ComboBoxItem(id, name);
		   names.add(item);
		}
	   
	   return names;
   }
	
	// a JComboBox will take a bunch of objects and use the "toString()" method
	// of those objects to print out what's in there. 
	// So I have provided to you an object to put people's names and ids in
	// and the combo box will print out their names. 
	// now you will want to get the ComboBoxItem object that is selected in the combo box
	// and get the corresponding row in the People table and make a person object out of that.
	class ComboBoxItem {
		private int id;
		private String name;
		
		public ComboBoxItem(int id, String name) {
			this.id = id;
			this.name = name;
		}
		
		public int getId() {
			return this.id;
		}
		
		public String getName() {
			return this.name;
		}
		
		public String toString() {
			return this.name;
		}
	}
	/*In connectToDB I will prepare all necessary information for the future use
	 * 1. table name
	 * 2. queryDB statement
	 * 3. header of displayed data
	 */
	private void connectToDB(String fileName) {
		   try {
			   conn = DriverManager.getConnection("jdbc:sqlite:"+fileName);
			   
			   //get table name
			   DatabaseMetaData dbmd = conn.getMetaData();
			   ResultSet rset = dbmd.getTables(null, null, "%", null);
			   String tableName = rset.getObject(3).toString();
			   //System.out.println(tableName);
			   
			   //set prepared statement
			   queryDB = conn.prepareStatement("SELECT * FROM "+tableName);
			   queryPerson = conn.prepareStatement("SELECT first, last, age, city, id FROM "+tableName+" WHERE first = ? AND last = ? AND id = ?");
			   querySendable = conn.prepareStatement("SELECT * FROM "+tableName+" WHERE sent = 0");
			   updateSent = conn.prepareStatement("UPDATE "+tableName+" SET sent = 1 WHERE first = ? AND last = ? AND id = ?");
			   //set header
			   setHeader();
			   
			   
			   
		   } catch (SQLException e) {
			   // TODO Auto-generated catch block
			   e.printStackTrace();
		   }
	}
	
	private void setHeader() {
		try {
			header = "";
			String linebreak = "";
			ResultSet rset = queryDB.executeQuery();
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
		ClientInterface ci = new ClientInterface();
		ci.setVisible(true);
		ci.setDefaultCloseOperation(EXIT_ON_CLOSE);
	}
}


