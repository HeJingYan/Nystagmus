package com.example.hejin.nystagmus;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.example.hejin.nystagmus.Utils.HttpUtil;
import com.example.hejin.nystagmus.Utils.L;
import com.example.hejin.nystagmus.Utils.T;
import com.example.hejin.nystagmus.Utils.Tool;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

import static com.example.hejin.nystagmus.R.id.fab;

public class IntroduceActivity extends AppCompatActivity {

    private ImageView bingPicImg;
    private static boolean bingLoad=false;//用来判断是否第一次打开这个活动用于加载照片
    private SharedPreferences prefs;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_introduce);

        if(Build.VERSION.SDK_INT>= Build.VERSION_CODES.LOLLIPOP)
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
        prefs= PreferenceManager.getDefaultSharedPreferences(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // 设置返回键和菜单栏可用，可见
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        //加载必应每日一图

        bingPicImg=(ImageView) findViewById(R.id.toolbar_image_view);

        if(!bingLoad&&isNetworkAvailable()&&ping())
        {
            //有网络连接并且连接到外网
            bingLoad=true;
            loadBingPic();
            L.d("已更新图片");
        }
        else
        {
            //从缓存加载
            String bingPic=prefs.getString("bing_pic",null);
            if(bingPic!=null)
            {
                //缓存里有就从还从里加载
                Glide.with(this).load(bingPic).into(bingPicImg);
            }
            else
            {
                //缓存里没有的话，就直接加载保存的照片
                Glide.with(this).load(R.drawable.background).into(bingPicImg);
            }
            L.d("加载缓存照片");
        }

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                T.showShort(IntroduceActivity.this,"正在访问学校主页");
                L.d("正在访问学校主页");
                Intent intent=new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("http://www.hit.edu.cn/"));
                startActivity(intent);
            }
        });
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId())
        {
            case  android.R.id.home:
            {
                this.finish();
                return true;
            }
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }
    //加载必应每日一图
    private void loadBingPic()
    {
        //String requestBingPic="http://guolin.tech/api/bing_pic";
        HttpUtil.sendOkHttpRequest(Tool.RequestBingPic,new okhttp3.Callback(){
            @Override
            public void onResponse(Call call,Response response) throws IOException {
                final String bingPic=response.body().string();
                //写入到缓存中
                SharedPreferences.Editor editor=prefs.edit();
                editor.putString("bing_pic",bingPic);
                editor.apply();
                runOnUiThread(new Runnable(){
                    @Override
                    public void run(){
                        Glide.with(IntroduceActivity.this).load(bingPic).into(bingPicImg);
                    }
                });
            }

            @Override
            public void onFailure(Call call,IOException e)
            {
                L.d(e.toString());
            }
        });
    }
    /**
     * 判断网络情况
     * @return false 表示没有网络 true 表示有网络
     */
    private boolean isNetworkAvailable()
    {
        // 得到网络连接信息
        ConnectivityManager manager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        // 去进行判断网络是否连接
        if (manager.getActiveNetworkInfo() != null) {
            return manager.getActiveNetworkInfo().isAvailable();
        }
        return false;
    }
    /**
     * 通过Ping来判断是否真有外网连接
     * @return flase 表示没有网络 true 表示有网络
     */
    private boolean ping()
    {
        try {
            String ip = "61.135.169.125";// ping 的地址，可以换成任何一种可靠的外网，这是百度的地址
            Process p = Runtime.getRuntime().exec("ping -c 1 -w 1 " + ip);// ping网址1次,超时时间2s

            // ping的状态
            //status 等于0的时候表示网络可用，status等于2时表示当前网络不可用。
            if(p==null)
            {
                return false;
            }
            int status = p.waitFor();
            if (status == 0) {
                //有网
                return true;
            } else {
                //无网
                return false;
            }
        }
        catch (IOException e)
        {
            return false;
        }
        catch (InterruptedException ie)
        {
            return false;
        }
    }
}
