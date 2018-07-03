package com.example.panzq.mywifiapplication.adapter;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.panzq.mywifiapplication.R;
import com.example.panzq.mywifiapplication.wifi.MScanWifi;

import java.util.ArrayList;

public class WifiAdapter extends BaseAdapter {
    private ArrayList<MScanWifi> wifis;
    private Activity context;
    private int selectedItem;
    public WifiAdapter(ArrayList<MScanWifi> wifis,Activity context)
    {
        this.wifis = wifis;
        this.context = context;
    }



    @Override
    public int getCount() {
        return wifis.size();
    }

    @Override
    public Object getItem(int i) {
        return wifis.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int position, View view, ViewGroup viewGroup) {

        Holder holder;
        if (view == null)
        {
            holder = new Holder();
            view = context.getLayoutInflater().inflate(R.layout.listitems,null);
            holder.ivWifilevel = (ImageView)view.findViewById(R.id.img_wifi_level);
            holder.tvSSID = (TextView)view.findViewById(R.id.tv1);
            holder.tvExist = (TextView)view.findViewById(R.id.tv2);
            view.setTag(holder);
        }else{
            holder = (Holder)view.getTag();
        }
        if (selectedItem == position)
        {
            view.setBackgroundColor(Color.BLUE);
        }else{
            view.setBackgroundColor(Color.GRAY);
        }
        MScanWifi wifi = wifis.get(position);
        holder.tvSSID.setText(wifi.getWifiName());
        if (wifi.getIsLock()) {
            holder.ivWifilevel.setImageResource(R.drawable.wifi_signal_lock);
            holder.ivWifilevel.setImageLevel(wifi.getLevel());
        } else {
            holder.ivWifilevel.setImageResource(R.drawable.wifi_signal_open);
            holder.ivWifilevel.setImageLevel(wifi.getLevel());
        }
        if (wifi.getIsExsit()) {
            holder.tvExist.setText("已保存");
            holder.tvExist.setVisibility(View.VISIBLE);
        }

        return view;
    }
    class Holder {
        ImageView ivWifilevel;
        TextView tvSSID;
        TextView tvExist;
    }
    public void setSelectedItem(int pos)
    {
        this.selectedItem = pos;
    }
}
