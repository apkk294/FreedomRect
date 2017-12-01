package com.lk.freedomrect;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * 创建者： user005
 * 时间：2017/11/24
 * Description：可自由伸缩大小，添加顶点的View.
 * 待优化：将绘制线和点放到一个for循环中，现在放到一个循环中会出现
 * 效果上 线是从点的中心开始绘制的，需要的效果是线从点的边缘开始绘制
 * 所以现在只能分两次绘制，把点绘制到线的上面，看上去效果是一样的
 */

public class FreedomRectView extends View {
    private static final String TAG = "RectView";

    private int   mDefaultLineLength;
    private Point mCenterPoint;

    //线相关
    private int mBorderWidth;
    private int mBorderColor = 0xFF3F51B5;
    private Paint mBorderPaint;

    //中心点相关的参数
    private int mPointRadius = 15;
    private int mPointColor  = 0xFFFF4081;
    private Bitmap mAddPointBitmap;
    private Paint  mAddPointPaint;
    private Paint  mPointPaint;

    //保存顶点坐标
    private ArrayList<Point>     mPoints     = new ArrayList<>();
    //保存每条线中点
    private ArrayList<LinePoint> mLinePoints = new ArrayList<>();

    //手指按下的时候的坐标
    private int mDownX, mDownY;
    //点击的顶点坐标
    private int mClickPointIndex;
    //点击的中心点下标
    private int mClickCenterPointIndex;

    private              int MODE              = 0x00000000;
    private              int MODE_NONE         = 0x00000000;
    private static final int MODE_POINT        = 0x000000aa;
    private static final int MODE_CENTER_POINT = 0x000000bb;
    private static final int MODE_INSIDE       = 0x000000cc;

    public FreedomRectView(Context context) {
        super(context);
        initParams();
    }

