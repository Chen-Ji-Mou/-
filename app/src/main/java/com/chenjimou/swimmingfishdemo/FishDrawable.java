package com.chenjimou.swimmingfishdemo;

import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.view.animation.LinearInterpolator;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class FishDrawable extends Drawable {

    private Paint mPaint;
    private Path mPath;
    // 鱼的重心，即鱼躯干的中心
    private PointF keyPoint;
    // 鱼尾中圆的圆心
    private PointF fishTailMiddlePoint;
    // 鱼尾小圆的圆心
    private PointF fishTailSmallPoint;
    // 鱼头在未播放动画时的朝向角度（以鱼的重心建立坐标系，鱼头圆心与重心之间连线与x轴正方向的夹角角度）
    private float fishHeadAngle = 90;
    // 鱼头的半径，鱼其他部位的大小都依据鱼头的半径决定
    private float HEAD_RADIUS = 30;
    // 鱼身的长度
    private float BODY_LENGTH = 3.2f * HEAD_RADIUS;
    // 鱼尾大圆的半径
    private float TAIL_BIG_CIRCLE_RADIUS = 0.7f * HEAD_RADIUS;
    // 鱼尾中圆的半径
    private float TAIL_MIDDLE_CIRCLE_RADIUS = 0.42f * HEAD_RADIUS;
    // 鱼尾小圆半径
    private float TAIL_SMALL_CIRCLE_RADIUS = 0.168f * HEAD_RADIUS;
    // 属性动画当前的值
    private float currentValue = 0;

    @IntDef(flag = true, value = {SIDE_LEFT, SIDE_RIGHT})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Side { }

    private static final int SIDE_LEFT = 0;
    private static final int SIDE_RIGHT = 1;

    public FishDrawable() {
        init();
    }

    private void init() {
        // 初始化
        mPath = new Path();
        mPaint = new Paint();
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setARGB(110, 244, 92, 71);
        // 鱼的重心也是用鱼头半径表示
        keyPoint = new PointF(5.324f * HEAD_RADIUS, 5.324f * HEAD_RADIUS);

        // 使用属性动画来实现鱼在原地不断摆动
        ValueAnimator valueAnimator = ValueAnimator.ofFloat(0, 360);
        // 设置播放时间
        valueAnimator.setDuration(1000);
        // 设置重复次数（无限次）
        valueAnimator.setRepeatCount(ValueAnimator.INFINITE);
        // 设置重复模式
        // RESTART：重新开始重复，REVERSE：反转重复
        valueAnimator.setRepeatMode(ValueAnimator.RESTART);
        // 设置插值器（线性）
        valueAnimator.setInterpolator(new LinearInterpolator());
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                currentValue = (float) animation.getAnimatedValue();
                invalidateSelf();
            }
        });
        // 开始播放
        valueAnimator.start();
    }

    @Override
    public void draw(@NonNull Canvas canvas) {

        // 鱼头的朝向角度随属性动画的值变化 --> 摆动方向：先左后右
        float headAngle = (float) (fishHeadAngle + Math.sin(Math.toRadians(currentValue)) * 10);

        // 鱼头的圆心坐标
        PointF headPoint = calculatePoint(keyPoint, BODY_LENGTH / 2, headAngle);

        // 绘画鱼头
        canvas.drawCircle(headPoint.x, headPoint.y, HEAD_RADIUS, mPaint);

        // 画右鱼鳍
        PointF rightFinsPoint = calculatePoint(headPoint, 0.9f * HEAD_RADIUS, headAngle - 110);
        makeFins(canvas, rightFinsPoint, headAngle, SIDE_RIGHT);

        // 画左鱼鳍
        PointF leftFinsPoint = calculatePoint(headPoint, 0.9f * HEAD_RADIUS, headAngle + 110);
        makeFins(canvas, leftFinsPoint, headAngle, SIDE_LEFT);

        // 鱼尾大圆的圆心，也是躯干连接尾部的圆心
        PointF bodyBottomCenterPoint = calculatePoint(headPoint, BODY_LENGTH, headAngle - 180);

        // 画鱼的躯干
        makeBody(canvas, headPoint, bodyBottomCenterPoint, headAngle);

        // 鱼尾上部分的朝向角度随鱼头的朝向角度变化 --> 摆动方向：先右后左
        float tailUpperAngle = (float) (headAngle + Math.cos(Math.toRadians(currentValue)) * 15);

        // 画鱼尾的上部分
        makeSegment(canvas, bodyBottomCenterPoint, TAIL_BIG_CIRCLE_RADIUS, TAIL_MIDDLE_CIRCLE_RADIUS,
                tailUpperAngle, true);

        // 鱼尾下部分的朝向角度随鱼头的朝向角度变化 --> 摆动方向：先左后右
        float tailBottomAngle = (float) (headAngle + Math.sin(Math.toRadians(currentValue)) * 25);

        // 画鱼尾的下部分
        makeSegment(canvas, fishTailMiddlePoint, TAIL_MIDDLE_CIRCLE_RADIUS, TAIL_SMALL_CIRCLE_RADIUS,
                tailBottomAngle, false);

        // 鱼尾三角形底边的半长随属性动画的值变化
        float tailTriangleLength = (float) Math.abs(Math.sin(Math.toRadians(currentValue)) * TAIL_BIG_CIRCLE_RADIUS);

        // 鱼尾三角形的朝向角度随鱼尾下部分的朝向角度变化 --> 摆动方向：先左后右
        float tailTriangleAngle = (float) (headAngle + Math.sin(Math.toRadians(currentValue)) * 35);

        // 画鱼尾的三角形
        makeTriangle(canvas, fishTailSmallPoint, TAIL_MIDDLE_CIRCLE_RADIUS * 2.7f,
                tailTriangleLength, tailTriangleAngle);
        makeTriangle(canvas, fishTailSmallPoint, TAIL_MIDDLE_CIRCLE_RADIUS * 2.7f - 10,
                tailTriangleLength - 20, tailTriangleAngle);
    }

    /**
     * 使用二阶贝塞尔曲线绘制鱼的躯干
     * @param bodyUpperCenterPoint 躯干连接头部的圆心，也就是鱼头的圆心
     * @param bodyBottomCenterPoint 躯干连接尾部的圆心，也就是鱼尾大圆的圆心
     * @param angle 鱼头的朝向角度
     */
    private void makeBody(Canvas canvas, PointF bodyUpperCenterPoint, PointF bodyBottomCenterPoint, float angle) {

        // 躯干的四个顶点
        PointF topLeftPoint = calculatePoint(bodyUpperCenterPoint, HEAD_RADIUS, angle + 90);
        PointF topRightPoint = calculatePoint(bodyUpperCenterPoint, HEAD_RADIUS, angle - 90);
        PointF bottomLeftPoint = calculatePoint(bodyBottomCenterPoint, TAIL_BIG_CIRCLE_RADIUS,
                angle + 90);
        PointF bottomRightPoint = calculatePoint(bodyBottomCenterPoint, TAIL_BIG_CIRCLE_RADIUS,
                angle - 90);

        // 二阶贝塞尔曲线的控制点 --- 决定鱼的胖瘦
        PointF controlLeft = calculatePoint(bodyUpperCenterPoint, BODY_LENGTH * 0.56f,
                angle + 130);
        PointF controlRight = calculatePoint(bodyUpperCenterPoint, BODY_LENGTH * 0.56f,
                angle - 130);

        // 绘制躯干
        mPath.reset();
        mPath.moveTo(topLeftPoint.x, topLeftPoint.y);
        mPath.quadTo(controlLeft.x, controlLeft.y, bottomLeftPoint.x, bottomLeftPoint.y);
        mPath.lineTo(bottomRightPoint.x, bottomRightPoint.y);
        mPath.quadTo(controlRight.x, controlRight.y, topRightPoint.x, topRightPoint.y);

        // 设置躯干的透明度
        mPaint.setAlpha(160);

        canvas.drawPath(mPath, mPaint);
    }

    /**
     * 绘制鱼尾的三角形
     * @param topPoint 三角形的上顶点
     * @param findCenterLength 上顶点到底边的距离
     * @param findEdgeLength 底边中心点到左右两顶点的距离
     * @param angle 鱼头的朝向角度
     */
    private void makeTriangle(Canvas canvas, PointF topPoint, float findCenterLength,
                              float findEdgeLength, float angle) {
        // 三角形底边的中心点
        PointF centerPoint = calculatePoint(topPoint, findCenterLength, angle - 180);

        // 三角形底边的左右顶点
        PointF leftPoint = calculatePoint(centerPoint, findEdgeLength, angle + 90);
        PointF rightPoint = calculatePoint(centerPoint, findEdgeLength, angle - 90);

        // 画三角形，连接三角形的各顶点
        mPath.reset();
        mPath.moveTo(topPoint.x, topPoint.y);
        mPath.lineTo(leftPoint.x, leftPoint.y);
        mPath.lineTo(rightPoint.x, rightPoint.y);

        canvas.drawPath(mPath, mPaint);
    }

    /**
     * 绘制鱼尾部分（绘制梯形）
     * @param bottomCenterPoint 梯形下底圆的圆心
     * @param bigRadius 大圆的半径
     * @param smallRadius 小圆的半径
     * @param angle 鱼头的朝向角度
     * @param hasBigCircle 是否要绘制大圆（鱼尾分为两部分，只有上部分需要绘画大圆）
     */
    private void makeSegment(Canvas canvas, PointF bottomCenterPoint, float bigRadius,
                             float smallRadius, float angle, boolean hasBigCircle) {
        // 梯形上底圆的计算距离
        float upperCenterPointDistance;
        if (hasBigCircle){
            upperCenterPointDistance = TAIL_BIG_CIRCLE_RADIUS * (0.6f + 1);
        } else {
            upperCenterPointDistance = TAIL_MIDDLE_CIRCLE_RADIUS * (0.4f + 2.7f);
        }

        // 梯形上底圆的圆心
        PointF upperCenterPoint = calculatePoint(bottomCenterPoint,
                upperCenterPointDistance,
                angle - 180);

        if (hasBigCircle){
            fishTailMiddlePoint = upperCenterPoint;
        } else {
            fishTailSmallPoint = upperCenterPoint;
        }

        // 梯形的四个顶点
        PointF bottomLeftPoint = calculatePoint(bottomCenterPoint, bigRadius, angle + 90);
        PointF bottomRightPoint = calculatePoint(bottomCenterPoint, bigRadius, angle - 90);
        PointF upperLeftPoint = calculatePoint(upperCenterPoint, smallRadius, angle + 90);
        PointF upperRightPoint = calculatePoint(upperCenterPoint, smallRadius, angle - 90);

        if (hasBigCircle) {
            // 画大圆 --- 只在绘画上部分鱼尾的时候才绘画
            canvas.drawCircle(bottomCenterPoint.x, bottomCenterPoint.y, bigRadius, mPaint);
        }
        // 画小圆
        canvas.drawCircle(upperCenterPoint.x, upperCenterPoint.y, smallRadius, mPaint);

        // 画梯形，连接梯形的四个顶点
        mPath.reset();
        mPath.moveTo(upperLeftPoint.x, upperLeftPoint.y);
        mPath.lineTo(upperRightPoint.x, upperRightPoint.y);
        mPath.lineTo(bottomRightPoint.x, bottomRightPoint.y);
        mPath.lineTo(bottomLeftPoint.x, bottomLeftPoint.y);

        canvas.drawPath(mPath, mPaint);
    }

    /**
     * 使用二阶贝塞尔曲线绘画鱼鳍
     * @param startPoint 贝塞尔曲线的起始点
     * @param angle 鱼头的朝向角度
     * @param side 绘画的是哪边鱼鳍
     */
    private void makeFins(Canvas canvas, PointF startPoint, float angle, @Side int side) {
        // 控制点的角度
        float controlAngle = 115;
        // 鱼鳍的终点 --- 二阶贝塞尔曲线的终点
        PointF endPoint = calculatePoint(startPoint, 1.3f * HEAD_RADIUS, angle - 180);
        // 计算控制点
        PointF controlPoint = calculatePoint(startPoint, 1.3f * HEAD_RADIUS * 1.8f,
                side == 1 ? angle - controlAngle : angle + controlAngle);
        // 绘制
        mPath.reset();
        // 将画笔移动到起始点
        mPath.moveTo(startPoint.x, startPoint.y);
        // 二阶贝塞尔曲线
        mPath.quadTo(controlPoint.x, controlPoint.y, endPoint.x, endPoint.y);
        canvas.drawPath(mPath, mPaint);
    }

    /**
     * 以参照点建立坐标系，根据距离和角度，计算出目标点
     * @param referencePoint 参照点
     * @param distance 目标点与参照点的距离
     * @param angle 以参照点建立坐标系，目标点与参照点之间连线与x轴正方向的夹角角度
     * @return 目标点
     */
    private PointF calculatePoint(PointF referencePoint, float distance, float angle){
        // 根据cos函数计算出目标点的x坐标
        float deltaX = (float) (Math.cos(Math.toRadians(angle)) * distance);
        // 根据sin函数计算出目标点的y坐标
        // （因为JDK中三角函数坐标系的y轴与Android屏幕坐标轴的y轴相反，所以角度需要减180）
        float deltaY = (float) (Math.sin(Math.toRadians(angle - 180)) * distance);
        return new PointF(referencePoint.x + deltaX, referencePoint.y + deltaY);
    }

    @Override
    public void setAlpha(int alpha) {
        mPaint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        mPaint.setColorFilter(colorFilter);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    /**
     * 设置Drawable的宽度
     */
    @Override
    public int getIntrinsicWidth() {
        return (int) (10.648f * HEAD_RADIUS);
    }

    /**
     * 设置Drawable的高度
     */
    @Override
    public int getIntrinsicHeight() {
        return (int) (10.648f * HEAD_RADIUS);
    }
}
