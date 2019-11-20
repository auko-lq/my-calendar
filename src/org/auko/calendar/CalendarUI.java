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
    // 日历中选中的日期, 默认为当前date
    protected static Date selectedDate = new Date();

    private int[] dayOfWeek = new int[]{7, 1, 2, 3, 4, 5, 6};
    private String[] dayOfWeekZn = new String[]{"一", "二", "三", "四", "五", "六", "七"};

    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd EEE");
    private SimpleDateFormat fileFormat = new SimpleDateFormat("yyyy-MM-dd");

    private ThreadPoolExecutor thread = new ThreadPoolExecutor(10, 15, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(5));

    // toDoList contentBox
    private Box contentBox = Box.createVerticalBox();

    private HashMap<String, List<String>> toDoList = new HashMap<>();
    private File file;

    /**
     * 无参构造方法
     */
    public CalendarUI() {
        // conf最终指向根目录, 和src同级
        File conf = new File(System.getProperty("user.dir")).getParentFile();
        Properties props = new Properties();
        InputStream is = null;
        try {
            is = new FileInputStream(new File(conf.getAbsolutePath(), "config.properties"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        if(is == null){
            throw new RuntimeException("找不到properties文件");
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
     * 可定义初始位置
     *
     * @param initX 初始位置x
     * @param initY 初始位置y
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
     * 初始化
     */
    private void init() {
        // file 最终指向根目录 和src同级目录
        file = new File(new File(System.getProperty("user.dir")).getParentFile().getAbsolutePath(), "toDoList.txt");
        if(file.exists()){
            readToDoFile();
        }

        frame = new JFrame("calendar");

        JLayeredPane layeredPane = new JLayeredPane();
        layeredPane.setPreferredSize(new Dimension(width, height));

        // 初始化位置与大小
        frame.setBounds(initX, initY, width, height);
        frame.add(layeredPane);

        // 取消标题栏
        frame.setUndecorated(true);


        // 设置透明度
        frame.setOpacity(0.95f);

        // 添加背景层
        bgPanel = new JPanel();
        bgPanel.setBounds(0, 0, width, height);
        bgPanel.setBackground(bgColor);
        layeredPane.add(bgPanel, new Integer(0));

        // 添加前景层
        frontPanel = new JPanel();
        frontPanel.setBounds(0, 0, width, height);
        frontPanel.setOpaque(false);
        layeredPane.add(frontPanel, new Integer(1));

        // 前景层垂直盒子
        Box box = Box.createVerticalBox();

        // 持续更新当前时间
        updateTime();

        // 顶部面板, 包括时间和日期
        topPanel = new JPanel();
        topPanel.setOpaque(false);
        topPanel.setLayout(new BorderLayout());
        box.add(topPanel);
        showTopTime();

        box.add(Box.createVerticalStrut(20));

        // 中部面板, 包括日历本体和操作日历的控件
        centerPanel = new JPanel();
        centerPanel.setOpaque(false);
        box.add(centerPanel);
        showCalendar();

        box.add(Box.createVerticalStrut(20));

        // 底部面板, 包括代办事项与添加提醒
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
                // 关闭程序时把toDoList写入文件
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
     * 读取toDo文件
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
     * @Description: 更新日期
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
     * @Description: 显示顶部时间栏
     */
    private void showTopTime() {
        thread.execute(() -> {
            JLabel topTime = new JLabel(timeFormat.format(nowTime));
            JLabel topDate = new JLabel(dateFormat.format(nowTime));
            topTime.setForeground(Color.WHITE);
            topTime.setFont(new Font("宋体", Font.PLAIN, 65));
            topDate.setForeground(Color.WHITE);
            topDate.setFont(new Font("宋体", Font.PLAIN, 30));
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
     * 显示日历
     */
    private void showCalendar() {
        thread.execute(() -> {
            Box centerBox = Box.createVerticalBox();

            JPanel changeCalendarPanel = new JPanel();
            JPanel calendarTopPanel = new JPanel();
            JPanel calendarItemPanel = new JPanel();

            // 将面板透明, 露出背景层
            calendarItemPanel.setOpaque(false);
            changeCalendarPanel.setOpaque(false);

            changeCalendarPanel.setLayout(new BorderLayout());
            calendarTopPanel.setLayout(new GridLayout(1, 7));
            calendarItemPanel.setLayout(new GridLayout(6, 7));

            showChangeCalendar(changeCalendarPanel, calendarTopPanel, calendarItemPanel);

            showCalendarTop(calendarTopPanel);

            showCalendarItem(new Date(), calendarItemPanel);

            // 装入盒子, 方便添加间距
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
     * 显示操作日历的组件
     */
    private void showChangeCalendar(JPanel changeCalendarPanel, JPanel centerPanel, JPanel calendarItemPanel) {

        Box leftAndRight = Box.createHorizontalBox();
        JButton left = new JButton("<");
        JButton right = new JButton(">");

        Util.initBtnStyle(left);
        left.setFont(new Font("宋体", Font.PLAIN, 30));

        Util.initBtnStyle(right);
        right.setFont(new Font("宋体", Font.PLAIN, 30));

        leftAndRight.add(Box.createHorizontalStrut(20));
        leftAndRight.add(left);
        leftAndRight.add(Box.createHorizontalStrut(20));
        leftAndRight.add(right);
        changeCalendarPanel.add(leftAndRight, BorderLayout.WEST);

        Box selectCombo = Box.createHorizontalBox();
        JComboBox selectYear = new JComboBox(Util.fillYearItems(calendar.get(Calendar.YEAR)));
        JComboBox selectMonth = new JComboBox(Util.fillMOnthItems());
        selectYear.setSelectedItem(calendar.get(Calendar.YEAR) + " 年");
        selectMonth.setSelectedItem((calendar.get(Calendar.MONTH) + 1) + " 月");

        selectYear.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                // 把选项中的年份提取出来
                int selectedItem = Integer.parseInt(((String) e.getItem()).split(" ")[0]);
                calendar.set(Calendar.YEAR, selectedItem);
                showCalendarItem(calendar.getTime(), calendarItemPanel);
            }
        });

        selectMonth.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                // 把选项中的年份提取出来
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

            // 改变右边选择框的选择值, 选择框改变又会触发监听器来改变日历
            selectYear.setSelectedItem(calendar.get(Calendar.YEAR) + " 年");
            selectMonth.setSelectedItem((calendar.get(Calendar.MONTH) + 1) + " 月");
        });

        right.addActionListener(e -> {
            calendar.add(Calendar.MONTH, 1);

            // 改变右边选择框的选择值
            selectYear.setSelectedItem(calendar.get(Calendar.YEAR) + " 年");
            selectMonth.setSelectedItem((calendar.get(Calendar.MONTH) + 1) + " 月");
        });
    }

    /**
     * 显示日历上方的星期数
     */
    private void showCalendarTop(JPanel calendarTopPanel) {
        for (int i = 0; i < 7; i++) {
            JLabel weekDay = new JLabel(dayOfWeekZn[i], JLabel.CENTER);
            weekDay.setFont(new Font("宋体", Font.PLAIN, 30));
            calendarTopPanel.add(weekDay);
        }
    }

    /**
     * 显示日历item
     *
     * @param date   日历对应的日期
     * @param jPanel 日历所在的面板
     */
    private void showCalendarItem(Date date, JPanel jPanel) {
        // 先清除存在的日历item
        jPanel.removeAll();

        calendar.setTime(date);

        // 记录一下本月, 待会比较是否为本月
        int thisMonth = calendar.get(Calendar.MONTH);

        // 先从本月第一天的前6天开始遍历
        calendar.set(Calendar.DATE, -6);

        // 如果遍历到星期一, 则准备开始全部日历的输出
        while (dayOfWeek[calendar.get(Calendar.DAY_OF_WEEK) - 1] != 1) {
            calendar.add(Calendar.DATE, 1);
        }

        // 显示7*6 = 42天的日历
        for (int dayNum = 0; dayNum < 42; dayNum++) {
            JButton calendarItem = new JButton(String.valueOf(calendar.get(Calendar.DATE)));
            jPanel.add(calendarItem);

            // 修改按钮样式
            Util.initBtnStyle(calendarItem);
            calendarItem.setPreferredSize(new Dimension(70, 30));
            calendarItem.setFont(new Font("宋体", Font.BOLD, 23));

            if (Util.isToday(calendar)) {
                // 如果是今天, 修改样式
                calendarItem.setForeground(Color.BLACK);
                calendarItem.setContentAreaFilled(true);
                calendarItem.setBackground(Color.white);
            }

            if (Util.isSelectedDate(calendar, selectedDate)) {
                // 如果为选中的日历元素, 修改样式
                calendarItem.requestFocus();
            }


            if (calendar.get(Calendar.MONTH) != thisMonth) {
                // 不是本月, 改一下样式来区分
                calendarItem.setForeground(Color.LIGHT_GRAY);
                calendarItem.setFont(new Font("宋体", Font.BOLD, 18));
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
        // 显示完日历后, 把日历调回到初始位置, 方便翻日历
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
        JLabel title = new JLabel("待办事项");
        title.setFont(new Font("宋体", Font.PLAIN, 40));
        title.setForeground(Color.white);
        toDoTop.add(title, BorderLayout.WEST);

        JButton addBtn = new JButton("+");
        Util.initBtnStyle(addBtn);
        addBtn.setFont(new Font("宋体", Font.PLAIN, 50));
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
        // 需不需要将内容以label形式打印
        boolean needPrint = toDoContent != null && !"".equals(toDoContent);

        Box itemBox = Box.createVerticalBox();

        JPanel itemPanel = new JPanel();
        itemPanel.setOpaque(false);
        itemPanel.setLayout(new BorderLayout());

        JLabel prefix = new JLabel("- ");
        prefix.setForeground(Color.WHITE);
        prefix.setFont(new Font("宋体", Font.BOLD, 27));
        itemPanel.add(prefix, BorderLayout.WEST);

        JTextField toDoInput = new JTextField(" 请输入待办事项");

        JLabel toDoLabel = new JLabel();
        if (needPrint) {
            itemPanel.add(addToDo(toDoLabel, toDoContent), BorderLayout.CENTER);
        } else {
            toDoInput.setBorder(null);
            toDoInput.requestFocusInWindow();
            toDoInput.addFocusListener(new FocusListener() {
                @Override
                public void focusGained(FocusEvent e) {
                    if (toDoInput.getText().equals(" 请输入待办事项")) toDoInput.setText("");
                }

                @Override
                public void focusLost(FocusEvent e) {
                    if (toDoInput.getText().equals("")) toDoInput.setText(" 请输入待办事项");
                }
            });
            itemPanel.add(toDoInput, BorderLayout.CENTER);
        }

        // 用于组装右边两个按钮
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
                // 有内容才去移除
                removeToDo(toDoLabel.getText());
            }
            contentBox.remove(itemBox);
        });

        sureBtn.addActionListener(e -> {
            String content = toDoInput.getText();
            if (!"".equals(content) && !" 请输入待办事项".equals(content)) {
                // 不为空或修改了, 把输入框的内容显示出来, 并存入文件

                itemPanel.remove(toDoInput);
                btnBox.remove(sureBtn);
                itemPanel.add(addToDo(toDoLabel, content), BorderLayout.CENTER);

                keepToMap(fileFormat.format(selectedDate), content);
            }
        });
    }

    /**
     * 新增toDo
     *
     * @param toDo    文本组件
     * @param content 内容
     * @return 返回填成了内容的文本组件
     */
    private JLabel addToDo(JLabel toDo, String content) {
        toDo.setText(content);
        toDo.setFont(new Font("宋体", Font.BOLD, 25));
        toDo.setForeground(Color.WHITE);
        return toDo;
    }

    /**
     * 移除toDo
     *
     * @param content toDo的内容
     */
    private void removeToDo(String content) {
        List<String> list = toDoList.get(fileFormat.format(selectedDate));
        list.remove(content);
        toDoList.put(fileFormat.format(selectedDate), list);
    }


    /**
     * 保存到map
     *
     * @param toDoContent toDo内容
     */
    private void keepToMap(String date, String toDoContent) {
        List<String> list;
        if (toDoList.get(date) != null) {
            // 所选日期中有toDo, 获取list, 补充后再put进去
            list = toDoList.get(date);
        } else {
            // 所选日期没有toDo, 创建一个list put进去
            list = new ArrayList<>();
        }
        list.add(toDoContent);
        toDoList.put(date, list);
    }
}
