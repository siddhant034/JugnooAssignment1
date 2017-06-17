package com.example.siddhant.jugnooassignment1;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements LocationListener {

    private SearchView search_view;
    private ListView search_results_list_view;
    private ArrayList<Item> itemResults = new ArrayList<>();
    private ArrayList<Item> hotelResults = new ArrayList<>();
    private LocationManager locationManager;
    private String latitude, longitude;

    public String loadJSONFromAsset() {
        String json = null;
        try
        {
            InputStream is = this.getAssets().open("hotels.json");
            int size = is.available();
            byte buffer[] = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");
        }
        catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            return;
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, (LocationListener) this);

        search_view = (SearchView)findViewById(R.id.searchView);
        search_view.setQueryHint("Start typing to search...");
        search_results_list_view = (ListView)findViewById(R.id.listView);

        search_view.setOnQueryTextListener(new SearchView.OnQueryTextListener()
        {
            @Override
            public boolean onQueryTextSubmit(String query)
            {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText)
            {
                if(newText.length()>=1)
                {
                    search_results_list_view.setVisibility(View.VISIBLE);
                    new myAsyncTask().execute(newText);
                }
                else
                {
                    search_results_list_view.setVisibility(View.INVISIBLE);
                }
                return false;
            }
        });
    }

    String getAddress(String details)                       //returns hotel address out of the hotel details
    {
        int j;
        for (j = 0; j < details.length(); j++)
        {
            if (details.charAt(j) == ',')
            {
                break;
            }
        }
        j+=2;
        return details.substring(j);
    }

    Boolean checkAddress(String textSearch, String address)             //matches the search text with parts of address
    {
        String[] splitAdresses = address.split(", ");
        for(int i=0;i<splitAdresses.length;i++)
        {
            if(splitAdresses[i].toLowerCase().startsWith(textSearch.toLowerCase()))
                return true;
        }
        return false;
    }

    @Override
    public void onLocationChanged(Location location) {
        longitude = String.valueOf(location.getLongitude());
        latitude = String.valueOf(location.getLatitude());
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    class myAsyncTask extends AsyncTask<String,Void,String>
    {
        JSONArray place_json_array, hotel_json_array;
        String url = new String();
        String textSearch;
        ProgressDialog pd;

        @Override
        protected void onPreExecute()
        {
            super.onPreExecute();
            place_json_array = new JSONArray();
            hotel_json_array = new JSONArray();
            itemResults = new ArrayList<>();
            hotelResults = new ArrayList<>();
            pd = new ProgressDialog(MainActivity.this);
            pd.setCancelable(false);
            pd.setMessage("Searching...");
            pd.getWindow().setGravity(Gravity.CENTER);
            pd.show();
        }

        public String getURL(String input)
        {
            StringBuilder urlString = new StringBuilder();
            urlString.append("https://maps.googleapis.com/maps/api/place/autocomplete/json");
            urlString.append("?input=");
            try
            {
                urlString.append(URLEncoder.encode(input, "utf8"));
            }
            catch (UnsupportedEncodingException e)
            {
                e.printStackTrace();
            }
            urlString.append("&location=");
            urlString.append(latitude + "," + longitude);
            urlString.append("&key=" + "AIzaSyA5Qq29XD4oQxsLMT1cBCVt-x83hvpXhGE");
            return urlString.toString();
        }

        @Override
        protected String doInBackground(String... params)
        {
            this.textSearch = params[0];
            url = getURL(textSearch);
            String returnResult = getItemList(url);
            return returnResult;
        }

        public String getItemList(String url)
        {
            Item tempItem;
            HTTPHandler sh = new HTTPHandler();
            String jsonStr1 = sh.makeServiceCall(url);
            String jsonStr2 = loadJSONFromAsset();
            try
            {
                place_json_array = new JSONObject(jsonStr1).getJSONArray("predictions");
                hotel_json_array = new JSONObject(jsonStr2).getJSONArray("hotels");
                for(int i = 0;i < hotel_json_array.length(); i++)
                {
                    tempItem = new Item();
                    JSONObject obj = hotel_json_array.getJSONObject(i);
                    String details = obj.getString("details");

                    String address = getAddress(details);
                    if (checkAddress(textSearch,address))
                    {
                        tempItem.setItemDetails(obj.getString("details"));
                        hotelResults.add(tempItem);
                    }
                }
               // Log.i("hotelResults",hotelResults.toString());
                for (int i = 0; i < place_json_array.length(); i++)
                {
                    tempItem = new Item();
                    JSONObject obj = place_json_array.getJSONObject(i);
                    tempItem.setItemDetails(obj.getString("description"));
                    itemResults.add(tempItem);
                }
            } catch (Exception e)
            {
                e.printStackTrace();
                return ("Exception Caught");
            }
            return "OK";
        }

        @Override
        protected void onPostExecute(String s)
        {
            super.onPostExecute(s);
            if (s.equalsIgnoreCase("Exception Caught"))
            {
                Toast.makeText(getApplicationContext(), "Unable to connect to the server", Toast.LENGTH_LONG).show();
                pd.dismiss();
            }
            else if(s.equalsIgnoreCase("No result"))
            {
                Toast.makeText(getApplicationContext(), "No results found", Toast.LENGTH_LONG).show();
                pd.dismiss();
            }
            else
            {
                for(int i=0;i<itemResults.size();i++)
                    hotelResults.add(itemResults.get(i));
                search_results_list_view.setAdapter(new SearchResultsAdapter(getApplicationContext(), hotelResults));
                pd.dismiss();
            }
        }

    }

    class SearchResultsAdapter extends BaseAdapter
    {
        private LayoutInflater layoutInflater;

        private ArrayList<Item> itemDetails = new ArrayList<Item>();
        int count;
        Context context;

        public SearchResultsAdapter(Context context, ArrayList<Item> item_details)
        {
            layoutInflater = LayoutInflater.from(context);
            this.itemDetails = item_details;
            this.count = item_details.size();
            this.context = context;
        }

        public int getCount()
        {
            return count;
        }

        public Object getItem(int arg0)
        {
            return itemDetails.get(arg0);
        }

        public long getItemId(int arg0)
        {
            return arg0;
        }

        public View getView(int position, View convertView, ViewGroup parent)
        {
            ViewHolder holder;
            Item tempItem = itemDetails.get(position);
            if (convertView == null)
            {
                convertView = layoutInflater.inflate(R.layout.items_layout, null);
                holder = new ViewHolder();
                holder.item_details = (TextView) convertView.findViewById(R.id.item_details);
                convertView.setTag(holder);
            }
            else
            {
                holder = (ViewHolder) convertView.getTag();
            }

            holder.item_details.setText(tempItem.getItemDetails());
            return convertView;
        }
    }

    static class ViewHolder
    {
        TextView item_details;
    }

}
