package de.othaw.emi.blscanner;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import java.util.regex.Pattern;

//written by myself

public class DataEntry extends AppCompatActivity {
    //The UUID of the bluetooth service. This has to be identical to the UUID of the server.
    private UUID mServiceUUID = UUID.fromString("94f39d29-7d6d-437d-973b-fba39e49d4ee");
    private Button mButtonSend;
    private EditText mXSteps;
    private EditText mYSteps;
    private BluetoothSocket mSocket;

    /**Called when the activity is starting.
     * @param savedInstanceState  If the activity is being re-initialized after previously being shut down then this Bundle contains the data it most recently supplied in onSaveInstanceState(Bundle). Otherwise it is null.
     * **/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_entry);
        //Get the default bluetooth adapter
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Intent intent = getIntent();
        //Get the extra message, i.e. the MAC address of the device to pair to, which is put there by the main activity
        String MACAddress = intent.getStringExtra(MainActivity.EXTRA_MESSAGE);
        //Create a bluetooth device from the given MAC address
        BluetoothDevice mPi = mBluetoothAdapter.getRemoteDevice(MACAddress);
        mButtonSend = (Button)findViewById(R.id.button1);

        try {
            //Create a socket to the bluetooth device.
            mSocket = mPi.createRfcommSocketToServiceRecord(mServiceUUID);
            mSocket.connect();
            Toast.makeText(this, "You connected to " + mPi.getName() + ". Stopping looking for new devices.", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(this,"Couldn't connect to bluetooth device. Returning to main activity", Toast.LENGTH_SHORT).show();
            finish();
        }

        mButtonSend.setOnClickListener(new View.OnClickListener() {
            /**Called when a view has been clicked
             * @param view The view that was clicked.**/
            public void onClick(View view) {
                mXSteps = (EditText) findViewById(R.id.editText1);
                mYSteps = (EditText) findViewById(R.id.editText2);

                //Get the content of the text boxes
                String XVal = mXSteps.getText().toString();
                String YVal = mYSteps.getText().toString();

                //Check if the input is valid with the help of regex
                if (Pattern.matches("[0-9]+", XVal) && Pattern.matches("[0-9]+", YVal)) {
                    try {
                        //Disable the button so no more inputs can be made.
                        mButtonSend.setEnabled(false);
                        //Concatenate the strings to one string
                        String Vals = "x-Vals: " + XVal + "\n" + "y-Vals: " + YVal + "\n";
                        //Get the byte representation of the string
                        byte[] mmBuffer = Vals.getBytes();

                        //Create an input and an output string for the socket
                        InputStream sin = mSocket.getInputStream();
                        OutputStream sout = mSocket.getOutputStream();
                        //Send the byte representation of the string to the server
                        sout.write(mmBuffer);

                        byte[] mmRead = new byte[1];
                        //Read what the server sends back
                        sin.read(mmRead);
                        //Block the app as long as 1 is sent.
                        while("1".equals(new String(mmRead)))sin.read(mmRead);
                        Toast.makeText(view.getContext(), "Camera Finished the Work.", Toast.LENGTH_LONG).show();
                        //Reset the text boxes
                        mXSteps.setText("");
                        mYSteps.setText("");

                        //Enable the button again so more inputs can be made.
                        mButtonSend.setEnabled(true);
                    }
                    catch (IOException ignored) {
                        //Enable the button again so more inputs can be made.
                        mButtonSend.setEnabled(true);
                        Toast.makeText(view.getContext(),"IO Error. Returning to main activity", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                }
                else{
                    //Reset the text boxes
                    mXSteps.setText("");
                    mYSteps.setText("");
                    Toast.makeText(view.getContext(),"Wrong data format. Please enter a valid format eg. a positive Number", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    /**Called when the activity is resumed from a paused state.
     * **/
    @Override
    public void onResume(){
        super.onResume();
    }


    /**Called when the activity is paused. The connection is not closed here because of the way the Server on the Raspberry PI is set up.
     * **/
    @Override
    public void onPause() {
        super.onPause();
    }

    /**Called when the activity is destroyed, i.e. the app is terminated
     * **/
    @Override
    public void onDestroy() {
        super.onDestroy();
        try{
            mSocket.close();
        }
        catch (IOException e){
            finish();
        }
    }

    /**Called when the back button of the phone is pressed.
     * **/
    @Override
    public void onBackPressed(){
        try {
            mSocket.close();
        } catch (IOException e) {
            finish();
        }
        super.onBackPressed();
    }
}
