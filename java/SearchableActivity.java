package com.joelbw.BikeRackLocator;

import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONObject;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.joelbw.BikeRackLocator.MyLocation.LocationResult;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

public class SearchableActivity extends ListActivity {
	ArrayList<HashMap<String,String>> list = new ArrayList<HashMap<String,String>>();
	ArrayList<Rack> niceracks = new ArrayList<Rack>();
	BaseAvailableActivity glob;
	ProgressDialog dialog;
	
	String query;
	
	public double getDistance(double lat1, double lng1, double lat2, double lng2) {
		double R = 3959;
		double dLat = Math.toRadians(lat2-lat1);
		double dLng = Math.toRadians(lng2-lng1);
		lat1 = Math.toRadians(lat1);
		lat2 = Math.toRadians(lat2);

		double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
		        Math.sin(dLng/2) * Math.sin(dLng/2) * Math.cos(lat1) * Math.cos(lat2); 
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a)); 
		return Math.round((R * c) * 100.00) / 100.00;
	}
	
	public LocationResult locationResult = new LocationResult() {
		@Override
		public void gotLocation(final Location location) {
			glob.userLat = Double.toString(location.getLatitude());
			glob.userLng = Double.toString(location.getLongitude());
			
			GeoPoint myPoint = new GeoPoint((int)(Double.parseDouble(glob.userLat) * 1e6),
				    						(int)(Double.parseDouble(glob.userLng) * 1e6));
						
			// Now that we have the updated location, fetch the list.
			fetchList();
		};
	};
	
	public void updateList() {
			setListAdapter(new SimpleAdapter(this, list, R.layout.two_line_list_view, new String[] { "firstLine", "secondLine" }, new int[] { R.id.firstLine, R.id.secondLine }));
			
			ListView lv = getListView();
	        lv.setTextFilterEnabled(true);

	        dialog.dismiss();

			lv.setOnItemClickListener(new OnItemClickListener() {
	            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
	            	Intent myIntent = new Intent(getBaseContext(), BikeRackLocatorActivity.class);
	            	myIntent.putExtra("address", niceracks.get(position).address);
	            	myIntent.putExtra("installed", niceracks.get(position).installed);
	            	myIntent.putExtra("community", niceracks.get(position).community);
	            	myIntent.putExtra("latitude", niceracks.get(position).latitude);
	            	myIntent.putExtra("longitude", niceracks.get(position).longitude);
	            	myIntent.putExtra("distance", niceracks.get(position).distance);
	    	        startActivityForResult(myIntent, 0);
	            }
	        });
		//}
	}
	
	public void fetchList() {
		AsyncHttpClient client = new AsyncHttpClient();
        
        RequestParams params = new RequestParams();
        params.put("search", query);
        params.put("lat", glob.userLat);
        params.put("lng", glob.userLng);
        
        client.post("http://joelbw.com/search_racks.php", params, new AsyncHttpResponseHandler() {
	    	@Override
	    	public void onSuccess(String response) {
	    		try {
		    		JSONArray racks = new JSONArray(response);
		    			
		    		int i;
		    		for (i=0;i<racks.length();i++) {
		    			JSONObject rack = racks.getJSONObject(i);
		    			String address = rack.getString("Address");
		    			String installed = rack.getString("Installed");
		    			String community = rack.getString("CommunityName");
		    			String latitude = rack.getString("Latitude");
		    			String longitude = rack.getString("Longitude");
		    			//String distance = rack.getString("Distance");
		    			
	    	        	double distance = getDistance(Double.parseDouble(glob.userLat), Double.parseDouble(glob.userLng), Double.parseDouble(latitude), Double.parseDouble(longitude));

		    			HashMap<String,String> item = new HashMap<String,String>();
		    	        item.put("firstLine", address + ", " + community);
		    	        item.put("secondLine", installed + " rack(s) installed.  " + distance + " miles");
		    	        list.add( item );
		    	        niceracks.add(new Rack(address,installed, community, latitude,longitude, Double.toString(distance)));
		    		}
		    		
		    		updateList();
	    		}
	    		catch (Exception je)
	    		{
	    			Toast.makeText(getBaseContext(), je.getMessage(), Toast.LENGTH_LONG).show();
	    		}
	    	}
	    });
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.search_options_menu, menu);
	    return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection
	    switch (item.getItemId()) {
	    case R.id.search_search:
	    	onSearchRequested();
	    	return true;
	    case R.id.showmap_search:
	    	Intent myIntent = new Intent(getBaseContext(), BikeRackLocatorActivity.class);
	        startActivityForResult(myIntent, 0);
	        return true;
	    default:
	        return super.onOptionsItemSelected(item);
	    }
	}
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get the intent, verify the action and get the query
        Intent intent = getIntent();
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
        	query = intent.getStringExtra(SearchManager.QUERY);
        	glob = ((BaseAvailableActivity)getApplicationContext());

			dialog = ProgressDialog.show(this, "", "Updating list...");

    		fetchList();
        }
    }
}