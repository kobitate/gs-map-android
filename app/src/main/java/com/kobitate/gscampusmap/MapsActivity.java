package com.kobitate.gscampusmap;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.ArrayMap;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.CardView;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.algolia.search.saas.AlgoliaException;
import com.algolia.search.saas.Client;
import com.algolia.search.saas.CompletionHandler;
import com.algolia.search.saas.Index;
import com.algolia.search.saas.Query;
import com.flipboard.bottomsheet.BottomSheetLayout;
import com.flipboard.bottomsheet.OnSheetDismissedListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.interfaces.OnCheckedChangeListener;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.SecondarySwitchDrawerItem;
import com.mikepenz.materialdrawer.model.SectionDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;

import de.psdev.licensesdialog.LicensesDialogFragment;
import de.psdev.licensesdialog.licenses.ApacheSoftwareLicense20;
import de.psdev.licensesdialog.licenses.BSD3ClauseLicense;
import de.psdev.licensesdialog.licenses.License;
import de.psdev.licensesdialog.model.Notice;
import de.psdev.licensesdialog.model.Notices;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

	private GoogleMap 			mMap;
	private JSONObject 			buildings;
	private ArrayMap<String, JSONObject> polygons;
	private ArrayMap<String, Polygon> polygonsByBuildingID;
	private ArrayMap<String, ArrayList<Polygon>> polygonCategories;

	private final double 		START_LAT = 						32.42299418602006;
	private final double 		START_LNG = 						-81.78550992161036;
	private final float 		START_ZOOM = 						14.56219f;

	private final int 			POLYGON_ALPHA = 					77;
	private final float 		POLYGON_STROKE_WIDTH = 				3.0f;
	private final float 		POLYGON_STROKE_WIDTH_SELECTED = 	6.0f;

	private final int 			REQUEST_LOCATION_PERMISSION = 		0;

	private final int			DRAWER_ITEM_HOME = 					1;
	private final int			DRAWER_ITEM_ABOUT = 				2;
	private final int			DRAWER_ITEM_GITHUB = 				3;

	private final int			DRAWER_SWITCH_ACADEMIC = 			4;
	private final int			DRAWER_SWITCH_ADMIN = 				5;
	private final int			DRAWER_SWITCH_ATHLETICS = 			6;
	private final int			DRAWER_SWITCH_RESIDENTIAL =			7;
	private final int			DRAWER_SWITCH_STUDENT = 			8;
	private final int			DRAWER_SWITCH_SUPPORT = 			9;

	private Drawer				drawer;

	private TextView 			infoTitle;
	private TextView 			infoBuildingNumber;
	private BottomSheetLayout 	infoCard;
	private TextView 			infoAddress;
	private TextView 			infoAddressCity;
	private TextView 			infoDetails;
	private TextView 			infoTypeText;
	private CardView 			infoType;
	private AppCompatImageView 	infoTypeIcon;

	private AppCompatImageView	menuLaunch;

	private CardView 			searchCard;
	private LinearLayout 		searchOuter;
	private EditText 			searchBox;
	private ListView 			searchResults;
	private RelativeLayout 		searchResultsOuter;

	private ArrayList<JSONObject> lastSearch;

	private Resources 			res;
	private Polygon 			lastPolygon = null;

	private Client				algolia;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_maps);

		res = getResources();

		setupDrawer();

		algolia = new Client(res.getString(R.string.algolia_app_id), res.getString(R.string.algolia_api_key));

		setupMap();
		setupInfoCard();
		setupSearch();

		polygons = 				new ArrayMap<>();
		polygonsByBuildingID = 	new ArrayMap<>();

		polygonCategories = 	new ArrayMap<>();

		polygonCategories.put("academic", 		new ArrayList<Polygon>());
		polygonCategories.put("admin", 			new ArrayList<Polygon>());
		polygonCategories.put("athletics", 		new ArrayList<Polygon>());
		polygonCategories.put("residential", 	new ArrayList<Polygon>());
		polygonCategories.put("student", 		new ArrayList<Polygon>());
		polygonCategories.put("support", 		new ArrayList<Polygon>());



	}

	private void setupDrawer() {


		OnCheckedChangeListener drawerSwitchListener = new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(IDrawerItem drawerItem, CompoundButton buttonView, boolean isChecked) {
				String hideType;
				switch ((int) drawerItem.getIdentifier()) {
					case DRAWER_SWITCH_ACADEMIC :
						hideType = "academic";
						break;
					case DRAWER_SWITCH_ADMIN :
						hideType = "admin";
						break;
					case DRAWER_SWITCH_ATHLETICS :
						hideType = "athletics";
						break;
					case DRAWER_SWITCH_RESIDENTIAL :
						hideType = "residential";
						break;
					case DRAWER_SWITCH_STUDENT :
						hideType = "student";
						break;
					case DRAWER_SWITCH_SUPPORT :
						hideType = "support";
						break;
					default:
						hideType = "";
				}
				ArrayList<Polygon> polygonsToHide = polygonCategories.get(hideType);
				for (Polygon p : polygonsToHide) {
					p.setVisible(isChecked);
					p.setClickable(isChecked);
				}
			}
		};

		drawer = new DrawerBuilder()
				.withActivity(this)
				.withHeader(R.layout.drawer_header)
				.withTranslucentStatusBar(false)
				.addDrawerItems(

						new PrimaryDrawerItem()
								.withIdentifier(DRAWER_ITEM_HOME)
								.withName(R.string.drawer_map_home)
								.withIcon(R.drawable.map),

						////////// BUILDING TYPE TOGGLES //////////
						new SecondarySwitchDrawerItem()
								.withName(R.string.building_type_academic)
								.withChecked(true)
								.withSelectable(false)
								.withIdentifier(DRAWER_SWITCH_ACADEMIC)
								.withOnCheckedChangeListener(drawerSwitchListener),
						new SecondarySwitchDrawerItem()
								.withName(R.string.building_type_admin)
								.withChecked(true)
								.withSelectable(false)
								.withIdentifier(DRAWER_SWITCH_ADMIN)
								.withOnCheckedChangeListener(drawerSwitchListener),
						new SecondarySwitchDrawerItem()
								.withName(R.string.building_type_athletics)
								.withChecked(true)
								.withSelectable(false)
								.withIdentifier(DRAWER_SWITCH_ATHLETICS)
								.withOnCheckedChangeListener(drawerSwitchListener),
						new SecondarySwitchDrawerItem()
								.withName(R.string.building_type_residential)
								.withChecked(true)
								.withSelectable(false)
								.withIdentifier(DRAWER_SWITCH_RESIDENTIAL)
								.withOnCheckedChangeListener(drawerSwitchListener),
						new SecondarySwitchDrawerItem()
								.withName(R.string.building_type_student)
								.withChecked(true)
								.withSelectable(false)
								.withIdentifier(DRAWER_SWITCH_STUDENT)
								.withOnCheckedChangeListener(drawerSwitchListener),
						new SecondarySwitchDrawerItem()
								.withName(R.string.building_type_support)
								.withChecked(true)
								.withSelectable(false)
								.withIdentifier(DRAWER_SWITCH_SUPPORT)
								.withOnCheckedChangeListener(drawerSwitchListener),

						new SectionDrawerItem()
								.withName(R.string.drawer_section_about),

						new PrimaryDrawerItem()
								.withIdentifier(DRAWER_ITEM_ABOUT)
								.withName(R.string.drawer_legal)
								.withIcon(R.drawable.book),
						new PrimaryDrawerItem()
								.withIdentifier(DRAWER_ITEM_GITHUB)
								.withName(R.string.drawer_github)
								.withIcon(R.drawable.new_window)
				)
				.withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
					@Override
					public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
						switch ((int) drawerItem.getIdentifier()) {
							case DRAWER_ITEM_HOME:

								break;
							case DRAWER_ITEM_ABOUT:
								showNotices();
								break;
							case DRAWER_ITEM_GITHUB:
								drawer.setSelection(DRAWER_ITEM_HOME);
								Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://github.com/TheRealGitCub/GSCampusMap"));
								startActivity(browserIntent);
								break;
						}
						return true;
					}
				})
				.build();

		drawer.getDrawerLayout().setFitsSystemWindows(false);

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

	private void setupSearch() {
		searchCard = 			(CardView) 			findViewById(R.id.searchCard);
		searchOuter = 			(LinearLayout) 		findViewById(R.id.searchOuter);
		searchBox = 			(EditText) 			findViewById(R.id.searchBox);
		searchResults = 		(ListView) 			findViewById(R.id.searchResults);
		searchResultsOuter =	(RelativeLayout)	findViewById(R.id.searchResultsOuter);

		menuLaunch = 			(AppCompatImageView)findViewById(R.id.menuLaunch);

		menuLaunch.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				drawer.openDrawer();
			}
		});

		searchOuter.setPadding(0, getStatusBarHeight(), 0, 0);
		searchBox.setHint(R.string.search_placeholder);


		final Index search = algolia.getIndex("dev_campusmap");

		final CompletionHandler searchCompletionHandler = new CompletionHandler() {
			@Override
			public void requestCompleted(JSONObject result, AlgoliaException e) {

				ArrayList<String[]> adapterResult = new ArrayList<>();
				lastSearch = new ArrayList<>();

				try {
					JSONArray hits = result.getJSONArray("hits");
					for (int i = 0; i < hits.length(); i++) {
						JSONObject b = hits.getJSONObject(i);
						Polygon p = polygonsByBuildingID.get(b.getString("objectID"));

						if (p != null) {
							if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
								adapterResult.add(new String[]{
										Html.fromHtml(b.getString("name"), Html.FROM_HTML_MODE_LEGACY).toString()
								});
							}
							else {
								//noinspection deprecation
								adapterResult.add(new String[]{
										Html.fromHtml(b.getString("name")).toString()
								});
							}
							lastSearch.add(b);
						}

					}
				} catch (JSONException e1) {
					e1.printStackTrace();
				}

				SearchAdapter resultsAdapter = new SearchAdapter(getApplicationContext(), R.id.searchResults, adapterResult);
				searchResults.setAdapter(resultsAdapter);
				searchResultsOuter.setVisibility(View.VISIBLE);


			}
		};

		searchBox.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

			}

			@Override
			public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

			}

			@Override
			public void afterTextChanged(Editable editable) {
				String searchQuery = searchBox.getText().toString();
				if (searchQuery.length() == 0) {
					searchResultsOuter.setVisibility(View.GONE);
				}
				else {
					search.searchAsync(new Query(searchQuery), searchCompletionHandler);
				}
			}
		});

		searchBox.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			@Override
			public void onFocusChange(View view, boolean focused) {
				if (focused && searchBox.getText().length() > 0) {
					searchResultsOuter.setVisibility(View.VISIBLE);
					infoCard.dismissSheet();
				}
			}
		});

		searchResults.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
				try {

					JSONObject b = lastSearch.get(i);
					Polygon p = polygonsByBuildingID.get(b.getString("objectID"));

					if (p == null) {
						String toastString = String.format(res.getString(R.string.search_building_notfound), b.getString("objectID"));
						Toast.makeText(MapsActivity.this, toastString, Toast.LENGTH_LONG).show();
					}
					else {
						polygonClick(p);
						searchResultsOuter.setVisibility(View.GONE);

						LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
						for (LatLng latLng : p.getPoints()) {
							boundsBuilder.include(latLng);
						}

						LatLngBounds bounds = boundsBuilder.build();

						mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 300));
					}

				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		});
	}

	@Override
	public void onMapReady(GoogleMap googleMap) {
		mMap = googleMap;

		LatLng startPos = new LatLng(START_LAT, START_LNG);
		mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(startPos, START_ZOOM));
		mMap.setPadding(0, 230, 0, 0);

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
			//noinspection ResultOfMethodCallIgnored
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
			polygonsByBuildingID.put(b.getString("name_js"), polygon);
			polygonCategories.get(b.getString("polygon_type")).add(polygon);

		}

		mMap.setOnPolygonClickListener(new GoogleMap.OnPolygonClickListener() {
			@Override
			public void onPolygonClick(Polygon polygon) {
				polygonClick(polygon);
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

	private void polygonClick(Polygon polygon) {
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
			infoAddressCity = 		(TextView)				infoCard.findViewById(R.id.infoAddressCity);
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
				//noinspection deprecation
				infoTitle.setText(Html.fromHtml(p.getString("name_popup")));
				if (p.getString("bldg_details") == null) {
					infoDetails.setText("");
				}
				else {
					//noinspection deprecation
					infoDetails.setText(Html.fromHtml(p.getString("bldg_details")));
				}
			}

			if (p.getString("bldg_number") == null || p.getString("bldg_number").equals("null")) {
				infoBuildingNumber.setText("");
			}
			else {
				String buildingNumberString = String.format(res.getString(R.string.building_number), p.getString("bldg_number"));
				infoBuildingNumber.setText(buildingNumberString);

			}

			if (p.getString("loc_address") == null || p.getString("loc_address").equals("null")) {
				infoAddress.setText("");
				infoAddress.setVisibility(View.GONE);
				infoAddressCity.setVisibility(View.GONE);
			}
			else {
				infoAddress.setText(p.getString("loc_address"));
				infoAddress.setVisibility(View.VISIBLE);
				infoAddressCity.setVisibility(View.VISIBLE);
			}

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

	private void showNotices() {
		final Notices notices = new Notices();
		notices.addNotice(new Notice(
				"GS Campus Map",
				"https://github.com/therealgitcub/GSCampusMap",
				"Kobi Tate",
				new ApacheSoftwareLicense20()
		));
		notices.addNotice(new Notice(
				"Campus Photography",
				"https://www.flickr.com/photos/georgiasouthern",
				"Georgia Southern University/Jeremy Wilburn",
				new CreativeCommonsAttributionNonCommercialNoDerivs20()
		));
		notices.addNotice(new Notice(
				"Material Drawer",
				"https://github.com/mikepenz/MaterialDrawer",
				"Mike Penz",
				new ApacheSoftwareLicense20()
		));
		notices.addNotice(new Notice(
				"LicensesDialog",
				"http://psdev.de/LicensesDialog/",
				"Philip Schiffer",
				new ApacheSoftwareLicense20()
		));
		notices.addNotice(new Notice(
				"Flipboard BottomSheet",
				"https://github.com/Flipboard/bottomsheet",
				"Flipboard",
				new BSD3ClauseLicense()
		));
		notices.addNotice(new Notice(
				"Google Maps Android API",
				"https://developers.google.com/maps/terms",
				"Google Inc.",
				new GoogleAPILicenseInfo()
		));

		final LicensesDialogFragment fragment = new LicensesDialogFragment.Builder(this)
				.setNotices(notices)
				.setShowFullLicenseText(false)
				.build();

		fragment.show(getSupportFragmentManager(), null);

		drawer.setSelection(DRAWER_ITEM_HOME);


	}



}
