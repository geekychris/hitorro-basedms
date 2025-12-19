/*
 * Copyright (c) 2006-2025 Chris Collins
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.hitorro.basedms.db;

import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Copyright (c) 2003-2008 HiTorro All rights reserved. User: chris Date: Oct 9, 2006 Time: 3:15:33 PM
 */
public class MysqlUtil {

    //  Period / Interval on which Metrics are grouped
    public static final String PERIOD_INTERVAL_HOUR = "HOUR";
    public static final String PERIOD_INTERVAL_DAY = "DAY";
    public static final String PERIOD_INTERVAL_WEEK = "WEEK";
    public static final String PERIOD_INTERVAL_MONTH = "MONTH";
    public static final String PERIOD_INTERVAL_QUARTER = "QUARTER";
    public static final String PERIOD_INTERVAL_YEAR = "YEAR";

    //  Date Formatting
    protected static final String DATE_FORMAT_STRING = "yyyy/MM/dd"; //"dd MMM yyyy";
    protected static final String DATE_FORMAT_DISPLAY_STRING = "MMM dd, yyyy";
    protected static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat(DATE_FORMAT_STRING);
    protected static SimpleDateFormat DATE_FORMAT_DISPLAY = new SimpleDateFormat(DATE_FORMAT_DISPLAY_STRING);


    public static String toTimestamp(String date) {
        return " unix_timestamp('" + date + "') ";
    }

    public static String toDateIntervalTimestamp(Calendar dateFrom, String periodType, int periodFrame) {
        return toDateIntervalTimestamp(DATE_FORMAT.format(dateFrom.getTime()), periodType, periodFrame);
    }


    public static String toDateIntervalTimestamp(String strDateFrom, String periodType, int periodFrame) {
        return " unix_timestamp(" + toDateInterval(strDateFrom, periodType, periodFrame) + ") ";
    }

    /**
     * @param strDateFrom Start Date
     * @param periodType  Period Type
     * @param periodFrame Amount of Period Type frames to add to Start Date
     * @return MySQL SQL statement to add a time period to the specified date
     */
    public static String toDateInterval(String strDateFrom, String periodType, int periodFrame) {
        return " DATE_ADD('" + strDateFrom + "', INTERVAL " + periodFrame + " " + periodType + ") ";
    }

    public static String toDateInterval(Calendar dateFrom, String periodType, int periodFrame) {
        return " DATE_ADD('" + formatCalendar(dateFrom) + "', INTERVAL " + periodFrame + " " + periodType + ") ";
    }


    /**
     * @param calendar java.util.Calendar to be formatted as String
     * @return Calendar's underlying date formatted using the Date Format
     */
    public static String formatCalendar(Calendar calendar) {
        return DATE_FORMAT.format(calendar.getTime());
    }

    public static String formatCalendarDisplay(Calendar calendar) {
        return DATE_FORMAT_DISPLAY.format(calendar.getTime());
    }

    /**
     * @param filter Target string being searched
     * @return filter for finding all items like the specified filter
     */
    public static String filterLikeAll(String filter) {
        return "'%" + filter + "%'";
    }

    /**
     * Use this method if you need SQL to find the first saturday of a week containing the specified date
     * <p/>
     * SELECT '2007/01/05', DATE_SUB('2007/01/05', INTERVAL DATE_FORMAT('2007/01/05', '%w') + 1 DAY) Returns:
     * 2006/12/30
     *
     * @param date String date (e.g. "2007/01/05")
     * @return Date of Saturday for week containing the specified date
     */
    public static String getFirstDayOfWeek(String date) {
        return "DATE_SUB('" + date + "', INTERVAL DATE_FORMAT('" + date + "', '%w') + 1 DAY)";
    }


}
