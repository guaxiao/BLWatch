package com.tau.blwatch;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Created by Administrator on 2015/12/19 0019.
 */
public class LineRecyclerAdapter extends RecyclerView.Adapter<LineRecyclerAdapter.MyViewHolder> {

    private int mItemResource,mViewResource;
    private Context mContext;
    private String[] mDataList;

    private OnItemClickListener mOnItemClickListener;

    public LineRecyclerAdapter(Context context, String[] dataList, int itemResource, int viewResource){
        mContext = context;
        mDataList = dataList;
        mItemResource = itemResource;
        mViewResource = viewResource;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        MyViewHolder holder = new MyViewHolder(
                LayoutInflater.from(mContext).inflate(mViewResource, parent, false));
        return holder;
    }

    @Override
    public void onBindViewHolder(final MyViewHolder holder, int position) {
        holder.tv.setText(mDataList[position]);

        // 如果设置了回调，则设置点击事件
        if (mOnItemClickListener != null){
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int pos = holder.getLayoutPosition();
                    mOnItemClickListener.onItemClick(holder.itemView, pos);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return mDataList.length;
    }

    class MyViewHolder extends RecyclerView.ViewHolder {

        TextView tv;

        public MyViewHolder(View view) {
            super(view);
            tv = (TextView) view.findViewById(mItemResource);
        }
    }

    public interface OnItemClickListener{
        void onItemClick(View view, int position);
    }


    public void setOnItemClickListener(OnItemClickListener l){
        this.mOnItemClickListener = l;
    }
}