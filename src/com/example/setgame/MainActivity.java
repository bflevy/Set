package com.example.setgame;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.util.Log;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.GridView;
import android.widget.TextView;

public class MainActivity extends Activity {

  List<Card> mDeck, mCardsOnField, mCards;
  CardAdapter mCardAdapter;
  Vibrator mVibrator;

  GridView mGridView;

  Button mAddCardsButton, mResetButton, mPauseButton;

  TextView mCardsLeftText, mTimePassedText, mScoreText, mScoreText2;

  private BluetoothAdapter mBluetoothAdapter;
  private Set<BluetoothDevice> mPairedDevices;

  private long mStartTime = 0;
  private long mPauseTime = 0;

  private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

  private int mScore = 0, mScore2;
  private int mPlayers, mPlayerNum;

  private static final int MESSAGE_READ = 1;

  private static final int MESSAGE_TYPE_SCORE = 1;
  private static final int MESSAGE_TYPE_DRAW = 2;
  private static final int MESSAGE_TYPE_REMOVE = 3;
  private static final int MESSAGE_TYPE_RESET = 4;
  private static final int MESSAGE_TYPE_LOCK = 5;
  private static final int MESSAGE_TYPE_GAME_OVER = 6;

  private static final int FIELD_SIZE = 12;

  private String mMessageToSend = "";

  ConnectedThread mConnectedThread;
  ConnectThread mConnectThread;
  AcceptThread mAcceptThread;

  Handler mMessageHandler = new Handler() {
    @Override
    public synchronized void handleMessage(final Message msg) {
      switch (msg.what) {
        case MESSAGE_READ: {
          byte[] readBuf = (byte[]) msg.obj;
          // construct a string from the valid bytes in the buffer
          String readMessage = new String(readBuf, 0, msg.arg1);
          Log.d("Set", "Read Message: " + readMessage);
          String[] messages = readMessage.split("M");
          handleMessages(messages);
          break;
        }
        default:
          break;
      }
    }

  };

  private void handleMessages(String[] messages) {
    int messagePos = 0;
    messageHandlingLoop: for (String message : messages) {
      Log.d("Set", "Seperate Message: " + message);
      String[] messageTokens = message.split(" ");
      int messageType = Integer.parseInt(messageTokens[0]);
      switch (messageType) {
        case MESSAGE_TYPE_SCORE: {
          int scoreChange = Integer.parseInt(messageTokens[1]);
          if (mPlayerNum == 1) {
            modifyScore(scoreChange, 2);
          } else {
            modifyScore(scoreChange, 1);
          }
          break;
        }
        case MESSAGE_TYPE_DRAW: {
          int num = Integer.parseInt(messageTokens[1]);
          Card.Shape shape = Card.Shape.values()[Integer.parseInt(messageTokens[2])];
          Card.Pattern pattern = Card.Pattern.values()[Integer.parseInt(messageTokens[3])];
          int color = Card.colors[Integer.parseInt(messageTokens[4])];
          for (Card card : mDeck) {
            if (card.getNum() == num && card.getShape() == shape && card.getPattern() == pattern
                && card.getColor() == color) {
              drawCard(card);
              mCardAdapter.notifyDataSetChanged();
              break;
            }
          }
          break;
        }
        case MESSAGE_TYPE_REMOVE: {
          int num = Integer.parseInt(messageTokens[1]);
          Card.Shape shape = Card.Shape.values()[Integer.parseInt(messageTokens[2])];
          Card.Pattern pattern = Card.Pattern.values()[Integer.parseInt(messageTokens[3])];
          int color = Card.colors[Integer.parseInt(messageTokens[4])];
          for (Card card : mCardsOnField) {
            if (card.getNum() == num && card.getShape() == shape && card.getPattern() == pattern
                && card.getColor() == color) {
              mCardsOnField.remove(card);
              mCardAdapter.notifyDataSetChanged();
              break;
            }
          }
          break;
        }
        case MESSAGE_TYPE_RESET: {
          resetBoard();
          break;
        }
        case MESSAGE_TYPE_LOCK: {
          for (int i = 0; i < 3; i++) {
            int num = Integer.parseInt(messageTokens[1 + i * 4]);
            Card.Shape shape = Card.Shape.values()[Integer.parseInt(messageTokens[2 + i * 4])];
            Card.Pattern pattern =
                Card.Pattern.values()[Integer.parseInt(messageTokens[3 + i * 4])];
            int color = Card.colors[Integer.parseInt(messageTokens[4 + i * 4])];
            for (Card card : mCardsOnField) {
              if (card.getNum() == num && card.getShape() == shape && card.getPattern() == pattern
                  && card.getColor() == color) {
                card.setLocked(true);
                break;
              }
            }
          }
          mCardAdapter.notifyDataSetChanged();
          failureVibrate();

          final String[] restOfMessages = new String[messages.length - messagePos - 1];
          for (int i = 0; i < restOfMessages.length; i++) {
            restOfMessages[i] = messages[i + messagePos + 1];
          }

          final Handler handler = new Handler();
          handler.postDelayed(new Runnable() {
            @Override
            public void run() {
              handleMessages(restOfMessages);
            }
          }, 500);

          break messageHandlingLoop;
        }
        case MESSAGE_TYPE_GAME_OVER: {
          String dialogText =
              "Game Over! Time: " + getTime() + ", Player 1 Score: " + mScore + ", Player 2 Score:"
                  + mScore2;
          showGameOverDialog(dialogText);
          break;
        }
        default:
          break;
      }
      messagePos++;
    }

  }

