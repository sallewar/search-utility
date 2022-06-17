/*
 * 
 */
package com.searchUtility.core.utils;

import java.text.SimpleDateFormat;
import java.util.*;



public class Utils {

	/**
	 * Instantiates a new utils.
	 */
	private Utils() {
	}
	
	public static String getTimeStamp() {
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd.HH-mm");
		GregorianCalendar gc = new GregorianCalendar();
		return simpleDateFormat.format(gc.getTime());
	}

	

}