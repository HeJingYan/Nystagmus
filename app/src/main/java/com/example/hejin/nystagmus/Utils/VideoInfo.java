package com.example.hejin.nystagmus.Utils;

/**
 * Created by HJY on 2017/9/12.
 */

public class VideoInfo implements Cloneable {
    private String name;
    private String date;
    private String periodTime;
    private String absolutePath;
    public VideoInfo(String name, String date, String periodTime, String absolutePath)
    {
        this.name=name;
        this.date=date;
        this.periodTime=periodTime;
        this.absolutePath=absolutePath;
    }
    public void setName(String name)
    {
        this.name=name;
    }
    public void setData(String data)
    {
        this.date=data;
    }
    public void setPeriodTime(String periodTime)
    {
        this.periodTime=periodTime;
    }
    public void setAbsolutePath(String absolutePath)
    {
        this.absolutePath=absolutePath;
    }
    public String getName()
    {
        return this.name;
    }
    public String getDate()
    {
        return this.date;
    }
    public String getPeriodTime()
    {
        return this.periodTime;
    }
    public String getAbsolutePath()
    {
        return this.absolutePath;
    }
    public VideoInfo clone() throws CloneNotSupportedException
    {
        //浅拷贝
        return (VideoInfo)super.clone();
    }
    public boolean equals(Object otherObject)
    {
        if(this==otherObject) return true;

        if(otherObject==null) return false;

        if(getClass()!=otherObject.getClass()) return false;

        VideoInfo other=(VideoInfo)otherObject;

        return this.name.equals(other.name)//必须对应着散列码相同
                &&this.date.equals(other.date)
                &&this.periodTime.equals(other.periodTime)
                &&this.absolutePath.equals(other.absolutePath);
    }
}
