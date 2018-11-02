package com.wind.carnavi.adapter;

import android.content.Context;

import com.wind.carnavi.model.Destination;
import com.wind.carnavi.util.CommonAdapter;
import com.wind.carnavi.util.ViewHolder;

import java.util.List;
import com.wind.carnavi.R;

/**
 * Created by houjian on 2018/6/21.
 */

public class SearchTextAdapter extends CommonAdapter<Destination> {

    public SearchTextAdapter(Context context, List<Destination> data, int layoutId) {
        super(context, data, layoutId);
    }

    @Override
    public void convert(ViewHolder holder, int position) {
        holder.setText(R.id.item_search_tv_content, mData.get(position).getContent());
    }
}
