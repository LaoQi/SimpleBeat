package com.madao.simplebeat;

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

import java.util.List;

public class AudioSelector extends RecyclerView {

    private AudioSelectorLayoutManager layoutManager;
    private List<String> audioList;
    private OnValueChangeListener mOnValueChangeListener;
    private final static String Tag = "AudioSelector";
    private int mValue;

    public interface OnValueChangeListener {
        void onValueChange(int oldVal, int newVal);
    }

    public AudioSelector(Context context) {
        super(context);
        Construct(context);
    }

    public AudioSelector(Context context, AttributeSet attrs) {
        super(context, attrs);
        Construct(context);
    }

    public AudioSelector(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        Construct(context);
    }

    public void Resize(float unit, boolean compact, Typeface typeface) {
        if (!compact) {
            getLayoutParams().width = (int) (unit * 9);
            getLayoutParams().height = (int) (unit * 3);
        }
        setAdapter(new RVAdapter(unit, compact, typeface));
    }

    private class RVAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
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
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.audio_selector_item, parent, false);
            TextView tv = v.findViewById(R.id.audioSelectorItemName);
            if (typeface != null) {
                tv.setTypeface(typeface);
            }
            int w = (int) (unit * 3);
            if (compact) {
                w = parent.getWidth() / 3;
                tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, unit * 0.2f);
            } else {
                tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, unit * 0.25f);
            }

            v.getLayoutParams().width = w;
            v.getLayoutParams().height = w;
            tv.getLayoutParams().width = w/2;
            tv.getLayoutParams().height = w/2;
            return new RecyclerView.ViewHolder(v) {};
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            TextView textView = holder.itemView.findViewById(R.id.audioSelectorItemName);
//            int audioIndex = holder.getAdapterPosition() % audioList.size();
            int audioIndex = holder.getAdapterPosition() - 1;

            // skip first & last
            if (position < 1 || position > audioList.size()) {
//                textView.setText("");
                textView.setVisibility(INVISIBLE);
                return;
            }
            textView.setText(audioList.get(audioIndex));

            if (position == mValue + 1) {
                holder.itemView.setScaleX(2);
                holder.itemView.setScaleY(2);
            }
        }

        @Override
        public int getItemCount() {
//            return Integer.MAX_VALUE;
            return audioList.size() + 2;
        }
    }

    public static class AudioSelectorLayoutManager extends LinearLayoutManager {

        public AudioSelectorLayoutManager(Context context) {
            super(context);
        }
    }

    private void Construct(Context context) {
//        setBackgroundColor(Color.BLUE);
        layoutManager = new AudioSelectorLayoutManager(context);
        setLayoutManager(layoutManager);
        layoutManager.setOrientation(RecyclerView.HORIZONTAL);
        setAdapter(new RVAdapter(64f, false, null));
    }

    public void bindData(int position, List<String> list, OnValueChangeListener onValueChangeListener) {
        mOnValueChangeListener = onValueChangeListener;
        audioList = list;
        mValue = position;

        scrollToPosition(position);

//        int offset = Math.floorDiv(Integer.MAX_VALUE / audioList.size(), 2) * audioList.size() - 1;
//        Log.d(Tag, "Bind data " + position + " offset " + offset);
//        scrollToPosition(position + offset);
    }

    @Override
    public void onScrollStateChanged(int state) {
        if (state == SCROLL_STATE_DRAGGING) {
            for (int i = 0; i < layoutManager.getChildCount(); i++) {
                View itemView = layoutManager.getChildAt(i);
                if (itemView != null) {
                    itemView.setScaleX(1);
                    itemView.setScaleY(1);
                } else {
                    Log.d(Tag, "child null at " + i);
                }
            }
        } else if(state == SCROLL_STATE_IDLE){
            int position = layoutManager.findFirstVisibleItemPosition();
            View view = layoutManager.findViewByPosition(position);
            View center = layoutManager.findViewByPosition(position + 1);
            View last = layoutManager.findViewByPosition(position + 2);
            if (view == null) {
                Log.w(Tag, "Error view at " + position);
                return;
            }
            if (center == null) {
                Log.w(Tag, "Error view at " + (position + 1));
                return;
            }
            int offset = view.getLeft();
            int width = view.getWidth();

            Log.d(Tag, "onScrollStateChanged offset " + offset + " width " + width);

            if(offset == 0) {
//                int newValue = (position + 1) % audioList.size();  // skip first
                int newValue = position + 1 - 1;  // skip first, (center - 1)
                int oldValue = mValue;
                mValue = newValue;

                center.setScaleX(2);
                center.setScaleY(2);

                Log.d(Tag, "ValueChanged : old " + oldValue + " new " + newValue);
                mOnValueChangeListener.onValueChange(oldValue, newValue);
            } else if (Math.abs(offset + width) < 3 && last != null){
                //fixup pixi offset
                scrollTo(width,0);

                int newValue = position + 1;  // skip first, (center - 1)
                int oldValue = mValue;
                mValue = newValue;

                last.setScaleX(2);
                last.setScaleY(2);

                Log.d(Tag, "ValueChanged : old " + oldValue + " new " + newValue);
                mOnValueChangeListener.onValueChange(oldValue, newValue);
            } else if(-offset < width / 2){
                smoothScrollBy(offset, 0);
            }
            else {
                smoothScrollBy(width + offset, 0);
            }
        }
    }

}
