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

import static com.example.com.refreshloadlistview.R.layout.refresh;

/**
 * Created by grace on 2018/4/4.
 */

public class RefreshLoadListView extends ListView implements AbsListView.OnScrollListener {
    private static final String TAG = "RefreshLoadListView";

    private RefreshLoadHelper mRefreshLoadHelper;
    private boolean isRemake = false;//标记重写触摸事件
    private boolean isLastVisibleItem = false;//当前显示的最后一个item是否最后一个item
    private int mScrollState;
    private OnRefreshDataListener mRefreshDataListener;

    private int firstVisibleItem;
    private int totalItemCount;
    private int startY;
    private int movespace;

    private int scrollMode = 0;//滑动模式
    public static final int NONE_PULL = 0;//正常
    private static final int DOWN_PULL = 1;//下拉
    private static final int UP_PULL = 2;//上拉
    private int scrollState = 0;//滑动的状态
    private final int NONE = 0;//正常状态
    private final int PULL = 1;//下拉或上拉状态
    private final int RELEASE = 2;//释放刷新状态
    private final int REFRESHING = 3;//刷新状态

    public RefreshLoadListView(Context context) {
        super(context);
        initView();
    }

    public RefreshLoadListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public RefreshLoadListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView();
    }

    private void initView() {
        mRefreshLoadHelper = new RefreshLoadHelper();
        setOnScrollListener(this);
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        mScrollState = scrollState;
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        this.firstVisibleItem = firstVisibleItem;
        this.totalItemCount = totalItemCount;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                startY = (int) ev.getY();
                return true;
            case MotionEvent.ACTION_MOVE:
                if(scrollState == REFRESHING){
                    break;
                }
                movespace = startY - (int) ev.getY();
                if (movespace < 0 && firstVisibleItem == 0) {
                    scrollMode = DOWN_PULL;
                } else if (movespace > 0 && isLastVisibleItem) {
                    scrollMode = UP_PULL;
                } else {
                    scrollMode = NONE_PULL;
                    break;
                }
                isRemake = true;
                switch (scrollState) {
                    case NONE:
                        if (Math.abs(movespace) > 0) {
                            scrollState = PULL;
                            mRefreshLoadHelper.setPlayArrowPullAnim(false);
                            mRefreshLoadHelper.refreshViewByState();
                        }
                        break;
                    case PULL:
                        if (Math.abs(movespace) - mRefreshLoadHelper.getHeight() > 0) {
                            scrollState = RELEASE;
                            mRefreshLoadHelper.refreshViewByState();
                        }
                        break;
                    case RELEASE:
                        if(Math.abs(movespace) > mRefreshLoadHelper.getHeight() && scrollMode == UP_PULL){
                            startY -= movespace - mRefreshLoadHelper.getHeight();
                        }
                        if (Math.abs(movespace) - mRefreshLoadHelper.getHeight() < 0) {
                            scrollState = PULL;
                            mRefreshLoadHelper.setPlayArrowPullAnim(true);
                            mRefreshLoadHelper.refreshViewByState();
                        }
                        break;
                    case REFRESHING:
                        return super.onTouchEvent(ev);
                }
                mRefreshLoadHelper.setHeightByMovespace(Math.abs(movespace));
                if(scrollMode == DOWN_PULL){
                    return true;
                }
                break;
            case MotionEvent.ACTION_UP:
                isLastVisibleItem = false;
                int lastPos = firstVisibleItem + getChildCount();
                if (lastPos == totalItemCount) {
                    View lastView = getChildAt(getChildCount() - 1);
                    if (lastView.getBottom() == getHeight() && scrollState != RELEASE && scrollState != REFRESHING) {
                        isLastVisibleItem = true;
                    }
                }
                if (isRemake) {
                    isRemake = false;
                    mRefreshLoadHelper.setPlayArrowPullAnim(false);
                    if (scrollState == RELEASE) {
                        scrollState = REFRESHING;
                        mRefreshLoadHelper.refreshViewByState();
                    } else {
                        scrollState = NONE;
                        mRefreshLoadHelper.refreshViewByState();
                    }
                    return true;
                }
        }
        return super.onTouchEvent(ev);
    }

    private void onRefreshLoad() {
        if (mRefreshDataListener != null) {
            if (scrollMode == DOWN_PULL) {
                mRefreshDataListener.onRefresh();
            } else if (scrollMode == UP_PULL) {
                mRefreshDataListener.onLoad();
            }
        } else {
            refreshComplete();
        }
    }

    public void refreshComplete() {
        scrollState = NONE;
        mRefreshLoadHelper.refreshViewByState();
        scrollMode = NONE_PULL;
    }

    public void setOnRefreshDataListener(OnRefreshDataListener listener) {
        mRefreshDataListener = listener;
    }

    public interface OnRefreshDataListener {
        void onRefresh();

        void onLoad();
    }

    private class RefreshLoadHelper {
        RefreshLoadView refreshView;
        RefreshLoadView loadView;

        public RefreshLoadHelper() {
            refreshView = new RefreshLoadView(RefreshLoadView.MODE_REFRESH);
            loadView = new RefreshLoadView(RefreshLoadView.MODE_LOAD);
            addHeaderView(refreshView.rootView);
            addFooterView(loadView.rootView);
        }

        public void setHeight(int height) {
            if (scrollMode == DOWN_PULL) {
                refreshView.setHeight(height);
            } else if (scrollMode == UP_PULL) {
                if (height > 10) {
                    return;
                }
                loadView.setHeight(height);
            }
        }

        public int getHeight() {
            if (scrollMode == DOWN_PULL) {
                return refreshView.height;
            } else if (scrollMode == UP_PULL) {
                return loadView.height;
            }
            return 0;
        }

        public void setHeightByMovespace(int movespace) {
            if (scrollMode == DOWN_PULL) {
                refreshView.setHeight(-refreshView.height + movespace);
            } else if (scrollMode == UP_PULL) {
                int height = -loadView.height + movespace;
                if (height > 10) {
                    return;
                }
                loadView.setHeight(height);
            }
        }

        public void setPlayArrowPullAnim(boolean isPlay) {
            if (scrollMode == DOWN_PULL) {
                refreshView.isPlayArrowPullAnim = isPlay;
            } else if (scrollMode == UP_PULL) {
                loadView.isPlayArrowPullAnim = isPlay;
            }
        }

        public void refreshViewByState() {
            if (scrollMode == DOWN_PULL) {
                refreshView.refreshViewByState();
            } else if (scrollMode == UP_PULL) {
                loadView.refreshViewByState();
            }
        }
    }

    private class RefreshLoadView {
        View rootView;
        ImageView arrowIv;
        ProgressBar refreshPb;
        TextView tipTv;

        int height;
        boolean isPlayArrowPullAnim = false;//上拉或下拉时，是否播放箭头动画

        int mode = 0;
        static final int MODE_REFRESH = 0;
        static final int MODE_LOAD = 1;

        public RefreshLoadView(int mode) {
            this.mode = mode;
            rootView = LayoutInflater.from(getContext()).inflate(refresh, null);
            arrowIv = (ImageView) rootView.findViewById(R.id.refresh_arrow);
            refreshPb = (ProgressBar) rootView.findViewById(R.id.refresh_progress);
            tipTv = (TextView) rootView.findViewById(R.id.refresh_tip);

            measureView(rootView);
            height = rootView.getMeasuredHeight();
            setHeight(-height);
        }

        private void measureView(View view) {
            ViewGroup.LayoutParams lp = view.getLayoutParams();
            if (lp == null) {
                lp = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            }
            int width = ViewGroup.getChildMeasureSpec(0, 0, lp.width);
            int height = lp.height;
            if (height > 0) {
                height = View.MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
            } else {
                height = View.MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
            }
            view.measure(width, height);
        }

        public void setHeight(int height) {
            if (mode == MODE_REFRESH) {
                rootView.setPadding(rootView.getPaddingLeft(), height, rootView.getPaddingRight(), rootView.getPaddingBottom());
            } else {
                rootView.setPadding(rootView.getPaddingLeft(), rootView.getPaddingTop(), rootView.getPaddingRight(), height);
            }
            rootView.invalidate();
        }

        public void refreshViewByState() {
            switch (scrollState) {
                case NONE:
                    arrowIv.setVisibility(View.VISIBLE);
                    refreshPb.setVisibility(View.GONE);
                    setHeight(-height);
                    break;
                case PULL:
                    if (isPlayArrowPullAnim) {
                        Animation pullAnim;
                        if (mode == MODE_REFRESH) {
                            pullAnim = AnimationUtils.loadAnimation(getContext(), R.anim.arrow_rotate_to_down);
                        } else {
                            pullAnim = AnimationUtils.loadAnimation(getContext(), R.anim.arrow_rotate_to_up);
                        }
                        arrowIv.clearAnimation();
                        arrowIv.setAnimation(pullAnim);
                    }
                    if (mode == MODE_REFRESH) {
                        tipTv.setText("下拉刷新");
                    } else {
                        tipTv.setText("上拉加载");
                    }
                    break;
                case RELEASE:
                    Animation releaseAnim;
                    if (mode == MODE_REFRESH) {
                        releaseAnim = AnimationUtils.loadAnimation(getContext(), R.anim.arrow_rotate_to_up);
                        tipTv.setText("松开刷新");
                    } else {
                        releaseAnim = AnimationUtils.loadAnimation(getContext(), R.anim.arrow_rotate_to_down);
                        tipTv.setText("松开加载");
                    }
                    arrowIv.clearAnimation();
                    arrowIv.setAnimation(releaseAnim);
                    break;
                case REFRESHING:
                    arrowIv.setVisibility(View.GONE);
                    refreshPb.setVisibility(View.VISIBLE);
                    mRefreshLoadHelper.setHeight(10);
                    if (mode == MODE_REFRESH) {
                        tipTv.setText("正在刷新...");
                    } else {
                        tipTv.setText("正在加载...");
                    }
                    arrowIv.clearAnimation();
                    onRefreshLoad();
                    break;
            }
        }
    }
}
