package com.madao.simplebeat;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class AudioSelector extends RecyclerView {

    private Context mContext;
    private LinearLayoutManager layoutManager;
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
            int first = layoutManager.findFirstVisibleItemPosition();
            if (first == selectedPosition && selectedPosition > 0) {
                smoothScrollToPosition(selectedPosition - 1);
            } else {
                smoothScrollToPosition(selectedPosition + 1);
            }
        }

        private void onClickItem(View view, int position) {
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

        @SuppressLint("UseCompatLoadingForDrawables")
        private void highlight(View view) {
            view.setBackground(mContext.getDrawable(R.drawable.audio_item_checked_shape));
            ((TextView) view).setTextColor(mContext.getColor(R.color.blue_700));
        }

        @SuppressLint("UseCompatLoadingForDrawables")
        private void resetItem(View view) {
            view.setBackground(mContext.getDrawable(R.drawable.audio_item_normal_shape));
            ((TextView) view).setTextColor(mContext.getColor(R.color.gray_300));
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            TextView textView = holder.itemView.findViewById(R.id.audioSelectorItemName);
            textView.setText(audioList.get(holder.getAdapterPosition()));
            textView.setOnClickListener(v -> onClickItem(v, holder.getAdapterPosition()));
            if (holder.getAdapterPosition() == originPosition) {
                highlight(textView);
                lastItem = textView;
                selectedPosition = holder.getAdapterPosition();
            }
        }

        @Override
        public int getItemCount() {
            return audioList.size();
        }
    };

    private void Construct(Context context) {
        mContext = context;
//        setBackgroundColor(Color.BLUE);
        layoutManager = new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL,false);
        setLayoutManager(layoutManager);
        layoutManager.setOrientation(RecyclerView.HORIZONTAL);
        setAdapter(adapter);
    }

    public void bindData(int position, List<String> list, OnValueChangeListener onValueChangeListener) {
        listener = onValueChangeListener;
        audioList = list;
        originPosition = position;
        new Handler(mContext.getMainLooper()).postDelayed(() -> smoothScrollToPosition(originPosition + 1), 100);
    }
}
