package module6;

import processing.core.PApplet;
import de.fhpotsdam.unfolding.UnfoldingMap;
import de.fhpotsdam.unfolding.utils.MapUtils;
import parsing.ParseFeed;
import de.fhpotsdam.unfolding.providers.*;
import de.fhpotsdam.unfolding.providers.Google.*;
import de.fhpotsdam.unfolding.providers.MBTilesMapProvider;

import java.util.ArrayList;
import java.util.List;
import de.fhpotsdam.unfolding.data.Feature;
import de.fhpotsdam.unfolding.data.GeoJSONReader;
import java.util.*;
import processing.core.PConstants;
import processing.core.PGraphics;
import java.text.DecimalFormat;
import java.util.HashMap;


import de.fhpotsdam.unfolding.marker.Marker;

/**
 * Visualizes life expectancy in different countries.
 *
 * It loads the country shapes from a GeoJSON file via a data reader, and loads the population density values from
 * another CSV file (provided by the World Bank). The data value is encoded to transparency via a simplistic linear
 * mapping.
 */
public class LifeExpectancy extends PApplet {

	UnfoldingMap map;
	HashMap<String, Float> lifeExpMap;
	HashMap<String, Float> gdpPerCapitaMap;

	private CommonMarker lastSelected;
	private CommonMarker lastClicked;

	// The files containing city names and info and country names and info
	private List<Marker> countryMarkers;
	private String countryFile = "countries.geo.json";
	private List<Marker> cityMarkers;
	private String cityFile = "city-data.json";

	/** This is where to find the local tiles, for working without an Internet connection */
	public static String mbTilesString = "blankLight-1-3.mbtiles";


	public void setup() {
		size(1000, 1000, OPENGL);
		//map = new UnfoldingMap(this, 50, 50, 700, 500, new Google.GoogleMapProvider());
		map = new UnfoldingMap(this, 0, 0, 700, 500, new MBTilesMapProvider(mbTilesString));
		MapUtils.createDefaultEventDispatcher(this, map);

		// Load lifeExpectancy data
		lifeExpMap = ParseFeed.loadLifeExpectancyFromCSV(this,"LifeExpectancyWorldBank.csv");
		gdpPerCapitaMap = ParseFeed.loadGDPperCapitaFromCSV(this,"GDPperCapitaWorldBank.csv");

		//System.out.println(gdpPerCapitaMap);

		// Load country polygons and adds them as markers
		List<Feature> countries = GeoJSONReader.loadData(this, countryFile);
		countryMarkers = MapUtils.createSimpleMarkers(countries);


		List<Feature> cities = GeoJSONReader.loadData(this, cityFile);
		cityMarkers = new ArrayList<Marker>();
		for(Feature city : cities) {
			cityMarkers.add(new CityMarker(city));
		}

		map.addMarkers(countryMarkers);
		//map.addMarkers(cityMarkers);

		//System.out.println(countryMarkers.get(0).getId());
		//System.out.println(countryMarkers);
		// Country markers are shaded according to life expectancy (only once)
		shadeCountries();
	}

	public void draw() {
		// Draw map tiles and country markers
		map.draw();
	}

	public void draw(Marker marker) {

		fill(255, 250, 240);

		int xbase = 0;
		int ybase = 0;
		rect(xbase, ybase, 200, 75);

		fill(0);
		textAlign(LEFT, CENTER);
		textSize(12);
		String countryId = marker.getId();

		if (lifeExpMap.containsKey(countryId) && gdpPerCapitaMap.containsKey(countryId)){
			DecimalFormat decimalFormat = new DecimalFormat("#.##");

			float lifeExp = lifeExpMap.get(countryId);
			float gdpPerCapita = gdpPerCapitaMap.get(countryId);

			String lifeExp_2 = decimalFormat.format(lifeExp);
			String gdpPerCapita_2 = decimalFormat.format(gdpPerCapita);

			String countryName = marker.getStringProperty("name");
			text(countryName + ":\n"+"Life Expectancy: " + lifeExp_2+" yrs"
							+"\n" + "GDP per Capita: " + "$"+gdpPerCapita_2,
					xbase+25, ybase+25);
		} else {
			text("No Data in World Bank",
					xbase+25, ybase+25);
		}
	}




	//Helper method to color each country based on life expectancy
	//Red-orange indicates low (near 40)
	//Blue indicates high (near 100)
	private void shadeCountries() {
		for (Marker marker : countryMarkers) {
			// Find data for country of the current marker
			String countryId = marker.getId();
			//System.out.println(lifeExpMap.containsKey(countryId));
			if (lifeExpMap.containsKey(countryId)) {
				float lifeExp = lifeExpMap.get(countryId);
				// Encode value as brightness (values range: 40-90)
				int colorLevel = (int) map(lifeExp, 40, 90, 10, 255);
				marker.setColor(color(255-colorLevel, 100, colorLevel));
			} else {
				marker.setColor(color(150,150,150));
			}
		}
	}

	public void mouseClicked() {
		// Deselect all marker
		for (Marker marker : map.getMarkers()) {
			marker.setSelected(false);
		}

		// Select hit marker
		Marker marker = map.getFirstHitMarker(mouseX, mouseY);
		if (marker != null) {
			marker.setSelected(true);
			draw(marker);
		}
	}


}