package com.osrsdatasync;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

/**
 * Side panel: account, Export (save everything now), the files' folder with a Copy Folder Path button, and when
 * the files were last written. Swing thread only.
 */
class ExportPanel extends PluginPanel
{
	private final JLabel account = new JLabel();
	private final JLabel status = new JLabel();
	private final JButton export = new JButton("Export");
	private final JButton copy = new JButton("Copy Folder Path");
	private final JTextArea folder = new JTextArea();

	ExportPanel(Runnable onExport, Runnable onCopyFolder)
	{
		setLayout(new BorderLayout());
		setBorder(new EmptyBorder(10, 10, 10, 10));

		JLabel title = new JLabel("OSRS Data Sync");
		title.setFont(FontManager.getRunescapeBoldFont());
		title.setForeground(ColorScheme.BRAND_ORANGE);
		account.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		status.setForeground(ColorScheme.LIGHT_GRAY_COLOR);

		export.setToolTipText("Re-read everything and save all files now");
		copy.setToolTipText("Copy the folder with this account's files, to paste into your file browser");
		export.addActionListener(e -> onExport.run());
		copy.addActionListener(e -> onCopyFolder.run());

		// The folder, selectable and wrapped (long paths)
		folder.setEditable(false);
		folder.setLineWrap(true);
		folder.setOpaque(false);
		folder.setFont(FontManager.getRunescapeSmallFont());
		folder.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		folder.setBorder(new EmptyBorder(6, 0, 0, 0));

		JPanel rows = new JPanel(new GridLayout(0, 1, 0, 6));
		rows.setBackground(ColorScheme.DARK_GRAY_COLOR);
		rows.add(title);
		rows.add(account);
		rows.add(export);
		rows.add(copy);
		rows.add(status);

		JPanel content = new JPanel(new BorderLayout());
		content.setBackground(ColorScheme.DARK_GRAY_COLOR);
		content.add(rows, BorderLayout.NORTH);
		content.add(folder, BorderLayout.CENTER);
		add(content, BorderLayout.NORTH);

		setPlayer(null);
	}

	/** The logged-in account's name, or null when logged out (Export is off then). */
	void setPlayer(String name)
	{
		account.setText(name == null ? "Not logged in" : name);
		export.setEnabled(name != null);
		if (name == null && status.getText().isEmpty())
		{
			status.setText("Log in to export");
		}
	}

	/** The folder the files are written to. */
	void setFolder(String path)
	{
		folder.setText(path);
	}

	void exported(String time)
	{
		status.setText("Exported " + time);
	}

	void saved(String time)
	{
		status.setText("Saved " + time);
	}

	void message(String text)
	{
		status.setText(text);
	}

	// For tests
	String statusText()
	{
		return status.getText();
	}

	boolean exportEnabled()
	{
		return export.isEnabled();
	}

	String folderText()
	{
		return folder.getText();
	}
}
