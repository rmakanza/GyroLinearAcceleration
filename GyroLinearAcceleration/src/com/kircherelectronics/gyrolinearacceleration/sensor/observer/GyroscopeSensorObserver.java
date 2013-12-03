package com.kircherelectronics.gyrolinearacceleration.sensor.observer;

/*
 * Copyright 2013, Kaleb Kircher - Boki Software, Kircher Electronics
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * An gyroscope sensor observer interface. Classes that need to observe the
 * gyroscope sensor for updates should do so with this interface.
 * 
 * @author Kaleb
 * @version %I%, %G%
 */
public interface GyroscopeSensorObserver
{
	/**
	 * Notify observers when new gyroscope measurements are available.
	 * @param gyroscope the rotation values (x, y, z)
	 * @param timeStamp the time of the sensor update.
	 */
	public void onGyroscopeSensorChanged(float[] gyroscope, long timeStamp);
}
