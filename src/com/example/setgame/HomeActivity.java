package com.example.setgame;

import java.util.Set;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class HomeActivity extends Activity {

  private Button mSinglePlayerButton, mMultiPlayerButton;

  private BluetoothAdapter mBluetoothAdapter;
  private Set<BluetoothDevice> mPairedDevices;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_home);

    mSinglePlayerButton = (Button) findViewById(R.id.singlePlayerButton);
    mMultiPlayerButton = (Button) findViewById(R.id.multiPlayerButton);

    mSinglePlayerButton.setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View v) {
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        intent.putExtra("Players", 1);
        intent.putExtra("PlayerNum", 1);
        startActivity(intent);
      }
    });

    mMultiPlayerButton.setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View v) {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        mPairedDevices = mBluetoothAdapter.getBondedDevices();

        if (mPairedDevices.size() == 0) {
          Toast.makeText(HomeActivity.this, "Pair with a bluetooth device first.",
              Toast.LENGTH_SHORT).show();
        } else {
          Intent intent = new Intent(getApplicationContext(), MultiPlayerActivity.class);
          startActivity(intent);
        }

      }
    });
  }


  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.home, menu);
    return true;
  }

}
