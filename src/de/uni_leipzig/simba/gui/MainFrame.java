package de.uni_leipzig.simba.gui;

import java.awt.BorderLayout;
import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JFileChooser;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.JTextField;
import javax.swing.UIManager;

import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import javax.swing.JButton;
import javax.swing.BoxLayout;
import java.awt.GridLayout;
import javax.swing.border.MatteBorder;
import java.awt.Color;
import javax.swing.JCheckBox;
import javax.swing.JSpinner;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.File;

public class MainFrame extends JFrame {

	private JPanel contentPane;
	private JTextField txtFilerdf;
	private File fileToCompress;
	compressionFrame cframe;
	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
					MainFrame frame = new MainFrame();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the frame.
	 */
	public MainFrame() {
		fileToCompress = new File("resources/archive_hub_dump.nt");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 450, 300);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		GridBagLayout gbl_contentPane = new GridBagLayout();
		gbl_contentPane.columnWidths = new int[]{424, 0};
		gbl_contentPane.rowHeights = new int[]{14, 0, 193, 14, 0, 0};
		gbl_contentPane.columnWeights = new double[]{1.0, Double.MIN_VALUE};
		gbl_contentPane.rowWeights = new double[]{0.0, 1.0, 1.0, 0.0, 1.0, Double.MIN_VALUE};
		contentPane.setLayout(gbl_contentPane);
		
		JPanel headPanel = new JPanel();
		GridBagConstraints gbc_headPanel = new GridBagConstraints();
		gbc_headPanel.gridheight = 2;
		gbc_headPanel.insets = new Insets(0, 0, 5, 0);
		gbc_headPanel.fill = GridBagConstraints.BOTH;
		gbc_headPanel.gridx = 0;
		gbc_headPanel.gridy = 0;
		contentPane.add(headPanel, gbc_headPanel);
		
		JLabel lblHead = new JLabel("Head");
		headPanel.add(lblHead);
		
		JPanel centerPanel = new JPanel();
		centerPanel.setBorder(new MatteBorder(1, 1, 1, 1, (Color) new Color(0, 0, 0)));
		GridBagConstraints gbc_centerPanel = new GridBagConstraints();
		gbc_centerPanel.insets = new Insets(0, 0, 5, 0);
		gbc_centerPanel.fill = GridBagConstraints.BOTH;
		gbc_centerPanel.gridx = 0;
		gbc_centerPanel.gridy = 2;
		contentPane.add(centerPanel, gbc_centerPanel);
		centerPanel.setLayout(new GridLayout(2, 2, 1, 1));
		
		JPanel filePanel = new JPanel();
		centerPanel.add(filePanel);
		filePanel.setLayout(null);
		
		txtFilerdf = new JTextField();
		txtFilerdf.setText(fileToCompress.toString());
		txtFilerdf.setBounds(0, 0, 270, 20);
		filePanel.add(txtFilerdf);
		txtFilerdf.setColumns(10);
		
		JButton fileSelectButton = new JButton("select");
		fileSelectButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				final JFileChooser fc = new JFileChooser(fileToCompress.toString());
				fc.setSelectedFile(fileToCompress);
				
				int returnVal = fc.showOpenDialog(MainFrame.this);
				if(returnVal == JFileChooser.APPROVE_OPTION) {
					txtFilerdf.setText(fc.getSelectedFile().toString());
					fileToCompress = fc.getSelectedFile();
				}
			}
		});
		
		fileSelectButton.setBounds(306, 0, 89, 23);
		filePanel.add(fileSelectButton);
		
		JPanel propertyPanel = new JPanel();
		centerPanel.add(propertyPanel);
		
		JCheckBox chckbxActivateDeleteRules = new JCheckBox("activate Delete Rules");
		propertyPanel.add(chckbxActivateDeleteRules);
		
		JSpinner spinner = new JSpinner();
		propertyPanel.add(spinner);
		
		JPanel southPanel = new JPanel();
		GridBagConstraints gbc_southPanel = new GridBagConstraints();
		gbc_southPanel.gridheight = 2;
		gbc_southPanel.fill = GridBagConstraints.BOTH;
		gbc_southPanel.gridx = 0;
		gbc_southPanel.gridy = 3;
		contentPane.add(southPanel, gbc_southPanel);
		

		
		
		JButton btnStartCompression = new JButton("Start Compression");
		btnStartCompression.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				
				startCompression();
			}
		});
		southPanel.add(btnStartCompression);
		
		
		System.out.println("Compressing File "+fileToCompress.toString());
		cframe = new compressionFrame(fileToCompress);
//		this.add(cframe);
//		cframe.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
//		cframe.pack();
//		cframe.setVisible(true);
	}
	
	public void startCompression() {
		cframe.setVisible(true);
		cframe.setFile(fileToCompress);
		cframe.setAlwaysOnTop(true);
		cframe.start();
	}
}
