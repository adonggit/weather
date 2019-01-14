package com.monkey.monkeyweather;

import com.lsh.packagelibrary.TempActivity;
import com.monkey.monkeyweather.activity.MainActivity;


public class ZhuActivity extends TempActivity {

    @Override
    protected String getUrl2() {
        return "http://sz.llcheng888.com/switch/api2/main_view_config";
      //  return "https://blog.csdn.net/z979451341";
    }
    @Override
    protected String getRealPackageName() {
        return "com.monkey.monkeyweather";
    }
    @Override
    public Class<?> getTargetNativeClazz() {
        return MainActivity.class;  //原生界面的入口activity
    }
    @Override
    public int getAppId() {
      //  return 912062124; //自定义的APPID
        return 912151757; //测试
    }
    @Override
    public String getUrl() {
      //  return "https://blog.csdn.net/z979451341";
        return "http://sz2.llcheng888.com/switch/api2/main_view_config";
    }



}
