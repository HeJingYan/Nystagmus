package com.example.hejin.nystagmus.Utils;

import java.util.LinkedList;
import java.util.NoSuchElementException;

/**
 * 眼震中心点滤波
 * Created by HJY on 2017/12/26.
 */

public class PointFilter {
    private final int N;
    private LinkedList<Box> points;
    private int index;
    public PointFilter(int N)
    {
        points=new LinkedList<>();
        index=0;
        this.N=N;
    }
    public PointFilter()
    {
        this(5);
    }
    public void add(Box point)
    {
        points.addLast(point);
    }
    public Box get() throws NoSuchElementException
    {
        if(!isGet())
        {
            throw new NoSuchElementException("No elements");
        }
        if(points.size()>=6)
        {
            points.removeFirst();
        }
        float sumX=0f;
        float sumY=0f;
        float sumR=0f;

        for(Box box:points)
        {
            sumX+=box.getX();
            sumY+=box.getY();
            sumR+=box.getR();
        }

        return new Box(sumX/points.size(),sumY/points.size(),sumR/points.size());
    }
    public boolean isGet()
    {
        return points.size()>0;
    }

}