  Handler mTimerHandler = new Handler();
  Runnable mTimerRunnable = new Runnable() {

    @Override
    public void run() {
      mTimePassedText.setText("Time: " + getTime());

      mTimerHandler.postDelayed(this, 500);
    }
  };


  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    mStartTime = System.currentTimeMillis();
    mTimerHandler.postDelayed(mTimerRunnable, 0);

    mAddCardsButton = (Button) findViewById(R.id.addCardsButton);
    mResetButton = (Button) findViewById(R.id.resetButton);
    mPauseButton = (Button) findViewById(R.id.pauseButton);

    mCardsLeftText = (TextView) findViewById(R.id.cardsLeftText);
    mTimePassedText = (TextView) findViewById(R.id.timePassedText);
    mScoreText = (TextView) findViewById(R.id.scoreText);
    mScoreText2 = (TextView) findViewById(R.id.scoreText2);

    Bundle extras = getIntent().getExtras();
    mPlayers = extras.getInt("Players");
    mPlayerNum = extras.getInt("PlayerNum");

    if (mPlayers == 1) {
      mScoreText2.setVisibility(View.INVISIBLE);
    } else {
      setupBluetooth();
      resetScore();
    }

    mVibrator = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);

    mGridView = (GridView) findViewById(R.id.gridview);

    mDeck = new ArrayList<Card>();
    mCards = new ArrayList<Card>();

    for (int num = 1; num < 4; num++) {
      for (Card.Shape shape : Card.Shape.values()) {
        for (Card.Pattern pattern : Card.Pattern.values()) {
          for (int color : Card.colors) {
            Card card = new Card(num, shape, pattern, color);
            mDeck.add(card);
            mCards.add(card);
          }
        }
      }
    }

    mCardsOnField = new ArrayList<Card>();

    if (mPlayerNum == 1 || mPlayers == 1) {
      drawCards(FIELD_SIZE);
      sendMessage();
    }

    mCardAdapter = new CardAdapter(this, R.layout.field_card, mCardsOnField);
    mGridView.setAdapter(mCardAdapter);
    mGridView.setOnTouchListener(new View.OnTouchListener() {

      @Override
      public boolean onTouch(View v, MotionEvent event) {
        float currentXPosition = event.getX();
        float currentYPosition = event.getY();
        int position = mGridView.pointToPosition((int) currentXPosition, (int) currentYPosition);

        View view = mGridView.getChildAt(position);
        if (view instanceof FieldCard) {
          FieldCard cardTouched = (FieldCard) view;

          switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
              cardTouched.setTouched(true);
              cardTouched.invalidate();
              break;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
              if (cardTouched.getTouched() && !cardTouched.getLocked()) {
                cardTouched.togglePressed();

                mVibrator.vibrate(50);

                List<Card> cardsPressed = new ArrayList<Card>();
                for (Card card : mCardsOnField) {
                  if (card.isPressed()) {
                    cardsPressed.add(card);
                  }
                }
                if (cardsPressed.size() == 3) {
                  threePressed(cardsPressed);
                }
              }
              break;
            }
          }
        }

        if (event.getAction() == MotionEvent.ACTION_UP
            || event.getAction() == MotionEvent.ACTION_CANCEL) {
          for (int i = 0; i < mGridView.getChildCount(); i++) {
            View gridItem = mGridView.getChildAt(i);

            if (gridItem instanceof FieldCard) {
              FieldCard cardTouched = (FieldCard) gridItem;
              cardTouched.setTouched(false);
              cardTouched.invalidate();
            }
          }
        }


        return true;
      }
    });

    mAddCardsButton.setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View v) {
        if (isSetOnField()) {
          failureVibrate();
          // modifyScoreAndUpdate(-1, mPlayerNum);
          // sendMessage();
        } else {
          successVibrate();
          drawCards(3);
          // modifyScoreAndUpdate(3, mPlayerNum);
          sendMessage();
          mCardAdapter.notifyDataSetChanged();
        }
      }
    });

    mResetButton.setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View v) {
        resetGame();
      }
    });

    mPauseButton.setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View v) {
        pauseGame();
      }
    });

  }

  private void pauseGame() {
    mTimerHandler.removeCallbacks(mTimerRunnable);
    mPauseTime = System.currentTimeMillis();
    AlertDialog.Builder dialog =
        new AlertDialog.Builder(MainActivity.this,
            android.R.style.Theme_Black_NoTitleBar_Fullscreen).setMessage("PAUSED")
            .setPositiveButton("RESUME", new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialog, int which) {
                mStartTime += System.currentTimeMillis() - mPauseTime;
                mTimerHandler.postDelayed(mTimerRunnable, 0);
              }
            });

    dialog.show();
  }

  private void resetGame() {
    resetBoard();

    resetUpdate();
    drawCards(FIELD_SIZE);
    sendMessage();

    mCardAdapter.notifyDataSetChanged();
  }


  private void resetBoard() {
    mStartTime = System.currentTimeMillis();
    resetScore();

    mDeck.clear();
    mCardsOnField.clear();

    mDeck.addAll(mCards);
    for (Card card : mCards) {
      card.setLocked(false);
    }
  }

  private void threePressed(List<Card> cardsPressed) {
    boolean isSet = true;

    isSet = checkIfSet(cardsPressed);

    if (isSet) {
      mCardsOnField.removeAll(cardsPressed);
      updateLocks(cardsPressed);
      updateRemovals(cardsPressed);
      if (mCardsOnField.size() < FIELD_SIZE) {
        drawCards(3);
      }
      successVibrate();
      modifyScoreAndUpdate(1, mPlayerNum);
      sendMessage();

      if (mDeck.size() == 0 && !isSetOnField()) {
        String dialogText;
        if (mPlayers == 1) {
          dialogText = "You Win! Time: " + getTime() + ", Score: " + mScore;
        } else {
          dialogText =
              "Game Over! Time: " + getTime() + ", Player 1 Score: " + mScore + ", Player 2 Score:"
                  + mScore2;
        }
        queueMessage("" + MESSAGE_TYPE_GAME_OVER);
        sendMessage();

        showGameOverDialog(dialogText);
      }
    } else {
      failureVibrate();
      for (Card card : cardsPressed) {
        card.togglePressed();
      }
      modifyScoreAndUpdate(-1, mPlayerNum);
      sendMessage();
    }

    mCardAdapter.notifyDataSetChanged();
  }

  private void showGameOverDialog(String dialogText) {
    AlertDialog.Builder dialog =
        new AlertDialog.Builder(MainActivity.this, android.R.style.Theme_DeviceDefault)
            .setTitle("Game Over").setMessage(dialogText)
            .setPositiveButton("Reset", new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialog, int which) {
                resetGame();
                sendMessage();
              }
            });

    dialog.show();
  }

  private void modifyScoreAndUpdate(int mod, int player) {
    if (mPlayers > 1) {
      String output = MESSAGE_TYPE_SCORE + " " + mod;
      queueMessage(output);
    }
    modifyScore(mod, player);
  }

  private void modifyScore(int mod, int player) {
    if (player == 1) {
      mScore += mod;
    } else {
      mScore2 += mod;
    }
    updateScoreLabel(player);
  }



  private void resetScore() {
    mScore = 0;
    mScore2 = 0;
    updateScoreLabel(1);
    if (mPlayers > 1) {
      updateScoreLabel(2);
    }
  }

  private void updateScoreLabel(int player) {
    if (mPlayers == 1) {
      mScoreText.setText("Score: " + mScore);
    } else {
      if (player == 1) {
        mScoreText.setText("Player1: " + mScore);
      } else {
        mScoreText2.setText("Player2: " + mScore2);
      }
    }
  }

  private void failureVibrate() {
    mVibrator.vibrate(150);
  }

  private void successVibrate() {
    long[] pattern = {100, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50};
    mVibrator.vibrate(pattern, -1);
  }

  private boolean checkIfSet(List<Card> cardsPressed) {
    // check if all 3 cards have the same color or different colors
    if (!(cardsPressed.get(0).getColor() == cardsPressed.get(1).getColor() && cardsPressed.get(1)
        .getColor() == cardsPressed.get(2).getColor())
        && !(cardsPressed.get(0).getColor() != cardsPressed.get(1).getColor()
            && cardsPressed.get(1).getColor() != cardsPressed.get(2).getColor() && cardsPressed
            .get(0).getColor() != cardsPressed.get(2).getColor())) {
      return false;
    }

    if (!(cardsPressed.get(0).getNum() == cardsPressed.get(1).getNum() && cardsPressed.get(1)
        .getNum() == cardsPressed.get(2).getNum())
        && !(cardsPressed.get(0).getNum() != cardsPressed.get(1).getNum()
            && cardsPressed.get(1).getNum() != cardsPressed.get(2).getNum() && cardsPressed.get(0)
            .getNum() != cardsPressed.get(2).getNum())) {
      return false;
    }

    if (!(cardsPressed.get(0).getShape() == cardsPressed.get(1).getShape() && cardsPressed.get(1)
        .getShape() == cardsPressed.get(2).getShape())
        && !(cardsPressed.get(0).getShape() != cardsPressed.get(1).getShape()
            && cardsPressed.get(1).getShape() != cardsPressed.get(2).getShape() && cardsPressed
            .get(0).getShape() != cardsPressed.get(2).getShape())) {
      return false;
    }

    if (!(cardsPressed.get(0).getPattern() == cardsPressed.get(1).getPattern() && cardsPressed.get(
        1).getPattern() == cardsPressed.get(2).getPattern())
        && !(cardsPressed.get(0).getPattern() != cardsPressed.get(1).getPattern()
            && cardsPressed.get(1).getPattern() != cardsPressed.get(2).getPattern() && cardsPressed
            .get(0).getPattern() != cardsPressed.get(2).getPattern())) {
      return false;
    }
    return true;
  }

  private void drawCards(int numCards) {
    List<Card> cards = new ArrayList<Card>();
    for (int i = 0; i < numCards && mDeck.size() > 0; i++) {
      cards.add(drawCard());
    }
    updateDraws(cards);
  }

  private void updateRemovals(List<Card> cards) {
    if (mPlayers > 1) {
      for (Card card : cards) {
        int colorPos = 0;
        while (card.getColor() != Card.colors[colorPos] && colorPos < 3) {
          colorPos++;
        }
        String output =
            MESSAGE_TYPE_REMOVE + " " + card.getNum() + " " + card.getShape().ordinal() + " "
                + card.getPattern().ordinal() + " " + colorPos;
        queueMessage(output);
      }

    }
  }

  private void updateLocks(List<Card> cards) {
    if (mPlayers > 1) {
      String output = "" + MESSAGE_TYPE_LOCK;
      for (Card card : cards) {
        int colorPos = 0;
        while (card.getColor() != Card.colors[colorPos] && colorPos < 3) {
          colorPos++;
        }
        output +=
            " " + card.getNum() + " " + card.getShape().ordinal() + " "
                + card.getPattern().ordinal() + " " + colorPos;

      }
      queueMessage(output);

    }
  }

  private void updateDraws(List<Card> cards) {
    if (mPlayers > 1) {
      for (Card card : cards) {
        int colorPos = 0;
        while (card.getColor() != Card.colors[colorPos] && colorPos < 3) {
          colorPos++;
        }
        String output =
            MESSAGE_TYPE_DRAW + " " + card.getNum() + " " + card.getShape().ordinal() + " "
                + card.getPattern().ordinal() + " " + colorPos;
        queueMessage(output);
      }

    }
  }

  private void resetUpdate() {
    if (mPlayers > 1) {
      String output = "" + MESSAGE_TYPE_RESET;
      queueMessage(output);
    }
  }

  private Card drawCard() {
    int pos = (int) (Math.random() * mDeck.size());
    Card card = mDeck.get(pos);
    drawCard(card);
    return card;
  }

  private void drawCard(Card card) {
    card.setPressed(false);
    mCardsOnField.add(card);
    mDeck.remove(card);
    mCardsLeftText.setText("Deck: " + mDeck.size());
  }

  private boolean isSetOnField() {
    List<Card> checkCards = new ArrayList<Card>();
    for (int i = 0; i < mCardsOnField.size(); i++) {
      for (int k = i + 1; k < mCardsOnField.size(); k++) {
        for (int j = k + 1; j < mCardsOnField.size(); j++) {
          checkCards.clear();
          checkCards.add(mCardsOnField.get(i));
          checkCards.add(mCardsOnField.get(j));
          checkCards.add(mCardsOnField.get(k));

          if (checkIfSet(checkCards)) {
            return true;
          }
        }
      }
    }

    return false;
  }

  private String getTime() {
    long millis = System.currentTimeMillis() - mStartTime;
    int seconds = (int) (millis / 1000);
    int minutes = seconds / 60;
    seconds = seconds % 60;
    return String.format("%d:%02d", minutes, seconds);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.main, menu);
    return true;
  }

  @Override
  protected void onPause() {
    super.onPause();
    pauseGame();
  }

  private void queueMessage(String message) {
    mMessageToSend += message + "M";

  }

  private void sendMessage() {
    if (mPlayers > 1) {
      Log.d("Set", "Send message: " + mMessageToSend);
      mConnectedThread.write(mMessageToSend.getBytes());
      mMessageToSend = "";
    }
  }

  private void setupBluetooth() {
    mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    mPairedDevices = mBluetoothAdapter.getBondedDevices();

    if (mPairedDevices.size() > 0) {
      if (mPlayerNum == 1) {
        mAcceptThread = new AcceptThread();
        mAcceptThread.run();
      } else {
        mConnectThread = new ConnectThread(mPairedDevices.iterator().next());
        mConnectThread.run();
      }
    }
  }

  private void manageConnectedSocket(BluetoothSocket socket) {
    mConnectedThread = new ConnectedThread(socket);
    mConnectedThread.start();
  }

  private class AcceptThread extends Thread {
    private final BluetoothServerSocket mmServerSocket;

    public AcceptThread() {
      // Use a temporary object that is later assigned to mmServerSocket,
      // because mmServerSocket is final
      BluetoothServerSocket tmp = null;
      try {
        // MY_UUID is the app's UUID string, also used by the client code
        tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord("Server", MY_UUID);
      } catch (IOException e) {}
      mmServerSocket = tmp;
    }

    @Override
    public void run() {
      BluetoothSocket socket = null;
      // Keep listening until exception occurs or a socket is returned
      Log.d("Set", "Looking for join to accept.");
      while (true) {
        try {
          socket = mmServerSocket.accept();
        } catch (IOException e) {
          break;
        }
        // If a connection was accepted
        if (socket != null) {
          Log.d("Set", "Accepted join request.");
          // Do work to manage the connection (in a separate thread)
          manageConnectedSocket(socket);
          try {
            mmServerSocket.close();
          } catch (IOException e) {}
          break;
        } else {
          Log.d("Set", "Failed to accept join request, socket is null.");
        }

      }
    }

    /** Will cancel the listening socket, and cause the thread to finish */
    public void cancel() {
      try {
        mmServerSocket.close();
      } catch (IOException e) {}
    }
  }

  private class ConnectThread extends Thread {
    private BluetoothSocket mmSocket;
    private final BluetoothDevice mmDevice;

    public ConnectThread(BluetoothDevice device) {
      // Use a temporary object that is later assigned to mmSocket,
      // because mmSocket is final
      BluetoothSocket tmp = null;
      mmDevice = device;

      // Get a BluetoothSocket to connect with the given BluetoothDevice
      try {
        // MY_UUID is the app's UUID string, also used by the server code
        tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
      } catch (IOException e) {}
      mmSocket = tmp;
    }

    @Override
    public void run() {
      // Cancel discovery because it will slow down the connection
      mBluetoothAdapter.cancelDiscovery();

      Log.d("Set", "Looking for host.");
      try {
        // Connect the device through the socket. This will block
        // until it succeeds or throws an exception
        mmSocket.connect();
      } catch (IOException connectException) {
        // Unable to connect; close the socket and get out
        try {

          try {
            Log.d("Set", "First connect failed, trying fallback conncet to host.");

            mmSocket =
                (BluetoothSocket) mmDevice.getClass()
                    .getMethod("createRfcommSocket", new Class[] {int.class}).invoke(mmDevice, 1);
            mmSocket.connect();

          } catch (Exception exception) {
            Log.d("Set", "Connecting to host failed.");
            mmSocket.close();
            return;
          }
        } catch (IOException closeException) {}

      }

      Log.d("Set", "Connected to host.");
      // Do work to manage the connection (in a separate thread)
      manageConnectedSocket(mmSocket);
    }

    /** Will cancel an in-progress connection, and close the socket */
    public void cancel() {
      try {
        mmSocket.close();
      } catch (IOException e) {}
    }
  }

  private class ConnectedThread extends Thread {
    private final BluetoothSocket mmSocket;
    private final InputStream mmInStream;
    private final OutputStream mmOutStream;

    public ConnectedThread(BluetoothSocket socket) {
      mmSocket = socket;
      InputStream tmpIn = null;
      OutputStream tmpOut = null;

      // Get the input and output streams, using temp objects because
      // member streams are final
      try {
        tmpIn = socket.getInputStream();
        tmpOut = socket.getOutputStream();
        Log.d("Set", "Setup I/O streams.");
      } catch (IOException e) {}

      mmInStream = tmpIn;
      mmOutStream = tmpOut;
    }

    @Override
    public void run() {
      byte[] buffer = new byte[1024]; // buffer store for the stream
      int bytes; // bytes returned from read()

      while (true) {
        try {
          // Read from the InputStream
          bytes = mmInStream.read(buffer);
          // Send the obtained bytes to the UI activity
          mMessageHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer).sendToTarget();
        } catch (IOException e) {
          break;
        }
      }

    }

    /* Call this from the main activity to send data to the remote device */
    public void write(byte[] bytes) {
      try {
        mmOutStream.write(bytes);
      } catch (IOException e) {}
    }

    /* Call this from the main activity to shutdown the connection */
    public void cancel() {
      try {
        mmSocket.close();
      } catch (IOException e) {}
    }
  }

}
