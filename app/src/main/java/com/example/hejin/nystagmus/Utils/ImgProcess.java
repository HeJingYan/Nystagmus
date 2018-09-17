package com.example.hejin.nystagmus.Utils;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_core.CvContour;
import org.bytedeco.javacpp.opencv_core.CvMemStorage;
import org.bytedeco.javacpp.opencv_core.CvPoint;
import org.bytedeco.javacpp.opencv_core.CvRect;
import org.bytedeco.javacpp.opencv_core.CvScalar;
import org.bytedeco.javacpp.opencv_core.CvSeq;
import org.bytedeco.javacpp.opencv_core.IplImage;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Point;
import org.bytedeco.javacpp.opencv_core.Rect;
import org.bytedeco.javacpp.opencv_core.Scalar;
import org.bytedeco.javacpp.opencv_core.Size;
import org.bytedeco.javacpp.opencv_imgproc;

import java.util.Vector;

import static java.lang.Math.sqrt;
import static org.bytedeco.javacpp.opencv_imgproc.CV_CHAIN_APPROX_NONE;
import static org.bytedeco.javacpp.opencv_imgproc.CV_RETR_CCOMP;
import static org.bytedeco.javacpp.opencv_imgproc.HOUGH_GRADIENT;

/**
 * 与图像处理相关的函数
 * Created by HJY on 2017/7/17.
 */

public class ImgProcess {
    private double EyeRatio;

    private Mat eye=new Mat();

    private Size size=new Size(9,9);

    private Scalar blue=new Scalar(0,0,255,0);
    private Scalar green=new Scalar(0,255,0,0);
    private Scalar red=new Scalar(255,0,0,0);
    private CvScalar cvwhite=new CvScalar(255,255,255,0);
    private CvScalar cvblue=new CvScalar(0,0,255,0);
    private CvScalar cvgreen=new CvScalar(0,255,0,0);
    private CvScalar cvred=new CvScalar(255,0,0,0);
    private CvScalar cvblack=new CvScalar(0,0,0,0);
    private Mat OriginalEye=new Mat();//用于保存原始图像

    private Vector<Box> circles=new Vector<Box>();

    private IplImage eyeImage;

    /**
     * 默认构造函数
     */
    public ImgProcess()
    {}
    /**
     * 开始参数设置
     */
    public void Start(Mat eye, double eyeratio)
    {
        eye=CropImage(eye);
        opencv_core.flip(eye,eye,1);//水平翻转
        this.eye=new Mat(eye);
        EyeRatio=eyeratio;
        eyeImage=new IplImage(eye);
        //保存原始图像数据
        OriginalEye=new Mat(eye);
    }
    /**
     * 输出眼睛图像
     * @return 眼睛图像
     */
    public Mat Outeye()
    {
        return eye;
    }

