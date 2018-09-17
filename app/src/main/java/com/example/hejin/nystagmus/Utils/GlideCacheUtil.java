package com.example.hejin.nystagmus.Utils;

import android.content.Context;
import android.os.Looper;
import android.text.TextUtils;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.cache.ExternalCacheDiskCacheFactory;
import com.bumptech.glide.load.engine.cache.InternalCacheDiskCacheFactory;

import java.io.File;
import java.math.BigDecimal;

/**
 * Glide缓存工具类
 * Glide自带清除缓存的功能,分别对应Glide.get(context).clearDiskCache();(清除磁盘缓存)与Glide.get(context).clearMemory();(清除内存缓存)两个方法.
 * 其中clearDiskCache()方法必须运行在子线程,clearMemory()方法必须运行在主线程,
 * Created by HJY on 2017/9/14.
 */

public class GlideCacheUtil {
    private static GlideCacheUtil inst;
    public static GlideCacheUtil getInstance()
    {
        if(inst==null)
        {
            inst=new GlideCacheUtil();
        }
        return inst;
    }

    /**
     * 清除图片磁盘缓存
     * @param context 上下文环境
     */
    public void clearImageDiskCache(final Context context)
    {
        try
        {
            if(Looper.myLooper()== Looper.getMainLooper())
            {
                //如果在主线程下,需要开启一个子线程来运行clearDiskCache()方法
                new Thread(new Runnable(){
                    @Override
                    public void run()
                    {
                        Glide.get(context).clearDiskCache();
                    }
                }).start();
            }
            else
            {
                Glide.get(context).clearDiskCache();
            }
        }
        catch (Exception e)
        {
            L.d("清除图片磁盘缓存 错误："+e.toString());
        }
    }

    /**
     * 清除图片内存缓存
     * @param context 上下文环境
     */
    public void clearImageMemoryCache(Context context)
    {
        try {
            if(Looper.myLooper()== Looper.getMainLooper())
            {
                //只能在主线程执行
                Glide.get(context).clearMemory();
            }
        }
        catch (Exception e)
        {
            L.d("清除图片内存缓存 错误："+e.toString());
        }
    }

    /**
     * 清除图片所有缓存
     * @param context 上下文环境
     */
    public void clearImageAllCache(Context context)
    {
        clearImageDiskCache(context);
        clearImageMemoryCache(context);
        String ImageExternalCatchDir=context.getExternalCacheDir()+ ExternalCacheDiskCacheFactory.DEFAULT_DISK_CACHE_DIR;
    }

    /**
     * 获取Glide造成的缓存大小
     * @param context 上下文环境
     * @return CacheSize
     */
    public String getCacheSize(Context context)
    {
        try {
            return getFormatSize(getFolderSize(new File(context.getCacheDir()+"/"+ InternalCacheDiskCacheFactory.DEFAULT_DISK_CACHE_DIR)));
        }
        catch (Exception e)
        {
            L.d("取Glide造成的缓存大小 错误"+e.toString());
        }
        return "";
    }

    /**
     * 获取指定文件夹内所有文件大小的和
     * @param file
     * @return
     * @throws Exception
     */
    private long getFolderSize(File file) throws Exception
    {
        long size=0;
        try {
            File[] fileList=file.listFiles();
            for(File aFileList:fileList)
            {
                if(aFileList.isDirectory())
                {
                    size+=getFolderSize(aFileList);
                }
                else
                {
                    size+=aFileList.length();
                }
            }
        }
        catch (Exception e)
        {
            L.d("获取指定文件夹内所有文件大小的和 错误："+e.toString());
        }
        return size;
    }
    /**
     * 删除指定目录下的的文件，这里用于缓存的删除
     * @param filePath
     * @param deleteThisPath
     */
    private void deleteFolderFile(String filePath, boolean deleteThisPath)
    {
        if(!TextUtils.isEmpty(filePath))
        {
            //判断不是空字符串
            try {
                File file=new File(filePath);
                if(file.isDirectory())
                {
                    File files[]=file.listFiles();
                    for(File file1:files)
                    {
                        deleteFolderFile(file1.getAbsolutePath(),true);
                    }
                }
                if(deleteThisPath)
                {
                    if(!file.isDirectory())
                    {
                        file.delete();
                    }
                    else
                    {
                        if(file.listFiles().length==0)
                        {
                            file.delete();
                        }
                    }
                }
            }
            catch (Exception e)
            {
                L.d("删除目录文件 错误："+e.toString());
            }
        }
    }
    /**
     * 格式化单位
     * @param size
     * @return
     */
    private static String getFormatSize(double size)
    {
        double kiloByte=size/1024;
        if(kiloByte<1)
        {
            return size+"Byte";
        }

        double megaByte=kiloByte/1024;
        if(megaByte<1)
        {
            BigDecimal result1=new BigDecimal(Double.toString(kiloByte));
            return result1.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString()+"KB";
        }

        double gigaByte = megaByte / 1024;
        if (gigaByte < 1) {
            BigDecimal result2 = new BigDecimal(Double.toString(megaByte));
            return result2.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString() + "MB";
        }

        double teraBytes = gigaByte / 1024;
        if (teraBytes < 1) {
            BigDecimal result3 = new BigDecimal(Double.toString(gigaByte));
            return result3.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString() + "GB";
        }

        BigDecimal result4 = new BigDecimal(teraBytes);

        return result4.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString() + "TB";

    }
}
