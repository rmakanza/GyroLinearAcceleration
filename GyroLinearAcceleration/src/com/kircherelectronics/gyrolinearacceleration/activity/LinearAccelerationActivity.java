package com.kircherelectronics.gyrolinearacceleration.activity;

/*
 * Copyright 2013, Kircher Electronics
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Calendar;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.View.OnTouchListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.androidplot.xy.XYPlot;
import com.kircherelectronics.gyrolinearacceleration.R;
import com.kircherelectronics.gyrolinearacceleration.gauge.GaugeAcceleration;
import com.kircherelectronics.gyrolinearacceleration.gauge.GaugeRotation;
import com.kircherelectronics.gyrolinearacceleration.plot.DynamicPlot;
import com.kircherelectronics.gyrolinearacceleration.plot.PlotColor;
import com.kircherelectronics.gyrolinearacceleration.sensor.AccelerationSensor;
import com.kircherelectronics.gyrolinearacceleration.sensor.LinearAccelerationSensor;
import com.kircherelectronics.gyrolinearacceleration.sensor.observer.AccelerationSensorObserver;
import com.kircherelectronics.gyrolinearacceleration.sensor.observer.LinearAccelerationSensorObserver;

/**
 * Produces an estimation of the linear acceleration using a fusion between the
 * acceleration sensor and the gyroscope sensor. The gyroscope is used to
 * determine the tilt of the device and Trigonometric calculations are used to
 * determine the gravity components of the tilt angles via Cardan angles to
 * determine linear acceleration.
 * 
 * @author Kaleb
 * 
 */
