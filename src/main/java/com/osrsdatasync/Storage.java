package com.osrsdatasync;

import com.google.gson.Gson;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.util.Filepath;
import net.runelite.client.util.LinkBrowser;

/**
 * Files under .runelite/plugin-data/osrsdatasync/&lt;player&gt;/. Every disk access, and turning data into
 * JSON, runs on the executor (one thread, so writes stay in order), never on the client thread.
 */
@Slf4j
class Storage
{
	interface Root
	{
		Filepath get() throws IOException;
	}

	private final Root root;
	private final ScheduledExecutorService executor;

	Storage(Root root, ScheduledExecutorService executor)
	{
		this.root = root;
		this.executor = executor;
	}

	// Last JSON written per file (player/file), so an unchanged file isn't written again. Executor thread only.
	private final Map<String, String> lastBody = new HashMap<>();

	// Called on the executor after each successful save or append (the side panel's "Saved" time)
	private volatile Runnable onSaved = () -> { };

	void setOnSaved(Runnable onSaved)
	{
		this.onSaved = onSaved;
	}

	/** Runs {@code task} on the executor after everything queued before it (e.g. once an export's writes are done). */
	void after(Runnable task)
	{
		executor.execute(task);
	}

	/** Opens the account's folder (or the plugin's folder when {@code player} is null) in the file browser. */
	void openFolder(String player)
	{
		executor.execute(() ->
		{
			try
			{
				Filepath dir = player == null ? root.get() : folder(player);
				dir.createDirectories();
				LinkBrowser.open(dir.toString());
			}
			catch (IOException | RuntimeException e)
			{
				log.warn("Could not open the folder", e);
			}
		});
	}

	/**
	 * Saves {@code data} as compact JSON with an "updated" time first. The JSON is made here on the executor, not on
	 * the client thread, so pass data the client thread won't change afterwards (fresh maps or copies). Skipped when
	 * nothing changed since the last save of that file.
	 */
	void writeJson(String player, String fileName, Gson gson, Map<String, Object> data)
	{
		executor.execute(() ->
		{
			String body;
			try
			{
				body = gson.toJson(data);
			}
			catch (RuntimeException e)
			{
				log.warn("Could not make {}/{}", player, fileName, e);
				return;
			}
			String key = player + "/" + fileName;
			if (body.equals(lastBody.get(key)))
			{
				return;
			}
			lastBody.put(key, body);
			String stamped = "{\"updated\":\"" + Instant.now() + "\"" + (body.length() > 2 ? "," : "") + body.substring(1);
			save(player, fileName, stamped);
		});
	}

	/** Forgets what was written, so the next save of each file happens even if unchanged (new login). */
	void forget()
	{
		executor.execute(lastBody::clear);
	}

	/** Replaces the file in one step (temp file + rename), so a reader never sees half a file. Executor only. */
	private void save(String player, String fileName, String text)
	{
		try
		{
			Filepath dir = folder(player);
			Filepath tmp = dir.joinSegment(fileName + ".tmp");
			tmp.write(text);
			tmp.moveTo(dir.joinSegment(fileName), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
			log.debug("Saved {}/{}", player, fileName);
			onSaved.run();
		}
		catch (IOException | RuntimeException e)
		{
			log.warn("Could not save {}/{}", player, fileName, e);
		}
	}

	/** Adds lines to a .jsonl file, one per entry, in one write. */
	void append(String player, String fileName, List<String> lines)
	{
		if (lines.isEmpty())
		{
			return;
		}
		StringBuilder text = new StringBuilder();
		for (String line : lines)
		{
			text.append(line).append('\n');
		}
		executor.execute(() ->
		{
			try (OutputStream out = folder(player).joinSegment(fileName)
				.openOutputStream(StandardOpenOption.CREATE, StandardOpenOption.APPEND, StandardOpenOption.WRITE))
			{
				out.write(text.toString().getBytes(StandardCharsets.UTF_8));
				onSaved.run();
			}
			catch (IOException | RuntimeException e)
			{
				log.warn("Could not append to {}/{}", player, fileName, e);
			}
		});
	}

	/** Reads a file on the executor and hands its text (null when missing) to {@code then}, still on the executor. */
	void read(String player, String fileName, Consumer<String> then)
	{
		executor.execute(() ->
		{
			String text = null;
			try
			{
				Filepath f = folder(player).joinSegment(fileName);
				if (f.isFile())
				{
					try (InputStream in = f.openInputStream())
					{
						text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
					}
				}
			}
			catch (IOException | RuntimeException e)
			{
				log.warn("Could not read {}/{}", player, fileName, e);
			}
			then.accept(text);
		});
	}

	private Filepath folder(String player) throws IOException
	{
		Filepath dir = root.get().joinSegment(player);
		dir.createDirectories();
		return dir;
	}
}
