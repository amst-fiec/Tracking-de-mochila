package espol.edu.bagtraking.Activity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;


import espol.edu.bagtraking.Modelo.Variables;
import espol.edu.bagtraking.R;


public class Device_Bluetooth extends AppCompatActivity {
    private Button search;
    private Button connect;
    private EditText timems;
    private EditText ditanciaMax;
    private Button setData;
    private ListView listView;
    private BluetoothAdapter mBTAdapter;
    private static final int BT_ENABLE_REQUEST = 10;
    private static final int SETTINGS = 20;
    private UUID mDeviceUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private int mBufferSize = 50000; //Default
    public static final String DEVICE_EXTRA = "com.example.myapplication.SOCKET";
    public static final String DEVICE_UUID = "com.example.myapplication.uuid";
    private static final String DEVICE_LIST = "com.example.myapplication.devicelist";
    private static final String DEVICE_LIST_SELECTED = "com.example.myapplication.devicelistselected";
    public static final String BUFFER_SIZE = "com.example.myapplication.buffersize";
    private static final String TAG = "BlueTest5-MainActivity";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device__bluetooth);



        search = (Button) findViewById(R.id.buscar);
        connect = (Button) findViewById(R.id.conectar);
        setData = findViewById(R.id.setDate);
        timems = findViewById(R.id.tiempo_ms);
        ditanciaMax = findViewById(R.id.distancia_max);
        listView = (ListView) findViewById(R.id.lista);

        if (savedInstanceState != null) {
            ArrayList<BluetoothDevice> list = savedInstanceState.getParcelableArrayList(DEVICE_LIST);
            if (list != null) {
                initList(list);
                MyAdapter adapter = (MyAdapter) listView.getAdapter();
                int selectedIndex = savedInstanceState.getInt(DEVICE_LIST_SELECTED);
                if (selectedIndex != -1) {
                    adapter.setSelectedIndex(selectedIndex);
                    connect.setEnabled(true);
                }
            } else {
                initList(new ArrayList<BluetoothDevice>());
            }

        } else {
            initList(new ArrayList<BluetoothDevice>());
        }
        search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                mBTAdapter = BluetoothAdapter.getDefaultAdapter();

                if (mBTAdapter == null) {
                    Toast.makeText(getApplicationContext(), "Bluetooth not found", Toast.LENGTH_SHORT).show();
                } else if (!mBTAdapter.isEnabled()) {
                    Intent enableBT = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBT, BT_ENABLE_REQUEST);
                } else {
                    new SearchDevices().execute();
                }
            }
        });


        connect.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                BluetoothDevice device = ((MyAdapter) (listView.getAdapter())).getSelectedItem();
                Intent intent = new Intent(getApplicationContext(), MenuActivity.class);
                intent.putExtra(DEVICE_EXTRA, device);
                intent.putExtra(DEVICE_UUID, mDeviceUUID.toString());
                intent.putExtra(BUFFER_SIZE, mBufferSize);
                startActivity(intent);
            }
        });
        setData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(timems.getText().toString()!=""){
                    try {
                        Variables.INTERVALO_NOTIFICACION = Integer.parseInt(timems.getText().toString());
                        Toast.makeText(Device_Bluetooth.this, "Tiempo seteado a "+timems.getText(), Toast.LENGTH_SHORT).show();
                    }catch (Exception e){

                    }

                }
                if(ditanciaMax.getText().toString()!=""){
                    try {
                        Variables.MAXIMO_DISTANCIA = Double.parseDouble(ditanciaMax.getText().toString());
                        Toast.makeText(Device_Bluetooth.this, "Distancia seteado a "+ditanciaMax.getText(), Toast.LENGTH_SHORT).show();
                    }catch (Exception e){

                    }

                }

            }
        });



    }

    protected void onPause() {
// TODO Auto-generated method stub
        super.onPause();
    }

    @Override
    protected void onStop() {
// TODO Auto-generated method stub
        super.onStop();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case BT_ENABLE_REQUEST:
                if (resultCode == RESULT_OK) {
                    msg("Bluetooth Enabled successfully");
                    new SearchDevices().execute();
                } else {
                    msg("Bluetooth couldn't be enabled");
                }

                break;
            case SETTINGS: //If the settings have been updated
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                String uuid = prefs.getString("prefUuid", "Null");
                mDeviceUUID = UUID.fromString(uuid);
                Log.d(TAG, "UUID: " + uuid);
                String bufSize = prefs.getString("prefTextBuffer", "Null");
                mBufferSize = Integer.parseInt(bufSize);

                String orientation = prefs.getString("prefOrientation", "Null");
                Log.d(TAG, "Orientation: " + orientation);
                if (orientation.equals("Landscape")) {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                } else if (orientation.equals("Portrait")) {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                } else if (orientation.equals("Auto")) {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
                }
                break;
            default:
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     *  Toast
     * @param str
     */
    private void msg(String str) {
        Toast.makeText(getApplicationContext(), str, Toast.LENGTH_SHORT).show();
    }

    /**
     * Inicializar lista Adapter
     * @param objects
     */
    private void initList(List<BluetoothDevice> objects) {
        final MyAdapter adapter = new MyAdapter(getApplicationContext(), R.layout.list_item, R.id.lstContent, objects);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                adapter.setSelectedIndex(position);
                connect.setEnabled(true);
            }
        });
    }

    /**
     * Busca dispositivos emparejados. ¡No hace un escaneo! Solo dispositivos emparejados a través de Configuración-> Bluetooth
     * aparecerá con esto. No vi ninguna necesidad de reconstruir la rueda aquí.
     * @author ryder
     *
     */
    private class SearchDevices extends AsyncTask<Void, Void, List<BluetoothDevice>> {

        @Override
        protected List<BluetoothDevice> doInBackground(Void... params) {
            Set<BluetoothDevice> pairedDevices = mBTAdapter.getBondedDevices();
            List<BluetoothDevice> listDevices = new ArrayList<BluetoothDevice>();
            for (BluetoothDevice device : pairedDevices) {
                if(device.getName().equals("HC-06"))
                {
                    listDevices.add(device);
                }
            }
            return listDevices;

        }

        @Override
        protected void onPostExecute(List<BluetoothDevice> listDevices) {
            super.onPostExecute(listDevices);
            if (listDevices.size() > 0) {
                MyAdapter adapter = (MyAdapter) listView.getAdapter();
                adapter.replaceItems(listDevices);
            } else {
                msg("No se encontraron dispositivos Bluetooth emparejados");
            }
        }

    }

    /**
     * Personalizar mi lista de dispositivo
     * @author ryder
     *
     */
    private class MyAdapter extends ArrayAdapter<BluetoothDevice> {
        private int selectedIndex;
        private Context context;
        private int selectedColor = Color.parseColor("#abcdef");
        private List<BluetoothDevice> myList;

        public MyAdapter(Context ctx, int resource, int textViewResourceId, List<BluetoothDevice> objects) {
            super(ctx, resource, textViewResourceId, objects);
            context = ctx;
            myList = objects;
            selectedIndex = -1;
        }

        public void setSelectedIndex(int position) {
            selectedIndex = position;
            notifyDataSetChanged();
        }

        public BluetoothDevice getSelectedItem() {
            return myList.get(selectedIndex);
        }

        @Override
        public int getCount() {
            return myList.size();
        }

        @Override
        public BluetoothDevice getItem(int position) {
            return myList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        private class ViewHolder {
            TextView tv;
        }

        public void replaceItems(List<BluetoothDevice> list) {
            myList = list;
            notifyDataSetChanged();
        }

        public List<BluetoothDevice> getEntireList() {
            return myList;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View vi = convertView;
            ViewHolder holder;
            if (convertView == null) {
                vi = LayoutInflater.from(context).inflate(R.layout.list_item, null);
                holder = new ViewHolder();

                holder.tv = (TextView) vi.findViewById(R.id.lstContent);

                vi.setTag(holder);
            } else {
                holder = (ViewHolder) vi.getTag();
            }

            if (selectedIndex != -1 && position == selectedIndex) {
                holder.tv.setBackgroundColor(selectedColor);
            } else {
                holder.tv.setBackgroundColor(Color.WHITE);
            }
            BluetoothDevice device = myList.get(position);
            holder.tv.setText(device.getName() + "\n " + device.getAddress());

            return vi;
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
