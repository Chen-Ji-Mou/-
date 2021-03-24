package com.chenjimou.swimmingfishdemo;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ImageView;
import android.widget.RelativeLayout;

public class FishSwimLayout extends RelativeLayout {

    private Paint mPaint;
    private ImageView ivFish;
    private FishDrawable fishDrawable;
    // 手指触摸点的坐标
    private float touchX;
    private float touchY;
    // 点击屏幕后的波纹效果的大小变化值
    private float ripple;
    // 点击屏幕后的波纹效果的透明度变化值
    private int alpha;

    public FishSwimLayout(Context context) {
        this(context, null);
    }

    public FishSwimLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FishSwimLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        // 设置ViewGroup会执行onDraw方法（因为ViewGroup默认不执行onDraw）
        setWillNotDraw(false);

        // 初始化画笔
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(8);

        // 给ImageView设置鱼的drawable
        ivFish = new ImageView(context);
        LayoutParams layoutParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        ivFish.setLayoutParams(layoutParams);
        ivFish.setBackgroundColor(Color.GREEN);
        fishDrawable = new FishDrawable();
        ivFish.setImageDrawable(fishDrawable);
        addView(ivFish);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        mPaint.setAlpha(alpha);
        canvas.drawCircle(touchX, touchY, ripple * 100, mPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        touchX = event.getX();
        touchY = event.getY();

        mPaint.setAlpha(100);

        ObjectAnimator.ofFloat(this, "ripple", 0, 1f)
                .setDuration(1000)
                .start();

        return super.onTouchEvent(event);
    }

    public float getRipple() {
        return ripple;
    }

    public void setRipple(float ripple) {
        alpha = (int) (100 * (1 - ripple));
        this.ripple = ripple;
        invalidate();
    }
}
