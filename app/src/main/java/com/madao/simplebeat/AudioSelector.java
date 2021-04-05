package com.madao.simplebeat;

import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class AudioSelector extends ConstraintLayout {

    private Context mContext;
    private RecyclerView mSelector;
    private LinearLayoutManager linearLayoutManager;
    private List<String> audioList;
    private OnValueChangeListener listener;
    private int originPosition;

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

    private final RecyclerView.Adapter<RecyclerView.ViewHolder> adapter = new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        private View lastItem;
        private int selectedPosition = 0;

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.audio_selector_item, parent, false);
            v.getLayoutParams().width = parent.getWidth() / 3;
            return new RecyclerView.ViewHolder(v) {};
        }

        public void scroll() {
            int first = linearLayoutManager.findFirstVisibleItemPosition();
            if (first == selectedPosition && selectedPosition > 0) {
                mSelector.smoothScrollToPosition(selectedPosition - 1);
            } else {
                mSelector.smoothScrollToPosition(selectedPosition + 1);
            }
        }

        protected void onClickItem(View view, int position) {
            if (selectedPosition == position) {
                return;
            }
            int lastPosition = selectedPosition;
            selectedPosition = position;
            scroll();
            highlight(view);
            resetItem(lastItem);

            lastItem = view;
            listener.onValueChange(lastPosition, position);
        }

        protected void highlight(View view) {
            view.setBackground(mContext.getDrawable(R.drawable.audio_item_checked_shape));
            ((TextView) view).setTextColor(mContext.getColor(R.color.blue_700));
        }

        protected void resetItem(View view) {
            view.setBackground(mContext.getDrawable(R.drawable.audio_item_normal_shape));
            ((TextView) view).setTextColor(mContext.getColor(R.color.gray_300));
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            TextView textView = holder.itemView.findViewById(R.id.audioSelectorItemName);
            textView.setText(audioList.get(position));
            textView.setOnClickListener(v -> onClickItem(v, position));
            if (position == originPosition) {
                highlight(textView);
                lastItem = textView;
                selectedPosition = position;
            }
        }

        @Override
        public int getItemCount() {
            return audioList.size();
        }
    };

    private void Construct(Context context) {
        mContext = context;
        LayoutInflater.from(context).inflate(R.layout.audio_selector, this, true);
        mSelector = findViewById(R.id.AudioList);
        linearLayoutManager = new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL,false);
        mSelector.setLayoutManager(linearLayoutManager);
    }

    public void bindData(int position, List<String> list, OnValueChangeListener onValueChangeListener) {
        listener = onValueChangeListener;
        audioList = list;
        originPosition = position;
        mSelector.setAdapter(adapter);
        new Handler(mContext.getMainLooper()).postDelayed(() -> mSelector.smoothScrollToPosition(originPosition + 1), 100);
    }
}
