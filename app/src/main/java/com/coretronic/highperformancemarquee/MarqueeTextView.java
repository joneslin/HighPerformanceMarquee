package com.coretronic.highperformancemarquee;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.os.Process;
import android.util.AttributeSet;
import android.view.MotionEvent;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;


public class MarqueeTextView extends RecyclerView {

    public static final float TEXT_MAX_SIZE =200;
    private static final int HANDLER_MESSAGE_ID_SCROLL = 1;
    private static final int HANDLER_MESSAGE_ID_TWINK = 2;
    private Context context;
    private LinearLayoutManager linearLayoutManager;
    private int endPosition;
    private AtomicBoolean shouldStop = new AtomicBoolean(true);
    private boolean isTwinkling;
    private int textSpeed = 30;

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case HANDLER_MESSAGE_ID_SCROLL:
                    MarqueeTextView.this.smoothScrollToPosition(100);
                    break;
                case HANDLER_MESSAGE_ID_TWINK:
                    MarqueeTextView.this.setVisibility(
                            MarqueeTextView.this.getVisibility() == VISIBLE ?
                                    INVISIBLE : VISIBLE
                    );
                    break;
            }
        }
    };

    public MarqueeTextView(Context context) {
        super(context);
        this.context = context;
    }

    public MarqueeTextView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
    }

    public MarqueeTextView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.context = context;
    }

    public void initialize(final String text, final int textSpeed, final int textColor, final boolean isBold, final boolean isTwinkling) {
        this.isTwinkling = isTwinkling;
        this.textSpeed = textSpeed;

        setHasFixedSize(true);
        setHorizontalScrollBarEnabled(false);
        setVerticalScrollBarEnabled(false);
        setNestedScrollingEnabled(false);


        CustomLinearLayoutManager.MILLISECONDS_PER_INCH = textSpeed * 10;
        linearLayoutManager = new CustomLinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false);
        setLayoutManager(linearLayoutManager);

        int textLength = text.length();
        int subTextLength = 5;
        final List<String> dataset = new ArrayList<>();
        dataset.add(" ");   // leading padding
        for (int n = 0; n < textLength; n += subTextLength)
            dataset.add(text.substring(n, Math.min(n + subTextLength, textLength)));
        dataset.add(" ");   // trailing padding
        endPosition = dataset.size() - 1;

        this.post(new Runnable() {
            @Override
            public void run() {
                int viewHeight = getHeight();
                float density = getResources().getDisplayMetrics().density;
                float textSize = Math.min(viewHeight / (density * 1.3f), TEXT_MAX_SIZE);

                MarqueeTextAdapter adapter = new MarqueeTextAdapter(dataset, textSize, textColor, isBold);
                setAdapter(adapter);
            }
        });
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        start();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stop();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_MOVE)
            return true;

        return super.dispatchTouchEvent(ev);
    }

    @Override
    public void onScrolled(int dx, int dy) {
        super.onScrolled(dx, dy);
        if (linearLayoutManager.findLastCompletelyVisibleItemPosition() == endPosition) {
            MarqueeTextView.this.scrollToPosition(0);
        }
    }

    public void start() {
        if (shouldStop.get()) {
            shouldStop.set(false);
            new Thread() {
                public void run() {
                    android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                    while (!shouldStop.get()) {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        Message msg = handler.obtainMessage();
                        msg.what = HANDLER_MESSAGE_ID_SCROLL;
                        msg.sendToTarget();
                    }
                }
            }.start();

            if (isTwinkling) {
                new Thread() {
                    @Override
                    public void run() {
                        while (!shouldStop.get()) {
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            Message msg = handler.obtainMessage();
                            msg.what = HANDLER_MESSAGE_ID_TWINK;
                            msg.sendToTarget();
                        }
                    }
                }.start();
            }
        }
    }

    public void stop() {
        shouldStop.set(true);
    }

}
