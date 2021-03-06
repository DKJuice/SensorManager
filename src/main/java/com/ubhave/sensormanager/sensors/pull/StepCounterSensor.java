/* **************************************************
 Copyright (c) 2012, University of Cambridge
 Neal Lathia, neal.lathia@cl.cam.ac.uk
 Kiran Rachuri, kiran.rachuri@cl.cam.ac.uk

This library was developed as part of the EPSRC Ubhave (Ubiquitous and
Social Computing for Positive Behaviour Change) Project. For more
information, please visit http://www.emotionsense.org

Permission to use, copy, modify, and/or distribute this software for any
purpose with or without fee is hereby granted, provided that the above
copyright notice and this permission notice appear in all copies.

THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR
IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 ************************************************** */

package com.ubhave.sensormanager.sensors.pull;

import java.util.Calendar;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.SystemClock;
import android.util.Log;

import com.ubhave.sensormanager.ESException;
import com.ubhave.sensormanager.config.pull.MotionSensorConfig;
import com.ubhave.sensormanager.data.SensorData;
import com.ubhave.sensormanager.data.pull.StepCounterData;
import com.ubhave.sensormanager.process.pull.StepCounterProcessor;
import com.ubhave.sensormanager.sensors.SensorUtils;

public class StepCounterSensor extends AbstractPullSensor
{
	protected final static Object lock = new Object();
	private static final String TAG = "StepCounterSensor";
	private static StepCounterSensor stepCounterSensor;
	private final SensorEventListener listener; // data listener
	private final SensorManager sensorManager; // Controls the hardware sensor

	private StepCounterData data;
	private float numSteps;
	private long lastBoot;

	public static StepCounterSensor getSensor(final Context context) throws ESException
	{
		if (stepCounterSensor == null)
		{
			synchronized (lock)
			{
				if (stepCounterSensor == null)
				{
					stepCounterSensor = new StepCounterSensor(context);
				}
			}
		}
		return stepCounterSensor;
	}

	protected StepCounterSensor(final Context context) throws ESException
	{
		super(context);
		sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
		if (getSensor() == null)
		{
			throw new ESException(ESException.SENSOR_UNAVAILABLE, getLogTag() + " is null (e.g., missing from device).");
		}
		else
		{
			listener = getListener();
		}
	}

	protected Sensor getSensor()
	{
		return sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
	}

	protected SensorEventListener getListener()
	{
		return new SensorEventListener()
		{
			/*
			 * This method is required by the API and is called when the
			 * accuracy of the readings being generated by the sensor changes.
			 * We currently don't do anything when this happens.
			 */
			public void onAccuracyChanged(final Sensor sensor, final int accuracy)
			{
			}

			/*
			 * This method is called when the sensor takes a reading: despite
			 * the name, it is called even if the data is the same as the
			 * previous one
			 */
			public void onSensorChanged(SensorEvent event)
			{
				try
				{
					if (isSensing)
					{
						synchronized (lock)
						{
							if (isSensing)
							{
								numSteps = event.values[0];
								long millisSinceSystemBoot = SystemClock.elapsedRealtime();
								lastBoot = System.currentTimeMillis() - millisSinceSystemBoot;
								
								Calendar calendar = Calendar.getInstance();
								calendar.setTimeInMillis(lastBoot);
								Log.d(TAG, "Num steps: "+numSteps);
								Log.d(TAG, "Last boot: "+calendar.getTime().toString());
							}
						}
					}
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
		};
	}

	@Override
	protected boolean startSensing()
	{
		int sensorDelay = (Integer) sensorConfig.getParameter(MotionSensorConfig.SAMPLING_DELAY);
		boolean registrationSuccess = sensorManager.registerListener(listener, getSensor(), sensorDelay);
		return registrationSuccess;
	}

	@Override
	protected void stopSensing()
	{
		sensorManager.unregisterListener(listener);
	}

	@Override
	public int getSensorType()
	{
		return SensorUtils.SENSOR_TYPE_STEP_COUNTER;
	}

	@Override
	protected void processSensorData()
	{
		Log.d(TAG, "processSensorData()");
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(lastBoot);
		Log.d(TAG, "Num steps: "+numSteps);
		Log.d(TAG, "Last boot: "+calendar.getTime().toString());
		StepCounterProcessor processor = (StepCounterProcessor) getProcessor();
		data = processor.process(pullSenseStartTimestamp, numSteps, lastBoot, sensorConfig.clone());
	}

	@Override
	protected String getLogTag()
	{
		return TAG;
	}

	@Override
	protected SensorData getMostRecentRawData()
	{
		Log.d(TAG, "getMostRecentRawData()");
		return data;
	}
}
