package com.qx.imui.util;

import android.content.Context;

import com.qx.imlib.qlog.QLog;
import com.qx.imui.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class TimeUtil {

    private static final String TAG = "TimeUtil";

    public static String getTimeString(Context resources, long timesamp) {
        String result = "";
        Calendar todayCalendar = Calendar.getInstance();
        Calendar otherCalendar = Calendar.getInstance();
        otherCalendar.setTimeInMillis(timesamp);

//        String timeFormat = resources.getString(R.string.qx_time_format);
//        String yearTimeFormat =  resources.getString(R.string.qx_time_format_1);
        String am_pm = "";
        int hour = otherCalendar.get(Calendar.HOUR_OF_DAY);
        if (hour >= 0 && hour < 6) {
            am_pm = resources.getString(R.string.qx_time_early_moning);
        } else if (hour >= 6 && hour < 12) {
            am_pm = resources.getString(R.string.qx_time_moning);
        } else if (hour == 12) {
            am_pm = resources.getString(R.string.qx_time_noon);
        } else if (hour > 12 && hour < 18) {
            am_pm = resources.getString(R.string.qx_time_afternoon);
        } else if (hour >= 18) {
            am_pm = resources.getString(R.string.qx_time_night);
        }

        boolean yearTemp = todayCalendar.get(Calendar.YEAR) == otherCalendar.get(Calendar.YEAR);
        if (yearTemp) {
            int todayMonth = todayCalendar.get(Calendar.MONTH);
            int otherMonth = otherCalendar.get(Calendar.MONTH);
            if (todayMonth == otherMonth) {//表示是同一个月
                int temp = todayCalendar.get(Calendar.DATE) - otherCalendar.get(Calendar.DATE);
                switch (temp) {
                    case 0:
                        result = getHourAndMin(timesamp);
                        break;
                    case 1:
                        result = resources.getString(R.string.qx_time_yesterday, getHourAndMin(timesamp));
                        break;
                    case 2:
                    case 3:
                    case 4:
                    case 5:
                    case 6:
                        int dayOfMonth = otherCalendar.get(Calendar.WEEK_OF_MONTH);
                        int todayOfMonth = todayCalendar.get(Calendar.WEEK_OF_MONTH);
                        if (dayOfMonth == todayOfMonth) {//表示是同一周
                            int dayOfWeek = otherCalendar.get(Calendar.DAY_OF_WEEK);
                            if (dayOfWeek != 1) {//判断当前是不是星期日     如想显示为：周日 12:09 可去掉此判断
                                String dayNames[] = resources.getResources().getStringArray(R.array.qx_day);
                                result = dayNames[otherCalendar.get(Calendar.DAY_OF_WEEK) - 1] + getHourAndMin(timesamp);
                            } else {
                                result = getTime(resources, am_pm, timesamp);
                            }
                        } else {
                            result = getTime(resources, am_pm, timesamp);
                        }
                        break;
                    default:
                        result = getTime(resources, am_pm, timesamp);
                        break;
                }
            } else {
                result = getTime(resources, am_pm, timesamp);
            }
        } else {
            result = getYearHourAndDay(resources, am_pm, timesamp);
        }
        return result;
    }

    public static String getDate(long time) {
        try {
            SimpleDateFormat format = new SimpleDateFormat("MM-dd");
            return format.format(new Date(time));
        } catch (Exception e) {
            e.printStackTrace();
            QLog.e(TAG, "时间格式化错误:getDate time:" + time);
        }
        return "";
    }


    /**
     * 当天的显示时间格式
     *
     * @param time
     * @return
     */
    public static String getYearHourAndDay(Context context, long time) {
        try {
            SimpleDateFormat format = new SimpleDateFormat(context.getString(R.string.qx_time_format_3));
            return format.format(new Date(time));
        } catch (Exception e) {
            e.printStackTrace();
            QLog.e(TAG, "时间格式化错误: getHourAndMin time:" + time);
        }
        return "";
    }

    /**
     * 当天的显示时间格式
     *
     * @param time
     * @return
     */
    public static String getHourAndMin(long time) {
        try {
            SimpleDateFormat format = new SimpleDateFormat("HH:mm");
            return format.format(new Date(time));
        } catch (Exception e) {
            e.printStackTrace();
            QLog.e(TAG, "时间格式化错误: getHourAndMin time:" + time);
        }
        return "";
    }


    /**
     * 获取完整格式例如：M月d日 晚上 15:30
     *
     * @param activity
     * @param am_pm
     * @param timesamp
     * @return
     */
    public static String getTime(Context activity, String am_pm, long timesamp) {
        String hourDayStr = "";
        try {
            SimpleDateFormat format = new SimpleDateFormat(activity.getString(R.string.qx_time_format_2));
            hourDayStr = format.format(new Date(timesamp));
        } catch (Exception e) {
            e.printStackTrace();
            QLog.e(TAG, "时间格式化错误: getTime time:" + am_pm);
        }
        return (hourDayStr + " " + am_pm + " " + getHourAndMin(timesamp));
    }

    /**
     * 获取完整格式例如：yyyy年M月d日 晚上 15:30
     *
     * @param activity
     * @param am_pm
     * @param timesamp
     * @return
     */
    public static String getYearHourAndDay(Context activity, String am_pm, long timesamp) {
        String yearHourDayStr = "";
        try {
            SimpleDateFormat format = new SimpleDateFormat(activity.getString(R.string.qx_time_format_3));
            yearHourDayStr = format.format(new Date(timesamp));
        } catch (Exception e) {
            e.printStackTrace();
            QLog.e(TAG, "时间格式化错误: getYearHourAndDay time:" + timesamp);
        }
        return (yearHourDayStr + " " + am_pm + " " + getHourAndMin(timesamp));
    }

    public static boolean isSameday(long curr, long last) {
        Date date1 = new Date(curr);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date1);

        int Year1 = calendar.get(Calendar.YEAR);
        int Month1 = calendar.get(Calendar.MONTH);
        int Day1 = calendar.get(Calendar.DAY_OF_MONTH);

        Date date2 = new Date(last);
        calendar.setTime(date2);

        int Year2 = calendar.get(Calendar.YEAR);
        int Month2 = calendar.get(Calendar.MONTH);
        int Day2 = calendar.get(Calendar.DAY_OF_MONTH);

        return (Year1 == Year2)
                && (Month1 == Month2)
                && (Day1 == Day2);
    }
}