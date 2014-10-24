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

import de.uni_leipzig.simba.decompress.DefaultDecompressor;
import de.uni_leipzig.simba.io.Status;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;

public class decompressionFrame extends JFrame implements Observer{

	private JPanel contentPane;
	JLabel lblCompressingFile;
	JLabel lblAction;
	JLabel lblStatus;
	JProgressBar progressBar;
	DefaultDecompressor decompr;
	File f;
	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					decompressionFrame frame = new decompressionFrame();
					
					frame.setVisible(true);
					frame.start();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	public decompressionFrame() {
		this(new File("resources/archive_hub_dump.nt"));
	}
	
	
	/**
	 * Create the frame.
	 */
	public decompressionFrame(File f) {
		this.f = f;
		setBounds(100, 100, 630, 233);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		contentPane.setLayout(new BorderLayout(0, 0));
		setContentPane(contentPane);
		
		JPanel panelHead = new JPanel();
		contentPane.add(panelHead, BorderLayout.NORTH);
		
		lblCompressingFile = new JLabel("Decompressing File resources\\archive_hub_dump.nt");
		panelHead.add(lblCompressingFile);
		
		JPanel panelCenter = new JPanel();
		contentPane.add(panelCenter, BorderLayout.CENTER);
		panelCenter.setLayout(null);
		
		progressBar = new JProgressBar();
		progressBar.setBounds(0, 11, 594, 33);
		progressBar.setStringPainted(true);
		progressBar.setMaximum(4);
		panelCenter.add(progressBar);
		
		lblAction = new JLabel("Current Action");
		lblAction.setForeground(Color.RED);
		lblAction.setBounds(0, 79, 567, 42);
		panelCenter.add(lblAction);
		
		lblStatus = new JLabel("Status");
		lblStatus.setForeground(Color.GREEN);
		lblStatus.setBounds(0, 48, 583, 33);
		panelCenter.add(lblStatus);
		
		
		decompr = new DefaultDecompressor(f);
		decompr.addObserver(this);
	
	}

	public void start() {
//		compr = new ModelCompressor(f);
		Thread thread = new Thread(decompr);
		//compr.setFile(f);
		//compr.setDelete(0);
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
		
	}
	
	
	public void setFile(File f) {
		this.f = f;
		//compr.setFile(f);
		lblCompressingFile.setText("Decompressing File "+f.toString());
	}
}
