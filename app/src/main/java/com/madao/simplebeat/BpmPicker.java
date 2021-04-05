package com.madao.simplebeat;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class BpmPicker extends RecyclerView {

    private int mValue;
    private int MinBPM;
    private int MaxBPM;
    private View lastView;
    private OnValueChangeListener mOnValueChangeListener;

    private LinearLayoutManager layoutManager;

    public interface OnValueChangeListener {
        void onValueChange(int oldValue, int newValue);
    };

    public void setValue(int v) {
        mValue = v;
        scrollToPosition(mValue - MinBPM);
    }

    public void setOnValueChangedListener(OnValueChangeListener listener) {
        mOnValueChangeListener = listener;
    }

    public BpmPicker(Context context) {
        super(context);
        Construct(context);
    }

    public BpmPicker(Context context, AttributeSet attrs) {
        super(context, attrs);
        Construct(context);
    }

    public BpmPicker(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        Construct(context);
    }

    private final RecyclerView.Adapter<RecyclerView.ViewHolder> adapter = new RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.bpm_picker_item, parent, false);
            v.getLayoutParams().width = parent.getWidth() / 3;
            return new RecyclerView.ViewHolder(v) {};
        }

        @SuppressLint("SetTextI18n")
        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            TextView textView = holder.itemView.findViewById(R.id.BpmPickerItem);

            if (position > 0 && position < MaxBPM - MinBPM + 2) {
                textView.setText("" + (position + MinBPM - 1));
            } else {
                textView.setText("");
            }
        }

        @Override
        public int getItemCount() {
            return MaxBPM - MinBPM + 3;
        }
    };

    private void Construct(Context context) {
        MinBPM = Constant.MinBPM;
        MaxBPM = Constant.MaxBPM;
        layoutManager = new LinearLayoutManager(context);
        setLayoutManager(layoutManager);
        layoutManager.setOrientation(RecyclerView.HORIZONTAL);
        setAdapter(adapter);
    }

    @Override
    public void onScrollStateChanged(int state) {
        Log.d("bruce","state:"+state);
        if(state == 0){
            int position = layoutManager.findFirstVisibleItemPosition();
            View view = layoutManager.findViewByPosition(position);
            View center = layoutManager.findViewByPosition(position + 1);
            assert view != null;
            assert center != null;

            int offset = view.getLeft();
            int width = view.getWidth();

            if(offset == 0) {
                int newValue = position + MinBPM;
                int oldValue = mValue;
                mValue = newValue;
                Log.d(getClass().getName(), "ValueChanged : old " + oldValue + " new " + newValue);
                mOnValueChangeListener.onValueChange(mValue, newValue);
            } else if(-offset < width / 2){
                smoothScrollBy(offset, 0);
            }
            else {
                smoothScrollBy(width + offset, 0);
            }
        }
    }

    @Override
    public void onScrolled(int dx, int dy) {
        int position = layoutManager.findFirstVisibleItemPosition();
        View center = layoutManager.findViewByPosition(position + 1);
        assert center != null;
        if (lastView != null) {
            lastView.setScaleX(1);
            lastView.setScaleY(1);
        }
        lastView = center;
        center.setScaleX(2);
        center.setScaleY(2);
    }
}
