package com.tau.blwatch.ui;

import android.content.Context;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Created by Administrator on 2015/12/19 0019.
 */
public class LineRecyclerAdapter extends RecyclerView.Adapter<LineRecyclerAdapter.MyViewHolder> {

    private static int mItemResource,mViewResource;
    private Context mContext;
    private String[] mDataList;

    private OnItemClickListener mOnItemClickListener;
    private int mSelectedItem = -1;

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
        holder.mTextView.setText(mDataList[position]);
        Log.d("onBindViewHolder","position="+position);

        // 如果设置了回调，则设置点击事件
        if (mOnItemClickListener != null){
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int pos = holder.getLayoutPosition();
                    mOnItemClickListener.onItemClick(holder.itemView, pos);

                    Log.d("onBindViewHolder", "position=" + pos);
                    setSelectItem(pos);
                }
            });
        }

        if(position == mSelectedItem){
            holder.mTextView.setTextColor(Color.BLUE);
        }else{
            holder.mTextView.setTextColor(Color.BLACK);
        }
    }

    public void notifyItemChangedAll(){
        for(int i = 0;i < getItemCount();i++){
            this.notifyItemChanged(i);
        }
    }

    public void setSelectItem(int selector){
        this.notifyItemChanged(selector);
        this.notifyItemChanged(mSelectedItem);
        mSelectedItem = selector;
    }

    public int getSelectItem(){
        return mSelectedItem;
    }

    @Override
    public int getItemCount() {
        return mDataList.length;
    }

    public final static class MyViewHolder extends RecyclerView.ViewHolder {

        public TextView mTextView;

        public MyViewHolder(View view) {
            super(view);
            mTextView = (TextView) view.findViewById(mItemResource);
        }
    }

    public interface OnItemClickListener{
        void onItemClick(View view, int position);
    }


    public void setOnItemClickListener(OnItemClickListener l){
        this.mOnItemClickListener = l;
    }
}