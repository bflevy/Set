package com.example.setgame;

import java.util.List;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.GridView;

public class CardAdapter extends ArrayAdapter<Card> {

  private final Context mContext;

  public CardAdapter(Context context, int textViewResourceId, List<Card> objects) {
    super(context, textViewResourceId, objects);
    mContext = context;
  }

  // create a new ImageView for each item referenced by the Adapter
  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    FieldCard cardView;
    if (convertView == null) { // if it's not recycled, initialize some attributes
      cardView = new FieldCard(mContext, getItem(position));
      cardView.setLayoutParams(new GridView.LayoutParams(200, 100));
    } else {
      cardView = (FieldCard) convertView;
      cardView.updateCard(getItem(position));
    }
    cardView.invalidate();
    return cardView;
  }
}
