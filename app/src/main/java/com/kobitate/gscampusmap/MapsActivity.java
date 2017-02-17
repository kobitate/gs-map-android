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
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.vision.text.Text;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.interfaces.OnCheckedChangeListener;
import com.mikepenz.materialdrawer.model.ExpandableBadgeDrawerItem;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.SecondarySwitchDrawerItem;
import com.mikepenz.materialdrawer.model.SectionDrawerItem;
import com.mikepenz.materialdrawer.model.SwitchDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

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

	private final String		ALGOLIA_APPLICATION_ID = 			"0T1K07PWTM";
	private final String		ALGOLIA_API_KEY =					"a70c25ee5839fcc5faf31f8595a2fa59";

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

		setupDrawer();

		algolia = new Client(ALGOLIA_APPLICATION_ID, ALGOLIA_API_KEY);

		setupMap();
		setupInfoCard();
		setupSearch();

		polygons = 				new ArrayMap<>();
		polygonsByBuildingID = 	new ArrayMap<>();

		polygonCategories = 	new ArrayMap<>();
		polygonCategories.put("academic", new ArrayList<Polygon>());
		polygonCategories.put("admin", new ArrayList<Polygon>());
		polygonCategories.put("athletics", new ArrayList<Polygon>());
		polygonCategories.put("residential", new ArrayList<Polygon>());
		polygonCategories.put("student", new ArrayList<Polygon>());
		polygonCategories.put("support", new ArrayList<Polygon>());

		res = getResources();


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
						Toast.makeText(MapsActivity.this, "Item by the ID " + b.getString("objectID") + " doesn't exist", Toast.LENGTH_SHORT).show();
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
				infoTitle.setText(Html.fromHtml(p.getString("name_popup")));
				if (p.getString("bldg_details") == null) {
					infoDetails.setText("");
				}
				else {
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
				new License() {
					@Override
					public String getName() {
						return "Creative Commons Attribution-NonCommercial-NoDerivs 2.0 Generic";
					}

					@Override
					public String readSummaryTextFromResources(Context context) {
						return "You are free to\n" +
								"\n" +
								"\tShare - copy and redistribute the material in any medium or format\n"+
								"\tThe licensor cannot revoke these freedoms as long as you follow the license terms.\n"+
								"\n" +
								"Under the following terms:\n" +
								"\n" +
								"\tAttribution — You must give appropriate credit, provide a link to the license, and indicate if changes were made. You may do so in any reasonable manner, but not in any way that suggests the licensor endorses you or your use.\n" +
								"\tNonCommercial — You may not use the material for commercial purposes.\n" +
								"\tNoDerivatives — If you remix, transform, or build upon the material, you may not distribute the modified material.\n" +
								"\n" +
								"\tNo additional restrictions — You may not apply legal terms or technological measures that legally restrict others from doing anything the license permits.\n" +
								"\n" +
								"Notices:\n" +
								"\n" +
								"\tYou do not have to comply with the license for elements of the material in the public domain or where your use is permitted by an applicable exception or limitation.\n" +
								"\tNo warranties are given. The license may not give you all of the permissions necessary for your intended use. For example, other rights such as publicity, privacy, or moral rights may limit how you use the material.\n";

					}

					@Override
					public String readFullTextFromResources(Context context) {
						return "THE WORK (AS DEFINED BELOW) IS PROVIDED UNDER THE TERMS OF THIS CREATIVE COMMONS PUBLIC LICENSE (\"CCPL\" OR \"LICENSE\"). THE WORK IS PROTECTED BY COPYRIGHT AND/OR OTHER APPLICABLE LAW. ANY USE OF THE WORK OTHER THAN AS AUTHORIZED UNDER THIS LICENSE OR COPYRIGHT LAW IS PROHIBITED.\r\n\r\nBY EXERCISING ANY RIGHTS TO THE WORK PROVIDED HERE, YOU ACCEPT AND AGREE TO BE BOUND BY THE TERMS OF THIS LICENSE. THE LICENSOR GRANTS YOU THE RIGHTS CONTAINED HERE IN CONSIDERATION OF YOUR ACCEPTANCE OF SUCH TERMS AND CONDITIONS.\r\n\r\n1. Definitions\r\n\r\n\"Collective Work\" means a work, such as a periodical issue, anthology or encyclopedia, in which the Work in its entirety in unmodified form, along with a number of other contributions, constituting separate and independent works in themselves, are assembled into a collective whole. A work that constitutes a Collective Work will not be considered a Derivative Work (as defined below) for the purposes of this License.\r\n\"Derivative Work\" means a work based upon the Work or upon the Work and other pre-existing works, such as a translation, musical arrangement, dramatization, fictionalization, motion picture version, sound recording, art reproduction, abridgment, condensation, or any other form in which the Work may be recast, transformed, or adapted, except that a work that constitutes a Collective Work will not be considered a Derivative Work for the purpose of this License. For the avoidance of doubt, where the Work is a musical composition or sound recording, the synchronization of the Work in timed-relation with a moving image (\"synching\") will be considered a Derivative Work for the purpose of this License.\r\n\"Licensor\" means the individual or entity that offers the Work under the terms of this License.\r\n\"Original Author\" means the individual or entity who created the Work.\r\n\"Work\" means the copyrightable work of authorship offered under the terms of this License.\r\n\"You\" means an individual or entity exercising rights under this License who has not previously violated the terms of this License with respect to the Work, or who has received express permission from the Licensor to exercise rights under this License despite a previous violation.\r\n2. Fair Use Rights. Nothing in this license is intended to reduce, limit, or restrict any rights arising from fair use, first sale or other limitations on the exclusive rights of the copyright owner under copyright law or other applicable laws.\r\n\r\n3. License Grant. Subject to the terms and conditions of this License, Licensor hereby grants You a worldwide, royalty-free, non-exclusive, perpetual (for the duration of the applicable copyright) license to exercise the rights in the Work as stated below:\r\n\r\nto reproduce the Work, to incorporate the Work into one or more Collective Works, and to reproduce the Work as incorporated in the Collective Works;\r\nto distribute copies or phonorecords of, display publicly, perform publicly, and perform publicly by means of a digital audio transmission the Work including as incorporated in Collective Works;\r\nThe above rights may be exercised in all media and formats whether now known or hereafter devised. The above rights include the right to make such modifications as are technically necessary to exercise the rights in other media and formats, but otherwise you have no rights to make Derivative Works. All rights not expressly granted by Licensor are hereby reserved, including but not limited to the rights set forth in Sections 4(d) and 4(e).\r\n\r\n4. Restrictions.The license granted in Section 3 above is expressly made subject to and limited by the following restrictions:\r\n\r\nYou may distribute, publicly display, publicly perform, or publicly digitally perform the Work only under the terms of this License, and You must include a copy of, or the Uniform Resource Identifier for, this License with every copy or phonorecord of the Work You distribute, publicly display, publicly perform, or publicly digitally perform. You may not offer or impose any terms on the Work that alter or restrict the terms of this License or the recipients' exercise of the rights granted hereunder. You may not sublicense the Work. You must keep intact all notices that refer to this License and to the disclaimer of warranties. You may not distribute, publicly display, publicly perform, or publicly digitally perform the Work with any technological measures that control access or use of the Work in a manner inconsistent with the terms of this License Agreement. The above applies to the Work as incorporated in a Collective Work, but this does not require the Collective Work apart from the Work itself to be made subject to the terms of this License. If You create a Collective Work, upon notice from any Licensor You must, to the extent practicable, remove from the Collective Work any reference to such Licensor or the Original Author, as requested.\r\nYou may not exercise any of the rights granted to You in Section 3 above in any manner that is primarily intended for or directed toward commercial advantage or private monetary compensation. The exchange of the Work for other copyrighted works by means of digital file-sharing or otherwise shall not be considered to be intended for or directed toward commercial advantage or private monetary compensation, provided there is no payment of any monetary compensation in connection with the exchange of copyrighted works.\r\nIf you distribute, publicly display, publicly perform, or publicly digitally perform the Work, You must keep intact all copyright notices for the Work and give the Original Author credit reasonable to the medium or means You are utilizing by conveying the name (or pseudonym if applicable) of the Original Author if supplied; the title of the Work if supplied; and to the extent reasonably practicable, the Uniform Resource Identifier, if any, that Licensor specifies to be associated with the Work, unless such URI does not refer to the copyright notice or licensing information for the Work. Such credit may be implemented in any reasonable manner; provided, however, that in the case of a Collective Work, at a minimum such credit will appear where any other comparable authorship credit appears and in a manner at least as prominent as such other comparable authorship credit.\r\nFor the avoidance of doubt, where the Work is a musical composition:\r\n\r\nPerformance Royalties Under Blanket Licenses. Licensor reserves the exclusive right to collect, whether individually or via a performance rights society (e.g. ASCAP, BMI, SESAC), royalties for the public performance or public digital performance (e.g. webcast) of the Work if that performance is primarily intended for or directed toward commercial advantage or private monetary compensation.\r\nMechanical Rights and Statutory Royalties. Licensor reserves the exclusive right to collect, whether individually or via a music rights agency or designated agent (e.g. Harry Fox Agency), royalties for any phonorecord You create from the Work (\"cover version\") and distribute, subject to the compulsory license created by 17 USC Section 115 of the US Copyright Act (or the equivalent in other jurisdictions), if Your distribution of such cover version is primarily intended for or directed toward commercial advantage or private monetary compensation.\r\nWebcasting Rights and Statutory Royalties. For the avoidance of doubt, where the Work is a sound recording, Licensor reserves the exclusive right to collect, whether individually or via a performance-rights society (e.g. SoundExchange), royalties for the public digital performance (e.g. webcast) of the Work, subject to the compulsory license created by 17 USC Section 114 of the US Copyright Act (or the equivalent in other jurisdictions), if Your public digital performance is primarily intended for or directed toward commercial advantage or private monetary compensation.\r\n5. Representations, Warranties and Disclaimer\r\n\r\nUNLESS OTHERWISE MUTUALLY AGREED BY THE PARTIES IN WRITING, LICENSOR OFFERS THE WORK AS-IS AND MAKES NO REPRESENTATIONS OR WARRANTIES OF ANY KIND CONCERNING THE WORK, EXPRESS, IMPLIED, STATUTORY OR OTHERWISE, INCLUDING, WITHOUT LIMITATION, WARRANTIES OF TITLE, MERCHANTIBILITY, FITNESS FOR A PARTICULAR PURPOSE, NONINFRINGEMENT, OR THE ABSENCE OF LATENT OR OTHER DEFECTS, ACCURACY, OR THE PRESENCE OF ABSENCE OF ERRORS, WHETHER OR NOT DISCOVERABLE. SOME JURISDICTIONS DO NOT ALLOW THE EXCLUSION OF IMPLIED WARRANTIES, SO SUCH EXCLUSION MAY NOT APPLY TO YOU.\r\n\r\n6. Limitation on Liability. EXCEPT TO THE EXTENT REQUIRED BY APPLICABLE LAW, IN NO EVENT WILL LICENSOR BE LIABLE TO YOU ON ANY LEGAL THEORY FOR ANY SPECIAL, INCIDENTAL, CONSEQUENTIAL, PUNITIVE OR EXEMPLARY DAMAGES ARISING OUT OF THIS LICENSE OR THE USE OF THE WORK, EVEN IF LICENSOR HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.\r\n\r\n7. Termination\r\n\r\nThis License and the rights granted hereunder will terminate automatically upon any breach by You of the terms of this License. Individuals or entities who have received Collective Works from You under this License, however, will not have their licenses terminated provided such individuals or entities remain in full compliance with those licenses. Sections 1, 2, 5, 6, 7, and 8 will survive any termination of this License.\r\nSubject to the above terms and conditions, the license granted here is perpetual (for the duration of the applicable copyright in the Work). Notwithstanding the above, Licensor reserves the right to release the Work under different license terms or to stop distributing the Work at any time; provided, however that any such election will not serve to withdraw this License (or any other license that has been, or is required to be, granted under the terms of this License), and this License will continue in full force and effect unless terminated as stated above.\r\n8. Miscellaneous\r\n\r\nEach time You distribute or publicly digitally perform the Work or a Collective Work, the Licensor offers to the recipient a license to the Work on the same terms and conditions as the license granted to You under this License.\r\nIf any provision of this License is invalid or unenforceable under applicable law, it shall not affect the validity or enforceability of the remainder of the terms of this License, and without further action by the parties to this agreement, such provision shall be reformed to the minimum extent necessary to make such provision valid and enforceable.\r\nNo term or provision of this License shall be deemed waived and no breach consented to unless such waiver or consent shall be in writing and signed by the party to be charged with such waiver or consent.\r\nThis License constitutes the entire agreement between the parties with respect to the Work licensed here. There are no understandings, agreements or representations with respect to the Work not specified here. Licensor shall not be bound by any additional provisions that may appear in any communication from You. This License may not be modified without the mutual written agreement of the Licensor and You.";
					}

					@Override
					public String getVersion() {
						return "2.0";
					}

					@Override
					public String getUrl() {
						return "https://creativecommons.org/licenses/by-nc-nd/2.0/";
					}
				}
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
				new License() {
					@Override
					public String getName() {
						return "Google Maps Android API Notices";
					}

					@Override
					public String readSummaryTextFromResources(Context context) {
						return readFullTextFromResources(context);
					}

					@Override
					public String readFullTextFromResources(Context context) {
						return GoogleApiAvailability.getInstance().getOpenSourceSoftwareLicenseInfo(getApplicationContext());
					}

					@Override
					public String getVersion() {
						return "January 27, 2017";
					}

					@Override
					public String getUrl() {
						return "https://developers.google.com/maps/terms";
					}
				}
		));

		final LicensesDialogFragment fragment = new LicensesDialogFragment.Builder(this)
				.setNotices(notices)
				.setShowFullLicenseText(false)
				.build();

		fragment.show(getSupportFragmentManager(), null);

		drawer.setSelection(DRAWER_ITEM_HOME);


	}



}
