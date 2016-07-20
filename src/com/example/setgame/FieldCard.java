package com.example.setgame;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;
import android.widget.ImageView;

import com.example.setgame.Card.Pattern;

public class FieldCard extends ImageView {

  private Card mCard;
  private Paint mShapePaint;
  private Paint mEdgePaint;
  private final Path mTrianglePath;

  private int mWidth;
  private int mHeight;

  private final Bitmap mCardBackground;
  private final Bitmap mCardBackgroundPressed;

  private boolean mTouched;

  public FieldCard(Context context, Card card) {
    super(context);
    mCard = card;
    init();
    mTrianglePath = new Path();

    mCardBackground = BitmapFactory.decodeResource(getResources(), R.drawable.card);
    mCardBackgroundPressed = BitmapFactory.decodeResource(getResources(), R.drawable.card_pressed);
  }

  public void updateCard(Card card) {
    mCard = card;
    init();
  }

  public void togglePressed() {
    mCard.togglePressed();
    this.invalidate();
  }

  public void setTouched(boolean touched) {
    mTouched = touched;
  }

  public boolean getTouched() {
    return mTouched;
  }

  public boolean getLocked() {
    return mCard.getLocked();
  }

  private void init() {
    mTouched = false;

    mShapePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    mShapePaint.setColor(mCard.getColor());

    switch (mCard.getPattern()) {
      case solid:
        mShapePaint.setStyle(Paint.Style.FILL_AND_STROKE);
        break;
      case striped:
        mShapePaint.setStyle(Paint.Style.FILL_AND_STROKE);
        mShapePaint.setAlpha(60);
        break;
      case clear:
        mShapePaint.setStyle(Paint.Style.STROKE);
        mShapePaint.setStrokeWidth(3);
        break;
      default:
        break;
    }

    mEdgePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    mEdgePaint.setColor(Color.BLACK);
    mEdgePaint.setStyle(Paint.Style.STROKE);
    mEdgePaint.setStrokeWidth(3);
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    mWidth = View.MeasureSpec.getSize(widthMeasureSpec);
    mHeight = View.MeasureSpec.getSize(heightMeasureSpec);

    setMeasuredDimension(mWidth, mHeight);
  }

  @Override
  protected void onDraw(Canvas canvas) {

    Bitmap cardBackground =
        mCard.isPressed() || mTouched || mCard.getLocked()
            ? mCardBackgroundPressed
            : mCardBackground;
    canvas.drawBitmap(cardBackground, (mWidth - mCardBackground.getWidth()) / 2,
        (mHeight - mCardBackground.getHeight()) / 2, null);

    paintShapes(canvas, mShapePaint);

    if (mCard.getPattern() == Pattern.striped) {
      paintShapes(canvas, mEdgePaint);
    }
  }

  private void paintShapes(Canvas canvas, Paint shapePaint) {
    int width = 40;
    int xSeperation = width + 10;
    int yOffset = (mHeight - width) / 2;

    int point1x, point1y, point2x, point2y, point3x, point3y;
    for (int i = 0; i < mCard.getNum(); i++) {
      int xOffset = (mWidth - mCard.getNum() * xSeperation) / 2;
      switch (mCard.getShape()) {
        case square:
          canvas.drawRect(xOffset + i * xSeperation, yOffset, xOffset + i * xSeperation + width,
              yOffset + width, shapePaint);
          break;
        case circle:
          canvas.drawCircle(xOffset + i * xSeperation + width / 2, yOffset + width / 2, width / 2,
              shapePaint);
          break;
        case triangle:
          point1x = xOffset + i * xSeperation + width / 2;
          point1y = yOffset;
          point2x = xOffset + i * xSeperation;
          point2y = yOffset + width;
          point3x = xOffset + i * xSeperation + width;
          point3y = yOffset + width;

          mTrianglePath.reset();
          mTrianglePath.setFillType(Path.FillType.EVEN_ODD);
          mTrianglePath.moveTo(point1x, point1y);
          mTrianglePath.lineTo(point2x, point2y);
          mTrianglePath.lineTo(point3x, point3y);
          mTrianglePath.lineTo(point1x, point1y);
          mTrianglePath.close();
          canvas.drawPath(mTrianglePath, shapePaint);
          break;
        default:
          break;
      }
    }
  }
}
