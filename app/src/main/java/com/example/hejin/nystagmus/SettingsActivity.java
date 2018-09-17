package com.example.hejin.nystagmus;

import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.example.hejin.nystagmus.Utils.GlideCacheUtil;
import com.example.hejin.nystagmus.Utils.L;
import com.example.hejin.nystagmus.Utils.T;
import com.example.hejin.nystagmus.Utils.Tool;

public class SettingsActivity extends AppCompatActivity {

    private EditText edit_LeftCameraAddress;
    private EditText edit_RightCameraAddress;
    private SharedPreferences pref;
    private SharedPreferences.Editor editor;
    private static String tempLeftAddress="";//用于在应用打开生存周期内暂存地址信息
    private static String tempRightAddress="";//用于在应用打开生存周期内暂存地址信息
    private static String tempGrayValue="";//用于在应用打开生存周期内暂存灰度值信息,暂时用字符串表示
    private static boolean isTemp=false;//用于判断应用是否是第一次打开
    private Button cameraAddressUpdate;

    private EditText edit_GrayValue;
    private Button recognitionParameterUpdate;
    private TextView cacheSize;
    private Button cacheClear;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        if(Build.VERSION.SDK_INT>= Build.VERSION_CODES.LOLLIPOP)
        {
            //大于安卓5.0即API21版本可用
            //导航栏颜色与状态栏统一
            View decorView = getWindow().getDecorView();
            int option = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;//如果想要隐藏导航栏，可以加上View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            decorView.setSystemUiVisibility(option);
            getWindow().setStatusBarColor(getResources().getColor(R.color.blue));
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // 设置返回键可用，可见
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        edit_LeftCameraAddress=(EditText)findViewById(R.id.camera_address_left_input);
        edit_RightCameraAddress=(EditText)findViewById(R.id.camera_address_right_input);
        edit_GrayValue=(EditText) findViewById(R.id.recognition_gray_value_input);

        cameraAddressUpdate=(Button)findViewById(R.id.camera_address_update_button);
        recognitionParameterUpdate=(Button) findViewById(R.id.recognition_parameter_update_button);

        cacheSize=(TextView) findViewById(R.id.cache_size);
        cacheClear=(Button) findViewById(R.id.clear_cache_button);

        edit_GrayValue.addTextChangedListener(new MyTextWatcher(edit_GrayValue));//增加一个文本监听类

        pref=getSharedPreferences("CameraAddress",MODE_PRIVATE);
        editor=pref.edit();

        cameraAddressUpdate.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v)
            {
                switch (v.getId())
                {
                    case R.id.camera_address_update_button:
                    {
                        //更新地址
                        String inputLeftAddress=edit_LeftCameraAddress.getText().toString();
                        String inputRightAddress=edit_RightCameraAddress.getText().toString();
                        saveAddress(inputLeftAddress,inputRightAddress);
                        Tool.AddressLeftEye=inputLeftAddress;
                        Tool.AddressRightEye=inputRightAddress;
                        T.showShort(SettingsActivity.this,"网络摄像头地址修改成功");
                        L.d("网络摄像头地址修改成功");
                        break;
                    }
                    default:
                        break;
                }
            }
        });
        recognitionParameterUpdate.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v)
            {
                switch (v.getId())
                {
                    case R.id.recognition_parameter_update_button:
                    {
                        //更新参数
                        String inputGrayValueStr=edit_GrayValue.getText().toString();
                        int inputGrayValue;
                        try
                        {
                            inputGrayValue= Integer.parseInt(inputGrayValueStr);
                        }
                        catch (NumberFormatException e)
                        {
                            T.showShort(SettingsActivity.this,R.string.label_input_error);
                            L.d(R.string.label_input_error+e.toString());
                            return;
                        }
                        if(inputGrayValue<255&&inputGrayValue>0)
                        {
                            //符合标准,其实在文本监听那块就已经有了判断
                            saveParameter(inputGrayValue);
                            Tool.RecognitionGrayValue=inputGrayValue;
                            T.showShort(SettingsActivity.this,"图像识别参数修改成功");
                            L.d("图像识别参数修改成功");
                        }
                        else
                        {
                            T.showShort(SettingsActivity.this,R.string.label_input_error);
                        }
                    }
                    default:
                        break;
                }
            }
        });
        cacheClear.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v)
            {
                switch (v.getId())
                {
                    case R.id.clear_cache_button:
                    {
                        //清理缓存
                        GlideCacheUtil.getInstance().clearImageAllCache(SettingsActivity.this);
                        String cache= GlideCacheUtil.getInstance().getCacheSize(SettingsActivity.this);
                        cacheSize.setText(cache);
                        break;
                    }
                    default:break;
                }
            }
        });
    }
    @Override
    public void onResume()
    {
        //在活动交互时调用
        //先进行参数恢复
        String leftAddress;
        String rightAddress;
        int grayValue;
        if(!isTemp)
        {
            leftAddress=pref.getString("LeftCameraAddress",Tool.AddressLeftEye);
            rightAddress=pref.getString("RightCameraAddress",Tool.AddressRightEye);
            grayValue=pref.getInt("GrayValue",Tool.RecognitionGrayValue);
            isTemp=true;
        }
        else
        {
            leftAddress=tempLeftAddress;
            rightAddress=tempRightAddress;
            try {
                grayValue= Integer.parseInt(tempGrayValue);
            }
            catch (NumberFormatException e)
            {
                grayValue=Tool.RecognitionGrayValue;
            }
        }
        edit_LeftCameraAddress.setText(leftAddress);
        edit_RightCameraAddress.setText(rightAddress);
        edit_GrayValue.setText(Integer.toString(grayValue));
        //显示缓存
        String cache= GlideCacheUtil.getInstance().getCacheSize(SettingsActivity.this);
        cacheSize.setText(cache);
        super.onResume();
    }
    @Override
    public void onPause()
    {
        //在退出活动时暂存信息，下次打开时直接可以显示
        tempLeftAddress=edit_LeftCameraAddress.getText().toString();
        tempRightAddress=edit_RightCameraAddress.getText().toString();
        tempGrayValue=edit_GrayValue.getText().toString();
        super.onPause();
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId())
        {
            case  android.R.id.home:
            {
                //返回
                this.finish();
                break;
            }
            case R.id.settings_defalut:
            {
                //恢复默认设置
                edit_LeftCameraAddress.setText(Tool.AddressLeftEyeDefault);
                edit_RightCameraAddress.setText(Tool.AddressRightEyeDefault);
                edit_GrayValue.setText(String.valueOf(Tool.RecognitionGrayValueDefault));
                //cameraAddressUpdate.performClick();//模拟点击事件
                //为了给出不一样的提醒，不采用模拟点击按钮了
                saveAddress(Tool.AddressLeftEyeDefault,Tool.AddressRightEyeDefault);
                saveParameter(Tool.RecognitionGrayValueDefault);
                Tool.AddressLeftEye=Tool.AddressLeftEyeDefault;
                Tool.AddressRightEye=Tool.AddressRightEyeDefault;
                Tool.RecognitionGrayValue=Tool.RecognitionGrayValueDefault;
                T.showShort(SettingsActivity.this,"恢复默认成功");
                L.d("恢复默认成功");
                break;
            }
            default:
                break;
        }
        return true;
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        //创建右上方菜单栏按钮
        getMenuInflater().inflate(R.menu.settings_menu,menu);
        return true;
    }
    private void saveAddress(String leftAddress, String rightAddress)
    {
        editor.putString("LeftCameraAddress",leftAddress);//左眼写入
        editor.putString("RightCameraAddress",rightAddress);//右眼写入
        editor.apply();//执行应用
    }
    private void saveParameter(int grayValue)
    {
        editor.putInt("GrayValue",grayValue);
        editor.apply();//执行应用
    }

    private class MyTextWatcher implements TextWatcher {

        private EditText editText;
        private int value;

        MyTextWatcher(EditText editText) {
            this.editText = editText;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter) {
            //实时判断文本变化是否正确
            String str = text.toString();

            try
            {
                value= Integer.parseInt(str);
            }
            catch (NumberFormatException e)
            {
                T.showShort(SettingsActivity.this,R.string.label_input_error);
                L.d(R.string.label_input_error+e.toString());
                return;
            }
            if(value>255||value<1)
            {
                T.showShort(SettingsActivity.this,R.string.label_input_error);
            }
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    }
}

