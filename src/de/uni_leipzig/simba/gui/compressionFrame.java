package de.uni_leipzig.simba.gui;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Observable;
import java.util.Observer;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.JLabel;
import java.awt.GridLayout;
import javax.swing.JProgressBar;

import de.uni_leipzig.simba.compress.IndexBasedCompressor;
import de.uni_leipzig.simba.compress.ModelCompressor;
import de.uni_leipzig.simba.io.Status;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;

public class compressionFrame extends JFrame implements Observer{

	private JPanel contentPane;
	JLabel lblCompressingFile;
	JLabel lblAction;
	JLabel lblStatus;
	JProgressBar progressBar;
	JButton btnShowLog ;
	JTextArea textArea;
	ModelCompressor compr;
	File f;
	boolean delete = false;
	boolean hdt = true;
	int deleteBorder = 0;
	
	
	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					compressionFrame frame = new compressionFrame();
					
					frame.setVisible(true);
					frame.start();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	public compressionFrame() {
		this(new File("resources/archive_hub_dump.nt"));
	}
	
	
	/**
	 * Create the frame.
	 */
	public compressionFrame(File f) {
		this.f = f;
		setBounds(100, 100, 630, 433);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		contentPane.setLayout(new BorderLayout(0, 0));
		setContentPane(contentPane);
		
		JPanel panelHead = new JPanel();
		contentPane.add(panelHead, BorderLayout.NORTH);
		
		lblCompressingFile = new JLabel("Compressing File "+f.toString());
		panelHead.add(lblCompressingFile);
		
		JPanel panelCenter = new JPanel();
		contentPane.add(panelCenter, BorderLayout.CENTER);
		panelCenter.setLayout(null);
		
		progressBar = new JProgressBar();
		progressBar.setBounds(0, 11, 594, 33);
		progressBar.setStringPainted(true);
		progressBar.setMaximum(7);
		panelCenter.add(progressBar);
		
		lblAction = new JLabel("Current Action");
		lblAction.setForeground(Color.RED);
		lblAction.setBounds(0, 79, 567, 42);
		panelCenter.add(lblAction);
		
		lblStatus = new JLabel("Status");
		lblStatus.setForeground(Color.GREEN);
		lblStatus.setBounds(0, 48, 583, 33);
		panelCenter.add(lblStatus);
		
		btnShowLog = new JButton("Show Log");
		btnShowLog.setEnabled(false);
		btnShowLog.setBounds(0, 120, 89, 23);
		panelCenter.add(btnShowLog);
		
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setBounds(0, 143, 594, 218);
		panelCenter.add(scrollPane);
		
		textArea = new JTextArea();
		scrollPane.setViewportView(textArea);
		textArea.setEditable(false);
		textArea.setEnabled(false);
		
		btnShowLog.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				try
                {
					textArea.setEnabled(true);
                    FileReader reader = new FileReader(compr.getLogFile());
                    BufferedReader br = new BufferedReader(reader);
                    textArea.read( br, null );
                    br.close();
                    textArea.requestFocus();
                }
                catch(Exception e2) { System.out.println(e2); }
			}
		});
		
		
		compr = new ModelCompressor(f);
		compr.addObserver(this);
	
	}

	public void start() {
//		compr = new ModelCompressor(f);
		Thread thread = new Thread(compr);
		compr.setFile(f);
		if(delete)
			compr.setDelete(deleteBorder);
		else
			compr.setDelete(0);
		compr.setHDT(hdt);
		thread.start();
//		compr.compress(f, 0);
	}
	
	@Override
	public void update(Observable o, Object arg) {
		if(arg.toString().equalsIgnoreCase("finished")) {
			this.dispose();
		}
		
		Status status = (Status) arg;
		
		progressBar.setValue(progressBar.getValue()+1);
		lblStatus.setText(status.getFeedback());
		lblAction.setText(status.getStatus());
		
		if(status.isFinished())
			btnShowLog.setEnabled(true);
//		try {
//			this.wait();
//		} catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
	}
	
	
	public void setFile(File f) {
		this.f = f;
		compr.setFile(f);
		lblCompressingFile.setText("Compressing File "+f.toString());
	}
	
	public void setDeleteBorder(int border) {
		if(border > 0) {
			this.deleteBorder = border;
			this.delete = true;
		}
		
	}
}
