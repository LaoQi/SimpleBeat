package com.madao.simplebeat;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class BpmPicker extends RecyclerView {

    private final static String Tag = "BpmPicker";
    private int mValue;
    private OnValueChangeListener mOnValueChangeListener;

    private BpmLayoutManager layoutManager;

    public interface OnValueChangeListener {
        void onValueChange(int oldValue, int newValue);
    }

    public void setValue(int v) {
        mValue = v;
        scrollToPosition(mValue - Constant.MinBPM);
        layoutManager.setChildScale();
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

    public void Resize(float unit, boolean compact, Typeface typeface) {
        if (!compact) {
            getLayoutParams().width = (int) (unit * 9);
            getLayoutParams().height = (int) (unit * 2);
        }
        setAdapter(new RVAdapter(unit, compact, typeface));
        scrollToPosition(mValue - Constant.MinBPM);
    }

    public class BpmLayoutManager extends LinearLayoutManager {
        public BpmLayoutManager(Context context) {
            super(context);
        }

        public void setChildScale() {
            for (int i = 0; i < getChildCount(); i++) {
                View itemView = getChildAt(i);
                if (itemView != null) {
                    itemView.setScaleX(1);
                    itemView.setScaleY(1);
                } else {
                    Log.d(Tag, "child null at " + i);
                }
            }
        }
    }

    private static class RVAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        final private float unit;
        final private boolean compact;
        final private Typeface typeface;

        private RVAdapter(float unit, boolean compact, Typeface typeface) {
            this.unit = unit;
            this.compact = compact;
            this.typeface = typeface;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.bpm_picker_item, parent, false);
            if (compact) {
                v.getLayoutParams().width = parent.getWidth() / 3;
                v.getLayoutParams().height = (int) unit;
            } else {
                v.getLayoutParams().width = (int) (unit * 3);
                v.getLayoutParams().height = (int) (unit * 2);
            }
            return new RecyclerView.ViewHolder(v) {};
        }

        @SuppressLint("SetTextI18n")
        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            TextView textView = holder.itemView.findViewById(R.id.BpmPickerItem);
            if (typeface != null) {
                textView.setTypeface(typeface);
            }
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, unit * 0.8f);
            if (position > 0 && position < Constant.MaxBPM - Constant.MinBPM + 2) {
                textView.setText("" + (position + Constant.MinBPM - 1));
            } else {
                textView.setText("");
            }
        }

        @Override
        public int getItemCount() {
            return Constant.MaxBPM - Constant.MinBPM;
//            return Constant.MaxBPM - Constant.MinBPM + 3;
        }
    }

    private void Construct(Context context) {
//        setBackgroundColor(Color.RED);
        layoutManager = new BpmLayoutManager(context);
        setLayoutManager(layoutManager);
        layoutManager.setOrientation(RecyclerView.HORIZONTAL);
        setAdapter(new RVAdapter(64f, false, null));
    }

    @Override
    public void onScrollStateChanged(int state) {
        if (state == SCROLL_STATE_DRAGGING) {
            layoutManager.setChildScale();
        } else if(state == SCROLL_STATE_IDLE){
            int position = layoutManager.findFirstVisibleItemPosition();
            View view = layoutManager.findViewByPosition(position);
            View center = layoutManager.findViewByPosition(position + 1);
            if (view == null) {
                Log.w(Tag, "Error view at " + position);
                return;
            }
            assert center != null;

            int offset = view.getLeft();
            int width = view.getWidth();

            if(offset == 0) {
                int newValue = position + Constant.MinBPM;
                int oldValue = mValue;
                mValue = newValue;

                center.setScaleX(2);
                center.setScaleY(2);

                Log.d(Tag, "ValueChanged : old " + oldValue + " new " + newValue);
                mOnValueChangeListener.onValueChange(mValue, newValue);
            } else if(-offset < width / 2){
                smoothScrollBy(offset, 0);
            }
            else {
                smoothScrollBy(width + offset, 0);
            }
        }
    }

}
