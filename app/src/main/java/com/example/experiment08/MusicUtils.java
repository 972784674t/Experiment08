package com.example.experiment08;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 加载本地存储当中的音乐文件到集合中
 */

class MusicUtils {

    static List<Music> getMusicList(Context context){

        List<Music> musicList = new ArrayList<>();

        ContentResolver contentResolver;
        //获取ContentResolver对象
        contentResolver = context.getContentResolver();

        //获取本地音乐的存储地址
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

        //查询音乐文件
        Cursor cursor = contentResolver.query(uri,null,null,null,null);
        int id = 0;
        assert cursor != null;
        while ( cursor.moveToNext() ){
            //指定文件夹中的音乐文件
//            if(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA)).indexOf("qqmusic/ringtones") > 0) {
                Music music = new Music();
                id++;
                music.setMusic_id(String.valueOf(id));
                music.setMusic_name(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)));
                music.setMusic_author(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)));
                music.setMusic_Record(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)));
                music.setMusic_url(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA)));

                //持续时间为long对象，将其转换为string
                Long time = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION));
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("mm:ss",Locale.getDefault());
                String duration = simpleDateFormat.format(new Date(time));
                music.setMusic_Draution(duration);

                musicList.add(music);
//            }
        }
        cursor.close();
        return musicList;
    }
}
