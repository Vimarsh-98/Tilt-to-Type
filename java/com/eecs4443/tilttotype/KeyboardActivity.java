package com.eecs4443.tilttotype;

import androidx.appcompat.app.AppCompatActivity;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

public class KeyboardActivity extends AppCompatActivity implements SensorEventListener {
    private final int KEYBOARD_ROWS = 5;
    private final int KEYBOARD_COLS = 10;

    private final int SPACE_ROW = 4;
    private final int SPACE_COL = 2;
    private final int SPACE_WIDTH = 5; //number of EXTRA key widths the spacebar takes

    private final float RADIANS_TO_DEGREES = 57.2957795f;
    private final float TILT_THRESHOLD = 20f; //the degrees needed for a tilt to register

    private boolean tiltMode;

    private TextView typedText;
    private TableLayout table;
    private View[][] keyboard = new View[KEYBOARD_ROWS][KEYBOARD_COLS];
    private Key focusKey;
    private int focusX, focusY;

    private SensorManager sm;
    private Sensor accSensor, magSensor;
    private float[] accValues = new float[3];
    private float[] magValues = new float[3];
    private float pitch, roll;
    private float lastTime;

    public StringBuilder sb = new StringBuilder();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.keyboard_layout);

        Bundle b = getIntent().getExtras();
        tiltMode = b.getBoolean("tiltMode");

        typedText = (TextView)findViewById(R.id.typedText);
        table = (TableLayout)findViewById(R.id.keyboardLayout);

        if (tiltMode) {
            typedText.setFocusable(false);
            typedText.setFocusableInTouchMode(false);
            table.setVisibility(View.VISIBLE);
        }
        else {
            typedText.setFocusable(true);
            typedText.setFocusableInTouchMode(true);
            typedText.requestFocus();
            table.setVisibility(View.INVISIBLE);
        }

        //get keys from layout and place in array
        for (int i = 0; i < KEYBOARD_ROWS; i++) {
            TableRow tRow = (TableRow) table.getChildAt(i);
            for (int j = 0; j < KEYBOARD_COLS; j++) {
                //account for size of the spacebar
                if (i == SPACE_ROW) {
                    if (j > SPACE_COL && j <= SPACE_COL + SPACE_WIDTH)
                        keyboard[i][j] = keyboard[SPACE_ROW][SPACE_COL];
                    else if (j > SPACE_COL + SPACE_WIDTH)
                        keyboard[i][j] = tRow.getChildAt(j - SPACE_WIDTH);
                    else
                        keyboard[i][j] = tRow.getChildAt(j);
                } else
                    keyboard[i][j] = tRow.getChildAt(j);

                if (tiltMode) {
                    keyboard[i][j].setFocusable(true);
                    keyboard[i][j].setFocusableInTouchMode(true);
                }
                else {
                    keyboard[i][j].setFocusable(false);
                    keyboard[i][j].setFocusableInTouchMode(false);
                }
            }
        }

        setFocus(0, 0);

        sm = (SensorManager)getSystemService(SENSOR_SERVICE);
        accSensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magSensor = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        lastTime = System.nanoTime();
    }

    //used for setting position of the focus key directly
    private void setFocus(int x, int y)
    {
        if (tiltMode && x >= 0 && x <= KEYBOARD_ROWS && y >= 0 && y <= KEYBOARD_COLS) {
            keyboard[x][y].requestFocus();
            focusKey = (Key) keyboard[x][y];
            focusX = x;
            focusY = y;
        }
    }

    //used for shifting the position of the focus key one at a time
    private void setFocus(boolean vertical, boolean positive)
    {
        if (tiltMode) {
            if (vertical) {
                if (positive) {
                    if (focusX == KEYBOARD_ROWS - 1)
                        focusX = 0;
                    else
                        focusX++;
                } else {
                    if (focusX == 0)
                        focusX = KEYBOARD_ROWS - 1;
                    else
                        focusX--;
                }
            } else {
                if (positive) {
                    if (focusY == KEYBOARD_COLS - 1)
                        focusY = 0;
                    else
                        focusY++;
                } else {
                    if (focusY == 0)
                        focusY = KEYBOARD_COLS - 1;
                    else
                        focusY--;
                }
            }

            keyboard[focusX][focusY].requestFocus();
            focusKey = (Key) keyboard[focusX][focusY];
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        sm.registerListener(this, accSensor, SensorManager.SENSOR_DELAY_NORMAL);
        sm.registerListener(this, magSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy)
    {
        // not needed, but we need to provide an implementation anyway
    }

    @Override
    public void onSensorChanged(SensorEvent se)
    {
        float now = System.nanoTime();
        if ((now - lastTime) / 1000000000f >= 0.4f) { //only update focus every 0.4 seconds at most
            lastTime = now;
            if (se.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
                accValues = se.values;
            if (se.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
                magValues = se.values;

            if (accValues != null && magValues != null) {
                float[] R = new float[9];
                float[] I = new float[9];
                boolean success = SensorManager.getRotationMatrix(R, I, accValues, magValues);
                if (success) {
                    float[] orientation = new float[3];
                    SensorManager.getOrientation(R, orientation);

                    pitch = orientation[1] * RADIANS_TO_DEGREES;
                    roll = -orientation[2] * RADIANS_TO_DEGREES;

                    if (-pitch > TILT_THRESHOLD) {
                        setFocus(true, true); //move focus down
                    } else if (pitch > TILT_THRESHOLD) {
                        setFocus(true, false); //move focus up
                    }

                    if (-roll > TILT_THRESHOLD) {
                        setFocus(false, true); //move focus right
                    } else if (roll > TILT_THRESHOLD) {
                        setFocus(false, false); //moves focus left
                    }
                }
            }
        }
    }

    public void clickKeyboard(View v)
    {
        focusKey.keyAction();
        typedText.setText(sb);
    }
}