package org.auko.calendar;

import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ThreadPoolExecutor;

import javax.management.RuntimeErrorException;
import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;

/**
 * @packageName: calendar
 * @className: CalendarUI
 * @Description:
 * @author: auko
 * @data 2019-10-25 8:42
 */
public class CalendarUI {

    public static void main(String argv[]) {
        CalendarUI calendar = new CalendarUI();
    }

    private int height;
    private int width;

    private int initX;
    private int initY;

    private Color bgColor = new Color(27, 88, 167);

    private Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT+08:00"));
    private Date nowTime = new Date();
    // ������ѡ�е�����, Ĭ��Ϊ��ǰdate
    protected static Date selectedDate = new Date();

    private int[] dayOfWeek = new int[]{7, 1, 2, 3, 4, 5, 6};
    private String[] dayOfWeekZn = new String[]{"һ", "��", "��", "��", "��", "��", "��"};

    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd EEE");
    private SimpleDateFormat fileFormat = new SimpleDateFormat("yyyy-MM-dd");

    private ThreadPoolExecutor thread = new ThreadPoolExecutor(10, 15, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(5));

    // toDoList contentBox
    private Box contentBox = Box.createVerticalBox();

    private HashMap<String, List<String>> toDoList = new HashMap<>();
    private File file;

    /**
     * �޲ι��췽��
     */
    public CalendarUI() {
        // conf����ָ���Ŀ¼, ��srcͬ��
        File conf = new File(System.getProperty("user.dir")).getParentFile();
        Properties props = new Properties();
        InputStream is = null;
        try {
            is = new FileInputStream(new File(conf.getAbsolutePath(), "config.properties"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        if(is == null){
            throw new RuntimeException("�Ҳ���properties�ļ�");
        }else{
            try {
                props.load(is);
                initX = Integer.parseInt(props.getProperty("initX"));
                initY = Integer.parseInt(props.getProperty("initY"));
                width = Integer.parseInt(props.getProperty("width"));
                height = Integer.parseInt(props.getProperty("height"));
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        init();
    }

    /**
     * �ɶ����ʼλ��
     *
     * @param initX ��ʼλ��x
     * @param initY ��ʼλ��y
     */
    public CalendarUI(int initX, int initY, int width, int height) {
        this.initX = initX;
        this.initY = initY;
        this.width = width;
        this.height = height;
        init();
    }


    public JFrame frame = null;
    public JPanel bgPanel = null;
    public JPanel frontPanel = null;
    public JPanel topPanel = null;
    public JPanel centerPanel = null;
    public JPanel bottomPanel = null;


    /**
     * ��ʼ��
     */
    private void init() {
        // file ����ָ���Ŀ¼ ��srcͬ��Ŀ¼
        file = new File(new File(System.getProperty("user.dir")).getParentFile().getAbsolutePath(), "toDoList.txt");
        if(file.exists()){
            readToDoFile();
        }

        frame = new JFrame("calendar");

        JLayeredPane layeredPane = new JLayeredPane();
        layeredPane.setPreferredSize(new Dimension(width, height));

        // ��ʼ��λ�����С
        frame.setBounds(initX, initY, width, height);
        frame.add(layeredPane);

        // ȡ��������
        frame.setUndecorated(true);


        // ����͸����
        frame.setOpacity(0.95f);

        // ��ӱ�����
        bgPanel = new JPanel();
        bgPanel.setBounds(0, 0, width, height);
        bgPanel.setBackground(bgColor);
        layeredPane.add(bgPanel, new Integer(0));

        // ���ǰ����
        frontPanel = new JPanel();
        frontPanel.setBounds(0, 0, width, height);
        frontPanel.setOpaque(false);
        layeredPane.add(frontPanel, new Integer(1));

        // ǰ���㴹ֱ����
        Box box = Box.createVerticalBox();

        // �������µ�ǰʱ��
        updateTime();

        // �������, ����ʱ�������
        topPanel = new JPanel();
        topPanel.setOpaque(false);
        topPanel.setLayout(new BorderLayout());
        box.add(topPanel);
        showTopTime();

        box.add(Box.createVerticalStrut(20));

        // �в����, ������������Ͳ��������Ŀؼ�
        centerPanel = new JPanel();
        centerPanel.setOpaque(false);
        box.add(centerPanel);
        showCalendar();

        box.add(Box.createVerticalStrut(20));

        // �ײ����, ���������������������
        bottomPanel = new JPanel();
        bottomPanel.setOpaque(false);
        bottomPanel.setLayout(new BorderLayout());
        box.add(bottomPanel);
        showToDo();

        frontPanel.add(box);

        frame.setResizable(false);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                super.windowClosing(e);
                // �رճ���ʱ��toDoListд���ļ�
                file.getParentFile().mkdirs();
                try {
                    file.createNewFile();
                    PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file)));
                    for (Map.Entry<String, List<String>> entry : toDoList.entrySet()) {
                        for (String toDo : entry.getValue()) {
                            printWriter.println(entry.getKey() + "=" + toDo);
                        }
                    }
                    printWriter.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });
        frame.setVisible(true);
    }

    /**
     * ��ȡtoDo�ļ�
     */
    private void readToDoFile() {
        try (
                BufferedReader bufferedReader = new BufferedReader(new FileReader(file))
        ) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                String date = line.substring(0, line.indexOf("="));
                String content = line.substring(line.indexOf("=") + 1);
                keepToMap(date, content);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * @Author: auko on 2019-11-11 10:09
     * @Description: ��������
     */
    private void updateTime() {
        thread.execute(() -> {
            while (true) {
                this.nowTime = new Date();
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
    }


    /**
     * @Author: auko on 2019-10-25 9:22
     * @Description: ��ʾ����ʱ����
     */
    private void showTopTime() {
        thread.execute(() -> {
            JLabel topTime = new JLabel(timeFormat.format(nowTime));
            JLabel topDate = new JLabel(dateFormat.format(nowTime));
            topTime.setForeground(Color.WHITE);
            topTime.setFont(new Font("����", Font.PLAIN, 65));
            topDate.setForeground(Color.WHITE);
            topDate.setFont(new Font("����", Font.PLAIN, 30));
            Box topBox = Box.createVerticalBox();
            topBox.add(Box.createVerticalStrut(15));
            topBox.add(topTime);
            topBox.add(topDate);
            Box topBoxHorizontal = Box.createHorizontalBox();
            topBoxHorizontal.add(Box.createHorizontalStrut(30));
            topBoxHorizontal.add(topBox);

            topPanel.add(topBoxHorizontal, BorderLayout.WEST);

            while (true) {
                topTime.setText(timeFormat.format(nowTime));
                topDate.setText(dateFormat.format(nowTime));
            }
        });
    }


    /**
     * ��ʾ����
     */
    private void showCalendar() {
        thread.execute(() -> {
            Box centerBox = Box.createVerticalBox();

            JPanel changeCalendarPanel = new JPanel();
            JPanel calendarTopPanel = new JPanel();
            JPanel calendarItemPanel = new JPanel();

            // �����͸��, ¶��������
            calendarItemPanel.setOpaque(false);
            changeCalendarPanel.setOpaque(false);

            changeCalendarPanel.setLayout(new BorderLayout());
            calendarTopPanel.setLayout(new GridLayout(1, 7));
            calendarItemPanel.setLayout(new GridLayout(6, 7));

            showChangeCalendar(changeCalendarPanel, calendarTopPanel, calendarItemPanel);

            showCalendarTop(calendarTopPanel);

            showCalendarItem(new Date(), calendarItemPanel);

            // װ�����, ������Ӽ��
            centerBox.add(changeCalendarPanel);
            centerBox.add(Box.createVerticalStrut(20));
            centerBox.add(calendarTopPanel);
            centerBox.add(Box.createVerticalStrut(20));
            centerBox.add(calendarItemPanel);
            centerBox.add(Box.createVerticalStrut(20));
            centerPanel.add(centerBox);
        });
    }

    /**
     * ��ʾ�������������
     */
    private void showChangeCalendar(JPanel changeCalendarPanel, JPanel centerPanel, JPanel calendarItemPanel) {

        Box leftAndRight = Box.createHorizontalBox();
        JButton left = new JButton("<");
        JButton right = new JButton(">");

        Util.initBtnStyle(left);
        left.setFont(new Font("����", Font.PLAIN, 30));

        Util.initBtnStyle(right);
        right.setFont(new Font("����", Font.PLAIN, 30));

        leftAndRight.add(Box.createHorizontalStrut(20));
        leftAndRight.add(left);
        leftAndRight.add(Box.createHorizontalStrut(20));
        leftAndRight.add(right);
        changeCalendarPanel.add(leftAndRight, BorderLayout.WEST);

        Box selectCombo = Box.createHorizontalBox();
        JComboBox selectYear = new JComboBox(Util.fillYearItems(calendar.get(Calendar.YEAR)));
        JComboBox selectMonth = new JComboBox(Util.fillMOnthItems());
        selectYear.setSelectedItem(calendar.get(Calendar.YEAR) + " ��");
        selectMonth.setSelectedItem((calendar.get(Calendar.MONTH) + 1) + " ��");

        selectYear.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                // ��ѡ���е������ȡ����
                int selectedItem = Integer.parseInt(((String) e.getItem()).split(" ")[0]);
                calendar.set(Calendar.YEAR, selectedItem);
                showCalendarItem(calendar.getTime(), calendarItemPanel);
            }
        });

        selectMonth.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                // ��ѡ���е������ȡ����
                int selectedItem = Integer.parseInt(((String) e.getItem()).split(" ")[0]);
                calendar.set(Calendar.MONTH, selectedItem - 1);
                showCalendarItem(calendar.getTime(), calendarItemPanel);
            }
        });

