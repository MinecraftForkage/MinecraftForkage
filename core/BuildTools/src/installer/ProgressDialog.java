package installer;

import immibis.bon.IProgressListener;

import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Window;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Random;

public class ProgressDialog extends JDialog implements IProgressListener {
	/* not serializable */
	private static final long serialVersionUID = new Random().nextLong();

	public JLabel label;
	public JProgressBar bar;
	public ProgressDialog(Window parent) {
		super(parent);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				System.exit(1);
			}
		});
		setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		setTitle("MCF Installer Progress");
		
		JPanel root = new JPanel();
		setContentPane(root);
		root.setLayout(null);
		
		JPanel panel = new JPanel();

		panel.setLayout(null);
		panel.setBounds(0, 0, 321, 106);
		root.add(panel);
		
		label = new JLabel("<label>");
		label.setBounds(10, 29, 301, 14);
		panel.add(label);
		label.setHorizontalAlignment(SwingConstants.CENTER);
		
		bar = new JProgressBar();
		bar.setBounds(10, 71, 301, 24);
		panel.add(bar);
		
		setVisible(true);
		
		setPreferredSize(panel.getSize());
		pack();
		Dimension clientSize = root.getSize();
		Dimension windowSize = getSize();
		// increase preferred size by border size
		setPreferredSize(new Dimension(windowSize.width*2 - clientSize.width, windowSize.height*2 - clientSize.height));
		pack();
		
		setLocationRelativeTo(null);
	}
	
	public static ProgressDialog openModal(Window parent, String title) {
		ProgressDialog dlg = new ProgressDialog(parent);
		dlg.setTitle(title);
		dlg.setVisible(true);
		return dlg;
	}

	public void startIndeterminate(final String string) {
		EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				label.setText(string);
				bar.setIndeterminate(true);
				label.repaint();
			}
		});
	}

	public void initProgressBar(final int min, final int max) {
		EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				bar.setMinimum(min);
				bar.setMaximum(max);
				bar.setValue(0);
				bar.setIndeterminate(false);
			}
		});
	}

	public void incrementProgress(final int i) {
		EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				bar.setValue(bar.getValue()+i);
			}
		});
	}

	@Override
	public void start(final int max, final String text) {
		EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				initProgressBar(0, max);
				label.setText(text);
			}
		});
	}

	@Override
	public void set(final int value) {
		EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				bar.setValue(value);
			}
		});
	}

	@Override
	public void setMax(final int max) {
		EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				initProgressBar(0, max);
			}
		});
	}
}
