package org.auko.calendar;


import javax.swing.*;
import java.awt.*;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * @packageName: calendar
 * @className: Util
 * @Description: ������, ����ʵ��һЩ��ҵ���߼���ϵ����Ĺ���
 * @author: auko
 * @data 2019-11-18 13:39
 */
public class Util {

    /**
     * ������item ǰ��100�� ��200��
     *
     * @return ����������ַ�������
     */
    public static String[] fillYearItems(int currentYear) {
        String[] rtn = new String[200];
        for (int i = currentYear - 100, j = 0; i < currentYear + 100; i++, j++) {
            rtn[j] = i + " ��";
        }
        return rtn;
    }

    /**
     * ����·� 12����
     *
     * @return ����������·��ַ�������
     */
    public static String[] fillMOnthItems() {
        String[] rtn = new String[12];
        for (int i = 0; i < 12; i++) {
            rtn[i] = (i + 1) + " ��";
        }
        return rtn;
    }

    /**
     * �޸İ�ť��ʽ
     *
     * @param btn ��ť
     */
    public static void initBtnStyle(JButton btn) {
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setForeground(Color.WHITE);
    }

    /**
     * �ж������ǲ��ǽ���
     *
     * @param calendar ����
     * @return ���ز���ֵ
     */
    public static boolean isToday(Calendar calendar) {
        if (calendar == null) return false;
        Calendar today = Calendar.getInstance(TimeZone.getTimeZone("GMT+08:00"));
        return today.get(Calendar.YEAR) == calendar.get(Calendar.YEAR) &&
                today.get(Calendar.MONTH) == calendar.get(Calendar.MONTH) &&
                today.get(Calendar.DATE) == calendar.get(Calendar.DATE);
    }

    /**
     * �ж�����Ԫ���Ƿ�Ϊѡ�е�����
     *
     * @param calendar     ����
     * @param selectedDate ѡ�е�����
     * @return ���ز���ֵ
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
     * ͨ���ַ����ܷ�ת��Ϊ�������ж��Ƿ�Ϊ�����е�Ԫ��
     *
     * @param str ������ַ���
     * @return �����Ƿ�Ϊ����Ԫ�صĲ���ֵ
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