        selectCombo.add(selectYear);
        selectCombo.add(Box.createHorizontalStrut(20));
        selectCombo.add(selectMonth);
        selectCombo.add(Box.createHorizontalStrut(20));

        changeCalendarPanel.add(selectCombo, BorderLayout.EAST);

        left.addMouseListener(new MyMouseLisener(left));
        right.addMouseListener(new MyMouseLisener(right));

        left.addActionListener(e -> {
            calendar.add(Calendar.MONTH, -1);

            // �ı��ұ�ѡ����ѡ��ֵ, ѡ���ı��ֻᴥ�����������ı�����
            selectYear.setSelectedItem(calendar.get(Calendar.YEAR) + " ��");
            selectMonth.setSelectedItem((calendar.get(Calendar.MONTH) + 1) + " ��");
        });

        right.addActionListener(e -> {
            calendar.add(Calendar.MONTH, 1);

            // �ı��ұ�ѡ����ѡ��ֵ
            selectYear.setSelectedItem(calendar.get(Calendar.YEAR) + " ��");
            selectMonth.setSelectedItem((calendar.get(Calendar.MONTH) + 1) + " ��");
        });
    }

    /**
     * ��ʾ�����Ϸ���������
     */
    private void showCalendarTop(JPanel calendarTopPanel) {
        for (int i = 0; i < 7; i++) {
            JLabel weekDay = new JLabel(dayOfWeekZn[i], JLabel.CENTER);
            weekDay.setFont(new Font("����", Font.PLAIN, 30));
            calendarTopPanel.add(weekDay);
        }
    }

    /**
     * ��ʾ����item
     *
     * @param date   ������Ӧ������
     * @param jPanel �������ڵ����
     */
    private void showCalendarItem(Date date, JPanel jPanel) {
        // ��������ڵ�����item
        jPanel.removeAll();

        calendar.setTime(date);

        // ��¼һ�±���, ����Ƚ��Ƿ�Ϊ����
        int thisMonth = calendar.get(Calendar.MONTH);

        // �ȴӱ��µ�һ���ǰ6�쿪ʼ����
        calendar.set(Calendar.DATE, -6);

        // �������������һ, ��׼����ʼȫ�����������
        while (dayOfWeek[calendar.get(Calendar.DAY_OF_WEEK) - 1] != 1) {
            calendar.add(Calendar.DATE, 1);
        }

        // ��ʾ7*6 = 42�������
        for (int dayNum = 0; dayNum < 42; dayNum++) {
            JButton calendarItem = new JButton(String.valueOf(calendar.get(Calendar.DATE)));
            jPanel.add(calendarItem);

            // �޸İ�ť��ʽ
            Util.initBtnStyle(calendarItem);
            calendarItem.setPreferredSize(new Dimension(70, 30));
            calendarItem.setFont(new Font("����", Font.BOLD, 23));

            if (Util.isToday(calendar)) {
                // ����ǽ���, �޸���ʽ
                calendarItem.setForeground(Color.BLACK);
                calendarItem.setContentAreaFilled(true);
                calendarItem.setBackground(Color.white);
            }

            if (Util.isSelectedDate(calendar, selectedDate)) {
                // ���Ϊѡ�е�����Ԫ��, �޸���ʽ
                calendarItem.requestFocus();
            }


            if (calendar.get(Calendar.MONTH) != thisMonth) {
                // ���Ǳ���, ��һ����ʽ������
                calendarItem.setForeground(Color.LIGHT_GRAY);
                calendarItem.setFont(new Font("����", Font.BOLD, 18));
            }

            calendarItem.addFocusListener(new MyFocusListener(calendarItem, calendar));
            calendarItem.addMouseListener(new MyMouseLisener(calendarItem));

            Date tempDate = calendar.getTime();
            calendarItem.addActionListener(e -> {
                selectedDate = tempDate;
                showToDoList();
            });

            calendar.add(Calendar.DATE, 1);
        }
        // ��ʾ��������, ���������ص���ʼλ��, ���㷭����
        calendar.setTime(date);
    }


    private void showToDo() {
        thread.execute(() -> {
            Box bottomBox = Box.createVerticalBox();

            JPanel toDoTopPanel = new JPanel();
            toDoTopPanel.setOpaque(false);
            toDoTopPanel.setLayout(new BorderLayout());

            showToDoTop(toDoTopPanel);

            bottomBox.add(Box.createVerticalStrut(20));

            JPanel toDoListPanel = new JPanel();
            toDoListPanel.setOpaque(false);
            toDoListPanel.setLayout(new BorderLayout());

            toDoListPanel.add(contentBox, BorderLayout.CENTER);
            showToDoList();

            bottomBox.add(toDoTopPanel);
            bottomBox.add(Box.createVerticalStrut(20));
            bottomBox.add(toDoListPanel);

            bottomPanel.add(bottomBox, BorderLayout.CENTER);
        });
    }


    private void showToDoTop(JPanel toDoTop) {
        JLabel title = new JLabel("��������");
        title.setFont(new Font("����", Font.PLAIN, 40));
        title.setForeground(Color.white);
        toDoTop.add(title, BorderLayout.WEST);

        JButton addBtn = new JButton("+");
        Util.initBtnStyle(addBtn);
        addBtn.setFont(new Font("����", Font.PLAIN, 50));
        toDoTop.add(addBtn, BorderLayout.EAST);

        addBtn.addMouseListener(new MyMouseLisener(addBtn));

        addBtn.addActionListener(e -> createToDoItem(null));
    }


    private void showToDoList() {
        contentBox.removeAll();

        List<String> list;
        try {
            list = toDoList.get(fileFormat.format(selectedDate));
            if(list == null) list = new ArrayList<>();
        } catch (NullPointerException ex) {
            list = new ArrayList<>();
        }
        for (String content : list) {
            createToDoItem(content);
        }
    }

    private void createToDoItem(String toDoContent) {
        // �費��Ҫ��������label��ʽ��ӡ
        boolean needPrint = toDoContent != null && !"".equals(toDoContent);

        Box itemBox = Box.createVerticalBox();

        JPanel itemPanel = new JPanel();
        itemPanel.setOpaque(false);
        itemPanel.setLayout(new BorderLayout());

        JLabel prefix = new JLabel("- ");
        prefix.setForeground(Color.WHITE);
        prefix.setFont(new Font("����", Font.BOLD, 27));
        itemPanel.add(prefix, BorderLayout.WEST);

        JTextField toDoInput = new JTextField(" �������������");

        JLabel toDoLabel = new JLabel();
        if (needPrint) {
            itemPanel.add(addToDo(toDoLabel, toDoContent), BorderLayout.CENTER);
        } else {
            toDoInput.setBorder(null);
            toDoInput.requestFocusInWindow();
            toDoInput.addFocusListener(new FocusListener() {
                @Override
                public void focusGained(FocusEvent e) {
                    if (toDoInput.getText().equals(" �������������")) toDoInput.setText("");
                }

                @Override
                public void focusLost(FocusEvent e) {
                    if (toDoInput.getText().equals("")) toDoInput.setText(" �������������");
                }
            });
            itemPanel.add(toDoInput, BorderLayout.CENTER);
        }

        // ������װ�ұ�������ť
        Box btnBox = Box.createHorizontalBox();

        btnBox.add(Box.createHorizontalStrut(10));

        JButton sureBtn = new JButton("ok");
        if (!needPrint) {
            Util.initBtnStyle(sureBtn);
            sureBtn.addMouseListener(new MyMouseLisener(sureBtn));
            btnBox.add(sureBtn);
        }

        btnBox.add(Box.createHorizontalStrut(10));

        JButton cancelBtn = new JButton("cancel");
        Util.initBtnStyle(cancelBtn);
        cancelBtn.addMouseListener(new MyMouseLisener(cancelBtn));
        btnBox.add(cancelBtn);

        itemPanel.add(btnBox, BorderLayout.EAST);

        itemBox.add(itemPanel);
        itemBox.add(Box.createVerticalStrut(10));
        contentBox.add(itemBox);

        cancelBtn.addActionListener(e -> {
            if (needPrint) {
                // �����ݲ�ȥ�Ƴ�
                removeToDo(toDoLabel.getText());
            }
            contentBox.remove(itemBox);
        });

        sureBtn.addActionListener(e -> {
            String content = toDoInput.getText();
            if (!"".equals(content) && !" �������������".equals(content)) {
                // ��Ϊ�ջ��޸���, ��������������ʾ����, �������ļ�

                itemPanel.remove(toDoInput);
                btnBox.remove(sureBtn);
                itemPanel.add(addToDo(toDoLabel, content), BorderLayout.CENTER);

                keepToMap(fileFormat.format(selectedDate), content);
            }
        });
    }

    /**
     * ����toDo
     *
     * @param toDo    �ı����
     * @param content ����
     * @return ������������ݵ��ı����
     */
    private JLabel addToDo(JLabel toDo, String content) {
        toDo.setText(content);
        toDo.setFont(new Font("����", Font.BOLD, 25));
        toDo.setForeground(Color.WHITE);
        return toDo;
    }

    /**
     * �Ƴ�toDo
     *
     * @param content toDo������
     */
    private void removeToDo(String content) {
        List<String> list = toDoList.get(fileFormat.format(selectedDate));
        list.remove(content);
        toDoList.put(fileFormat.format(selectedDate), list);
    }


    /**
     * ���浽map
     *
     * @param toDoContent toDo����
     */
    private void keepToMap(String date, String toDoContent) {
        List<String> list;
        if (toDoList.get(date) != null) {
            // ��ѡ��������toDo, ��ȡlist, �������put��ȥ
            list = toDoList.get(date);
        } else {
            // ��ѡ����û��toDo, ����һ��list put��ȥ
            list = new ArrayList<>();
        }
        list.add(toDoContent);
        toDoList.put(date, list);
    }
}
