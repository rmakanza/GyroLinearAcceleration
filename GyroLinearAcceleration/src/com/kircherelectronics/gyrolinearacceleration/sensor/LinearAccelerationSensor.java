package com.kircherelectronics.gyrolinearacceleration.sensor;

import java.util.ArrayList;
import java.util.Arrays;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.util.Log;

import com.kircherelectronics.gyrolinearacceleration.filters.MeanFilter;
import com.kircherelectronics.gyrolinearacceleration.sensor.observer.AccelerationSensorObserver;
import com.kircherelectronics.gyrolinearacceleration.sensor.observer.GravitySensorObserver;
import com.kircherelectronics.gyrolinearacceleration.sensor.observer.GyroscopeSensorObserver;
import com.kircherelectronics.gyrolinearacceleration.sensor.observer.LinearAccelerationSensorObserver;
import com.kircherelectronics.gyrolinearacceleration.sensor.observer.MagneticSensorObserver;

/*
 * Copyright (C) 2013, Kaleb Kircher - Boki Software, Kircher Engineering, LLC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/**
 * An implementation of an acceleration and gyroscope sensor fusion. The
 * algorithm determines the linear acceleration of the device by using Cardan
 * angles.
 * 
 * @author Kaleb
 * @see http://en.wikipedia.org/wiki/Low-pass_filter
 * @version %I%, %G%
 */
