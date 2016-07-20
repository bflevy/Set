package com.example.setgame;

import android.graphics.Color;

public class Card {

  private int num;
  private Shape shape;
  private Pattern pattern;
  private int color;

  private boolean mPressed = false;
  private boolean mLocked = false;

  public enum Shape {
    circle, triangle, square
  };
  public enum Pattern {
    clear, striped, solid
  }

  public static int[] colors = {Color.RED, Color.BLUE, Color.GREEN};

  public Card(int num, Shape shape, Pattern pattern, int color) {
    this.num = num;
    this.shape = shape;
    this.pattern = pattern;
    this.color = color;
  }

  // random card
  public Card() {
    num = (int) (Math.random() * 3) + 1;
    shape = Shape.values()[(int) (Math.random() * 3)];
    pattern = Pattern.values()[(int) (Math.random() * 3)];
    color = colors[(int) (Math.random() * 3)];
  }

  public void togglePressed() {
    mPressed = !mPressed;
  }

  public void setPressed(boolean pressed) {
    mPressed = pressed;
  }

  public boolean isPressed() {
    return mPressed;
  }

  public void setLocked(boolean locked) {
    mLocked = locked;
  }

  public boolean getLocked() {
    return mLocked;
  }

  public int getNum() {
    return num;
  }

  public void setNum(int num) {
    this.num = num;
  }

  public Shape getShape() {
    return shape;
  }

  public void setShape(Shape shape) {
    this.shape = shape;
  }

  public Pattern getPattern() {
    return pattern;
  }

  public void setPattern(Pattern pattern) {
    this.pattern = pattern;
  }

  public int getColor() {
    return color;
  }

  public void setColor(int color) {
    this.color = color;
  }

}
