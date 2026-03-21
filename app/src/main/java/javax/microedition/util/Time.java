/*
 * Copyright 2024 Yury Kharchenko
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

package javax.microedition.util;

import android.util.Log;

public class Time {
	private static final String TAG = "EmulatorTime";
	private static float speed = 1.0f;
	private static long lastRealTime = System.currentTimeMillis();
	private static long lastVirtualTime = lastRealTime;

	public static synchronized void setSpeed(float s) {
		lastVirtualTime = currentTimeMillis();
		lastRealTime = System.currentTimeMillis();
		speed = s;
		Log.d(TAG, "Speed multiplier set to: " + s);
	}

	public static float getSpeed() {
		return speed;
	}

	public static synchronized long currentTimeMillis() {
		long now = System.currentTimeMillis();
		return lastVirtualTime + (long) ((now - lastRealTime) * speed);
	}

	public static synchronized long nanoTime() {
		return currentTimeMillis() * 1_000_000L;
	}

	public static void sleep(long ms) throws InterruptedException {
		if (speed <= 0) {
			Thread.sleep(ms);
			return;
		}
		Thread.sleep((long) (ms / speed));
	}
}
