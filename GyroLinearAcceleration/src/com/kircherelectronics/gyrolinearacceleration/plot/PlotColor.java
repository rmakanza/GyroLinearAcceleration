package com.kircherelectronics.gyrolinearacceleration.plot;

import android.content.Context;

import com.kircherelectronics.gyrolinearacceleration.R;

/*
 * Cardan Linear Acceleration
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
 * Manages colors.
 * 
 * @author Kaleb
 * @version %I%, %G%
 */
public class PlotColor
{
	private int lightBlue;
	private int lightPurple;
	private int lightGreen;
	private int lightOrange;
	private int lightRed;

	private int midBlue;
	private int midPurple;
	private int midGreen;
	private int midOrange;
	private int midRed;

	private int darkBlue;
	private int darkPurple;
	private int darkGreen;
	private int darkOrange;
	private int darkRed;

	public PlotColor(Context context)
	{
		lightBlue = context.getResources().getColor(R.color.light_blue);
		lightPurple = context.getResources().getColor(R.color.light_purple);
		lightGreen = context.getResources().getColor(R.color.light_green);
		lightOrange = context.getResources().getColor(R.color.light_orange);
		lightRed = context.getResources().getColor(R.color.light_red);

		midBlue = context.getResources().getColor(R.color.mid_blue);
		midPurple = context.getResources().getColor(R.color.mid_purple);
		midGreen = context.getResources().getColor(R.color.mid_green);
		midOrange = context.getResources().getColor(R.color.mid_orange);
		midRed = context.getResources().getColor(R.color.mid_red);

		darkBlue = context.getResources().getColor(R.color.dark_blue);
		darkPurple = context.getResources().getColor(R.color.dark_purple);
		darkGreen = context.getResources().getColor(R.color.dark_green);
		darkOrange = context.getResources().getColor(R.color.dark_orange);
		darkRed = context.getResources().getColor(R.color.dark_red);
	}

	public int getLightBlue()
	{
		return lightBlue;
	}

	public int getLightPurple()
	{
		return lightPurple;
	}

	public int getLightGreen()
	{
		return lightGreen;
	}

	public int getLightOrange()
	{
		return lightOrange;
	}

	public int getLightRed()
	{
		return lightRed;
	}

	public int getMidBlue()
	{
		return midBlue;
	}

	public int getMidPurple()
	{
		return midPurple;
	}

	public int getMidGreen()
	{
		return midGreen;
	}

	public int getMidOrange()
	{
		return midOrange;
	}

	public int getMidRed()
	{
		return midRed;
	}

	public int getDarkBlue()
	{
		return darkBlue;
	}

	public int getDarkPurple()
	{
		return darkPurple;
	}

	public int getDarkGreen()
	{
		return darkGreen;
	}

	public int getDarkOrange()
	{
		return darkOrange;
	}

	public int getDarkRed()
	{
		return darkRed;
	}
}
