package com.kircherelectronics.gyrolinearacceleration.sensor;

import java.util.ArrayList;

import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import com.kircherelectronics.gyrolinearacceleration.sensor.observer.AccelerationSensorObserver;
import com.kircherelectronics.gyrolinearacceleration.sensor.observer.GravitySensorObserver;

/**
 * Gravity Sensor is a subject in an Observer Pattern for classes that need to
 * be provided with acceleration measurements. Acceleration Sensor implements
 * Sensor.TYPE_GRAVITY and provides methods for managing SensorEvents and
 * rotations. Unlike Sensor.TYPE_ACCELERATION, Sensor.TYPE_GRAVITY uses a sensor
 * fusion to help sequester the gravity and linear acceleration components of
 * the signal, providing only the gravity measurement. This is advantageous in
 * many implementations. For example, Sensor.TYPE_GRAVITY provides more accurate
 * tilt compensations than Sensor.TYPE_ACCELERATION, especially while the device
 * is experiencing linear acceleration.
 * 
 * Note that Sensor.TYPE_GRAVITY is not implemented on all Android devices.
 * Appropriate steps to fall back onto Sensor.TYPE_ACCELERATION should be taken.
 * To help facilitate this process, GravityAccelerationSensor and
 * AccelerationSensor both register the type AccelerationSensorObserver, so
 * observers can be ambiguous to the configuration.
 * 
 * @author Kaleb
 * @version %I%, %G%
 */
public class GravitySensor implements SensorEventListener
{
	/*
	 * Developer Note: Quaternions are used for the internal representations of
	 * the rotations which prevents the polar anomalies associated with Gimbal
	 * lock when using Euler angles for the rotations.
	 */
	
	private static final String tag = GravitySensor.class.getSimpleName();

	// Keep track of observers.
	private ArrayList<GravitySensorObserver> observersAcceleration;

	// Keep track of the application mode. Vehicle Mode occurs when the device
	// is in the Landscape orientation and the sensors are rotated to face the
	// -Z-Axis (along the axis of the camera).
	private boolean vehicleMode = false;

	// We need the Context to register for Sensor Events.
	private Context context;

	// Keep a local copy of the acceleration values that are copied from the
	// sensor event.
	private float[] gravity = new float[3];

	// The time stamp of the most recent Sensor Event.
	private long timeStamp = 0;

	// Quaternion data structures to rotate a matrix from the absolute Android
	// orientation to the orientation that the device is actually in. This is
	// needed because the the device sensors orientation is fixed in hardware.
	// Also remember the many algorithms require a NED orientation which is not
	// the same as the absolute Android orientation. Do not confuse this
	// rotation with a rotation into absolute earth frame!
	private Rotation yQuaternion;
	private Rotation xQuaternion;
	private Rotation rotationQuaternion;

	// We need the SensorManager to register for Sensor Events.
	private SensorManager sensorManager;

	// The vectors that will be rotated when the application is in Vehicle Mode.
	private Vector3D vIn;
	private Vector3D vOut;

	/**
	 * Initialize the state.
	 * 
	 * @param context
	 *            the Activities context.
	 */
	public GravitySensor(Context context)
	{
		super();

		this.context = context;

		initQuaternionRotations();

		observersAcceleration = new ArrayList<GravitySensorObserver>();

		sensorManager = (SensorManager) this.context
				.getSystemService(Context.SENSOR_SERVICE);

	}

	/**
	 * Register for Sensor.TYPE_GRAVITY measurements.
	 * 
	 * @param observer
	 *            The observer to be registered.
	 */
	public void registerGravityObserver(GravitySensorObserver observer)
	{
		// If there are currently no observers, but one has just requested to be
		// registered, register to listen for sensor events from the device.
		if (observersAcceleration.size() == 0)
		{
			sensorManager.registerListener(this,
					sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY),
					SensorManager.SENSOR_DELAY_FASTEST);
		}

		// Only register the observer if it is not already registered.
		int i = observersAcceleration.indexOf(observer);
		if (i == -1)
		{
			observersAcceleration.add(observer);
		}
	}

	/**
	 * Remove Sensor.TYPE_GRAVITY measurements.
	 * 
	 * @param observer
	 *            The observer to be removed.
	 */
	public void removeGravityObserver(AccelerationSensorObserver observer)
	{
		int i = observersAcceleration.indexOf(observer);
		if (i >= 0)
		{
			observersAcceleration.remove(i);
		}

		// If there are no observers, then don't listen for Sensor Events.
		if (observersAcceleration.size() == 0)
		{
			sensorManager.unregisterListener(this);
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy)
	{
		// Do nothing.
	}

	@Override
	public void onSensorChanged(SensorEvent event)
	{
		if (event.sensor.getType() == Sensor.TYPE_GRAVITY)
		{
			System.arraycopy(event.values, 0, gravity, 0,
					event.values.length);

			timeStamp = event.timestamp;

			if (vehicleMode)
			{
				gravity = quaternionToDeviceVehicleMode(gravity);
			}

			notifyGravityObserver();
		}
	}

	/**
	 * Vehicle mode occurs when the device is put into the landscape
	 * orientation. On Android phones, the positive Y-Axis of the sensors faces
	 * towards the top of the device. In vehicle mode, we want the sensors to
	 * face the negative Z-Axis so it is aligned with the camera of the device.
	 * 
	 * @param vehicleMode
	 *            true if in vehicle mode.
	 */
	public void setVehicleMode(boolean vehicleMode)
	{
		this.vehicleMode = vehicleMode;
	}

	/**
	 * To avoid anomalies at the poles with Euler angles and Gimbal lock,
	 * quaternions are used instead.
	 */
	private void initQuaternionRotations()
	{
		// Rotate by 90 degrees or pi/2 radians.
		double rotation = Math.PI / 2;

		// Create the rotation around the x-axis
		Vector3D xV = new Vector3D(1, 0, 0);
		xQuaternion = new Rotation(xV, rotation);

		// Create the rotation around the y-axis
		Vector3D yV = new Vector3D(0, 1, 0);
		yQuaternion = new Rotation(yV, -rotation);

		// Create the composite rotation.
		rotationQuaternion = yQuaternion.applyTo(xQuaternion);
	}

	/**
	 * Notify observers with new measurements.
	 */
	private void notifyGravityObserver()
	{
		for (GravitySensorObserver a : observersAcceleration)
		{
			a.onGravitySensorChanged(this.gravity, this.timeStamp);
		}
	}

	/**
	 * Orient the measurements from the absolute Android device rotation into
	 * the current device orientation. Note that the rotation is different based
	 * on the current rotation of the device relative to the absolute Android
	 * rotation. Do not confuse this with a rotation into absolute earth frame,
	 * or the NED orientation that the algorithm assumes.
	 * 
	 * @param measurements
	 *            the measurements referenced to the absolute Android
	 *            orientation.
	 * @return the measurements referenced to the current device rotation.
	 * 
	 * @see http 
	 *      ://developer.android.com/reference/android/hardware/SensorEvent.html
	 *      #values
	 */
	private float[] quaternionToDeviceVehicleMode(float[] matrix)
	{

		vIn = new Vector3D(matrix[0], matrix[1], matrix[2]);
		vOut = rotationQuaternion.applyTo(vIn);

		float[] rotation =
		{ (float) vOut.getX(), (float) vOut.getY(), (float) vOut.getZ() };

		return rotation;
	}
}
