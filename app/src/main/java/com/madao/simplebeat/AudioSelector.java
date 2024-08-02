package com.madao.simplebeat;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
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
    private LooperLayoutManager layoutManager;
    private List<String> audioList;
    private OnValueChangeListener listener;
    private int originPosition;
    private final static String Tag = "AudioSelector";

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
        setBackgroundColor(Color.RED);
        if (!compact) {
            getLayoutParams().width = (int) (unit * 9);
            getLayoutParams().height = (int) (unit * 3);
        }
        setAdapter(new RVAdapter(unit, compact, typeface));
//        scrollToPosition();
    }

    private class RVAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private View lastItem;
        private int selectedPosition = 0;

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
            if (compact) {
                int w = parent.getWidth() / 2;
                v.getLayoutParams().width = w;
                v.getLayoutParams().height = w;
            } else {
                v.getLayoutParams().width = (int) (unit * 3);
                v.getLayoutParams().height = (int) (unit * 3);
            }
            return new RecyclerView.ViewHolder(v) {};
        }

        public void scroll() {
//            int first = layoutManager.findFirstVisibleItemPosition();
//            if (first == selectedPosition && selectedPosition > 0) {
//                smoothScrollToPosition(selectedPosition - 1);
//            } else {
//                smoothScrollToPosition(selectedPosition + 1);
//            }
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
            Log.d(Tag, "onValueChange " + lastPosition + " " + position);
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
            if (typeface != null) {
                textView.setTypeface(typeface);
            }
//            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, unit * 0.8f);
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
    }

    public static class LooperLayoutManager extends RecyclerView.LayoutManager {

        @Override
        public RecyclerView.LayoutParams generateDefaultLayoutParams() {
            return new RecyclerView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        @Override
        public boolean canScrollHorizontally() {
            return true;
        }

        @Override
        public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
            Log.d(Tag, "onLayoutChildren");
            if (getItemCount() <= 0) {
                return;
            }
            if (state.isPreLayout()) {
                return;
            }
            detachAndScrapAttachedViews(recycler);

            int offset = 0;
            for (int i = 0; i < getItemCount(); i++) {
                View itemView = recycler.getViewForPosition(i);
                addView(itemView);
                measureChildWithMargins(itemView, 0, 0);
                int width = getDecoratedMeasuredWidth(itemView);
                int height = getDecoratedMeasuredHeight(itemView);
                layoutDecorated(itemView, offset, 0, offset + width, height);

                offset += width;
                if (offset > getWidth()) {
                    break;
                }
            }
        }

        @Override
        public int scrollHorizontallyBy(int dx, RecyclerView.Recycler recycler, RecyclerView.State state) {
            int travel = fill(dx, recycler, state);
            if (travel == 0) {
                return 0;
            }
            offsetChildrenHorizontal(-travel);
            recyclerHideView(dx, recycler, state);
            return travel;
        }

        private int fill(int dx, RecyclerView.Recycler recycler, RecyclerView.State state) {
            if (getChildCount() == 0) return 0;
            if (dx > 0) {
                View lastView = getChildAt(getChildCount() - 1);
                if (lastView == null) {
                    return 0;
                }
                int lastPos = getPosition(lastView);
                if (lastView.getRight() < getWidth()) {
                    View scrap;
                    if (lastPos == getItemCount() - 1) {
                        scrap = recycler.getViewForPosition(0);
                    } else {
                        scrap = recycler.getViewForPosition(lastPos + 1);
                    }
                    addView(scrap);
                    measureChildWithMargins(scrap, 0, 0);
                    int width = getDecoratedMeasuredWidth(scrap);
                    int height = getDecoratedMeasuredHeight(scrap);
                    layoutDecorated(scrap,lastView.getRight(), 0,
                            lastView.getRight() + width, height);
                    return dx;
                }
            } else {
                View firstView = getChildAt(0);
                if (firstView == null) {
                    return 0;
                }
                int firstPos = getPosition(firstView);

                if (firstView.getLeft() >= 0) {
                    View scrap = null;
                    if (firstPos == 0) {
                        scrap = recycler.getViewForPosition(getItemCount() - 1);
                    } else {
                        scrap = recycler.getViewForPosition(firstPos - 1);
                    }
                    addView(scrap, 0);
                    measureChildWithMargins(scrap,0,0);
                    int width = getDecoratedMeasuredWidth(scrap);
                    int height = getDecoratedMeasuredHeight(scrap);
                    layoutDecorated(scrap, firstView.getLeft() - width, 0,
                            firstView.getLeft(), height);
                }
            }
            return dx;
        }

        private void recyclerHideView(int dx, RecyclerView.Recycler recycler, RecyclerView.State state) {
            for (int i = 0; i < getChildCount(); i++) {
                View view = getChildAt(i);
                if (view == null) {
                    continue;
                }
                if (dx > 0) {
                    if (view.getRight() < 0) {
                        removeAndRecycleView(view, recycler);
                    }
                } else {
                    if (view.getLeft() > getWidth()) {
                        removeAndRecycleView(view, recycler);
                    }
                }
            }
        }

    }

    private void Construct(Context context) {
        mContext = context;
//        setBackgroundColor(Color.BLUE);
        layoutManager = new LooperLayoutManager();
        setLayoutManager(layoutManager);
        setAdapter(new RVAdapter(64f, false, null));
    }

    public void bindData(int position, List<String> list, OnValueChangeListener onValueChangeListener) {
        listener = onValueChangeListener;
        audioList = list;
        originPosition = position;
        new Handler(mContext.getMainLooper()).postDelayed(() -> smoothScrollToPosition(originPosition + 1), 100);
    }

//    @Override
//    public void onScrollStateChanged(int state) {
//        if (state == SCROLL_STATE_DRAGGING) {
//            for (int i = 0; i < layoutManager.getChildCount(); i++) {
//                View itemView = layoutManager.getChildAt(i);
//                if (itemView != null) {
//                    itemView.setScaleX(1);
//                    itemView.setScaleY(1);
//                } else {
//                    Log.d(Tag, "child null at " + i);
//                }
//            }
//        } else if(state == SCROLL_STATE_IDLE){
//            int position = layoutManager.findFirstVisibleItemPosition();
//            View view = layoutManager.findViewByPosition(position);
//            View center = layoutManager.findViewByPosition(position + 1);
//            if (view == null) {
//                Log.w("bruce", "Error view at " + position);
//                return;
//            }
//            assert center != null;
//
//            int offset = view.getLeft();
//            int width = view.getWidth();
//
//            if(offset == 0) {
//
//                center.setScaleX(2);
//                center.setScaleY(2);
//
//            } else if(-offset < width / 2){
//                smoothScrollBy(offset, 0);
//            }
//            else {
//                smoothScrollBy(width + offset, 0);
//            }
//        }
//    }

}
