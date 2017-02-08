package com.kobitate.gscampusmap;

import android.graphics.Color;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.ArrayMap;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

	private GoogleMap mMap;
	private JSONObject buildings;
	private ArrayMap<String, JSONObject> polygons;

	Polygon lastPolygon = null;

	private final double 	START_LAT 	=  32.421205;
	private final double 	START_LNG 	= -81.782044;
	private final float 	START_ZOOM 	=  14.0f;

	private final int 		POLYGON_ALPHA 					= 77;
	private final float 	POLYGON_STROKE_WIDTH 			= 3.0f;
	private final float 	POLYGON_STROKE_WIDTH_SELECTED 	= 6.0f;

	private TextView 		infoTitle;
	private TextView		infoBuildingNumber;
	private CardView		infoCard;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_maps);
		// Obtain the SupportMapFragment and get notified when the map is ready to be used.
		SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
				.findFragmentById(R.id.map);
		mapFragment.getMapAsync(this);

		infoTitle = (TextView) findViewById(R.id.infoTitle);
		infoBuildingNumber = (TextView) findViewById(R.id.infoBuildingNumber);
		infoCard = (CardView) findViewById(R.id.infoCard);

		polygons = new ArrayMap<>();
	}

	@Override
	public void onMapReady(GoogleMap googleMap) {
		mMap = googleMap;

		LatLng startPos = new LatLng(START_LAT, START_LNG);
		mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(startPos, START_ZOOM));

		buildings = parseBuildings();

		try {
			addPolygons();
		} catch (JSONException e) {
			Log.e(getString(R.string.app_name), "Error adding buildings");
			e.printStackTrace();
		}
	}

	// Function from Stack Overflow, CC BY-SA 3.0
	// Source: http://stackoverflow.com/a/19945484/1465353
	private JSONObject parseBuildings() {
		String json = null;
		try {
			InputStream in = getAssets().open("buildings.json");
			int size = in.available();
			byte[] buffer = new byte[size];
			in.read(buffer);
			in.close();
			json = new String(buffer, "UTF-8");
			return new JSONObject(json);

		} catch (IOException e) {
			Log.e(getString(R.string.app_name), "Error opening JSON file");
			e.printStackTrace();
		} catch (JSONException e) {
			Log.e(getString(R.string.app_name), "Error parsing JSON file to JSONObject");
			e.printStackTrace();
		}
		return null;
	}

	private void addPolygons() throws JSONException {
		Iterator<String> keys = buildings.keys();
		while (keys.hasNext()) {
			String key = keys.next();
			JSONObject b = buildings.getJSONObject(key);

			String polygonCoordString = b.getString("polygon_coords");

			if (polygonCoordString.length() == 0) {
				continue;
			}

			polygonCoordString = polygonCoordString.substring(1);
			polygonCoordString = polygonCoordString.substring(0, polygonCoordString.length()-1);

			String[] polygonCoords = polygonCoordString.split("],\\[");
			PolygonOptions polygonOptions = new PolygonOptions();

			for (String coords : polygonCoords) {
				String[] coordsSplit = coords.split(",");
				double lat = Double.valueOf(coordsSplit[0]);
				double lng = Double.valueOf(coordsSplit[1]);
				polygonOptions.add(new LatLng(lat, lng));
			}

			System.out.println(b.getString("polygon_type"));

			switch (b.getString("polygon_type")) {
				case "academic":
					polygonOptions.strokeColor(ContextCompat.getColor(getApplicationContext(), R.color.mapAcademic));
					polygonOptions.fillColor(alpha(R.color.mapAcademic, POLYGON_ALPHA));
					break;
				case "admin":
					polygonOptions.strokeColor(ContextCompat.getColor(getApplicationContext(), R.color.mapAdmin));
					polygonOptions.fillColor(alpha(R.color.mapAdmin, POLYGON_ALPHA));
					break;
				case "athletics":
					polygonOptions.strokeColor(ContextCompat.getColor(getApplicationContext(), R.color.mapAthletics));
					polygonOptions.fillColor(alpha(R.color.mapAthletics, POLYGON_ALPHA));
					break;
				case "residential":
					polygonOptions.strokeColor(ContextCompat.getColor(getApplicationContext(), R.color.mapResidential));
					polygonOptions.fillColor(alpha(R.color.mapResidential, POLYGON_ALPHA));
					break;
				case "student":
					polygonOptions.strokeColor(ContextCompat.getColor(getApplicationContext(), R.color.mapStudent));
					polygonOptions.fillColor(alpha(R.color.mapStudent, POLYGON_ALPHA));
					break;
				case "support":
					polygonOptions.strokeColor(ContextCompat.getColor(getApplicationContext(), R.color.mapSupport));
					polygonOptions.fillColor(alpha(R.color.mapSupport, POLYGON_ALPHA));
					break;
			}

			polygonOptions.strokeWidth(POLYGON_STROKE_WIDTH);
			polygonOptions.clickable(true);

			Polygon polygon = mMap.addPolygon(polygonOptions);

			polygons.put(polygon.getId(), b);

		}

		mMap.setOnPolygonClickListener(new GoogleMap.OnPolygonClickListener() {
			@Override
			public void onPolygonClick(Polygon polygon) {
				if (infoCard.getVisibility() == View.GONE) {
					infoCard.setVisibility(View.VISIBLE);
					infoCard.setAlpha(1.0f);

				}

				polygon.setStrokeWidth(POLYGON_STROKE_WIDTH_SELECTED);
				if (lastPolygon != null) {
					lastPolygon.setStrokeWidth(POLYGON_STROKE_WIDTH);
				}

				lastPolygon = polygon;

				try {
					JSONObject p = polygons.get(polygon.getId());
					infoTitle.setText(p.getString("name_popup"));
					// @TODO Do the string like you're supposed to
					infoBuildingNumber.setText("Building #" + p.getString("bldg_number"));
				} catch (JSONException e) {
					e.printStackTrace();
				}

			}
		});

		mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
			@Override
			public void onMapClick(LatLng latLng) {
				if (infoCard.getVisibility() == View.VISIBLE) {
					infoCard.setVisibility(View.GONE);
					infoCard.setAlpha(0.0f);
				}
				if (lastPolygon != null) {
					lastPolygon.setStrokeWidth(POLYGON_STROKE_WIDTH);
				}
			}
		});
	}

	public int alpha(int color, int alpha) {
		color = ContextCompat.getColor(getApplicationContext(), color);
		int red = Color.red(color);
		int blue = Color.blue(color);
		int green = Color.green(color);
		return Color.argb(alpha, red, green, blue);
	}

}
