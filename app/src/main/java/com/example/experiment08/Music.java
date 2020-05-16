package com.example.experiment08;

import androidx.lifecycle.ViewModel;

class Music extends ViewModel {

    //歌曲名id
    private String music_id;

    //歌曲名
    private String music_name;

    //作者
    private String music_author;

    //唱片
    private String music_Record;

    //文件时长
    private String music_Draution;

    //路径
    private String music_url;

    //已经播放时长
    private String music_CurrentPosition; //已经播放时长


    String getMusic_id() {
        return music_id;
    }

    void setMusic_id(String music_id) {
        this.music_id = music_id;
    }

    String getMusic_name() {
        return music_name;
    }

    void setMusic_name(String music_name) {
        this.music_name = music_name;
    }

    String getMusic_author() {
        return music_author;
    }

    void setMusic_author(String music_author) {
        this.music_author = music_author;
    }

    String getMusic_Draution() {
        return music_Draution;
    }

    void setMusic_Draution(String music_Draution) {
        this.music_Draution = music_Draution;
    }

    String getMusic_Record() {
        return music_Record;
    }

    void setMusic_Record(String music_Record) {
        this.music_Record = music_Record;
    }

    String getMusic_url() {
        return music_url;
    }

    void setMusic_url(String music_url) {
        this.music_url = music_url;
    }
}
