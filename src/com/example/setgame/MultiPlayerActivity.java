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

public class MultiPlayerActivity extends Activity {

  private Button mJoinButton, mHostButton;

  private BluetoothAdapter mBluetoothAdapter;
  private Set<BluetoothDevice> mPairedDevices;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_multi_player);

    mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    mPairedDevices = mBluetoothAdapter.getBondedDevices();

    if (mPairedDevices.size() == 0) {

    }

    mJoinButton = (Button) findViewById(R.id.joinButton);
    mHostButton = (Button) findViewById(R.id.hostButton);

    mJoinButton.setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View v) {
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        intent.putExtra("Players", 2);
        intent.putExtra("PlayerNum", 2);
        startActivity(intent);
      }
    });

    mHostButton.setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View v) {
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        intent.putExtra("Players", 2);
        intent.putExtra("PlayerNum", 1);
        startActivity(intent);
      }
    });
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.multi_player, menu);
    return true;
  }

}
