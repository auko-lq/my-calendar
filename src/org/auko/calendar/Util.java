package org.auko.calendar;


import javax.swing.*;
import java.awt.*;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * @packageName: calendar
 * @className: Util
 * @Description: 工具类, 用于实现一些与业务逻辑关系不大的功能
 * @author: auko
 * @data 2019-11-18 13:39
 */
public class Util {

    /**
     * 填充年份item 前后100年 共200年
     *
     * @return 返回填充后的字符串数组
     */
    public static String[] fillYearItems(int currentYear) {
        String[] rtn = new String[200];
        for (int i = currentYear - 100, j = 0; i < currentYear + 100; i++, j++) {
            rtn[j] = i + " 年";
        }
        return rtn;
    }

    /**
     * 填充月份 12个月
     *
     * @return 返回填充后的月份字符串数组
     */
    public static String[] fillMOnthItems() {
        String[] rtn = new String[12];
        for (int i = 0; i < 12; i++) {
            rtn[i] = (i + 1) + " 月";
        }
        return rtn;
    }

    /**
     * 修改按钮样式
     *
     * @param btn 按钮
     */
    public static void initBtnStyle(JButton btn) {
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setForeground(Color.WHITE);
    }

    /**
     * 判断日历是不是今天
     *
     * @param calendar 日历
     * @return 返回布尔值
     */
    public static boolean isToday(Calendar calendar) {
        if (calendar == null) return false;
        Calendar today = Calendar.getInstance(TimeZone.getTimeZone("GMT+08:00"));
        return today.get(Calendar.YEAR) == calendar.get(Calendar.YEAR) &&
                today.get(Calendar.MONTH) == calendar.get(Calendar.MONTH) &&
                today.get(Calendar.DATE) == calendar.get(Calendar.DATE);
    }

    /**
     * 判断日历元素是否为选中的日期
     *
     * @param calendar     日历
     * @param selectedDate 选中的日期
     * @return 返回布尔值
     */
    public static boolean isSelectedDate(Calendar calendar, Date selectedDate) {
        if (selectedDate == null) return false;
        Calendar selectedCalendar = Calendar.getInstance();
        selectedCalendar.setTime(selectedDate);
        return selectedCalendar.get(Calendar.YEAR) == calendar.get(Calendar.YEAR) &&
                selectedCalendar.get(Calendar.MONTH) == calendar.get(Calendar.MONTH) &&
                selectedCalendar.get(Calendar.DATE) == calendar.get(Calendar.DATE);
    }

    /**
     * 通过字符串能否转换为数字来判断是否为日历中的元素
     *
     * @param str 传入的字符串
     * @return 返回是否为日历元素的布尔值
     */
    public static boolean isCalendarItem(String str) {
        boolean rtn;
        try {
            Integer.parseInt(str);
            rtn = true;
        } catch (Exception e) {
            rtn = false;
        }
        return rtn;
    }
}
