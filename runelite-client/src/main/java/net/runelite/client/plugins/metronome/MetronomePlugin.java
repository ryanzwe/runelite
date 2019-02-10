/*
 * Copyright (c) 2018, SomeoneWithAnInternetConnection
 * Copyright (c) 2018, oplosthee <https://github.com/oplosthee>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.runelite.client.plugins.metronome;

import com.google.inject.Provides;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.SoundEffectID;
import net.runelite.api.events.GameTick;
import net.runelite.client.Notifier;
import net.runelite.client.RuneLite;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;


import javax.sound.sampled.*;

import java.io.*;
import java.util.Objects;

import static jdk.nashorn.internal.runtime.regexp.joni.Config.log;

@PluginDescriptor(
		name = "Metronome",
		description = "Play a sound on a specified tick to aid in efficient skilling",
		tags = {"skilling", "tick", "timers"},
		enabledByDefault = false
)
public class MetronomePlugin extends Plugin {
	@Inject
	private Client client;

	@Inject
	private MetronomePluginConfiguration config;

	private int tickCounter = 0;
	private boolean shouldTock = false;
	private boolean loaded = false;
	@Provides
	MetronomePluginConfiguration provideConfig(ConfigManager configManager) {
		return configManager.getConfig(MetronomePluginConfiguration.class);
	}
	@Subscribe
	public void onGameTick(GameTick tick) {
		if (config.tickCount() == 0) {
			return;
		}
		if (++tickCounter % config.tickCount() == 0) {

			if (config.enableTock()) {
				playCustomSound();
			} else {
				client.playSoundEffect(SoundEffectID.GE_INCREMENT_PLOP);
			}
		}
	}

	private void playCustomSound() {
		Clip clip = null;

		// Try to load the user sound from ~/.runelite/notification.wav
		File file = new File(RuneLite.RUNELITE_DIR, "shortBeep.wav");
		log.println(file.toURI().toString());
		if (file.exists()) {
			try {
				InputStream fileStream = new BufferedInputStream(new FileInputStream(file));
				try (AudioInputStream sound = AudioSystem.getAudioInputStream(fileStream)) {
					clip = AudioSystem.getClip();
					clip.open(sound);
				}
			} catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
				clip = null;
				log.println("Unable to play notification sound");
			}
		}
		if (clip == null) {
			// Otherwise load from the classpath
			InputStream fileStream = new BufferedInputStream(Notifier.class.getResourceAsStream("notification.wav"));
			try (AudioInputStream sound = AudioSystem.getAudioInputStream(fileStream)) {
				clip = AudioSystem.getClip();
				clip.open(sound);
			} catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
				e.printStackTrace();
				return;
			}
		}
		setVolume(clip,config.volume());
		clip.start();
	}
	private void setVolume(Clip clip,float volume) {
		FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
		gainControl.setValue(20f * (float) Math.log10(volume/100));
	}
}
