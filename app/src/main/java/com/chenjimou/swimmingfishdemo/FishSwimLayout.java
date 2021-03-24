package com.chenjimou.swimmingfishdemo;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.PointF;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import androidx.annotation.RequiresApi;

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
        fishDrawable = new FishDrawable();
        ivFish.setImageDrawable(fishDrawable);
        addView(ivFish);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // 设置透明度，实现波纹渐变消失
        mPaint.setAlpha(alpha);
        // 画波纹
        canvas.drawCircle(touchX, touchY, ripple * 100, mPaint);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public boolean onTouchEvent(MotionEvent event) {

        // 获取触摸点的坐标
        touchX = event.getX();
        touchY = event.getY();

        // 设置透明度为100
        mPaint.setAlpha(100);

        // 使用属性动画改变波纹变化值
        ObjectAnimator.ofFloat(this, "ripple", 0, 1f)
                .setDuration(1000)
                .start();

        // 让鱼转向游动到触摸点
        fishSwimming();

        return super.onTouchEvent(event);
    }

    /**
     * 使用三阶贝塞尔曲线绘制鱼的游动
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void fishSwimming() {

        // 鱼的重心：相对于Drawable的坐标
        PointF fishInsideKeyPoint = fishDrawable.getKeyPoint();

        // 鱼的重心：相对于布局的坐标（绝对坐标）
        PointF fishOutsideKeyPoint = new PointF(ivFish.getX() + fishInsideKeyPoint.x, ivFish.getY() + fishInsideKeyPoint.y);

        // 鱼头圆心的坐标：相对于布局的坐标（控制点1）
        final PointF fishOutsideHeadPoint = new PointF(ivFish.getX() + fishDrawable.getFishHeadPoint().x,
                ivFish.getY() + fishDrawable.getFishHeadPoint().y);

        // 点击坐标（结束点）
        PointF touchPoint = new PointF(touchX, touchY);

        float angle = includeAngle(fishOutsideKeyPoint, fishOutsideHeadPoint, touchPoint) / 2;
        float delta = includeAngle(fishOutsideKeyPoint, new PointF(fishOutsideKeyPoint.x + 1, fishOutsideKeyPoint.y), fishOutsideHeadPoint);

        // 控制点2的坐标
        PointF controlPoint = fishDrawable.calculatePoint(fishOutsideKeyPoint,
                fishDrawable.getHEAD_RADIUS() * 1.6f, angle + delta);

        // 计算出鱼游动路线的三阶贝塞尔曲线
        // 因为属性动画是作用在ivFish上，所以坐标需要减去相对于Drawable的坐标值
        Path path = new Path();
        path.moveTo(fishOutsideKeyPoint.x - fishInsideKeyPoint.x, fishOutsideKeyPoint.y - fishInsideKeyPoint.y);
        path.cubicTo(fishOutsideHeadPoint.x - fishInsideKeyPoint.x, fishOutsideHeadPoint.y - fishInsideKeyPoint.y,
                controlPoint.x - fishInsideKeyPoint.x, controlPoint.y - fishInsideKeyPoint.y,
                touchX - fishInsideKeyPoint.x, touchY - fishInsideKeyPoint.y);

        // 使用属性动画绘制鱼游动
        ObjectAnimator objectAnimator = ObjectAnimator.ofFloat(ivFish, "x", "y", path);
        objectAnimator.setDuration(2000);
        // 设置鱼游动时摆动频率加快
        objectAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                fishDrawable.setFrequence(1f);
            }

            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                fishDrawable.setFrequence(3f);
            }
        });
        // 设置鱼头的转向
        final PathMeasure pathMeasure = new PathMeasure(path, false);
        final float[] tan = new float[2];
        objectAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                // 执行了整个周期的百分之多少
                float fraction = animation.getAnimatedFraction();
                pathMeasure.getPosTan(pathMeasure.getLength() * fraction, null, tan);
                float angle = (float) Math.toDegrees(Math.atan2(-tan[1], tan[0]));
                fishDrawable.setFishHeadAngle(angle);
            }
        });
        objectAnimator.start();
    }

    /**
     * 算法：以O点为原点建立Android坐标系，知道O、A、B三点，计算出夹角AOB的角度
     * @return 夹角AOB的角度
     */
    private float includeAngle(PointF O, PointF A, PointF B) {
        float AOB = (A.x - O.x) * (B.x - O.x) + (A.y - O.y) * (B.y - O.y);
        float OALength = (float) Math.sqrt((A.x - O.x) * (A.x - O.x) + (A.y - O.y) * (A.y - O.y));
        float OBLength = (float) Math.sqrt((B.x - O.x) * (B.x - O.x) + (B.y - O.y) * (B.y - O.y));
        float cosAOB = AOB / (OALength * OBLength);
        float angleAOB = (float) Math.toDegrees(Math.acos(cosAOB));
        float direction = (A.y - B.y) / (A.x - B.x) - (O.y - B.y) / (O.x - B.x);
        if (direction == 0) {
            if (AOB >= 0) {
                return 0;
            } else {
                return 180;
            }
        } else {
            if (direction > 0) {
                return -angleAOB;
            } else {
                return angleAOB;
            }
        }
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
