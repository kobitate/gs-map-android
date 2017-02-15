package com.kobitate.gscampusmap;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.ArrayMap;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.CardView;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.flipboard.bottomsheet.BottomSheetLayout;
import com.flipboard.bottomsheet.OnSheetDismissedListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

	private GoogleMap 			mMap;
	private JSONObject 			buildings;
	private ArrayMap<String, JSONObject> polygons;

	private final double 		START_LAT = 	32.421205;
	private final double 		START_LNG = 	-81.782044;
	private final float 		START_ZOOM = 	14.0f;

	private final int 			POLYGON_ALPHA = 					77;
	private final float 		POLYGON_STROKE_WIDTH = 				3.0f;
	private final float 		POLYGON_STROKE_WIDTH_SELECTED = 	6.0f;

	private final int 			REQUEST_LOCATION_PERMISSION = 		0;

	private TextView 			infoTitle;
	private TextView 			infoBuildingNumber;
	private BottomSheetLayout 	infoCard;
	private TextView 			infoAddress;
	private TextView 			infoDetails;
	private TextView 			infoTypeText;
	private CardView 			infoType;
	private AppCompatImageView 	infoTypeIcon;

	private CardView 			searchCard;
	private LinearLayout 		searchOuter;
	private EditText 			searchBox;
	private ListView 			searchResults;

	private Resources 			res;
	private Polygon 			lastPolygon = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_maps);

		setupMap();
		setupInfoCard();

		polygons = new ArrayMap<>();

		searchCard = 		(CardView) 		findViewById(R.id.searchCard);
		searchOuter = 		(LinearLayout) 	findViewById(R.id.searchOuter);
		searchBox = 		(EditText) 		findViewById(R.id.searchBox);
		searchResults = 	(ListView) 		findViewById(R.id.searchResults);

		searchOuter.setPadding(0, getStatusBarHeight(), 0, 0);
		searchBox.setHint(R.string.search_placeholder);

		res = getResources();


	}

	private void setupMap() {
		// Obtain the SupportMapFragment and get notified when the map is ready to be used.
		SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
				.findFragmentById(R.id.map);
		mapFragment.getMapAsync(this);
	}

	private void setupInfoCard() {

		infoCard = (BottomSheetLayout) findViewById(R.id.infoCard);
		infoCard.setShouldDimContentView(false);
		infoCard.setInterceptContentTouch(false);
		infoCard.setPeekSheetTranslation(300);

		infoCard.addOnSheetDismissedListener(new OnSheetDismissedListener() {
			@Override
			public void onDismissed(BottomSheetLayout bottomSheetLayout) {
				infoTitle = null;
				infoBuildingNumber = null;
				if (lastPolygon != null) {
					lastPolygon.setStrokeWidth(POLYGON_STROKE_WIDTH);
				}

			}
		});
	}

	@Override
	public void onMapReady(GoogleMap googleMap) {
		mMap = googleMap;

		LatLng startPos = new LatLng(START_LAT, START_LNG);
		mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(startPos, START_ZOOM));
		mMap.setPadding(0, searchOuter.getMeasuredHeight() - 32, 0, 0);

		buildings = parseBuildings();

		try {
			addPolygons();
		} catch (JSONException e) {
			Log.e(getString(R.string.app_name), "Error adding buildings");
			e.printStackTrace();
		}

		if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
			ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
			return;
		}
		mMap.setMyLocationEnabled(true);
	}

	// Function from Stack Overflow, CC BY-SA 3.0
	// Source: http://stackoverflow.com/a/19945484/1465353
	private JSONObject parseBuildings() {
		String json;
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

				clearSearchFocus();

				polygon.setStrokeWidth(POLYGON_STROKE_WIDTH_SELECTED);
				if (lastPolygon != null && !lastPolygon.equals(polygon)) {
					lastPolygon.setStrokeWidth(POLYGON_STROKE_WIDTH);
				}

				lastPolygon = polygon;

				try {
					JSONObject p = polygons.get(polygon.getId());

					if (infoTitle == null) {
						infoCard.showWithSheetView(getLayoutInflater().inflate(R.layout.building_info, infoCard, false));
					}
					else {
						infoCard.peekSheet();
					}

					infoTitle = 			(TextView) 				infoCard.findViewById(R.id.infoTitle);
					infoBuildingNumber = 	(TextView) 				infoCard.findViewById(R.id.infoBuildingNumber);
					infoAddress = 			(TextView) 				infoCard.findViewById(R.id.infoAddress);
					infoDetails = 			(TextView) 				infoCard.findViewById(R.id.infoDetails);
					infoTypeText = 			(TextView) 				infoCard.findViewById(R.id.infoTypeText);
					infoType =				(CardView) 				infoCard.findViewById(R.id.infoType);
					infoTypeIcon = 			(AppCompatImageView) 	infoCard.findViewById(R.id.infoTypeIcon);

					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
						infoTitle.setText(Html.fromHtml(p.getString("name_popup"), Html.FROM_HTML_MODE_LEGACY));
						if (p.getString("bldg_details") == null || p.getString("bldg_details").equals("null")) {
							infoDetails.setText("");
						}
						else {
							infoDetails.setText(Html.fromHtml(p.getString("bldg_details"), Html.FROM_HTML_MODE_LEGACY));
						}
					}
					else {
						infoTitle.setText(Html.fromHtml(p.getString("name_popup")));
						if (p.getString("bldg_details") == null) {
							infoDetails.setText("");
						}
						else {
							infoDetails.setText(Html.fromHtml(p.getString("bldg_details")));
						}
					}

					// @TODO Do the string like you're supposed to
					if (p.getString("bldg_number") == null || p.getString("bldg_number").equals("null")) {
						infoBuildingNumber.setText("");
					}
					else {
						String buildingNumberString = String.format(res.getString(R.string.building_number), p.getString("bldg_number"));
						infoBuildingNumber.setText(buildingNumberString);

					}
					infoAddress.setText(p.getString("loc_address"));

					switch (p.getString("polygon_type")) {
						case "academic":
							infoType.setCardBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.mapAcademic));
							infoTypeIcon.setImageResource(R.drawable.academic);
							infoTypeText.setText(res.getString(R.string.building_type_academic));
							break;
						case "admin":
							infoType.setCardBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.mapAdmin));
							infoTypeIcon.setImageResource(R.drawable.admin);
							infoTypeText.setText(res.getString(R.string.building_type_admin));
							break;
						case "athletics":
							infoType.setCardBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.mapAthletics));
							infoTypeIcon.setImageResource(R.drawable.athletics);
							infoTypeText.setText(res.getString(R.string.building_type_athletics));
							break;
						case "residential":
							infoType.setCardBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.mapResidential));
							infoTypeIcon.setImageResource(R.drawable.residential);
							infoTypeText.setText(res.getString(R.string.building_type_residential));
							break;
						case "student":
							infoType.setCardBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.mapStudent));
							infoTypeIcon.setImageResource(R.drawable.student);
							infoTypeText.setText(res.getString(R.string.building_type_student));
							break;
						case "support":
							infoType.setCardBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.mapSupport));
							infoTypeIcon.setImageResource(R.drawable.support);
							infoTypeText.setText(res.getString(R.string.building_type_support));
							break;
						default:
							infoType.setVisibility(View.GONE);
					}

				} catch (JSONException e) {
					e.printStackTrace();
				}

			}
		});

		mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
			@Override
			public void onMapClick(LatLng latLng) {
				infoCard.dismissSheet();
				clearSearchFocus();
			}
		});
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		if (requestCode == REQUEST_LOCATION_PERMISSION) {
			if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				mMap.setMyLocationEnabled(true);
				Toast.makeText(this, "Location Permission Granted", Toast.LENGTH_SHORT).show();
			}
		}
	}

	private int alpha(int color, int alpha) {
		color = ContextCompat.getColor(getApplicationContext(), color);

		int red = 		Color.red(color);
		int blue = 		Color.blue(color);
		int green = 	Color.green(color);

		return Color.argb(alpha, red, green, blue);
	}

	private int getStatusBarHeight() {
		int result = 0;
		int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
		if (resourceId > 0) {
			result = getResources().getDimensionPixelSize(resourceId);
		}
		return result;
	}

	private void clearSearchFocus() {
		searchBox.clearFocus();
		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(searchBox.getWindowToken(), 0);
	}



}
