package com.example.com.refreshloadlistview;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * Created by grace on 2018/4/20.
 */

public class LoadListView extends ListView implements AbsListView.OnScrollListener{
    private View mFooterView;
    private ImageView mArrowIv;
    private ProgressBar mRefreshPb;
    private TextView mTipTv;

    private int firstVisibleItem;
    private int totalItemCount;

    private int startY;
    private int scrollSpace;
    private boolean isPlayPullAnim = false;
    private int measureHeight;
    private boolean isRemake = false;

    private OnLoadDataListener mOnLoadDataListener;

    private int state = 0;
    private static final int NONE = 0;
    private static final int PULL = 1;
    private static final int RELEASE = 2;
    private static final int REFRESHING = 3;

    public LoadListView(Context context) {
        super(context);
        initView();
    }

    public LoadListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public LoadListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView();
    }

    private void initView(){
        mFooterView = LayoutInflater.from(getContext()).inflate(R.layout.refresh, null);
        mArrowIv = (ImageView)mFooterView.findViewById(R.id.refresh_arrow);
        mRefreshPb = (ProgressBar)mFooterView.findViewById(R.id.refresh_progress);
        mTipTv = (TextView)mFooterView.findViewById(R.id.refresh_tip);

        measureView(mFooterView);
        measureHeight = mFooterView.getMeasuredHeight();
        setHeight(-measureHeight);
        addFooterView(mFooterView);
        refreshViewByState();

        setOnScrollListener(this);
    }

    private void measureView(View view){
        ViewGroup.LayoutParams lp = view.getLayoutParams();
        if(lp == null){
            lp = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }
        int width = ViewGroup.getChildMeasureSpec(0, 0, lp.width);
        int height = lp.height;
        if(height > 0){
            height = View.MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
        }else{
            height = View.MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        }
        view.measure(width, height);
    }

    private void setHeight(int paddingTop){
        if(paddingTop > 0){
            return;
        }
        mFooterView.setPadding(mFooterView.getPaddingLeft(), paddingTop, mFooterView.getPaddingRight(), mFooterView.getPaddingBottom());
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {

    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        this.firstVisibleItem = firstVisibleItem;
        this.totalItemCount = totalItemCount;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        switch (ev.getAction()){
            case MotionEvent.ACTION_DOWN:
                startY = (int)ev.getY();
                if((firstVisibleItem + getChildCount()) == totalItemCount) {
                    View lastView = getChildAt(getChildCount() - 1);
                    if (lastView.getBottom() == getHeight()) {
                        isRemake = true;
                    }
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if(isRemake){
                    scrollSpace = startY - (int)ev.getY();
                    setHeight(-measureHeight + scrollSpace);
                    switch (state){
                        case NONE:
                            if(scrollSpace > 0){
                                state = PULL;
                                refreshViewByState();
                            }
                            break;
                        case PULL:
                            if(scrollSpace - measureHeight > 0){
                                state = RELEASE;
                                refreshViewByState();
                            }
                            break;
                        case RELEASE:
                            if(scrollSpace > measureHeight){
                                startY -= scrollSpace - measureHeight;
                            }
                            if(scrollSpace - measureHeight < 0){
                                state = PULL;
                                isPlayPullAnim = true;
                                refreshViewByState();
                            }
                            break;
                        case REFRESHING:

                            break;
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                if(isRemake){
                    isRemake = false;
                    if(state == RELEASE){
                        state = REFRESHING;
                        refreshViewByState();
                        //load data
                        if(mOnLoadDataListener != null){
                            mOnLoadDataListener.onLoad();
                        }else{
                            loadComplete();
                        }
                    }else{
                        state = NONE;
                        refreshViewByState();
                    }
                }
                break;
        }
        return super.onTouchEvent(ev);
    }

    public void loadComplete(){
        state = NONE;
        refreshViewByState();
    }

    private void refreshViewByState(){
        switch (state){
            case NONE:
                mArrowIv.setVisibility(View.VISIBLE);
                mRefreshPb.setVisibility(View.GONE);
                setHeight(-measureHeight);
                break;
            case PULL:
                if(isPlayPullAnim){
                    Animation animation = AnimationUtils.loadAnimation(getContext(), R.anim.arrow_rotate_to_down);
                    mArrowIv.clearAnimation();
                    mArrowIv.setAnimation(animation);
                    isPlayPullAnim = false;
                }
                mTipTv.setText("上拉加载");
                break;
            case RELEASE:
                Animation animation = AnimationUtils.loadAnimation(getContext(), R.anim.arrow_rotate_to_up);
                mArrowIv.clearAnimation();
                mArrowIv.setAnimation(animation);
                mTipTv.setText("松开加载");
                break;
            case REFRESHING:
                mArrowIv.clearAnimation();
                mArrowIv.setVisibility(View.GONE);
                mRefreshPb.setVisibility(View.VISIBLE);
                mTipTv.setText("正在加载...");
                break;
        }
    }

    public void setOnLoadDataListener(OnLoadDataListener listener){
        mOnLoadDataListener = listener;
    }

    interface OnLoadDataListener {
        void onLoad();
    }
}
