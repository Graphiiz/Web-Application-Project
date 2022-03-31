package wordfind;

import java.awt.event.KeyEvent;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class WordFinder extends JFrame {

	JMenuBar menuBar;
	JMenu menu;
	JMenuItem menuItem;
	
	JTextField inputField;
	JTextArea textArea;
	JButton clearButton;
	
	JFileChooser jFileChooser;
	private JPanel topPanel; // the top line of objects is going to go here
	WordList wordList;
	
	String textVal = "";
	
	private static final int FRAME_WIDTH = 310;
	private static final int FRAME_HEIGHT = 250;

	public WordFinder() {

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		// set the size correctly
		setSize(FRAME_WIDTH, FRAME_HEIGHT);
		
		
		jFileChooser = new JFileChooser(".");
		wordList = new WordList();
		topPanel = new JPanel();
		createMenus();
		
		// there should be objects in the top panel
		final int WIDTH = 10;
		SearchListener listener = new SearchListener();
		inputField = new JTextField(WIDTH);
		inputField.addCaretListener(listener);
		
		JLabel inputLabel = new JLabel("Find: ",JLabel.RIGHT);
		
		clearButton = new JButton("Clear");
		clearButton.addActionListener((e)->{inputField.setText("");});
		
		topPanel.add(inputLabel);
		topPanel.add(inputField);
		topPanel.add(clearButton);
		this.add(topPanel,BorderLayout.NORTH);
		
		// There should probably be something passed into the JScrollPane
		textArea = new JTextArea();
		textArea.setWrapStyleWord(true);
		textArea.setEditable(false);
		JScrollPane listScroller = new JScrollPane(textArea);
		listScroller.setPreferredSize(new Dimension(250, 80));
		
		// and of course you will want them to be properly aligned in the frame
		this.add(listScroller, BorderLayout.CENTER);

	}
	//---------------------------------------------------------------------------
	private void createMenus() {
		
		menuBar = new JMenuBar();
		
		/* add a "File" menu with:
		 * "Open" item which allows you to choose a new file
		 * "Exit" item which ends the process with System.exit(0);
		 * Key shortcuts are optional
		 */
		JMenu menu = createFileMenu();
		menuBar.add(menu);
		this.setJMenuBar(menuBar);
		
	}
	public JMenu createFileMenu(){
		JMenu menu = new JMenu("File");
		menu.add(createOpenFileItem());
		menu.add(createFileExitItem());
		return menu;
	}
	public JMenuItem createOpenFileItem() {
		JMenuItem item = new JMenuItem("Open");
		OpenFileListener listener = new OpenFileListener();
		item.addActionListener(listener);
		return item;
	}
	public JMenuItem createFileExitItem(){
		JMenuItem item = new JMenuItem("Exit");      
		item.addActionListener((e) -> System.exit(0));
		return item;
	 }
//---------------------------------------------------------------------------
	class SearchListener implements CaretListener {
		@Override
		public void caretUpdate(CaretEvent e) {
			try {
				String inputText = inputField.getText();
				if(!(inputText.equals(textVal))) {
					textVal = inputText;
					textArea.setText("");
					List<String> searchResult = wordList.find(inputText); // figure out from WordList how to get this
					for (Object s : searchResult) {
						textArea.append(s.toString());
						textArea.append("\n");
					}
					textArea.setCaretPosition(0);
				}
			}
			catch(Exception ex) {}
		}
	}
	class OpenFileListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			int returnVal = jFileChooser.showOpenDialog(getParent());
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				System.out.println("You chose to open this file: " + jFileChooser.getSelectedFile().getAbsolutePath());
				inputField.setText("");
				try {
					InputStream in = new FileInputStream(jFileChooser.getSelectedFile().getAbsolutePath());
					wordList.load(in);
				}
				catch(FileNotFoundException ex) {
					System.out.println("File Not Found!!");
				}
				catch(IOException ex) {
					System.out.println("Caught IOException: "+ex.getMessage());
				}
				List<String> searchResult = wordList.find(""); // figure out from WordList how to get this
				textArea.setText("");
				for (Object s : searchResult) {
					textArea.append(s.toString());
					textArea.append("\n");
				}
				textArea.setCaretPosition(0);
			}
		}
	}

	public static void main(String[] args) {

		WordFinder wordFinder = new WordFinder();
		wordFinder.setVisible(true);
	}
}
