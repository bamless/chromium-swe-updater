package com.bamless.chromiumsweupdater.utils;

import android.support.annotation.NonNull;

/**
 * Class that represent a build timestamp.
 */
public class BuildTime implements Comparable<BuildTime> {
    private int day = 1;
    private int month = 1;
    private int year = 1970;

    private int hour = 0;
    private int min = 0;
    private int sec = 0;

    protected BuildTime() {
    }

    public BuildTime(int day, int month, int year, int hour, int min, int sec) {
        setDay(day);
        setMonth(month);
        setYear(year);
        setHour(hour);
        setMin(min);
        setSec(sec);
    }

    public static BuildTime parseBuildTime(String buildtime) {
        BuildTime bt = new BuildTime();

        String[] datetime = buildtime.split(" ");
        if(datetime.length != 2)
            throw new IllegalArgumentException("Malformed input. the build time should " +
                    "be formatted this way: d/m/y hour:min:sec");

        String[] date = datetime[0].split("/");
        if(date.length != 3)
            throw new IllegalArgumentException("Error while parsing the date");

        bt.setDay(Integer.parseInt(date[0].charAt(0) == '0' ? date[0].charAt(1)+"" : date[0]));
        bt.setMonth(Integer.parseInt(date[1].charAt(0) == '0' ? date[1].charAt(1)+"" : date[1]));
        bt.setYear(Integer.parseInt(date[2].charAt(0) == '0' ? date[2].charAt(1)+"" : date[2]));

        String[] time = datetime[1].split(":");
        if(time.length != 3)
            throw new IllegalArgumentException("Error while parsing the time");

        bt.setHour(Integer.parseInt(time[0].charAt(0) == '0' ? time[0].charAt(1) + "" : time[0]));
        bt.setMin(Integer.parseInt(time[1].charAt(0) == '0' ? time[1].charAt(1) + "" : time[1]));
        bt.setSec(Integer.parseInt(time[2].charAt(0) == '0' ? time[2].charAt(1) + "" : time[2]));

        return bt;
    }

    public int getDay() {
        return day;
    }

    public void setDay(int day) {
        //check day
        this.day = day;
    }

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        if(month < 1 || month > 12)
            throw new IllegalArgumentException("month must be between 1 and 12");
        this.month = month;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        if(year < 1970)
            throw new IllegalArgumentException("Are you mad? There is no year prior to 1970");
        this.year = year;
    }

    public int getHour() {
        return hour;
    }

    public void setHour(int hour) {
        if(hour < 0 || hour > 23)
            throw new IllegalArgumentException("hour must be between 0 and 23");
        this.hour = hour;
    }

    public int getMin() {
        return min;
    }

    public void setMin(int min) {
        if(min < 0 || min > 59)
            throw new IllegalArgumentException("min must be between 0 and 59");
        this.min = min;
    }

    public int getSec() {
        return sec;
    }

    public void setSec(int sec) {
        if(sec < 0 || sec > 59)
            throw new IllegalArgumentException("sec must be between 0 and 59");
        this.sec = sec;
    }

    @Override
    public int compareTo(@NonNull BuildTime o) {
        int res = 0;
        if((res = year - o.year) != 0) return res;
        if((res = month - o.month) != 0) return res;
        if((res = day - o.day) != 0) return res;

        if((res = hour - o.hour) != 0) return res;
        if((res = min - o.min) != 0) return res;
        if((res = sec - o.sec) != 0) return res;
        return 0;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(day < 10 ? "0"+ day +"/" : day + "/");
        sb.append(month < 10 ? "0"+ month +"/" : month + "/");
        sb.append(year + " ");
        sb.append(hour < 10 ? "0"+ hour +":" : hour + ":");
        sb.append(min < 10 ? "0"+ min +":" : min + ":");
        sb.append(sec < 10 ? "0"+ sec : sec);
        return sb.toString();
    }

    public String dateToString() {
        StringBuilder sb = new StringBuilder();
        sb.append(day < 10 ? "0"+ day +"/" : day + "/");
        sb.append(month < 10 ? "0"+ month +"/" : month + "/");
        sb.append(year);
        return sb.toString();
    }

    public String hourToString() {
        StringBuilder sb = new StringBuilder();
        sb.append(hour < 10 ? "0"+ hour +":" : hour + ":");
        sb.append(min < 10 ? "0"+ min +":" : min + ":");
        sb.append(sec < 10 ? "0"+ sec : sec);
        return sb.toString();
    }
}
