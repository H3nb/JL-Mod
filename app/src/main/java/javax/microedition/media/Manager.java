/*
 * Copyright 2012 Kulikov Dmitriy
 * Copyright 2017-2020 Nikita Shakarun
 * Copyright 2021-2023 Yury Kharchenko
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package javax.microedition.media;

import android.Manifest;
import android.webkit.MimeTypeMap;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.microedition.io.Connector;
import javax.microedition.media.protocol.DataSource;
import javax.microedition.media.protocol.SourceStream;
import javax.microedition.media.tone.ToneManager;
import javax.microedition.util.ContextHolder;

import ru.woesss.j2me.mmapi.Plugin;
import ru.woesss.j2me.mmapi.synth.SynthPluginFactory;

public class Manager {
	public static final String TONE_DEVICE_LOCATOR = "device://tone";
	public static final String MIDI_DEVICE_LOCATOR = "device://midi";

	private static final String RESOURCE_LOCATOR = "resource://";
	private static final String FILE_LOCATOR = "file://";
	private static final String CAPTURE_AUDIO_LOCATOR = "capture://audio";
	private static final TimeBase DEFAULT_TIMEBASE = () -> javax.microedition.util.Time.nanoTime() / 1000L;
	private static final List<Plugin> PLUGINS = new ArrayList<>();
	private static final List<WeakReference<Player>> PLAYERS = new ArrayList<>();

	public static Player createPlayer(String locator) throws IOException, MediaException {
		if (locator == null) {
			throw new IllegalArgumentException();
		}
		Player player;
		if (MIDI_DEVICE_LOCATOR.equals(locator) || TONE_DEVICE_LOCATOR.equals(locator)) {
			player = null;
			for (Plugin plugin : PLUGINS) {
				player = plugin.createPlayer(locator);
				if (player != null) {
					break;
				}
			}
			if (player == null) {
				player = new MicroPlayer(locator);
			}
		} else if (locator.startsWith(FILE_LOCATOR) || locator.startsWith(RESOURCE_LOCATOR)) {
			InputStream stream = Connector.openInputStream(locator);
			String extension = locator.substring(locator.lastIndexOf('.') + 1);
			String type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
			return createPlayer(stream, type);
		} else if (locator.startsWith(CAPTURE_AUDIO_LOCATOR) &&
				ContextHolder.requestPermission(Manifest.permission.RECORD_AUDIO)) {
			player = new RecordPlayer();
		} else {
			player = new BasePlayer();
		}
		addPlayer(player);
		return player;
	}

	public static Player createPlayer(DataSource source) throws IOException, MediaException {
		if (source == null) {
			throw new IllegalArgumentException();
		}
		Player player;
		String type = source.getContentType();
		String[] supportedTypes = getSupportedContentTypes(null);
		if (type != null && Arrays.asList(supportedTypes).contains(type.toLowerCase())) {
			source.connect();
			SourceStream[] sourceStreams = source.getStreams();
			if (sourceStreams == null || sourceStreams.length == 0) {
				throw new MediaException();
			}
			SourceStream sourceStream = sourceStreams[0];
			InputStream stream = new InternalSourceStream(sourceStream);
			InternalDataSource datasource = new InternalDataSource(stream, type);
			player = new MicroPlayer(datasource);
		} else {
			player = new BasePlayer();
		}
		addPlayer(player);
		return player;
	}

	public static Player createPlayer(final InputStream stream, String type)
			throws IOException, MediaException {
		if (stream == null) {
			throw new IllegalArgumentException();
		}
		Player player = null;
		InternalDataSource datasource = new InternalDataSource(stream, type);
		for (Plugin plugin : PLUGINS) {
			player = plugin.createPlayer(datasource);
			if (player != null) {
				break;
			}
		}
		if (player == null) {
			String[] supportedTypes = getSupportedContentTypes(null);
			if (type != null && Arrays.asList(supportedTypes).contains(type.toLowerCase())) {
				player = new MicroPlayer(datasource);
			} else {
				player = new BasePlayer();
			}
		}
		addPlayer(player);
		return player;
	}

	private static void addPlayer(Player player) {
		synchronized (PLAYERS) {
			PLAYERS.add(new WeakReference<>(player));
		}
	}

	public static void updateRates(float speed) {
		synchronized (PLAYERS) {
			Iterator<WeakReference<Player>> iterator = PLAYERS.iterator();
			while (iterator.hasNext()) {
				Player player = iterator.next().get();
				if (player == null) {
					iterator.remove();
				} else if (player instanceof MicroPlayer) {
					((MicroPlayer) player).updateSpeed(speed);
				}
			}
		}
	}

	public static String[] getSupportedContentTypes(String str) {
		return new String[]{"audio/wav", "audio/x-wav", "audio/midi", "audio/x-midi",
				"audio/mpeg", "audio/aac", "audio/amr", "audio/amr-wb", "audio/mp3",
				"audio/mp4", "audio/mmf", "audio/x-tone-seq"};
	}

	public static String[] getSupportedProtocols(String str) {
		return new String[]{"device", "file", "http", "resource"};
	}

	public static TimeBase getSystemTimeBase() {
		return DEFAULT_TIMEBASE;
	}

	public synchronized static void playTone(int note, int duration, int volume)
			throws MediaException {
		ToneManager.getInstance().playTone(note, duration, volume);
	}

	static {
		SynthPluginFactory.loadPlugins(PLUGINS);
	}
}
