package com.example.experiment08;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

/**
 * @author cimo
 */
public class MusicAdapter extends RecyclerView.Adapter<MusicAdapter.MusicItem> {

    //记录上次选择位置
    private static int LAST_CHOICE = -1;
    //记录当前选择位置
    private static int NOW_CHOICE = -1;

    private List<Music> musics = new ArrayList<>();

    void setMusics(List<Music> musics) {
        this.musics = musics;
    }

    private OnItemClickListener onItemClickListener;

    void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    //点击事件接口回调
    public interface OnItemClickListener {
        void OnItemClick(View view, int position);
    }

    @NonNull
    @Override
    public MusicItem onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View itemView = layoutInflater.inflate(R.layout.music_cell, parent, false);
        return new MusicItem(itemView);
    }

    /**
     * 用于设置RecyclerView中的元素
     *
     * @param holder   item元素
     * @param position item位置
     */
    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull final MusicItem holder, final int position) {

        Music music = musics.get(position);
        holder.musicName.setText(music.getMusic_name());
        holder.musicAuthor.setText(music.getMusic_author());
        holder.musicTime.setText(music.getMusic_Draution());
        holder.musicRecord.setText(music.getMusic_Record());
        holder.musicId.setText("# " + music.getMusic_id());
        holder.itemView.setTag(position);

        //设置选中样式
        if (NOW_CHOICE == position) {
            holder.itemChoice.setVisibility(View.VISIBLE);
        } else {
            holder.itemChoice.setVisibility(View.GONE);
        }

        //点击事件接口回调
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onItemClickListener.OnItemClick(v, position);
                //点击时设置选中样式
                if (LAST_CHOICE != -1) {
                    notifyItemChanged(LAST_CHOICE);
                }
                holder.itemChoice.setVisibility(View.VISIBLE);
                LAST_CHOICE = position;
                NOW_CHOICE = position;
                notifyDataSetChanged();
            }
        });

    }

    @Override
    public int getItemCount() {
        return musics.size();
    }

    /**
     * 内部类MusicItem
     * 用于绑定RecyclerView中的元素
     */
    static class MusicItem extends RecyclerView.ViewHolder {

        TextView musicName, musicAuthor, musicTime, musicRecord, musicId, itemChoice;

        MusicItem(@NonNull View itemView) {
            super(itemView);
            musicName = itemView.findViewById(R.id.musicname);
            musicAuthor = itemView.findViewById(R.id.musicauthor);
            musicTime = itemView.findViewById(R.id.musictime);
            musicRecord = itemView.findViewById(R.id.songrecord);
            musicId = itemView.findViewById(R.id.songid);
            itemChoice = itemView.findViewById(R.id.itemchoice);
        }
    }

}
