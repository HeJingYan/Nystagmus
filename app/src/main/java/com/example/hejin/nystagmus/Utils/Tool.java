package com.example.hejin.nystagmus.Utils;

import android.os.Environment;

import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_imgproc;

import java.sql.Date;
import java.text.SimpleDateFormat;

/**
 * Created by HJY on 2017/7/17.
 */

public class Tool {
    public static final int ALL_EYE=5;//摄像头双眼都在
    public static final int NOT_LEYE=2;//摄像头没有左眼
    public static final int NOT_REYE=0;//摄像头没有右眼
    public static final int NOT_ALLEYE=8;//摄像头双眼都没有
    public static final int VEDIO_EYE=1;//本地视频双眼都在
    public static final int VEDIO_ONLY_EYE=6;//本地视频单眼

    public static String AddressLeftEye="http://192.168.43.231:8090/?action=stream?dummy=param.mjpg";//左眼网络地址
    public static String AddressRightEye="http://192.168.43.127:8090/?action=stream?dummy=param.mjpg";//右眼网络地址

    public static final String AddressLeftEyeDefault="http://192.168.43.231:8090/?action=stream?dummy=param.mjpg";//左眼网络地址
    public static final String AddressRightEyeDefault="http://192.168.43.127:8090/?action=stream?dummy=param.mjpg";//右眼网络地址

    public static final String RequestBingPic="http://guolin.tech/api/bing_pic";

    public static final int TimerSecondNum=50;//1s时间内定时器的间隔
    public static final int HighTidePeriodSecond=3;//最大眼震反应期时间,单位为s
    public static final double SPVMaxValue=0.5f;//SPV最大临界值，超过这个值即眼震眩晕异常
    public static final float SPVConversionRatio=10f;//在计算波形斜率时所用的换算比例



    public static int RecognitionGrayValue=55;
    public static final int RecognitionGrayValueDefault=55;

    public static final int VideoTransmitTestCode=6;

    public static final String StorageVideoPath=Environment.getExternalStorageDirectory().getAbsolutePath()+"/NystagmusMovies";
    public static final int StorageVideoWidth=160;
    public static final int StorageVideoHeigh=72;
    public static final int StorageVideoFPS=50;

    private static final SimpleDateFormat format=new SimpleDateFormat("yyyyMMddHHmmss");

    public static opencv_core.Mat MergeMat(opencv_core.Mat leftMat, opencv_core.Mat rightMat)
    {
        opencv_core.Size size=new opencv_core.Size(leftMat.cols()+rightMat.cols(),Max(leftMat.rows(),rightMat.rows()));
        opencv_core.Mat mergeMat=new opencv_core.Mat(size,opencv_core.CV_MAKE_TYPE(leftMat.depth(),3));
        opencv_core.Rect leftRect=new opencv_core.Rect(0,0,leftMat.cols(),leftMat.rows());
        opencv_core.Rect rightRect=new opencv_core.Rect(leftMat.cols(),0,rightMat.cols(),rightMat.rows());
        opencv_core.Mat out_leftMat=mergeMat.apply(leftRect);
        opencv_core.Mat out_rightMat=mergeMat.apply(rightRect);
        if(leftMat.type()==opencv_core.CV_8U)
        {
            opencv_imgproc.cvtColor(leftMat,out_leftMat,opencv_imgproc.CV_GRAY2BGR);
        }
        else
        {
            leftMat.copyTo(out_leftMat);
        }
        if(rightMat.type()==opencv_core.CV_8U)
        {
            opencv_imgproc.cvtColor(rightMat,out_rightMat,opencv_imgproc.CV_GRAY2BGR);
        }
        else
        {
            rightMat.copyTo(out_rightMat);
        }
        return mergeMat.clone();
    }

    public static int Max(int a,int b)
    {
        return a>b?a:b;
    }

    public static int Min(int a,int b)
    {
        return a>b?b:a;
    }

    public static String getStorageVideoPath()
    {
        Date date=new Date(System.currentTimeMillis());

        String timeNow=format.format(date);

        return StorageVideoPath+"/"+timeNow+".avi";
    }

    public static double distance(Box x,Box y)
    {
        //求两点之间的绝对距离
        return Math.sqrt(Math.pow(x.getX()-y.getX(),2)+Math.pow(x.getY()-y.getY(),2));
    }

}