public class LinearAccelerationActivity extends Activity implements Runnable,
		OnTouchListener, LinearAccelerationSensorObserver,
		AccelerationSensorObserver
{

	// Indicate if the output should be logged to a .csv file
	private boolean logData = false;

	// Decimal formats for the UI outputs
	private DecimalFormat df;

	// Graph plot for the UI outputs
	private DynamicPlot dynamicPlot;

	// Outputs for the acceleration and LPFs
	private float[] acceleration = new float[3];
	private float[] linearAcceleration = new float[3];

	// Touch to zoom constants for the dynamicPlot
	private float distance = 0;
	private float zoom = 1.2f;

	// The Acceleration Gauge
	private GaugeRotation gaugeAccelerationTilt;

	// The LPF Gauge
	private GaugeRotation gaugeLinearAccelTilt;

	// The Acceleration Gauge
	private GaugeAcceleration gaugeAcceleration;

	// The LPF Gauge
	private GaugeAcceleration gaugeLinearAcceleration;

	// Icon to indicate logging is active
	private ImageView iconLogger;

	// The generation of the log output
	private int generation = 0;

	// Plot keys for the acceleration plot
	private int plotAccelXAxisKey = 0;
	private int plotAccelYAxisKey = 1;
	private int plotAccelZAxisKey = 2;

	// Plot keys for the LPF Wikipedia plot
	private int plotLinearAccelXAxisKey = 3;
	private int plotLinearAccelYAxisKey = 4;
	private int plotLinearAccelZAxisKey = 5;

	// Color keys for the acceleration plot
	private int plotAccelXAxisColor;
	private int plotAccelYAxisColor;
	private int plotAccelZAxisColor;

	// Color keys for the LPF Wikipedia plot
	private int plotLinearAccelXAxisColor;
	private int plotLinearAccelYAxisColor;
	private int plotLinearAccelZAxisColor;

	// Log output time stamp
	private long logTime = 0;

	// Plot colors
	private PlotColor color;

	private AccelerationSensor accelerationSensor;
	private LinearAccelerationSensor linearAccelerationSensor;

	// Acceleration plot titles
	private String plotAccelXAxisTitle = "AX";
	private String plotAccelYAxisTitle = "AY";
	private String plotAccelZAxisTitle = "AZ";

	// LPF Wikipedia plot titles
	private String plotLinearAccelXAxisTitle = "lAX";
	private String plotLinearAccelYAxisTitle = "lAY";
	private String plotLinearAccelZAxisTitle = "lAZ";

	// Output log
	private String log;

	// Acceleration UI outputs
	private TextView xAxis;
	private TextView yAxis;
	private TextView zAxis;

	private Handler handler;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_linear_acceleration);

		View view = findViewById(R.id.plot_layout);
		view.setOnTouchListener(this);

		TextView accelerationLable = (TextView) view
				.findViewById(R.id.label_acceleration_name_0);
		accelerationLable.setText("Acceleration");

		TextView lpfLable = (TextView) view
				.findViewById(R.id.label_acceleration_name_1);
		lpfLable.setText("Fused");

		// Create the graph plot
		XYPlot plot = (XYPlot) findViewById(R.id.plot_sensor);
		plot.setTitle("Acceleration");
		dynamicPlot = new DynamicPlot(plot);
		dynamicPlot.setMaxRange(11.2);
		dynamicPlot.setMinRange(-11.2);

		// Create the acceleration UI outputs
		xAxis = (TextView) findViewById(R.id.value_x_axis);
		yAxis = (TextView) findViewById(R.id.value_y_axis);
		zAxis = (TextView) findViewById(R.id.value_z_axis);

		// Create the logger icon
		iconLogger = (ImageView) findViewById(R.id.icon_logger);
		iconLogger.setVisibility(View.INVISIBLE);

		// Format the UI outputs so they look nice
		df = new DecimalFormat("#.##");

		linearAccelerationSensor = new LinearAccelerationSensor(this);
		accelerationSensor = new AccelerationSensor(this);

		// Initialize the plots
		initColor();
		initPlot();
		initGauges();

		handler = new Handler();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.linear_acceleration, menu);
		return true;
	}

	/**
	 * Event Handling for Individual menu item selected Identify single menu
	 * item by it's id
	 * */
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{

		// Log the data
		case R.id.menu_settings_logger_plotdata:
			startDataLog();
			return true;

			// Reset the data
		case R.id.menu_settings_reset:
			linearAccelerationSensor.onPause();
			linearAccelerationSensor.onStart();
			return true;

			// Log the data
		case R.id.menu_settings_help:
			showHelpDialog();
			return true;

		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onPause()
	{
		super.onPause();

		accelerationSensor.removeAccelerationObserver(this);
		accelerationSensor.removeAccelerationObserver(linearAccelerationSensor);
		linearAccelerationSensor.removeAccelerationObserver(this);

		linearAccelerationSensor.onPause();

		if (logData)
		{
			writeLogToFile();
		}

		handler.removeCallbacks(this);
	}

	@Override
	public void onResume()
	{
		super.onResume();

		handler.post(this);

		accelerationSensor.registerAccelerationObserver(this);
		accelerationSensor
				.registerAccelerationObserver(linearAccelerationSensor);
		linearAccelerationSensor.registerAccelerationObserver(this);

		linearAccelerationSensor.onStart();
	}

	/**
	 * Pinch to zoom.
	 */
	@Override
	public boolean onTouch(View v, MotionEvent e)
	{
		// MotionEvent reports input details from the touch screen
		// and other input controls.
		float newDist = 0;

		switch (e.getAction())
		{

		case MotionEvent.ACTION_MOVE:

			// pinch to zoom
			if (e.getPointerCount() == 2)
			{
				if (distance == 0)
				{
					distance = fingerDist(e);
				}

				newDist = fingerDist(e);

				zoom *= distance / newDist;

				dynamicPlot.setMaxRange(zoom * Math.log(zoom));
				dynamicPlot.setMinRange(-zoom * Math.log(zoom));

				distance = newDist;
			}
		}

		return false;
	}

	@Override
	public void onAccelerationSensorChanged(float[] acceleration, long timeStamp)
	{
		// Get a local copy of the sensor values
		System.arraycopy(acceleration, 0, this.acceleration, 0,
				acceleration.length);
	}

	@Override
	public void onLinearAccelerationSensorChanged(float[] linearAcceleration,
			long timeStamp)
	{
		// Get a local copy of the sensor values
		System.arraycopy(linearAcceleration, 0, this.linearAcceleration, 0,
				linearAcceleration.length);
	}

	@Override
	public void run()
	{
		handler.postDelayed(this, 100);

		plotData();
		logData();

	}

	/**
	 * Create the plot colors.
	 */
	private void initColor()
	{
		color = new PlotColor(this);

		plotAccelXAxisColor = color.getDarkBlue();
		plotAccelYAxisColor = color.getDarkGreen();
		plotAccelZAxisColor = color.getDarkRed();

		plotLinearAccelXAxisColor = color.getMidBlue();
		plotLinearAccelYAxisColor = color.getMidGreen();
		plotLinearAccelZAxisColor = color.getMidRed();
	}

	/**
	 * Create the output graph line chart.
	 */
	private void initPlot()
	{
		addPlot(plotAccelXAxisTitle, plotAccelXAxisKey, plotAccelXAxisColor);
		addPlot(plotAccelYAxisTitle, plotAccelYAxisKey, plotAccelYAxisColor);
		addPlot(plotAccelZAxisTitle, plotAccelZAxisKey, plotAccelZAxisColor);

		addPlot(plotLinearAccelXAxisTitle, plotLinearAccelXAxisKey,
				plotLinearAccelXAxisColor);
		addPlot(plotLinearAccelYAxisTitle, plotLinearAccelYAxisKey,
				plotLinearAccelYAxisColor);
		addPlot(plotLinearAccelZAxisTitle, plotLinearAccelZAxisKey,
				plotLinearAccelZAxisColor);
	}

	/**
	 * Create the RMS Noise bar chart.
	 */
	private void initGauges()
	{
		gaugeAccelerationTilt = (GaugeRotation) findViewById(R.id.gauge_rotation_0);
		gaugeLinearAccelTilt = (GaugeRotation) findViewById(R.id.gauge_rotation_1);

		gaugeAcceleration = (GaugeAcceleration) findViewById(R.id.gauge_acceleration_0);
		gaugeLinearAcceleration = (GaugeAcceleration) findViewById(R.id.gauge_acceleration_1);
	}

	/**
	 * Add a plot to the graph.
	 * 
	 * @param title
	 *            The name of the plot.
	 * @param key
	 *            The unique plot key
	 * @param color
	 *            The color of the plot
	 */
	private void addPlot(String title, int key, int color)
	{
		dynamicPlot.addSeriesPlot(title, key, color);
	}

	private void showHelpDialog()
	{
		Dialog helpDialog = new Dialog(this);
		helpDialog.setCancelable(true);
		helpDialog.setCanceledOnTouchOutside(true);

		helpDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

		helpDialog.setContentView(getLayoutInflater().inflate(R.layout.help,
				null));

		helpDialog.show();
	}

	/**
	 * Begin logging data to an external .csv file.
	 */
	private void startDataLog()
	{
		if (logData == false)
		{
			CharSequence text = "Logging Data";
			int duration = Toast.LENGTH_SHORT;

			Toast toast = Toast.makeText(this, text, duration);
			toast.show();

			String headers = "Generation" + ",";

			headers += "Timestamp" + ",";

			headers += this.plotAccelXAxisTitle + ",";

			headers += this.plotAccelYAxisTitle + ",";

			headers += this.plotAccelZAxisTitle + ",";

			headers += this.plotLinearAccelXAxisTitle + ",";

			headers += this.plotLinearAccelYAxisTitle + ",";

			headers += this.plotLinearAccelZAxisTitle + ",";

			log = headers + "\n";

			iconLogger.setVisibility(View.VISIBLE);

			logData = true;
		}
		else
		{
			iconLogger.setVisibility(View.INVISIBLE);

			logData = false;
			writeLogToFile();
		}
	}

	/**
	 * Plot the output data in the UI.
	 */
	private void plotData()
	{
		dynamicPlot.setData(acceleration[0], plotAccelXAxisKey);
		dynamicPlot.setData(acceleration[1], plotAccelYAxisKey);
		dynamicPlot.setData(acceleration[2], plotAccelZAxisKey);

		dynamicPlot.setData(linearAcceleration[0], plotLinearAccelXAxisKey);
		dynamicPlot.setData(linearAcceleration[1], plotLinearAccelYAxisKey);
		dynamicPlot.setData(linearAcceleration[2], plotLinearAccelZAxisKey);

		dynamicPlot.draw();

		// Update the view with the new acceleration data
		xAxis.setText(df.format(acceleration[0]));
		yAxis.setText(df.format(acceleration[1]));
		zAxis.setText(df.format(acceleration[2]));

		gaugeAccelerationTilt.updateRotation(acceleration);
		gaugeLinearAccelTilt.updateRotation(linearAcceleration);

		gaugeAcceleration.updatePoint(acceleration[0], acceleration[1],
				Color.parseColor("#33b5e5"));

		gaugeLinearAcceleration.updatePoint(linearAcceleration[0],
				linearAcceleration[1], Color.parseColor("#33b5e5"));
	}

	/**
	 * Log output data to an external .csv file.
	 */
	private void logData()
	{
		if (logData)
		{
			if (generation == 0)
			{
				logTime = System.currentTimeMillis();
			}

			log += System.getProperty("line.separator");
			log += generation++ + ",";
			log += System.currentTimeMillis() - logTime + ",";

			log += acceleration[0] + ",";
			log += acceleration[1] + ",";
			log += acceleration[2] + ",";

			log += linearAcceleration[0] + ",";
			log += linearAcceleration[1] + ",";
			log += linearAcceleration[2] + ",";
		}
	}

	/**
	 * Write the logged data out to a persisted file.
	 */
	private void writeLogToFile()
	{
		Calendar c = Calendar.getInstance();
		String filename = "GyroLinearAcceleration-" + c.get(Calendar.YEAR)
				+ "-" + c.get(Calendar.DAY_OF_WEEK_IN_MONTH) + "-"
				+ c.get(Calendar.HOUR) + "-" + c.get(Calendar.HOUR) + "-"
				+ c.get(Calendar.MINUTE) + "-" + c.get(Calendar.SECOND)
				+ ".csv";

		File dir = new File(Environment.getExternalStorageDirectory()
				+ File.separator + "GyroLinearAcceleration" + File.separator
				+ "Logs" + File.separator + "Acceleration");
		if (!dir.exists())
		{
			dir.mkdirs();
		}

		File file = new File(dir, filename);

		FileOutputStream fos;
		byte[] data = log.getBytes();
		try
		{
			fos = new FileOutputStream(file);
			fos.write(data);
			fos.flush();
			fos.close();

			CharSequence text = "Log Saved";
			int duration = Toast.LENGTH_SHORT;

			Toast toast = Toast.makeText(this, text, duration);
			toast.show();
		}
		catch (FileNotFoundException e)
		{
			CharSequence text = e.toString();
			int duration = Toast.LENGTH_SHORT;

			Toast toast = Toast.makeText(this, text, duration);
			toast.show();
		}
		catch (IOException e)
		{
			// handle exception
		}
		finally
		{
			// Update the MediaStore so we can view the file without rebooting.
			// Note that it appears that the ACTION_MEDIA_MOUNTED approach is
			// now blocked for non-system apps on Android 4.4.
			MediaScannerConnection.scanFile(this, new String[]
			{ "file://" + Environment.getExternalStorageDirectory() }, null,
					new MediaScannerConnection.OnScanCompletedListener()
					{
						@Override
						public void onScanCompleted(final String path,
								final Uri uri)
						{

						}
					});
		}
	}

	/**
	 * Get the distance between fingers for the touch to zoom.
	 * 
	 * @param event
	 * @return
	 */
	private final float fingerDist(MotionEvent event)
	{
		float x = event.getX(0) - event.getX(1);
		float y = event.getY(0) - event.getY(1);
		return (float) Math.sqrt(x * x + y * y);
	}

}