    /**
     * 截取图像
     * @param image
     * @return
     */
    private Mat CropImage(Mat image)
    {
        Rect box = new Rect(image.cols()/4, image.rows()/5, image.cols()/2, image.rows()*3/5);
        return new Mat(image,box);
    }
    /**
     * 滤波、灰度化等处理，返回图像便于边缘检测
     * @param grayimg0 源图像
     * @return 灰度图像
     */
    private Mat GrayDetect(Mat grayimg0)
    {
        Mat grayimg =new Mat(grayimg0);

        opencv_imgproc.cvtColor(grayimg,grayimg, opencv_imgproc.COLOR_RGB2GRAY);//灰度化处理
        opencv_imgproc.medianBlur(grayimg,grayimg,9);//中值滤波
        //opencv_imgproc.blur(grayimg,grayimg,size);//均值滤波

        /*重构开运算，去除光斑*/
        Mat element_open= opencv_imgproc.getStructuringElement(opencv_imgproc.MORPH_ELLIPSE,new Size(15,15));//形态学开运算的内核
        opencv_imgproc.morphologyEx(grayimg,grayimg, opencv_imgproc.MORPH_OPEN,element_open);//开运算

        /*闭运算，去除睫毛*/
        Mat element_close= opencv_imgproc.getStructuringElement(opencv_imgproc.MORPH_ELLIPSE,new Size(9,9));//形态学闭运算的内核
        opencv_imgproc.morphologyEx(grayimg,grayimg, opencv_imgproc.MORPH_CLOSE,element_close);

        /*顶帽+低帽变换，将源图像加上低帽变换再减去顶帽变换，用以增强对比度*/
        Mat element_hot= opencv_imgproc.getStructuringElement(opencv_imgproc.MORPH_ELLIPSE,new Size(5,5));
        Mat topHat=new Mat();
        Mat bottomHat=new Mat();
        Mat tempHat=new Mat();
        opencv_imgproc.morphologyEx(grayimg,topHat, opencv_imgproc.MORPH_TOPHAT,element_hot);//顶帽运算
        opencv_imgproc.morphologyEx(grayimg,bottomHat, opencv_imgproc.MORPH_BLACKHAT,element_hot);//黑帽运算
        opencv_core.addWeighted(grayimg,1,bottomHat,1,0,tempHat);
        opencv_core.addWeighted(tempHat,1,topHat,-1,0,grayimg);

        Mat grayout=Binary(grayimg,Tool.RecognitionGrayValue);//阈值

        Mat grayout1=RemoveSmallRegion(grayout);//去除小面积
        return grayout1;
    }
    /**
     * 二值化处理
     * @param binaryimg 源图像
     * @param value 二值化阈值
     * @return 二值化图像
     */
    private Mat Binary(Mat binaryimg, int value)
    {
        Mat binaryout=new Mat();
        opencv_imgproc.threshold(binaryimg,binaryout,value,255, opencv_imgproc.THRESH_BINARY_INV);
        return binaryout;
    }
    /**
     * 去除小面积
     * @param src 源图像
     * @return 去除小面积后的图像
     */
    private Mat RemoveSmallRegion(Mat src)
    {
        Mat dst=new Mat();
        src.copyTo(dst);
        IplImage srcImage=new IplImage(dst);

        CvMemStorage storage= CvMemStorage.create();
        CvSeq cvContour=new CvSeq(null);

        CvRect rect;
        double tempArea;
        opencv_imgproc.cvFindContours(srcImage,storage,cvContour, Loader.sizeof(CvContour.class), opencv_imgproc.CV_RETR_CCOMP, opencv_imgproc.CV_CHAIN_APPROX_NONE);
        dst=new Mat();
        src.copyTo(dst);
        srcImage=new IplImage(dst);
        double maxArea=0;
        CvSeq tempContour=new CvSeq(cvContour);
        while (tempContour!=null&&!tempContour.isNull())
        {
            tempArea= opencv_imgproc.cvContourArea(tempContour);
            if(tempArea>maxArea)
            {
                maxArea=tempArea;
            }
            tempContour=tempContour.h_next();
        }
        int nCol=src.cols();
        int nRow=src.rows();
        while (cvContour!=null&&!cvContour.isNull())
        {
            rect= opencv_imgproc.cvBoundingRect(cvContour);
            if(rect.x()==1||rect.x()==nCol)
            {
                CvPoint point=new CvPoint(rect.x()+rect.width()/2,rect.y()+rect.height()/2);
                opencv_imgproc.cvFloodFill(srcImage,point,cvblack);
            }
            tempArea= opencv_imgproc.cvContourArea(cvContour);
            if(tempArea<maxArea)
            {
                CvPoint point=new CvPoint(rect.x()+rect.width()/2,rect.y()+rect.height()/2);
                opencv_imgproc.cvFloodFill(srcImage,point,cvblack);
            }
            cvContour=cvContour.h_next();
        }
        return dst;
    }
    /**
     * 寻找图像质心
     * @param src 源图像
     * @return 质心坐标
     */
    private Box GravityCenter(Mat src)
    {
        Box center=new Box(0,0,0);
        IplImage srcImg=new IplImage(src);
        opencv_imgproc.CvMoments moments=new opencv_imgproc.CvMoments();
        opencv_imgproc.cvMoments(srcImg,moments);
        double m00= opencv_imgproc.cvGetSpatialMoment(moments,0,0);
        if(m00==0)
        {
            return center;
        }
        double m10= opencv_imgproc.cvGetSpatialMoment(moments,1,0);
        double m01= opencv_imgproc.cvGetSpatialMoment(moments,0,1);
        center.setX(m10/m00);
        center.setY(m01/m00);
        return center;
    }
    /**
     * 寻找瞳孔圆心
     * @param src 源图像
     * @param points 源图像轮廓
     * @return 瞳孔圆心
     */
    private Box CircleFit(Mat src, Vector<Point> points)
    {
        Box center=GravityCenter(src);//获取质心坐标
        Point top=new Point(0,src.cols());
        Point bottom=new Point(0,0);
        Point right=new Point(0,0);
        Point left=new Point(src.cols(),0);
        long sum=points.size();
        int x,y;
        for(int i=0;i<sum;i++)
        {
            //取得四个顶点
            x=points.get(i).x();
            y=points.get(i).y();
            if(x<left.x())
            {
                //左侧
                left=points.get(i);
            }
            if(x>right.x())
            {
                right=points.get(i);
            }
            if(y<top.y())
            {
                top=points.get(i);
            }
            if(y>bottom.y())
            {
                bottom=points.get(i);
            }
        }
        double width=bottom.y()-top.y();
        double length=right.x()-left.x();
        if(length/width>1.15)
        {
            //眼皮遮挡眼球一部分
            double R=(right.x()-left.x()+(bottom.y()-left.y()))/3.0;
            double Y=(left.y()+bottom.y()-R)/2.0;
            center.setY(Y);
            center.setR(R);
        }
        else
        {
            //近似圆
            Box fitCenter=circleLeastFit(points);
            center.setR(fitCenter.getR());
        }
        return center;
    }
    /**
     * 二乘法拟合圆
     * @param points 待拟合点的集合
     * @return 拟合圆，包括圆心坐标和半径
     */
    private Box circleLeastFit(Vector<Point> points)
    {
        Box box=new Box(0.0d,0.0d,0.0d);
        long Sum=points.size();
        //如果少于三点，不能拟合圆，直接返回
        if(Sum<3)
        {
            return box;
        }
        int i;
        double X1 = 0;
        double Y1 = 0;
        double X2 = 0;
        double Y2 = 0;
        double X3 = 0;
        double Y3 = 0;
        double X1Y1 = 0;
        double X1Y2 = 0;
        double X2Y1 = 0;

        for (i = 0; i < Sum; ++i)
        {
            X1 += points.get(i).x();
            Y1 += points.get(i).y();
            X2 += points.get(i).x()*points.get(i).x();
            Y2 += points.get(i).y()*points.get(i).y();
            X3 += points.get(i).x()*points.get(i).x()*points.get(i).x();
            Y3 += points.get(i).y()*points.get(i).y()*points.get(i).y();
            X1Y1 += points.get(i).x()*points.get(i).y();
            X1Y2 += points.get(i).x()*points.get(i).y()*points.get(i).y();
            X2Y1 += points.get(i).x()*points.get(i).x()*points.get(i).y();
        }

        double C, D, E, G, H, N;
        double a, b, c;
        N = points.size();
        C = N*X2 - X1*X1;
        D = N*X1Y1 - X1*Y1;
        E = N*X3 + N*X1Y2 - (X2 + Y2)*X1;
        G = N*Y2 - Y1*Y1;
        H = N*X2Y1 + N*Y3 - (X2 + Y2)*Y1;
        a = (H*D - E*G) / (C*G - D*D);
        b = (H*C - E*D) / (D*D - G*C);
        c = -(a*X1 + b*Y1 + X2 + Y2) / N;

        box.setX(a/(-2));
        box.setY(b/(-2));
        box.setR(sqrt(a*a + b*b - 4 * c) / 2);
        return box;
    }
    /**
     * 霍夫检测圆，测试用，目前有bug
     * @param img 输入图像
     * @param minradius 最小半径
     * @param maxradius 最大半径
     * @return 圆心
     */
    private Point hough(org.bytedeco.javacpp.helper.opencv_core.CvArr img, double minradius, double maxradius)
    {
        //这段程序有待商榷，不一定对。等用到时再改吧。
        Point point=new Point();
        opencv_imgproc.cvHoughCircles(img,point,HOUGH_GRADIENT,minradius,maxradius);
        return point;
    }
    /**
     * 边缘检测
     * @param edgeimg 检测图像
     * @return 边缘图像
     */
    private Mat EdgeDetect(Mat edgeimg)
    {
        Mat edgeout=new Mat();
        opencv_imgproc.Canny(edgeimg,edgeout,100,250,3,false);
        return edgeout;
    }
    /**
     * 绘制源
     * @param circles 需要绘制的圆的集合
     * @param midImage 源图像
     */
    private void PlotC(Vector<Box> circles, IplImage midImage)
    {
        for(int i=0;i<circles.size();++i)
        {
            CvPoint center=new CvPoint((int) Math.round(circles.get(i).getX()),(int) Math.round(circles.get(i).getY()));
            int radius=(int)circles.get(i).getR();
            //opencv_imgproc.cvCircle(midImage,center,1,cvblue,-1,8,0);//画圆心
            //opencv_imgproc.cvCircle(midImage,center,radius,cvred,1,8,0);//画圆轮廓
            drawCross(midImage,center,cvwhite,1);//绘制十字光标
        }
    }
    /**
     * 绘制十字光标
     * @param img 源图像
     * @param point 十字点
     * @param color 颜色
     * @param thickness 线宽
     */
    private void drawCross(IplImage img, CvPoint point, CvScalar color, int thickness)
    {
        int heigth=img.height();
        int width=img.width();
        CvPoint above=new CvPoint(point.x(),0);
        CvPoint below=new CvPoint(point.x(),heigth);
        CvPoint left=new CvPoint(0,point.y());
        CvPoint right=new CvPoint(width,point.y());
        //绘制横线
        opencv_imgproc.cvLine(img,left,right,color,thickness,8,0);
        //绘制竖线
        opencv_imgproc.cvLine(img,above,below,color,thickness,8,0);
    }
    /**
     * 眼睛图像识别处理
     */
    public void Process()
    {
        double temparea;

        CvMemStorage storage= CvMemStorage.create();

        CvSeq cvContour=new CvSeq(null);
        CvSeq cvtempContour=new CvSeq(null);
        CvSeq cvContourKeep=new CvSeq(null);//用于绘制轮廓


        Mat grayImg=GrayDetect(eye);
        Mat edgeImg=EdgeDetect(grayImg);
        IplImage matImg=new IplImage(edgeImg);
        opencv_imgproc.cvFindContours(matImg,storage,cvContour, Loader.sizeof(CvContour.class), CV_RETR_CCOMP, CV_CHAIN_APPROX_NONE);


        if(!cvContour.isNull()&&cvContour.elem_size()>0)
        {
            //眼睛有轮廓
            double maxarea=0;//代表最大面积
            while (cvContour != null && !cvContour.isNull())
            {
                temparea= opencv_imgproc.cvContourArea(cvContour);

                if(temparea>maxarea)
                {
                    maxarea=temparea;
                    cvtempContour=cvContour;
                    continue;
                }
                cvContour=cvContour.h_next();
            }
            if(!cvtempContour.isNull())
            {
                cvContourKeep=new CvSeq(cvtempContour);
                Vector<Point> points=new Vector<Point>();
                for(int i=0;i<cvtempContour.total();++i)
                {
                    Point p=new Point(opencv_core.cvGetSeqElem(cvtempContour,i));
                    points.add(p);
                }
                //闭眼检测
                CvRect rect= opencv_imgproc.cvBoundingRect(cvtempContour);
                if((rect.width()/(float)rect.height())<EyeRatio&&(rect.height()/(float)rect.width())<EyeRatio&&rect.width()>0&&rect.height()>0)
                {
                    Box box=CircleFit(grayImg,points);//左眼拟合圆检测
                    if(box.getR()!=0)
                    {
                        //如果半径不为0
                        circles.add(box);
                    }
                }
            }
        }

        if(circles.size()>0)
        {
            PlotC(circles,eyeImage);//绘制左眼圆心和圆轮廓
        }

        //opencv_imgproc.cvDrawContours(eyeImage,cvContourKeep,cvgreen,cvgreen,1);//使用javacv绘制轮廓
        eye=new Mat(eyeImage);
    }
    public Iterable<Box> circles()
    {
        return circles;
    }
}
