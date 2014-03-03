package com.joelbw.BikeRackLocator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.markupartist.android.widget.ActionBar;
import com.markupartist.android.widget.ActionBar.Action;

public class BikeRackLocatorActivity extends MapActivity {
	private MapView myMapView;
	private List<Overlay> mapOverlays;
    BaseAvailableActivity glob;
    Map<Integer, Float> zFactor = new HashMap<Integer, Float>();
    LegacyLastLocationFinder myNewLocation;
    LocationManager locationManager;
    ActionBar actionBar;
    
    private class SearchAction implements Action {
        @Override
        public int getDrawable() {
            return R.drawable.ic_btn_search;
        }
        
        @Override
        public void performAction(View view) {
            onSearchRequested();
        }
    }
    
    private class ListViewAction implements Action {
    	@Override
    	public int getDrawable() {
    		return R.drawable.ic_menu_database;
    	}
    	
    	@Override
    	public void performAction(View view) {
    		Intent myIntent = new Intent(getBaseContext(), ListViewActivity.class);
	        startActivityForResult(myIntent, 0);
    	}
    }
    
    private class UpdateLocationAction implements Action {
    	@Override
    	public int getDrawable() {
    		return R.drawable.ic_menu_mylocation;
    	}
    	
    	@Override
    	public void performAction(View view) {
    		try {	    		
	    		getLocation();
	    	}
	    	catch (Exception je) {
	    		Toast.makeText(getBaseContext(), "Message: \n" + je.getMessage() + "\nStack Trace:\n" + je.getStackTrace().toString(), Toast.LENGTH_LONG).show();
	    	}
    	}
    }
    
	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}
	
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
	
	public void UpdateMap(String myLat, String myLng) {
		Drawable drawable = this.getResources().getDrawable(R.drawable.rack_location);
		final MyItemizedOverlay itemizedoverlay = new MyItemizedOverlay(drawable,this);
        
        AsyncHttpClient client = new AsyncHttpClient();
        
        actionBar.setProgressBarVisibility(View.VISIBLE);

        RequestParams params = new RequestParams();
        params.put("lat", myLat);
        params.put("lng", myLng);
        params.put("dst", Float.toString(zFactor.get(myMapView.getZoomLevel()) / 8));
        
        mapOverlays.clear();
        
        // Update current location
        Drawable drawable2 = this.getResources().getDrawable(R.drawable.blue_orb);
	    
        MyItemizedOverlay currentlocation = new MyItemizedOverlay(drawable2, this);
        
        GeoPoint myPoint = new GeoPoint((int)(Double.parseDouble(glob.userLat) * 1e6),
        								(int)(Double.parseDouble(glob.userLng) * 1e6));
        OverlayItem myLocation = new OverlayItem(myPoint, "Me", "Me");
        currentlocation.addOverlay(myLocation);
       
        mapOverlays.add(currentlocation);
        
        Drawable selectedRackDrawable = this.getResources().getDrawable(R.drawable.selected_rack);
		final MyItemizedOverlay selectedRackOverlay = new MyItemizedOverlay(selectedRackDrawable,this);
        
	    client.post("http://joelbw.com/racks.php", params, new AsyncHttpResponseHandler() {
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

		    	        if (glob.selectedRack != null && glob.selectedRack.latitude.equals(latitude) && glob.selectedRack.longitude.equals(longitude)) {
			    			GeoPoint point = new GeoPoint((int)(Double.parseDouble(glob.selectedRack.latitude) * 1e6),
			    					  (int)(Double.parseDouble(glob.selectedRack.longitude) * 1e6));
			    			selectedRackOverlay.addOverlay(new OverlayItem(point, glob.selectedRack.address + ", " + glob.selectedRack.community, glob.selectedRack.installed + " rack(s) installed.\nDistance: " + distance + " Miles"));
	
			    			mapOverlays.add(selectedRackOverlay);
		    	        }
		    	        else
		    	        {
			    	        GeoPoint point = new GeoPoint((int)(Double.parseDouble(latitude) * 1e6),
			    	        							  (int)(Double.parseDouble(longitude) * 1e6));
		    	        	itemizedoverlay.addOverlay(new OverlayItem(point, address + ", " + community, installed + " rack(s) installed.\nDistance: " + distance + " Miles."));
		    	        }
		    	    }
		    		
		    		if (itemizedoverlay.size() > 0) {
		    			mapOverlays.add(itemizedoverlay);
		    		}
		    		
		    		actionBar.setProgressBarVisibility(View.GONE);

	    		    myMapView.invalidate();
	    		}
	    		catch (Exception je)
	    		{
	    			Toast.makeText(getBaseContext(), "Error drawing overlay: " + je.getMessage(), Toast.LENGTH_LONG).show();
	    		}
	    	}
	    });
	}
	
	@Override
	public boolean dispatchTouchEvent(MotionEvent event) {
		boolean result = super.dispatchTouchEvent(event);
		if (event.getAction() == MotionEvent.ACTION_UP) {
			GeoPoint center = myMapView.getMapCenter();
			double lat = center.getLatitudeE6() / 1E6;
	        double lng = center.getLongitudeE6() /1E6;
	        int zoomLevel = myMapView.getZoomLevel();
	        if (lat != glob.centerLat && lng != glob.centerLng) {
	        	try {
	        		Location old = new Location("");
	        		Location current = new Location("");
	        		
	        		old.setLatitude(glob.centerLat);
	        		old.setLongitude(glob.centerLng);
	        		current.setLatitude(lat);
	        		current.setLongitude(lng);
	        		
	        		float distance = old.distanceTo(current);
	        		
		        	if (distance > zFactor.get(zoomLevel) || glob.zoomLevel != zoomLevel) {
		        		UpdateMap(Double.toString(lat), Double.toString(lng));
		        	}
		        	
		        	glob.centerLat = lat;
		        	glob.centerLng = lng;
		        	glob.zoomLevel = zoomLevel;
	        	}
	        	catch(Exception je)
	        	{
	        		// Probably a null result...
	        	}
	        }
		}
		return result;
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.options_menu, menu);
	    return true;
	}
	
	public void getLocation() {
        actionBar.setProgressBarVisibility(View.VISIBLE);

    	Location location = myNewLocation.getLastBestLocation(0, 0);
    	
    	glob.userLat = Double.toString(location.getLatitude());
		glob.userLng = Double.toString(location.getLongitude());
		
		// If this is the first update initialize.
		if (glob.centerLat == -1) {
			glob.centerLat = location.getLatitude();
			glob.centerLng = location.getLongitude();
		}
		
		GeoPoint myPoint = new GeoPoint((int)(Double.parseDouble(glob.userLat) * 1e6),
			    (int)(Double.parseDouble(glob.userLng) * 1e6));
		
		MapController mc = myMapView.getController();
		mc.animateTo(myPoint);
		mc.setZoom(18);
		UpdateMap(glob.userLat, glob.userLng);
		
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection
	    switch (item.getItemId()) {
	    case R.id.update_location:
	    	try {	    		
	    		getLocation();
	    	}
	    	catch (Exception je) {
	    		Toast.makeText(getBaseContext(), "OptionItemSelected Message: \n" + je.getMessage() + "\nStack Trace:\n" + je.getStackTrace().toString(), Toast.LENGTH_LONG).show();
	    	}
	        return true;
	    case R.id.search_map:
	    	onSearchRequested();
	    	return true;
	    case R.id.showlist:
		    Intent myIntent = new Intent(getBaseContext(), ListViewActivity.class);
	        startActivityForResult(myIntent, 0);
	        return true;
	    default:
	        return super.onOptionsItemSelected(item);
	    }
	}
	
	// Define a listener that responds to location updates
    LocationListener locationListener = new LocationListener() {
        public void onLocationChanged(Location location) {
        	try {        		
	        	locationManager.removeUpdates(locationListener);
	        	
	        	glob.userLat = Double.toString(location.getLatitude());
	    		glob.userLng = Double.toString(location.getLongitude());
	    		
	    		// If this is the first update initialize.
	    		if (glob.centerLat == -1) {
	    			glob.centerLat = location.getLatitude();
	    			glob.centerLng = location.getLongitude();
	    		}
	    		
	    		GeoPoint myPoint = new GeoPoint((int)(Double.parseDouble(glob.userLat) * 1e6),
	    			    (int)(Double.parseDouble(glob.userLng) * 1e6));

	    		// Do I want to actually update the map?  What if the user is looking far away from where they are at?
	        	//UpdateMap(Double.toString(location.getLatitude()), Double.toString(location.getLongitude()));
        	}
        	catch (Exception je)
        	{
        		Toast.makeText(getBaseContext(), "got update: \n" + je.getMessage() + "\nStack Trace:\n" + je.getStackTrace().toString(), Toast.LENGTH_LONG).show();
        	}
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {}

        public void onProviderEnabled(String provider) {}

        public void onProviderDisabled(String provider) {}
      };
      
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	try {
	        super.onCreate(savedInstanceState);
	        setProgress(0);
	        setContentView(R.layout.main);
	        
	        actionBar = (ActionBar) findViewById(R.id.actionbar);
		    actionBar.setTitle("Bike Chicago");
		    actionBar.addAction(new SearchAction());
		    actionBar.addAction(new ListViewAction());
		    actionBar.addAction(new UpdateLocationAction());
		    	     
	        MapView mapView = (MapView) findViewById(R.id.mapview);
	        myMapView = mapView;
	        mapOverlays = myMapView.getOverlays();
	        
	        glob = ((BaseAvailableActivity)getApplicationContext());
	        
	        myNewLocation = new LegacyLastLocationFinder(getApplicationContext());
	        
	        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
	        Criteria criteria = new Criteria();
	        criteria.setAccuracy(Criteria.ACCURACY_FINE);
	        
	        // Register the listener with the Location Manager to receive location updates
	        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
	        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 35000, 1500,locationListener);
	        
	        // Initialize my zoom factor map...
	        zFactor.put((int)21, (float)10);
    		zFactor.put((int)20, (float)20);
    		zFactor.put((int)19, (float)40);
    		zFactor.put((int)18, (float)80);	
    		zFactor.put((int)17, (float)160);
    		zFactor.put((int)16, (float)240);
    		zFactor.put((int)15, (float)560);
    		zFactor.put((int)14, (float)1120);
    		zFactor.put((int)13, (float)2240);
    		zFactor.put((int)12, (float)4480);
    		zFactor.put((int)11, (float)8960);
    		zFactor.put((int)10, (float)17920);
    		zFactor.put((int)9, (float)35840);
    		zFactor.put((int)8, (float)71680);
    		zFactor.put((int)7, (float)143360);
    		zFactor.put((int)6, (float)286720);
    		zFactor.put((int)5, (float)573440);
    		zFactor.put((int)4, (float)1146880);
    		zFactor.put((int)3, (float)2293760);
    		zFactor.put((int)2, (float)4587520);
    		zFactor.put((int)1, (float)9175040);
    		
	        Bundle extras = getIntent().getExtras();
        	
	        if (extras != null) {
	        	String address = extras.getString("address");
    			String installed = extras.getString("installed");
    			String community = extras.getString("community");
    			String latitude = extras.getString("latitude");
    			String longitude = extras.getString("longitude");
    			String distance = extras.getString("distance");
    			
    			glob.centerLat = Double.parseDouble(latitude);
    			glob.centerLng = Double.parseDouble(longitude);

    			GeoPoint point = new GeoPoint((int)(Double.parseDouble(latitude) * 1e6),
						  (int)(Double.parseDouble(longitude) * 1e6));
    			
    			glob.selectedRack = new Rack(address,installed, community, latitude,longitude, distance);
    			
    			MapController mc = myMapView.getController();
    			mc.animateTo(point);
    			mc.setZoom(19);
    			UpdateMap(latitude, longitude);
	        }
	        else
	        {
	        	getLocation();
	        }
    	}
    	catch (Exception je)
    	{
    		Toast.makeText(getBaseContext(), "onCreate Message: \n" + je.getMessage() + "\nStack Trace:\n" + je.getStackTrace().toString(), Toast.LENGTH_LONG).show();
    	}
    }
}