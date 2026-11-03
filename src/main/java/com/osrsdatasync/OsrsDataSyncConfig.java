package com.osrsdatasync;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(OsrsDataSyncConfig.GROUP)
public interface OsrsDataSyncConfig extends Config
{
	String GROUP = "osrsdatasync";

	@ConfigItem(
		keyName = "autoSave",
		name = "Save Automatically",
		description = "Save changes to the files as they happen. When off, the files are written only when you press"
			+ " Export in the side panel, or when you log out.",
		position = 0
	)
	default boolean autoSave()
	{
		return true;
	}
}
