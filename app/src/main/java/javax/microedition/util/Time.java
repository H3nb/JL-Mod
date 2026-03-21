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
	private static volatile float speed = 1.0f;

	// Anchor points for time virtualization
	// realOriginNanos uses System.nanoTime() (monotonic, high precision)
	private static long realOriginNanos = System.nanoTime();
	// virtualOriginNanos is the "virtual" epoch in nanoseconds
	private static long virtualOriginNanos = System.currentTimeMillis() * 1_000_000L;

	public static synchronized void setSpeed(float s) {
		long nowNanos = System.nanoTime();
		// Catch up virtual origin to current time before changing speed scale
		virtualOriginNanos = currentVirtualNanos(nowNanos);
		realOriginNanos = nowNanos;
		speed = s;
		Log.d(TAG, "Speed multiplier set to: " + s);
	}

	public static float getSpeed() {
		return speed;
	}

	private static long currentVirtualNanos(long nowNanos) {
		return virtualOriginNanos + (long) ((nowNanos - realOriginNanos) * (double) speed);
	}

	public static synchronized long currentTimeMillis() {
		return currentVirtualNanos(System.nanoTime()) / 1_000_000L;
	}

	public static synchronized long nanoTime() {
		return currentVirtualNanos(System.nanoTime());
	}

	/**
	 * Sleeps for a virtual duration.
	 * Truncates sleep duration based on speed multiplier.
	 */
	public static void sleep(long ms) throws InterruptedException {
		float s = speed;
		if (s <= 0) {
			// Avoid division by zero or negative speed issues
			Thread.sleep(ms);
			return;
		}
		// Calculate precise sleep time
		double targetMs = ms / (double) s;
		long millis = (long) targetMs;
		int nanos = (int) ((targetMs - millis) * 1_000_000);
		
		// Thread.sleep(0, 0) returns immediately, which is correct for high speed
		Thread.sleep(millis, nanos);
	}
}
