/**
 * Copyright (c) 2015-present, Horcrux.
 * All rights reserved.
 *
 * This source code is licensed under the MIT-style license found in the
 * LICENSE file in the root directory of this source tree.
 */


package versioned.host.exp.exponent.modules.api.components.svg;

import android.graphics.BitmapShader;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;

import com.facebook.react.bridge.ReadableArray;

public class Brush {
    private BrushType mType = BrushType.LINEAR_GRADIENT;
    private ReadableArray mPoints;
    private ReadableArray mColors;
    private boolean mUseObjectBoundingBox;
    private Rect mUserSpaceBoundingBox;

    public Brush(BrushType type, ReadableArray points, BrushUnits units) {
        mType = type;
        mPoints = points;
        mUseObjectBoundingBox = units == BrushUnits.OBJECT_BOUNDING_BOX;
    }

    public enum BrushType {
        LINEAR_GRADIENT(0),
        RADIAL_GRADIENT(1),
        PATTERN(2);
        BrushType(int ni) {
            nativeInt = ni;
        }

        final int nativeInt;
    }

    public enum BrushUnits {
        OBJECT_BOUNDING_BOX(0),
        USER_SPACE_ON_USE(1);
        BrushUnits(int ni) {
            nativeInt = ni;
        }
        final int nativeInt;
    }

    private static void parseGradientStops(ReadableArray value, int stopsCount, float[] stops, int[] stopsColors, float opacity) {
        int startStops = value.size() - stopsCount;
        for (int i = 0; i < stopsCount; i++) {
            stops[i] = (float) value.getDouble(startStops + i);
            stopsColors[i] = Color.argb(
                    (int) (value.getDouble(i * 4 + 3) * 255 * opacity),
                    (int) (value.getDouble(i * 4) * 255),
                    (int) (value.getDouble(i * 4 + 1) * 255),
                    (int) (value.getDouble(i * 4 + 2) * 255));

        }
    }

    public void setUserSpaceBoundingBox(Rect userSpaceBoundingBox) {
        mUserSpaceBoundingBox = userSpaceBoundingBox;
    }

    public void setGradientColors(ReadableArray colors) {
        mColors = colors;
    }

    private RectF getPaintRect(RectF pathBoundingBox) {
        RectF rect = mUseObjectBoundingBox ? pathBoundingBox : new RectF(mUserSpaceBoundingBox);
        float width = rect.width();
        float height = rect.height();
        float x = 0f;
        float y = 0f;

        if (mUseObjectBoundingBox) {
            x = rect.left;
            y = rect.top;
        }

        return new RectF(x, y, x + width, y + height);
    }

    public void setupPaint(Paint paint, RectF pathBoundingBox, float scale, float opacity) {
        RectF rect = getPaintRect(pathBoundingBox);
        float width = rect.width();
        float height = rect.height();
        float offsetX = rect.left;
        float offsetY = rect.top;

        int stopsCount = mColors.size() / 5;
        int[] stopsColors = new int[stopsCount];
        float[] stops = new float[stopsCount];
        parseGradientStops(mColors, stopsCount, stops, stopsColors, opacity);

        if (mType == BrushType.LINEAR_GRADIENT) {
            float x1 = PropHelper.fromPercentageToFloat(mPoints.getString(0), width, offsetX, scale);
            float y1 = PropHelper.fromPercentageToFloat(mPoints.getString(1), height, offsetY, scale);
            float x2 = PropHelper.fromPercentageToFloat(mPoints.getString(2), width, offsetX, scale);
            float y2 = PropHelper.fromPercentageToFloat(mPoints.getString(3), height, offsetY, scale);
            paint.setShader(
                    new LinearGradient(
                            x1,
                            y1,
                            x2,
                            y2,
                            stopsColors,
                            stops,
                            Shader.TileMode.CLAMP));
        } else if (mType == BrushType.RADIAL_GRADIENT) {
            float rx = PropHelper.fromPercentageToFloat(mPoints.getString(2), width, 0f, scale);
            float ry = PropHelper.fromPercentageToFloat(mPoints.getString(3), height, 0f, scale);
            float cx = PropHelper.fromPercentageToFloat(mPoints.getString(4), width, offsetX, scale);
            float cy = PropHelper.fromPercentageToFloat(mPoints.getString(5), height, offsetY, scale) / (ry / rx);
            // TODO: support focus point.
            //float fx = PropHelper.fromPercentageToFloat(mPoints.getString(0), width, offsetX, scale);
            //float fy = PropHelper.fromPercentageToFloat(mPoints.getString(1), height, offsetY, scale) / (ry / rx);
            Shader radialGradient = new RadialGradient(
                    cx,
                    cy,
                    rx,
                    stopsColors,
                    stops,
                    Shader.TileMode.CLAMP
            );

            Matrix radialMatrix = new Matrix();
            radialMatrix.preScale(1f, ry / rx);
            radialGradient.setLocalMatrix(radialMatrix);
            paint.setShader(radialGradient);
        } else {
            // todo: pattern support

            //Shader mShader1 = new BitmapShader(bitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
            //paint.setShader(mShader1);
            //bitmap.recycle();
        }
    }
}