public class LinearAccelerationSensor implements GyroscopeSensorObserver,
		AccelerationSensorObserver, MagneticSensorObserver, GravitySensorObserver
{
	public static final float EPSILON = 0.000000001f;

	private static final String tag = LinearAccelerationSensor.class
			.getSimpleName();
	private static final float NS2S = 1.0f / 1000000000.0f;
	private static final int MEAN_FILTER_WINDOW = 10;
	private static final int MIN_SAMPLE_COUNT = 30;

	// Keep track of observers.
	private ArrayList<LinearAccelerationSensorObserver> observersAcceleration;

	private boolean hasInitialOrientation = false;
	private boolean stateInitialized = false;

	private Context context;

	private long timestampOld = 0;

	// Calibrated maths.
	private float[] currentRotationMatrix;
	private float[] deltaRotationMatrix;
	private float[] deltaRotationVector;
	private float[] gyroscopeOrientation;

	// The gravity components of the acceleration signal.
	private float[] components = new float[3];

	private float[] linearAcceleration = new float[]
	{ 0, 0, 0 };

	// Raw accelerometer data
	private float[] acceleration = new float[]
	{ 0, 0, 0 };
	
	private float[] gravity = new float[]
			{ 0, 0, 0 };

	// Raw accelerometer data
	private float[] magnetic = new float[]
	{ 0, 0, 0 };

	private int gravitySampleCount = 0;
	private int magneticSampleCount = 0;

	private MeanFilter mfAcceleration;
	private MeanFilter mfMagnetic;
	private MeanFilter mfGravity;
	private MeanFilter mfLinearAcceleration;

	// The rotation matrix R transforming a vector from the device
	// coordinate system to the world's coordinate system which is
	// defined as a direct orthonormal basis. R is the identity
	// matrix when the device is aligned with the world's coordinate
	// system, that is, when the device's X axis points toward East,
	// the Y axis points to the North Pole and the device is facing
	// the sky. NOTE: the reference coordinate-system used by
	// getOrientation() is different from the world
	// coordinate-system defined for the rotation matrix R and
	// getRotationMatrix().
	private float[] initialRotationMatrix;

	private GravitySensor gravitySensor;
	private GyroscopeSensor gyroscopeSensor;
	private MagneticSensor magneticSensor;

	public LinearAccelerationSensor(Context context)
	{
		super();

		this.context = context;
		observersAcceleration = new ArrayList<LinearAccelerationSensorObserver>();

		initFilters();
		initSensors();
		reset();
		restart();
	}

	public void onStart()
	{
		restart();
	}

	public void onPause()
	{
		reset();
	}

	@Override
	public void onAccelerationSensorChanged(float[] acceleration, long timeStamp)
	{
		// Get a local copy of the raw magnetic values from the device sensor.
		System.arraycopy(acceleration, 0, this.acceleration, 0,
				acceleration.length);
	}
	
	@Override
	public void onGravitySensorChanged(float[] gravity, long timeStamp)
	{
		// Get a local copy of the raw magnetic values from the device sensor.
		System.arraycopy(gravity, 0, this.gravity, 0,
				gravity.length);

		// Use a mean filter to smooth the sensor inputs
		this.gravity = mfGravity.filterFloat(this.gravity);

		// Count the number of samples received.
		gravitySampleCount++;

		// Only determine the initial orientation after the acceleration sensor
		// and magnetic sensor have had enough time to be smoothed by the mean
		// filters. Also, only do this if the orientation hasn't already been
		// determined since we only need it once.
		if (gravitySampleCount > MIN_SAMPLE_COUNT
				&& magneticSampleCount > MIN_SAMPLE_COUNT
				&& !hasInitialOrientation)
		{
			calculateOrientation();
		}
		
	}

	@Override
	public void onGyroscopeSensorChanged(float[] gyroscope, long timestamp)
	{
		// don't start until first accelerometer/magnetometer orientation has
		// been acquired
		if (!hasInitialOrientation)
		{
			return;
		}

		// Initialization of the gyroscope based rotation matrix
		if (!stateInitialized)
		{
			currentRotationMatrix = matrixMultiplication(currentRotationMatrix,
					initialRotationMatrix);

			stateInitialized = true;
		}

		// This timestep's delta rotation to be multiplied by the current
		// rotation after computing it from the gyro sample data.
		if (timestampOld != 0 && stateInitialized)
		{
			final float dT = (timestamp - timestampOld) * NS2S;

			// Axis of the rotation sample, not normalized yet.
			float axisX = gyroscope[0];
			float axisY = gyroscope[1];
			float axisZ = gyroscope[2];

			// Calculate the angular speed of the sample
			float omegaMagnitude = (float) Math.sqrt(axisX * axisX + axisY
					* axisY + axisZ * axisZ);

			// Normalize the rotation vector if it's big enough to get the axis
			if (omegaMagnitude > EPSILON)
			{
				axisX /= omegaMagnitude;
				axisY /= omegaMagnitude;
				axisZ /= omegaMagnitude;
			}

			// Integrate around this axis with the angular speed by the timestep
			// in order to get a delta rotation from this sample over the
			// timestep. We will convert this axis-angle representation of the
			// delta rotation into a quaternion before turning it into the
			// rotation matrix.
			float thetaOverTwo = omegaMagnitude * dT / 2.0f;

			float sinThetaOverTwo = (float) Math.sin(thetaOverTwo);
			float cosThetaOverTwo = (float) Math.cos(thetaOverTwo);

			deltaRotationVector[0] = sinThetaOverTwo * axisX;
			deltaRotationVector[1] = sinThetaOverTwo * axisY;
			deltaRotationVector[2] = sinThetaOverTwo * axisZ;
			deltaRotationVector[3] = cosThetaOverTwo;

			SensorManager.getRotationMatrixFromVector(deltaRotationMatrix,
					deltaRotationVector);

			currentRotationMatrix = matrixMultiplication(currentRotationMatrix,
					deltaRotationMatrix);

			SensorManager.getOrientation(currentRotationMatrix,
					gyroscopeOrientation);

			// values[0]: azimuth, rotation around the Z axis.
			// values[1]: pitch, rotation around the X axis.
			// values[2]: roll, rotation around the Y axis.

			// Find the gravity component of the X-axis
			// = g*-cos(pitch)*sin(roll);
			components[0] = (float) (SensorManager.GRAVITY_EARTH
					* -Math.cos(gyroscopeOrientation[1]) * Math
					.sin(gyroscopeOrientation[2]));

			// Find the gravity component of the Y-axis
			// = g*-sin(pitch);
			components[1] = (float) (SensorManager.GRAVITY_EARTH * -Math
					.sin(gyroscopeOrientation[1]));

			// Find the gravity component of the Z-axis
			// = g*cos(pitch)*cos(roll);
			components[2] = (float) (SensorManager.GRAVITY_EARTH
					* Math.cos(gyroscopeOrientation[1]) * Math
					.cos(gyroscopeOrientation[2]));
			
			Log.d(tag, Arrays.toString(components));

			// Subtract the gravity component of the signal
			// from the input acceleration signal to get the
			// tilt compensated output.
			linearAcceleration[0] = (this.acceleration[0] - components[0]);
			linearAcceleration[1] = (this.acceleration[1] - components[1]);
			linearAcceleration[2] = (this.acceleration[2] - components[2]);

			linearAcceleration = mfLinearAcceleration
					.filterFloat(linearAcceleration);
		}

		timestampOld = timestamp;

		notifyLinearAccelerationObserver();
	}

	@Override
	public void onMagneticSensorChanged(float[] magnetic, long timeStamp)
	{
		// Get a local copy of the raw magnetic values from the device sensor.
		System.arraycopy(magnetic, 0, this.magnetic, 0, magnetic.length);

		// Use a mean filter to smooth the sensor inputs
		this.magnetic = mfMagnetic.filterFloat(this.magnetic);

		// Count the number of samples received.
		magneticSampleCount++;
	}

	/**
	 * Notify observers with new measurements.
	 */
	private void notifyLinearAccelerationObserver()
	{
		for (LinearAccelerationSensorObserver a : observersAcceleration)
		{
			a.onLinearAccelerationSensorChanged(this.linearAcceleration,
					this.timestampOld);
		}
	}

	/**
	 * Register for Sensor.TYPE_ACCELEROMETER measurements.
	 * 
	 * @param observer
	 *            The observer to be registered.
	 */
	public void registerAccelerationObserver(
			LinearAccelerationSensorObserver observer)
	{
		// Only register the observer if it is not already registered.
		int i = observersAcceleration.indexOf(observer);
		if (i == -1)
		{
			observersAcceleration.add(observer);
		}

	}

	/**
	 * Remove Sensor.TYPE_ACCELEROMETER measurements.
	 * 
	 * @param observer
	 *            The observer to be removed.
	 */
	public void removeAccelerationObserver(
			LinearAccelerationSensorObserver observer)
	{
		int i = observersAcceleration.indexOf(observer);
		if (i >= 0)
		{
			observersAcceleration.remove(i);
		}
	}

	/**
	 * Calculates orientation angles from accelerometer and magnetometer output.
	 * Note that we only use this *once* at the beginning to orient the
	 * gyroscope to earth frame. If you do not call this, the gyroscope will
	 * orient itself to whatever the relative orientation the device is in at
	 * the time of initialization.
	 */
	private void calculateOrientation()
	{
		hasInitialOrientation = SensorManager.getRotationMatrix(
				initialRotationMatrix, null, gravity, magnetic);

		// Remove the sensor observers since they are no longer required.
		if (hasInitialOrientation)
		{
			gravitySensor.removeGravityObserver(this);
			magneticSensor.removeMagneticObserver(this);
		}
	}

	/**
	 * Initialize the mean filters.
	 */
	private void initFilters()
	{
		mfAcceleration = new MeanFilter();
		mfAcceleration.setWindowSize(MEAN_FILTER_WINDOW);
		
		mfGravity = new MeanFilter();
		mfGravity.setWindowSize(MEAN_FILTER_WINDOW);

		mfLinearAcceleration = new MeanFilter();
		mfLinearAcceleration.setWindowSize(MEAN_FILTER_WINDOW);

		mfMagnetic = new MeanFilter();
		mfMagnetic.setWindowSize(MEAN_FILTER_WINDOW);
	}

	/**
	 * Initialize the data structures required for the maths.
	 */
	private void initMaths()
	{
		acceleration = new float[3];
		magnetic = new float[3];

		initialRotationMatrix = new float[9];

		deltaRotationVector = new float[4];
		deltaRotationMatrix = new float[9];
		currentRotationMatrix = new float[9];
		gyroscopeOrientation = new float[3];

		// Initialize the current rotation matrix as an identity matrix...
		currentRotationMatrix[0] = 1.0f;
		currentRotationMatrix[4] = 1.0f;
		currentRotationMatrix[8] = 1.0f;
	}

	/**
	 * Initialize the sensors.
	 */
	private void initSensors()
	{
		gravitySensor = new GravitySensor(context);
		magneticSensor = new MagneticSensor(context);
		gyroscopeSensor = new GyroscopeSensor(context);
	}

	/**
	 * Multiply matrix a by b. Android gives us matrices results in
	 * one-dimensional arrays instead of two, so instead of using some (O)2 to
	 * transfer to a two-dimensional array and then an (O)3 algorithm to
	 * multiply, we just use a static linear time method.
	 * 
	 * @param a
	 * @param b
	 * @return a*b
	 */
	private float[] matrixMultiplication(float[] a, float[] b)
	{
		float[] result = new float[9];

		result[0] = a[0] * b[0] + a[1] * b[3] + a[2] * b[6];
		result[1] = a[0] * b[1] + a[1] * b[4] + a[2] * b[7];
		result[2] = a[0] * b[2] + a[1] * b[5] + a[2] * b[8];

		result[3] = a[3] * b[0] + a[4] * b[3] + a[5] * b[6];
		result[4] = a[3] * b[1] + a[4] * b[4] + a[5] * b[7];
		result[5] = a[3] * b[2] + a[4] * b[5] + a[5] * b[8];

		result[6] = a[6] * b[0] + a[7] * b[3] + a[8] * b[6];
		result[7] = a[6] * b[1] + a[7] * b[4] + a[8] * b[7];
		result[8] = a[6] * b[2] + a[7] * b[5] + a[8] * b[8];

		return result;
	}

	/**
	 * Restarts all of the sensor observers and resets the activity to the
	 * initial state. This should only be called *after* a call to reset().
	 */
	private void restart()
	{
		gravitySensor.registerGravityObserver(this);
		magneticSensor.registerMagneticObserver(this);
		gyroscopeSensor.registerGyroscopeObserver(this);
	}

	/**
	 * Removes all of the sensor observers and resets the activity to the
	 * initial state.
	 */
	private void reset()
	{
		gravitySensor.removeGravityObserver(this);
		magneticSensor.removeMagneticObserver(this);
		gyroscopeSensor.removeGyroscopeObserver(this);

		initMaths();

		gravitySampleCount = 0;
		magneticSampleCount = 0;

		hasInitialOrientation = false;
		stateInitialized = false;
	}
}
