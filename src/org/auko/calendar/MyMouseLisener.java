package org.auko.calendar;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

/**
 * @packageName: calendar
 * @className: MyMouseLisener
 * @Description: 自定义鼠标监听器
 * @author: auko
 * @data 2019-11-18 13:03
 */
public class MyMouseLisener implements MouseListener {

    JButton btn;

    MyMouseLisener(JButton btn) {
        this.btn = btn;
    }

    @Override
    public void mouseClicked(MouseEvent e) {

    }

    @Override
    public void mousePressed(MouseEvent e) {

    }

    @Override
    public void mouseReleased(MouseEvent e) {

    }

    @Override
    public void mouseEntered(MouseEvent e) {
        btn.setBorderPainted(true);
    }

    @Override
    public void mouseExited(MouseEvent e) { btn.setBorderPainted(false); }
}
