package com.osrsdatasync;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

/** Side panel: account, Export (save everything now), Open Folder, and when the files were last written. Swing thread only. */
class ExportPanel extends PluginPanel
{
	private final JLabel account = new JLabel();
	private final JLabel status = new JLabel();
	private final JButton export = new JButton("Export");
	private final JButton open = new JButton("Open Folder");

	ExportPanel(Runnable onExport, Runnable onOpenFolder)
	{
		setLayout(new BorderLayout());
		setBorder(new EmptyBorder(10, 10, 10, 10));

		JLabel title = new JLabel("OSRS Data Sync");
		title.setFont(FontManager.getRunescapeBoldFont());
		title.setForeground(ColorScheme.BRAND_ORANGE);
		account.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		status.setForeground(ColorScheme.LIGHT_GRAY_COLOR);

		export.setToolTipText("Re-read everything and save all files now");
		open.setToolTipText("Open the folder with this account's files");
		export.addActionListener(e -> onExport.run());
		open.addActionListener(e -> onOpenFolder.run());

		JPanel rows = new JPanel(new GridLayout(0, 1, 0, 6));
		rows.setBackground(ColorScheme.DARK_GRAY_COLOR);
		rows.add(title);
		rows.add(account);
		rows.add(export);
		rows.add(open);
		rows.add(status);
		add(rows, BorderLayout.NORTH);

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
}