    public FreedomRectView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initParams();
    }

    public FreedomRectView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initParams();
    }

    private void initParams() {
        mDefaultLineLength = dip2px(getContext(), 100);

        mBorderWidth = dip2px(getContext(), 1);
        mBorderPaint = new Paint();
        mBorderPaint.setStyle(Paint.Style.STROKE);
        mBorderPaint.setColor(mBorderColor);
        mBorderPaint.setStrokeWidth(mBorderWidth);

        mPointRadius = dip2px(getContext(), mPointRadius);
        mPointPaint = new Paint();
        mPointPaint.setStyle(Paint.Style.FILL);
        mPointPaint.setColor(mPointColor);
        mPointPaint.setStrokeWidth(mPointRadius);
        mAddPointBitmap = BitmapFactory.decodeResource(
                getContext().getResources(),
                R.drawable.add_point);
        mAddPointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mAddPointPaint.setFilterBitmap(true);
        mAddPointPaint.setDither(true);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mCenterPoint = new Point(w / 2, h / 2);

        Point leftTop = new Point(mCenterPoint.x - mDefaultLineLength / 2, mCenterPoint.y - mDefaultLineLength / 2);
        Point rightTop = new Point(mCenterPoint.x + mDefaultLineLength / 2, mCenterPoint.y - mDefaultLineLength / 2);
        Point leftBottom = new Point(mCenterPoint.x - mDefaultLineLength / 2, mCenterPoint.y + mDefaultLineLength / 2);
        Point rightBottom = new Point(mCenterPoint.x + mDefaultLineLength / 2, mCenterPoint.y + mDefaultLineLength / 2);
        if (mPoints.size() == 0) {
            mPoints.add(leftTop);
            mPoints.add(rightTop);
            mPoints.add(rightBottom);
            mPoints.add(leftBottom);
        }
    }

    @SuppressLint("DrawAllocation")
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawLinePath(canvas);
        drawCenterBitmapAndPoint(canvas);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mDownX = (int) event.getX();
                mDownY = (int) event.getY();
                checkMode(mDownX, mDownY);
                break;
            case MotionEvent.ACTION_MOVE:
                int newX, newY;
                newX = (int) event.getX();
                newY = (int) event.getY();
                switch (MODE) {
                    case MODE_POINT:
                        mPoints.get(mClickPointIndex).x = newX;
                        mPoints.get(mClickPointIndex).y = newY;
                        postInvalidate();
                        break;
                    case MODE_CENTER_POINT:
                        break;
                }
                checkMode(newX, newY);
                break;
            case MotionEvent.ACTION_UP:
                Log.d(TAG, "onTouchEvent: action_up");
                switch (MODE) {
                    case MODE_POINT:
                        break;
                    case MODE_CENTER_POINT:
                        Log.d(TAG, "onTouchEvent: click center point up");
                        addPoint();
                        break;
                }
                break;
        }
        return true;
    }

    @Override
    public void postInvalidate() {
        mLinePoints.clear();
        super.postInvalidate();
    }

    /**
     * 添加顶点
     */
    private void addPoint() {
        mPoints.add(
                mClickCenterPointIndex,
                calculateNewPointCoordinate(mLinePoints.get(mClickCenterPointIndex)));
        postInvalidate();
    }

    /**
     * 计算新添加顶点坐标
     * 已知等边三角形两个点坐标，求第三个点的坐标
     * 参考：https://www.zybang.com/question/1854d2f4aefd66ca6174278ec1961e93.html
     * 计算两点连线的斜角
     * 参考：http://baixiaozhe.iteye.com/blog/914154
     *
     * @param linePoint 中心点
     * @return 新添加顶点坐标
     */
    private Point calculateNewPointCoordinate(LinePoint linePoint) {
        double xPow = Math.pow(linePoint.mNextPoint.x - linePoint.mLastPoint.x, 2);
        double yPow = Math.pow(linePoint.mNextPoint.y - linePoint.mLastPoint.y, 2);
        //计算已知两点之间的距离
        int leftRightDistance = (int) Math.sqrt(xPow + yPow);
        //计算已知两个点的斜角
        double a = Math.atan2(linePoint.mNextPoint.y - linePoint.mLastPoint.y,
                linePoint.mNextPoint.x - linePoint.mLastPoint.x);
        //因为根据两点计算等边三角形的第三点有两种可能，即Math.PI / 3和-Math.PI / 3
        int x3 = (int) (linePoint.mLastPoint.x +
                leftRightDistance * Math.cos(a + (-Math.PI / 3)));
        int y3 = (int) (linePoint.mLastPoint.y +
                leftRightDistance * Math.sin(a + (-Math.PI / 3)));
        return new Point(x3, y3);
    }

    /**
     * 判断点击的是顶点还是中心点
     */
    private void checkMode(int x, int y) {
        //是否是顶点
        int pointSize = mPoints.size();
        for (int i = 0; i < pointSize; i++) {
            if (isTouchPoint(x, y, mPoints.get(i))) {
                Log.d(TAG, "click point " + i);
                MODE = MODE_POINT;
                mClickPointIndex = i;
                return;
            }
        }
        //是否是中心点
        for (int i = 0; i < mLinePoints.size(); i++) {
            if (isTouchPoint(x, y, mLinePoints.get(i).mPoint)) {
                Log.d(TAG, "click line's center point " + i);
                MODE = MODE_CENTER_POINT;
                mClickCenterPointIndex = i;
                return;
            }
        }
        MODE = MODE_NONE;
    }

    /**
     * 判断坐标是否在指定点内
     *
     * @param touchX X坐标
     * @param touchY Y坐标
     * @param point  指定点，判断是否在这个点内
     * @return 坐标在指定点内返回true，反之false
     */
    private boolean isTouchPoint(int touchX, int touchY, Point point) {
        double distance = Math.sqrt(Math.pow(point.x - touchX, 2) + Math.pow(point.y - touchY, 2));
        return distance < mPointRadius;
    }

    /**
     * 绘制每条线中间的添加图片和顶点
     *
     * @param canvas 画布
     */
    private void drawCenterBitmapAndPoint(Canvas canvas) {
        int pointSize = mPoints.size();
        Rect resRect = new Rect(0, 0, mAddPointBitmap.getWidth(), mAddPointBitmap.getHeight());
        for (int i = 0; i < pointSize; i++) {
            Point lastPoint;
            Point nextPoint;
            if (i == 0) {   //最后一个点和第一个点之间的中间点
                lastPoint = mPoints.get(pointSize - 1);
                nextPoint = mPoints.get(0);
            } else {
                lastPoint = mPoints.get(i - 1);
                nextPoint = mPoints.get(i);
            }
            //中间点坐标x、y = 两点坐标x、y坐标分别相加 / 2
            Point centerPoint = new Point((lastPoint.x + nextPoint.x) / 2, (lastPoint.y + nextPoint.y) / 2);
            LinePoint linePoint = new LinePoint(centerPoint, lastPoint, nextPoint);
            RectF destRect = new RectF(
                    centerPoint.x - mPointRadius,
                    centerPoint.y - mPointRadius,
                    centerPoint.x + mPointRadius,
                    centerPoint.y + mPointRadius);
            canvas.drawBitmap(mAddPointBitmap, resRect, destRect, mAddPointPaint);
            mLinePoints.add(linePoint);
            //绘制顶点
            canvas.drawCircle(nextPoint.x, nextPoint.y, mPointRadius, mPointPaint);
        }
    }

    /**
     * 绘制线
     *
     * @param canvas 画布
     */
    private void drawLinePath(Canvas canvas) {
        int pointSize = mPoints.size();
        Path borderPath = new Path();
        borderPath.moveTo(mPoints.get(0).x, mPoints.get(0).y);
        for (int i = 0; i < pointSize; i++) {
            Point point = mPoints.get(i);
            borderPath.lineTo(point.x, point.y);
            if (i == pointSize - 1) {
                borderPath.close();
                canvas.drawPath(borderPath, mBorderPaint);
            }
        }
        borderPath.close();
        canvas.drawPath(borderPath, mBorderPaint);
    }

    /**
     * 根据手机的分辨率从 dp 的单位 转成为 px(像素)
     */
    private int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    /**
     * 根据手机的分辨率从 px(像素) 的单位 转成为 dp
     */
    private int px2dip(Context context, float pxValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f);
    }

    public List<Point> getPoints() {
        return mPoints;
    }
}
