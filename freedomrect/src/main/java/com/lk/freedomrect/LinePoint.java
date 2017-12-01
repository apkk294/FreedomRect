package com.lk.freedomrect;

import android.graphics.Point;

/**
 * 创建者： user005
 * 时间：2017/11/24
 * Description：记录每条线的左右两点坐标和中心点坐标.
 */

public class LinePoint {
    public Point mLastPoint;
    public Point mNextPoint;
    public Point mPoint;

    public LinePoint(Point point, Point lastPoint, Point nextPoint) {
        mPoint = point;
        mLastPoint = lastPoint;
        mNextPoint = nextPoint;
    }
}
