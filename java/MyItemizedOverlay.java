package com.joelbw.BikeRackLocator;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;

import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.OverlayItem;

public class MyItemizedOverlay extends ItemizedOverlay<OverlayItem> {
	 private ArrayList<OverlayItem> mOverlays = new ArrayList<OverlayItem>();
	 private Context mContext;
	 
	 public MyItemizedOverlay(Drawable defaultMarker, Context context)
	 {
		 super(boundCenterBottom(defaultMarker));
		 mContext = context;
	 }
	 public void addOverlay(OverlayItem overlay)
	 {
		 mOverlays.add(overlay);
		 populate();
	 }
	 @Override
	 protected OverlayItem createItem(int i)
	 {
		 return mOverlays.get(i);
	 }
	 @Override
	 public int size()
	 {
		 return mOverlays.size();
	 }
	 @Override
	 protected boolean onTap(int index)
	 {
		 final OverlayItem item = mOverlays.get(index);
		 
		 if (item.getTitle().equals("Me")) {
			 return true;
		 }
		 else
		 {
			 AlertDialog.Builder dialog = new AlertDialog.Builder(mContext);
			 dialog.setTitle(item.getTitle());
			 dialog.setMessage(item.getSnippet());
			 dialog.setPositiveButton("Directions", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					String latitude = Double.toString(item.getPoint().getLatitudeE6() / 1e6);
					String longitude = Double.toString(item.getPoint().getLongitudeE6() / 1e6);
						
					String dAddr = latitude + "," + longitude;
					BaseAvailableActivity glob = (BaseAvailableActivity)mContext.getApplicationContext();
					String sAddr = glob.userLat + "," + glob.userLng;
					
					String url = "http://maps.google.com/maps?saddr=" + sAddr + "&daddr=" + dAddr + "&dirflg=b";
					
					glob.selectedRack = new Rack(null,null, null, latitude,longitude, null);
					
					Intent myIntent = new Intent(android.content.Intent.ACTION_VIEW,  Uri.parse(url));
					mContext.startActivity(myIntent);
				}
			 });
			 dialog.show();
		 }
		 return true;
	 }
}
