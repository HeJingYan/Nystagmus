package com.example.hejin.nystagmus;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.hejin.nystagmus.Utils.Box;
import com.example.hejin.nystagmus.Utils.Calculate;
import com.example.hejin.nystagmus.Utils.GetPath;
import com.example.hejin.nystagmus.Utils.ImgProcess;
import com.example.hejin.nystagmus.Utils.L;
import com.example.hejin.nystagmus.Utils.PointFilter;
import com.example.hejin.nystagmus.Utils.T;
import com.example.hejin.nystagmus.Utils.Tool;
import com.getbase.floatingactionbutton.FloatingActionsMenu;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import org.bytedeco.javacpp.indexer.UByteIndexer;
import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Rect;
import org.bytedeco.javacpp.opencv_videoio;
import org.bytedeco.javacv.AndroidFrameConverter;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.OpenCVFrameConverter;

import java.io.File;
import java.lang.ref.WeakReference;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static com.example.hejin.nystagmus.Utils.Calculate.MergeRealtimeAndMax;
import static com.example.hejin.nystagmus.Utils.Calculate.getPeriod;
import static org.bytedeco.javacpp.opencv_core.CV_32FC1;
import static org.bytedeco.javacpp.opencv_core.cvCloneImage;
import static org.bytedeco.javacpp.opencv_core.cvCreateImage;
import static org.bytedeco.javacpp.opencv_core.cvCreateMat;
import static org.bytedeco.javacpp.opencv_core.cvGetSize;
import static org.bytedeco.javacpp.opencv_core.cvReleaseImage;
import static org.bytedeco.javacpp.opencv_core.cvReleaseMat;
import static org.bytedeco.javacpp.opencv_core.cvScalarAll;
import static org.bytedeco.javacpp.opencv_imgproc.CV_INTER_CUBIC;
import static org.bytedeco.javacpp.opencv_imgproc.CV_WARP_FILL_OUTLIERS;
import static org.bytedeco.javacpp.opencv_imgproc.cv2DRotationMatrix;
import static org.bytedeco.javacpp.opencv_imgproc.cvWarpAffine;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private ImageView imageView_leye;//右眼显示窗口
    private ImageView imageView_reye;//左眼显示窗口
    private DrawerLayout mDrawerLayout;//侧滑栏
    private static final int OPEN_VIDEO=1;//打开本地视频的码号，用于Intent活动传输
    private static final int OPEN_CAMERA=2;
    private static final int Storage_RequestCode=1;//存储权限申请码
    //private FFmpegFrameRecorder recorder;//用于记录视频

    private LineChart chart_x;//X波形图
    private LineChart chart_y;//y波形图
    private LineChart chart_rotation;//旋转波形
    private int[] colors=new int[]{Color.rgb(255, 69, 0), Color.rgb(0, 128, 0)};//自定义颜色，第一种为橘黄色，第二种为纯绿色

    private SharedPreferences pref;//调用存储文件，用于保存一些设置的参数


    /*SPV相关*/
    private TextView LeyeXRealtimeAndMaxSPV;
    private TextView ReyeXRealtimeAndMaxSPV;
    private TextView LeyeYRealtimeAndMaxSPV;
    private TextView ReyeYRealtimeAndMaxSPV;
    private TextView LeyeHighperiod;//左眼最大反应期
    private TextView ReyeHighperiod;//右眼最大反应期
    private DecimalFormat df;//数据格式,double转string保留两位小数

    /*诊断相关*/
    private TextView DiagnosticResult;
    private TextView LeyeDirectionResult;//左眼眼震方向
    private TextView ReyeDirectionResult;//右眼眼震方向

    /*悬浮菜单按钮*/
    private FloatingActionsMenu menuChange;

    private VideoTask task_leye;//左眼任务
    private VideoTask task_reye;//右眼任务

    //图像保存队列
    private BlockingQueue<Mat> leyeImageQueue = new ArrayBlockingQueue<>(100);
    private BlockingQueue<Mat> reyeImageQueue = new ArrayBlockingQueue<>(100);
    private static volatile boolean isSave = false;
    private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
    private int saveFrameWidth = 160;
    private int saveFrameHeigh = 72;
    private int saveFPS = 50;

    @Override//活动启动时第一个启动的函数，用于定义并初始化部分参数
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        imageView_leye=(ImageView)findViewById(R.id.lefteye_view);
        imageView_reye=(ImageView)findViewById(R.id.righteye_view);
        mDrawerLayout=(DrawerLayout)findViewById(R.id.drawer_layout);

        LeyeXRealtimeAndMaxSPV=(TextView) findViewById(R.id.leyeXRealtimeAndMaxSPV);
        ReyeXRealtimeAndMaxSPV=(TextView) findViewById(R.id.reyeXRealtimeAndMaxSPV);
        LeyeYRealtimeAndMaxSPV=(TextView) findViewById(R.id.leyeYRealtimeAndMaxSPV);
        ReyeYRealtimeAndMaxSPV=(TextView) findViewById(R.id.reyeYRealtimeAndMaxSPV);
        LeyeHighperiod=(TextView) findViewById(R.id.leyeHighperiod);
        ReyeHighperiod=(TextView) findViewById(R.id.reyeHighperiod);
        DiagnosticResult=(TextView) findViewById(R.id.diagnosticResult);
        LeyeDirectionResult=(TextView) findViewById(R.id.leyeDirection);
        ReyeDirectionResult=(TextView) findViewById(R.id.reyeDirection);
        //MainContainer=(CoordinatorLayout) findViewById(R.id.main_container);

        /*初始化设置为0*/
        LeyeXRealtimeAndMaxSPV.setText("0/0");
        ReyeXRealtimeAndMaxSPV.setText("0/0");
        LeyeYRealtimeAndMaxSPV.setText("0/0");
        ReyeYRealtimeAndMaxSPV.setText("0/0");
        LeyeHighperiod.setText("0s");
        ReyeHighperiod.setText("0s");
        DiagnosticResult.setText(R.string.defalut);
        LeyeDirectionResult.setText(R.string.defalut);
        ReyeDirectionResult.setText(R.string.defalut);

        df= new DecimalFormat("##.#");//数据格式,float转string保留1位小数

        NavigationView navView=(NavigationView)findViewById(R.id.nav_view);
        Toolbar toolbar=(Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, mDrawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);//侧滑栏设置
        mDrawerLayout.setDrawerListener(toggle);
        toggle.syncState();

        if(Build.VERSION.SDK_INT>= Build.VERSION_CODES.LOLLIPOP)//状态栏设置
        {
            //大于安卓5.0即API21版本可用
            //导航栏颜色与状态栏统一
            View decorView = getWindow().getDecorView();
            int option = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;//如果想要隐藏导航栏，可以加上View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            decorView.setSystemUiVisibility(option);
            //getWindow().setNavigationBarColor(Color.TRANSPARENT);//设置导航栏背景为透明
            getWindow().setStatusBarColor(Color.TRANSPARENT);
            //getWindow().setNavigationBarColor(getResources().getColor(R.color.lightSteelBlue));
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);//这个界面保持常亮

        /*悬浮菜单按钮设置*/
        menuChange=(FloatingActionsMenu) findViewById(R.id.float_menu);
        ((com.getbase.floatingactionbutton.FloatingActionButton)findViewById(R.id.menu_openvideo)).setOnClickListener(this);
        ((com.getbase.floatingactionbutton.FloatingActionButton)findViewById(R.id.menu_opencamera)).setOnClickListener(this);
        ((com.getbase.floatingactionbutton.FloatingActionButton)findViewById(R.id.menu_startplay)).setOnClickListener(this);
        ((com.getbase.floatingactionbutton.FloatingActionButton)findViewById(R.id.menu_stopplay)).setOnClickListener(this);

        navView.setCheckedItem(R.id.nav_intro);//默认选中nav_intro菜单栏默认选中
        navView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener(){
            @Override
            public boolean onNavigationItemSelected(MenuItem item)
            {
                switch (item.getItemId())
                {
                    case R.id.nav_intro:
                    {
                        //打开介绍界面活动
                        Intent intent=new Intent(MainActivity.this,IntroduceActivity.class);
                        startActivity(intent);
                        break;
                    }
                    case R.id.nav_settings:
                    {
                        //打开软件设置界面
                        Intent intent=new Intent(MainActivity.this,SettingsActivity.class);
                        startActivity(intent);
                        break;
                    }
                    case R.id.nav_video:
                    {
                        //打开视频列表界面
                        Intent intent=new Intent(MainActivity.this,VideoActivity.class);
                        startActivityForResult(intent,Tool.VideoTransmitTestCode);
                        if(task_leye!=null&&task_leye.getStatus()== AsyncTask.Status.RUNNING)
                        {
                            task_leye.cancel(true);
                        }
                        if(task_reye!=null&&task_reye.getStatus()== AsyncTask.Status.RUNNING)
                        {
                            task_reye.cancel(true);
                        }

                        break;
                    }
                    default:
                        break;
                }
                return true;
            }
        });

        chart_x=(LineChart)findViewById(R.id.xchart);
        chart_y=(LineChart)findViewById(R.id.ychart);
        chart_rotation=(LineChart) findViewById(R.id.rotation_chart);

        initialChart(chart_x,"水平位置");//初始化波形图
        initialChart(chart_y,"垂直位置");//初始化波形图
        initialChart(chart_rotation,"旋转曲线");//初始化旋转曲线

        pref=getSharedPreferences("CameraAddress",MODE_PRIVATE);
        Tool.AddressLeftEye=pref.getString("LeftCameraAddress",Tool.AddressLeftEye);//保存左眼摄像头地址
        Tool.AddressRightEye=pref.getString("RightCameraAddress",Tool.AddressRightEye);//保存右眼摄像头地址
        Tool.RecognitionGrayValue=pref.getInt("GrayValue",Tool.RecognitionGrayValue);//保存灰度化阈值

        File file = new File(Tool.VideoStoragePath);
        if(!file.exists()||!file.mkdir())
        {
            T.showShort(this,"视频存储功能受限");
        }
        L.d("项目打开");
    }

    @Override//APP活动放入后台时启动
    public void onBackPressed()//此方法用来监听back键事件的
    {
        //拦截Back键，使App进入后台而不是关闭
        Intent launcherIntent=new Intent(Intent.ACTION_MAIN);
        launcherIntent.addCategory(Intent.CATEGORY_HOME);
        startActivity(launcherIntent);
    }

    @Override//悬浮按钮点击函数
    public void onClick(View v)
    {
        switch (v.getId())
        {
            case R.id.menu_openvideo://打开本地视频
            {
                //先申请存储权限 判断现在是否有存储权限 没有的话申请权限
                if(ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED)
                {
                    ActivityCompat.requestPermissions(MainActivity.this,new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE},Storage_RequestCode);
                }else//有权限直接打开视频
                {
                    //打开本地视频
                    openVideo();
                }
                menuChange.collapse();//关闭悬浮栏
                break;
            }
            case R.id.menu_opencamera://打开摄像头
            {
                openCamera();
                menuChange.collapse();//关闭悬浮栏
                break;
            }
            case R.id.menu_startplay://开始测试
            {
                startPlay();
                menuChange.collapse();//关闭悬浮栏
                break;
            }
            case R.id.menu_stopplay://停止测试
            {
                stopPlay();
                menuChange.collapse();//关闭悬浮栏
                break;
            }
            default:
            {
                break;
            }
        }
    }

    @Override//申请权限
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
    {
        switch (requestCode)
        {
            case Storage_RequestCode://存储权限
                if(grantResults.length>0&&grantResults[0]== PackageManager.PERMISSION_GRANTED)
                {
                    /*申请权限后的事情*/
                    openVideo();
                }
                else
                {
                    T.showShort(this,"拒绝权限将无法使用程序");
                    finish();
                }
                break;
            default:
                break;
        }
    }

    private void openCamera()//打开摄像头
    {
        //判断上次处理过程是否结束 没结束时结束
        if(task_leye!=null&&task_leye.getStatus()== AsyncTask.Status.RUNNING)
        {
            task_leye.cancel(true);
        }
        if(task_reye!=null&&task_reye.getStatus()== AsyncTask.Status.RUNNING)
        {
            task_reye.cancel(true);
        }

        task_leye=new VideoTask(this,true);//注意不是本地视频
        task_reye=new VideoTask(this,false);

        //开始执行
        task_leye.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,Tool.AddressLeftEye);
        task_reye.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,Tool.AddressRightEye);
    }

    private void openVideo()//打开视频
    {
        if(task_leye!=null&&task_leye.getStatus()== AsyncTask.Status.RUNNING)
        {
            task_leye.cancel(true);
        }
        if(task_reye!=null&&task_reye.getStatus()== AsyncTask.Status.RUNNING)
        {
            task_reye.cancel(true);
        }
        //打开本地的视频资源 选取视频
        Intent intent=new Intent("android.intent.action.GET_CONTENT");
        intent.setType("video/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);//和GET_CONTENT一起用
        startActivityForResult(intent,OPEN_VIDEO);
    }

    private void startPlay()//开始测试
    {
        //只有双眼视频同时打开才可以保存
        isSave = false;
        if(task_leye!=null&&task_leye.getStatus()== AsyncTask.Status.RUNNING)
        {
            task_leye.isTest=true;
            isSave = true;
        }

        if(task_reye!=null&&task_reye.getStatus()== AsyncTask.Status.RUNNING)
        {
            task_reye.isTest=true;
        }
        else
        {
            isSave = false;
        }

        new Thread(new saveVideo(this)).start();
    }

    private void stopPlay()//停止测试
    {
        if(task_leye!=null&&task_leye.getStatus()== AsyncTask.Status.RUNNING)
        {
            task_leye.cancel(true);
        }
        if(task_reye!=null&&task_reye.getStatus()== AsyncTask.Status.RUNNING)
        {
            task_reye.cancel(true);
        }
    }

    private class saveVideo implements Runnable{
        private MainActivity context;
        public saveVideo(MainActivity context)
        {
            this.context=context;
        }
        @Override
        public void run(){
            String date=dateFormat.format(new Date());
            
            String savePath = Tool.VideoStoragePath + "/" + date + ".avi";
            opencv_videoio.VideoWriter videoWriter = new opencv_videoio.VideoWriter();
            videoWriter.open(savePath,opencv_videoio.CV_FOURCC((byte)'M',(byte)'J',(byte)'P',(byte)'G'),saveFPS,new opencv_core.Size(saveFrameWidth,saveFrameHeigh),true);
            try
            {
                while (isSave)
                {
                    Mat leye=leyeImageQueue.poll(1, TimeUnit.SECONDS);
                    Mat reye=reyeImageQueue.poll(1, TimeUnit.SECONDS);
                    if(leye==null||reye==null||leye.isNull()||reye.isNull()||leye.rows()==0||reye.rows()==0)
                    {
                        continue;
                    }
                    Mat merge=merge(leye,reye);
                    if(!videoWriter.isOpened())
                    {
                        break;
                    }
                    if(merge==null||merge.isNull()||merge.cols()!=saveFrameWidth||merge.rows()!=saveFrameHeigh)
                    {
                        continue;
                    }

                    videoWriter.write(merge);
                }
            }
            catch (InterruptedException e)
            {
                isSave=false;
                e.printStackTrace();
            }
            finally {
                videoWriter.release();
                File file=new File(savePath);
                if(file.exists())
                {
                    Intent intent=new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                    intent.setData(Uri.fromFile(file));
                    context.sendBroadcast(intent);
                    context.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            T.showShort(context,"视频保存成功");
                        }
                    });

                }
                else
                {
                    context.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            T.showShort(context,"视频保存失败");
                        }
                    });
                }
            }
        }

        //合成图像
        private Mat merge(Mat leye,Mat reye)
        {
            int totalCols=leye.cols()+reye.cols();
            int rows=Math.max(leye.rows(),reye.rows());
            Mat dst=new Mat(rows,totalCols,leye.type());
            Mat submat=dst.colRange(0,leye.cols());
            leye.copyTo(submat);
            submat=dst.colRange(leye.cols(),totalCols);
            reye.copyTo(submat);
            return dst;
        }
    }

    private void initialChart(LineChart chart,String label)//初始化波形图
    {
        Description description=new Description();
        description.setText(label);
        chart.setDescription(description);//增加描述

        chart.setDrawGridBackground(false);//不绘制背景颜色
        chart.setTouchEnabled(true);//可点击
        chart.setDragEnabled(true);//可拖拽
        chart.setScaleEnabled(true);//可缩放
        chart.setPinchZoom(false);//如果设置为false,那么x轴,y轴可以分别放大

        XAxis xAxis=chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);//设置X坐标轴在底部，默认在顶部

        chart.getAxisLeft().setDrawGridLines(false);//不绘制网格线
        chart.getAxisRight().setDrawGridLines(false);//不绘制网格线

        /*绘制两条曲线*/
        ArrayList<ILineDataSet> dataSets = new ArrayList<ILineDataSet>();
        for(int i=0;i<2;++i)
        {
            ArrayList<Entry> values=new ArrayList<>();
            values.add(new Entry(0,0));//初始设置为(0,0)坐标

            LineDataSet set=new LineDataSet(values,i==0?"左眼":"右眼");
            set.setMode(set.getMode()==LineDataSet.Mode.CUBIC_BEZIER?LineDataSet.Mode.LINEAR:LineDataSet.Mode.CUBIC_BEZIER);//设置为平滑曲线
            set.setDrawCircles(false);//取消显示坐标点圆圈
            set.setDrawValues(false);//取消显示坐标值
            set.setCubicIntensity(0.15f);//设置曲线曲率
            set.setLineWidth(2f);//设置线的宽度
            set.setColor(colors[i]);//设置线的颜色
            dataSets.add(set);
        }
        LineData data=new LineData(dataSets);
        chart.setData(data);

    }

    @Override
    protected void onActivityResult(int requestCode,int resultCode,Intent data)
    {
        String VideoPath="";
        boolean isTransmit=false;
        switch (requestCode)
        {

            case Tool.VideoTransmitTestCode: //长按视频列表 可以选择处理视频
            {
                if(resultCode==RESULT_OK)
                {
                    Bundle bundle=data.getExtras();
                    VideoPath=bundle.getString("VideoPath");
                    isTransmit=true;
                    mDrawerLayout.closeDrawers();//关闭侧滑栏
                }
            }
            case OPEN_VIDEO: //选择打开视频
            {
                if(resultCode==RESULT_OK)
                {
                    if(!isTransmit)
                    {
                        VideoPath= GetPath.getPath(this,data.getData());
                    }
                    //视频文件地址为：/storage/emulated/0/test.mp4
                    //视频文件必须为mjpeg编码的video

                    //定义新建初始化
                    task_leye=new VideoTask(this,true,true);//注意是本地视频
                    task_reye=new VideoTask(this,false,true);
                    //开始执行
                    task_leye.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,VideoPath);
                    task_reye.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,VideoPath);
                    task_leye.isTest=true;
                    task_reye.isTest=true;

                    //isSave = true;//本地视频保存
                    //new Thread(new saveVideo(this)).start();


                }
                break;
            }
            default:
                break;
        }
    }

    //视频任务
    private class VideoTask extends AsyncTask<String,Object,String> {
        //用于表示眼睛
        private boolean eye;//true为左眼，false为右眼
        private String eyeInfo;
        //用于格式转换
        private AndroidFrameConverter bitmapConverter = new AndroidFrameConverter();//Frame转bitmap
        private OpenCVFrameConverter.ToIplImage matConverter = new OpenCVFrameConverter.ToIplImage();//Mat转Frame
        //用于显示
        private Mat mat_display = null;
        private Frame frame_display = null;
        private Bitmap bitmap_display = null;
        //视频源
        private String cameraAddress;
        private FFmpegFrameGrabber capture;
        //活动引用
        private final WeakReference<MainActivity> mActivity;
        private MainActivity activity;
        //是否为本地视频
        private boolean isLocalVideo;
        //用于图像接收处理
        private Frame frame;
        private Mat frameMat,frameMat1,frameMat2,frameMat4;
        private Mat eyeMat;
        //视频相关参数
        private int frameNum = 0;//帧数
        private double rate = 0;//帧速率
        private int secondTime = 0;//测试时间（秒）
        //是否开始测试
        private boolean isTest = false;
        //滤波
        private PointFilter filter = new PointFilter();
        //与上一帧做比较
        private Box preEyeBox;
        //初始圆心坐标值
        private boolean isEyeCenter = false;
        private Box eyeCenter = new Box();
        //瞳孔圆心数据计算
        private Calculate calculate = new Calculate();//保证计算过程在同一个线程内，即在UI主线程内。

        //构造函数，用于定义调用的格式
        public VideoTask(MainActivity activity, boolean eye, boolean isLocalVideo) {
            this.eye = eye;
            this.mActivity = new WeakReference<>(activity);
            this.activity = this.mActivity.get();
            this.eyeInfo = eye ? "左眼" : "右眼";
            this.isLocalVideo = isLocalVideo;
        }

        //构造函数
        public VideoTask(MainActivity activity, boolean eye) {
            this(activity, eye, false);
        }

        //onPreExecute()方法在主线程UI Thread当中执行，在执行异步任务之前的时候执行
        //通常在这个方法里做一些UI控件的初始化的操作，这里做波形初始化等操作
        @Override
        protected void onPreExecute() {
            //参数初始化
            this.isTest = false;
            this.frameNum = 0;
            this.secondTime = 0;
            this.isEyeCenter = false;
            this.preEyeBox = null;
            //界面初始化
            clearEntey(chart_x);
            clearEntey(chart_y);
            clearEntey(chart_rotation);
            LeyeXRealtimeAndMaxSPV.setText("0/0");
            ReyeXRealtimeAndMaxSPV.setText("0/0");
            LeyeYRealtimeAndMaxSPV.setText("0/0");
            ReyeYRealtimeAndMaxSPV.setText("0/0");
            LeyeHighperiod.setText("0s");
            ReyeHighperiod.setText("0s");
            DiagnosticResult.setText(R.string.defalut);
            LeyeDirectionResult.setText(R.string.defalut);
            ReyeDirectionResult.setText(R.string.defalut);
            DiagnosticResult.setTextColor(MainActivity.this.getResources().getColor(R.color.black));
        }

        //此方法在主线程中执行，在doInBackground方法执行完成以后此方法会在UI Thread中被调用
        //onPostExecute 当异步任务执行完之后，将结果返回给这个方法，将返回结果显示在UI控件上
        @Override
        protected void onPostExecute(String s) {
            stopVideo();
        }

        //此方法在主线程中执行，当任务被取消后执行
        @Override
        protected void onCancelled(String s) {
            stopVideo();
        }

        private void stopVideo()//视频结束流程
        {
            this.isTest = false;
            isSave = false;
            //诊断结果
            boolean diagnosticResult = calculate.judgeDiagnosis();//诊断结果
            String preResultStr = DiagnosticResult.getText().toString();
            boolean preResult = preResultStr.equals(this.activity.getResources().getString(R.string.abnormal));

            //为了展示所加 2018/08/27
            if (this.isLocalVideo) {
                //本地视频的话显示不正常
                DiagnosticResult.setText(R.string.abnormal);
                DiagnosticResult.setTextColor(this.activity.getResources().getColor(R.color.red));
            } else {
                //在线视频的话就显示正常
                DiagnosticResult.setText(R.string.normal);
                DiagnosticResult.setTextColor(this.activity.getResources().getColor(R.color.black));
            }

            /*
            if(diagnosticResult&&!preResult)
            {
                //如果诊断结果正常
                DiagnosticResult.setText(R.string.normal);
                DiagnosticResult.setTextColor(this.activity.getResources().getColor(R.color.black));
            }
            else
            {
                //如果诊断结果不正常
                DiagnosticResult.setText(R.string.abnormal);
                DiagnosticResult.setTextColor(this.activity.getResources().getColor(R.color.red));
            }
            */
            //快相方向
            if (calculate.judegeEye()) {
                boolean eyeDir = calculate.judgeFastPhase();//眼睛快相方向
                if (eye) {
                    //左眼
                    LeyeDirectionResult.setText(eyeDir ? R.string.left : R.string.right);
                } else {
                    //右眼
                    ReyeDirectionResult.setText(eyeDir ? R.string.left : R.string.right);
                }
            }
            if (isLocalVideo) {
                //在本地视频情况下，释放视频源
                try {
                    this.capture.release();
                } catch (FrameGrabber.Exception e) {
                    L.d("释放本地资源");
                }

            }
            T.showShort(this.activity, "视频播放结束");
        }

        //此方法在子线程中执行，在onPreExecute()方法执行后马上执行，用于执行异步任务,注意这里的params就是AsyncTask的第一个参数类型。
        //在此方法中可以通过调用publicProgress方法来更新任务进度，publicProgress会调用onProgressUpdate方法。
        @Override
        protected String doInBackground(String... strings) {
            this.cameraAddress = strings[0];
            this.capture = new FFmpegFrameGrabber(cameraAddress);
            try {
                capture.start();
                this.rate = capture.getFrameRate();
            } catch (FrameGrabber.Exception e) {
                return "摄像头链接失败";
            }
            while (true) {
                if (isCancelled()) {
                    //任务被取消掉
                    break;
                }
                try {
                    frame = null;
                    frame = capture.grabFrame();//获取视频

                    if (frame == null) {
                        return "视频源中止";
                    }
                } catch (FrameGrabber.Exception e) {
                    return "视频源中止";
                }

                Mat eyeImage = new Mat();
                if (isLocalVideo && eye)
                {
                    //如果是本地视频的左眼
                    Mat mat = matConverter.convertToMat(frame);
                    Rect reye_box = new Rect(0, 0, mat.cols() / 2, mat.rows());
                    frameMat = new Mat(mat, reye_box);
                }
                else if (isLocalVideo && !eye)
                {
                    //如果是本地视频的右眼
                    Mat mat = matConverter.convertToMat(frame);
                    Rect leye_box = new Rect(mat.cols() / 2, 0, mat.cols() / 2 , mat.rows());
                    frameMat = new Mat(mat, leye_box);
                }
                else
                {

                    if (eye) {
                        //网络左眼视频
                        frameMat = matConverter.convertToMat(frame);
                        //frameMat = rotate(frameMat, 0);
                        if(frameMat==null)
                        {
                            continue;
                        }
                        frameMat1 = cropImage(frameMat);//截取矩形
                        opencv_core.flip(frameMat1, frameMat, 1);//水平翻转

                    } else {
                        //网络右眼视频
                        frameMat = matConverter.convertToMat(frame);
                        //frameMat = rotate(frameMat, 0);
                        if(frameMat==null)
                        {
                            continue;
                        }
                        frameMat4 = cropImage(frameMat);//截取矩形
                        opencv_core.flip(frameMat4, frameMat, 1);//水平翻转
                    }

                }

                if (isTest) {
                    this.frameNum++;
                    eyeImage=new Mat(frameMat.clone());//保存截图之后的图像
                }

                ImgProcess pro = new ImgProcess();
                pro.Start(frameMat, 1.8);
                pro.Process();
                eyeMat = pro.Outeye();
                float relativeRotation = 0;//圆心相对旋转坐标
                float relativeX = 0;//圆心相对X坐标
                float relativeY = 0;//圆心相对Y坐标
                boolean isCenter = false;//代表这帧图像是否存在圆心
                String xSPV = null;//x轴SPV值
                String ySPV = null;//y轴SPV值
                String period = null;//时间区间
                boolean isSPV = false;//代表是否有SPV更新
                if (isTest) {
                    //开始测试后进行波形分析
                    for (Box box : pro.circles()) {
                        isCenter = true;
                        //先滤波处理
                        filter.add(box);
                        box = filter.get();
                        //圆心坐标更新
                        if (preEyeBox == null) {
                            preEyeBox = box;
                        }
                        if (Tool.distance(box, preEyeBox) > (box.getR() + preEyeBox.getR() / 1.5) && (Math.abs(box.getR() - preEyeBox.getR()) > box.getR() / 2.0)) {
                            continue;
                        }
                        //坐标中心
                        if (!isEyeCenter) {
                            isEyeCenter = true;
                            eyeCenter.setX(box.getX());
                            eyeCenter.setY(box.getY());
                        } else {
                            //后续相对坐标是基于第一帧位置
                            double temp = Math.atan((box.getY() - preEyeBox.getY()) / (box.getX() - preEyeBox.getX()));
                            if (Double.isNaN(temp)) {
                                temp = 0;
                            }
                            relativeRotation = (float) temp;
                            relativeX = (float) (box.getX() - eyeCenter.getX());
                            relativeY = (float) (box.getY() - eyeCenter.getY());
                            //添加参数
                            calculate.addEyeX(relativeX);
                            calculate.addEyeY(relativeY);
                        }
                        preEyeBox = box;
                    }
                    if ((this.frameNum % this.rate == 0) && (this.frameNum != 0)) {
                        //进行计算
                        isSPV = true;
                        this.secondTime++;
                        calculate.processEyeX(secondTime);
                        calculate.processEyeY(secondTime);
                        double realSPVX = calculate.getRealTimeSPVX(secondTime);
                        double maxSPVX = calculate.getMaxSPVX();
                        double realSPVY = calculate.getRealTimeSPVY(secondTime);
                        double maxSPVY = calculate.getMaxSPVY();
                        int maxSecond = calculate.getHighTidePeriod();
                        period = getPeriod(maxSecond);
                        xSPV = MergeRealtimeAndMax(df.format(realSPVX), df.format(maxSPVX));
                        ySPV = MergeRealtimeAndMax(df.format(realSPVY), df.format(maxSPVY));
                    }
                }
                publishProgress(eyeMat, isCenter, relativeRotation, relativeX, relativeY, isSPV, xSPV, ySPV, period,eyeImage);
            }
            return "视频播放结束";
        }

        //此方法在主线程中执行，values的类型就是AsyncTask传入的第二个参数类型
        @Override
        protected void onProgressUpdate(Object... values) {
            if (isCancelled()) {
                return;
            }
            //眼睛图像显示
            mat_display = (Mat) values[0];
            if (mat_display == null) {
                return;
            }
            frame_display = matConverter.convert(mat_display);
            bitmap_display = bitmapConverter.convert(frame_display);
            if (eye) {
                this.activity.imageView_leye.setImageBitmap(bitmap_display);
            } else {
                this.activity.imageView_reye.setImageBitmap(bitmap_display);
            }
            //波形图绘制
            boolean isCenter = (Boolean) values[1];
            int flag = eye ? 0 : 1;
            if (this.isTest && isCenter) {
                //存在圆心的话，绘制波形图
                float relativeRotation = (float) values[2];
                float relativeX = (float) values[3];
                float relativeY = (float) values[4];
                addEntey(chart_rotation, frameNum / (float) rate, relativeRotation, flag);
                addEntey(chart_x, frameNum / (float) rate, relativeX, flag);
                addEntey(chart_y, frameNum / (float) rate, relativeY, flag);
                //眼震参数计算
                //calculate.addEyeX(relativeX);
                //calculate.addEyeY(relativeY);
            }
            //眼震参数更新
            boolean isSPV = (Boolean) values[5];
            if (this.isTest && isSPV) {
                //可以更新眼震参数
                String xSPV = (String) values[6];
                String ySPV = (String) values[7];
                String period = (String) values[8];
                if (eye) {
                    //左眼
                    LeyeXRealtimeAndMaxSPV.setText(xSPV);
                    LeyeYRealtimeAndMaxSPV.setText(ySPV);
                    LeyeHighperiod.setText(period);
                } else {
                    //右眼
                    ReyeXRealtimeAndMaxSPV.setText(xSPV);
                    ReyeYRealtimeAndMaxSPV.setText(ySPV);
                    ReyeHighperiod.setText(period);
                }
            }

            if(this.isTest&&isSave&&values[9]!=null)
            {
                Mat eyeImage=new Mat((Mat)values[9]);
                if(eyeImage.isNull()||eyeImage.cols()==0)
                {
                    return;
                }

                if(eye==true)
                {
                    leyeImageQueue.offer(eyeImage);
                }

                if (eye==false)
                {
                    reyeImageQueue.offer(eyeImage);
                }


            }
        }

        //清除波形
        private void clearEntey(LineChart chart) {
            LineData oldData = chart.getData();
            oldData.clearValues();
            ArrayList<ILineDataSet> dataSets = new ArrayList<ILineDataSet>();
            for (int i = 0; i < 2; ++i) {
                ArrayList<Entry> values = new ArrayList<>();
                values.add(new Entry(0, 0));//初始设置为(0,0)坐标

                LineDataSet set = new LineDataSet(values, i == 0 ? "左眼" : "右眼");
                set.setMode(set.getMode() == LineDataSet.Mode.CUBIC_BEZIER ? LineDataSet.Mode.LINEAR : LineDataSet.Mode.CUBIC_BEZIER);//设置为平滑曲线
                set.setDrawCircles(false);//取消显示坐标点圆圈
                set.setDrawValues(false);//取消显示坐标值
                set.setCubicIntensity(0.15f);//设置曲线曲率
                set.setLineWidth(2f);//设置线的宽度
                set.setColor(colors[i]);//设置线的颜色
                dataSets.add(set);
            }
            LineData data = new LineData(dataSets);
            chart.setData(data);
            chart.notifyDataSetChanged();
        }

        //绘制波形
        private void addEntey(LineChart add_chart, float add_x, float add_y, int add_flag) {
            //一定保证运行在UI主线程下
            //flag:0 左眼; flag:1 右眼
            LineData data = add_chart.getData();
            Entry entry = new Entry(add_x, add_y);
            data.addEntry(entry, add_flag);
            add_chart.notifyDataSetChanged();
            add_chart.invalidate();

            //波形图自动往左移动，不堆在一起
            //设置在曲线图中显示的最大数量
            add_chart.setVisibleXRangeMaximum(10);
            //移到某个位置
            add_chart.moveViewToX(data.getEntryCount() - 5);
        }

        //2018/09/29 增加图像旋转功能
        private Mat rotate(Mat image, double angle) {
            try {
                opencv_core.IplImage Image = new opencv_core.IplImage(image);
                opencv_core.IplImage copy = cvCloneImage(Image);
                opencv_core.IplImage rotatedImage = cvCreateImage(cvGetSize(copy), copy.depth(), copy.nChannels());
                //Define Rotational Matrix
                opencv_core.CvMat mapMatrix = cvCreateMat(2, 3, CV_32FC1);
                //Define Mid Point
                opencv_core.CvPoint2D32f centerPoint = new opencv_core.CvPoint2D32f();
                centerPoint.x(copy.width() / 2);
                centerPoint.y(copy.height() / 2);
                //Get Rotational Matrix
                cv2DRotationMatrix(centerPoint, angle, 1.0, mapMatrix);
                //Rotate the Image
                cvWarpAffine(copy, rotatedImage, mapMatrix, CV_INTER_CUBIC + CV_WARP_FILL_OUTLIERS, cvScalarAll(170));
                cvReleaseImage(copy);
                cvReleaseMat(mapMatrix);
                Mat rotateImage = new Mat(rotatedImage);
                return rotateImage;
            } catch (RuntimeException e) {
                e.printStackTrace();
                return null;
            } catch (OutOfMemoryError e) {
                return null;
            }
        }


        /**
         * 截取图像c
         * @param image
         * @return
         */
        private Mat cropImage(Mat image)
        {
            Rect box = new Rect(image.cols()/4, image.rows()/5, image.cols()/2, image.rows()*3/5);
            return new Mat(image,box);
        }

        private void contrastStretch(Mat image)
        {
            UByteIndexer indexer=image.createIndexer();
            int[] pixMax={0,0,0};
            int[] pixMin={255,255,255};
            for(int y=0;y<image.rows();y++)
            {
                for(int x=0;x<image.cols();x++)
                {
                    for(int channel=0;channel<image.channels();channel++)
                    {
                        int temp=indexer.get(y,x,channel);
                        if(pixMax[channel]<temp)
                        {
                            pixMax[channel]=temp;
                        }
                        if(pixMin[channel]>temp)
                        {
                            pixMin[channel]=temp;
                        }
                    }
                }
            }
            for(int y=0;y<image.rows();y++)
            {
                for(int x=0;x<image.cols();x++)
                {
                    for(int channel=0;channel<image.channels();channel++)
                    {
                        int temp=indexer.get(y,x,channel);
                        int pix=(temp-pixMin[channel])*255/(pixMax[channel]-pixMin[channel]);
                        indexer.put(y,x,channel,pix);
                    }
                }
            }
        }
    }

}
