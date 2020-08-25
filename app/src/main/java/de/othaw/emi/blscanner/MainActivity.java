package de.othaw.emi.blscanner;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

//modified version of https://www.youtube.com/watch?v=DPbCFAQCFhE

public class MainActivity extends AppCompatActivity {
    /**To make the code more readable the code 1 from Maifest.permissions is packed into a variable**/
    public static final int PERMISSION_TO_ACCESS_COARSE_LOCATION = 1;
    /**To make the code more readable the result code 11 is packed into a variable*/
    public static final int PERMISSION_TO_ENABLE_BLUETOOTH = 11;
    /**The button on the bottom of the screen. This button is used to start the Bluetooth device discovery**/
    private Button mStartScanningButton;
    /**This is the default Bluetooth adapter of the android device**/
    private BluetoothAdapter mBluetoothAdapter;
    /**A list adapter to handle the interactions with the list view**/
    private ArrayAdapter<String> mListAdapter;
    /**The location where an extra message for a new intent can be placed**/
    public static final String EXTRA_MESSAGE = "de.othaw.emi.blscanner.MESSAGE";

    /**Called when the activity is starting.
     * @param savedInstanceState  If the activity is being re-initialized after previously being shut down then this Bundle contains the data it most recently supplied in onSaveInstanceState(Bundle). Otherwise it is null.
     * **/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Set the content view to the XML file
        setContentView(R.layout.activity_main);
        //Get the default bluetooth adapter of the phone.
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        ListView mBLDeviceList = findViewById(R.id.devicesList);
        mStartScanningButton = findViewById(R.id.scanningBtn);
        mListAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        mBLDeviceList.setAdapter(mListAdapter);

        //Calls the function to check whether or not bluetooth is available and enabled
        checkBluetoothState();

        //Set an on click listener for the button to scan for bluetooth devices
        mStartScanningButton.setOnClickListener(new View.OnClickListener() {
            /**Called when a view has been clicked
             * @param v The view that was clicked.**/
            @Override
            public void onClick(View v) {
                if(mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()){
                    if(checkCoarseLocationPermission()){
                        mListAdapter.clear();
                        mBluetoothAdapter.startDiscovery();
                    }
                }
                else{
                    checkBluetoothState();
                }
            }
        });

        //Set an on click listener for a list item
        mBLDeviceList.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            /**Callback method to be invoked when an item in this AdapterView has been clicked.
             * @param parent The AdapterView where the click happened.
             * @param view The view within the AdapterView that was clicked (this will be a view provided by the adapter)
             * @param position The position of the view in the adapter.
             * @param id The row id of the item that was clicked.**/
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String[] Obj = mListAdapter.getItem(position).split("\n");
                //Cancel the discovery process to save energy
                mBluetoothAdapter.cancelDiscovery();
                Intent intent = new Intent(parent.getContext(), DataEntry.class);
                //Provide the new intent with the MAC address of the clicked item as a message
                intent.putExtra(EXTRA_MESSAGE, Obj[1]);
                startActivity(intent);
            }
        });
        checkCoarseLocationPermission();
    }

    /**Called when the activity is resumed from a paused state.
     * **/
    @Override
    protected void onResume(){
        super.onResume();
        //Register receiver
        registerReceiver(Receiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
        registerReceiver(Receiver, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED));
        registerReceiver(Receiver, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));
    }

    /**Called when the activity is paused.
     * **/
    @Override
    protected void onPause(){
        super.onPause();
        //Unregister receiver to save energy
        unregisterReceiver(Receiver);
    }

    /**This function checks whether or not the app has permission to access the coarse location. If not the the permission is requested.
     * */
    private boolean checkCoarseLocationPermission(){
        //For bluetooth to work, android requires the coarse location of the device. This if checks if the permission to access the coarse location is granted or not
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            //If the permission to access the coarse location is not given, it requests the permission
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_TO_ACCESS_COARSE_LOCATION);
            return false;
        }
        else{
            return true;
        }
    }

    /**This function checks the current state of the Bluetooth adapter and acts appropriately.
     * **/
    private void checkBluetoothState(){
        //If no Bluetooth adapter could be selected then Bluetooth is not supported
        if(mBluetoothAdapter == null){
            Toast.makeText(this, "Bluetooth is not supported on your device", Toast.LENGTH_SHORT).show();
        }
        else{
            if(mBluetoothAdapter.isEnabled()){
                if(mBluetoothAdapter.isDiscovering()){
                    Toast.makeText(this, "Device discovering in process...", Toast.LENGTH_SHORT).show();
                }
                else{
                    Toast.makeText(this, "Bluetooth is enabled", Toast.LENGTH_SHORT).show();
                    mStartScanningButton.setEnabled(true);
                }
            }
            //If the adapter was found but is not enabled, the app will ask the user to enable bluetooth
            else{
                Toast.makeText(this, "You need to enable Bluetooth", Toast.LENGTH_SHORT).show();
                Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                //Start an activity that asks the user to allow the usage of the bluetooth adapter
                startActivityForResult(enableIntent, PERMISSION_TO_ENABLE_BLUETOOTH);
            }
        }

    }

    /**Called when an activity you launched exits, giving you the requestCode you started it with, the resultCode it returned, and any additional data from it.
     * @param requestCode The integer request code originally supplied to startActivityForResult(), allowing you to identify who this result came from.
     * @param resultCode The integer result code returned by the child activity through its setResult().
     * @param data An Intent, which can return result data to the caller (various data can be attached to Intent "extras").
     * **/
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        //If the request Code is PERMISSION_TO_ENABLE_BLUETOOTH, checkBluetooth state again to ensure that bluetooth was enabled.
        if(requestCode == PERMISSION_TO_ENABLE_BLUETOOTH){
            checkBluetoothState();
        }
    }

    /**Callback for the result from requesting permissions. This method is invoked for every call on ActivityCompat.requestPermissions(android.app.Activity, String[], int).
     * @param requestCode The request code passed in ActivityCompat.requestPermissions(android.app.Activity, String[], int)
     * @param permissions The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions which is either PackageManager.PERMISSION_GRANTED or PackageManager.PERMISSION_DENIED. Never null.**/
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_TO_ACCESS_COARSE_LOCATION) {
            //Checks whether or not the request for a permission in the checkCoarseLocationPermission function was granted or not
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Coarse location access is allowed for the app. You can scan for bluetooth devices", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Coarse location access is not allowed for the app. You can't scan for bluetooth devices devices", Toast.LENGTH_LONG).show();
            }
        }
    }

    /**This receiver receives the discovered Bluetooth devices.**/
    private final BroadcastReceiver Receiver = new BroadcastReceiver() {
        /**This method is called when the BroadcastReceiver is receiving an Intent broadcast.
         * @param context The Context in which the receiver is running.
         * @param intent The Intent being received.**/
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)){
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                //Add the device that was found to the list that is displayed on the screen
                mListAdapter.add(device.getName() + "\n" + device.getAddress());
                mListAdapter.notifyDataSetChanged();
            }
            //If the discovery ended i.e. the app was paused of the maximum time to scan (typically 120 seconds) was exceeded, the button text is reset to default.
            else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)){
                mStartScanningButton.setText("Scanning for bluetooth devices");
            }
            //Updated text to reflect the state of discovery
            else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)){
                mStartScanningButton.setText("Scanning in progress ...");
            }
        }
    };


}
