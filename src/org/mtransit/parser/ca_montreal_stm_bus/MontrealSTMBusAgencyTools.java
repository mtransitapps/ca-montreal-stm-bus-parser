package org.mtransit.parser.ca_montreal_stm_bus;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.Utils;
import org.mtransit.parser.gtfs.GReader;
import org.mtransit.parser.gtfs.data.GCalendar;
import org.mtransit.parser.gtfs.data.GCalendarDate;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GSpec;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.gtfs.data.GTrip;
import org.mtransit.parser.mt.data.MDirectionType;
import org.mtransit.parser.mt.data.MRoute;
import org.mtransit.parser.mt.data.MSpec;
import org.mtransit.parser.mt.data.MTrip;

// http:www.stm.info/en/about/developers
// http://www.stm.info/sites/default/files/gtfs/gtfs_stm.zip
public class MontrealSTMBusAgencyTools extends DefaultAgencyTools {

	public static final String ROUTE_TYPE_FILTER = "3"; // bus only

	private String startWithFilter;

	public static void main(String[] args) {
		if (args == null || args.length == 0) {
			args = new String[3];
			args[0] = "input/gtfs.zip";
			args[1] = "../../mtransitapps/ca-montreal-stm-bus-android/res/raw/";
			args[2] = ""; // files-prefix
		}
		new MontrealSTMBusAgencyTools().start(args);
	}

	@Override
	public void start(String[] args) {
		System.out.printf("Generating STM bus data...\n");
		long start = System.currentTimeMillis();
		extractUsefulServiceIds(args);
		super.start(args);
		System.out.printf("Generating STM bus data... DONE in %s.\n", Utils.getPrettyDuration(System.currentTimeMillis() - start));
	}

	private void extractUsefulServiceIds(String[] args) {
		System.out.printf("Extracting useful service IDs...\n");
		GSpec gtfs = GReader.readGtfsZipFile(args[0], this);
		Integer todayStringInt = Integer.valueOf(new SimpleDateFormat("yyyyMMdd").format(new Date()));
		for (GCalendarDate gCalendarDate : gtfs.calendarDates) {
			if (gCalendarDate.date == todayStringInt) {
				this.startWithFilter = gCalendarDate.service_id.substring(0, 3);
			}
		}
		System.out.println("Filter: " + this.startWithFilter);
		gtfs = null;
		System.out.printf("Extracting useful service IDs... DONE\n");
	}

	@Override
	public boolean excludeRoute(GRoute gRoute) {
		if (ROUTE_TYPE_FILTER != null && !gRoute.route_type.equals(ROUTE_TYPE_FILTER)) {
			return true;
		}
		return super.excludeRoute(gRoute);
	}

	@Override
	public boolean excludeTrip(GTrip gTrip) {
		if (this.startWithFilter != null) {
			if (gTrip.service_id.startsWith(this.startWithFilter)) {
				return false; // keep
			}
			return true; // exclude
		}
		return super.excludeTrip(gTrip);
	}

	@Override
	public boolean excludeCalendarDate(GCalendarDate gCalendarDates) {
		if (this.startWithFilter != null) {
			if (gCalendarDates.service_id.startsWith(this.startWithFilter)) {
				return false; // keep
			}
			return true; // exclude
		}
		return super.excludeCalendarDate(gCalendarDates);
	}

	@Override
	public boolean excludeCalendar(GCalendar gCalendar) {
		if (this.startWithFilter != null) {
			if (gCalendar.service_id.startsWith(this.startWithFilter)) {
				return false; // keep
			}
			return true; // exclude
		}
		return super.excludeCalendar(gCalendar);
	}

	@Override
	public long getRouteId(GRoute gRoute) {
		return Integer.valueOf(gRoute.route_short_name); // use route short name instead of route ID
	}

	@Override
	public int getStopId(GStop gStop) {
		return Integer.valueOf(getStopCode(gStop)); // use stop code instead of stop ID
	}

	private static final Pattern P1NUITP2 = Pattern.compile("(\\(nuit\\))", Pattern.CASE_INSENSITIVE);

	@Override
	public String getRouteLongName(GRoute gRoute) {
		String result = gRoute.route_long_name;
		return cleanRouteLongName(result);
	}

	private static final Pattern EXPRESS = Pattern.compile("(express)", Pattern.CASE_INSENSITIVE);
	private static final String EXPRESS_REPLACEMENT = " ";

	private static final Pattern NAVETTE = Pattern.compile("(navette)", Pattern.CASE_INSENSITIVE);
	private static final String NAVETTE_REPLACEMENT = " ";

	private String cleanRouteLongName(String result) {
		result = P1NUITP2.matcher(result).replaceAll(StringUtils.EMPTY);
		result = EXPRESS.matcher(result).replaceAll(EXPRESS_REPLACEMENT);
		result = NAVETTE.matcher(result).replaceAll(NAVETTE_REPLACEMENT);
		result = Utils.replaceAll(result.trim(), START_WITH_ST, StringUtils.EMPTY);
		result = Utils.replaceAll(result, SPACE_ST, MSpec.SPACE);
		return MSpec.cleanLabelFR(result);
	}

	public static final String COLOR_GREEEN = "007339";
	public static final String COLOR_BLACK = "000000";
	public static final String COLOR_BLUE = "0060AA";


	public static final List<Integer> ROUTES_10MIN = Arrays.asList(new Integer[] { //
			18, 24, 32, 33, 44, 45, 48, 49, 51, 55, 64, 67, 69, 80, 90, 97, // 0
					103, 105, 106, 121, 136, 139, 141, 161, 165, 171, 187, 193, // 1
					211, // 2
					406, 470 }); // 4


	@Override
	public String getRouteColor(GRoute gRoute) {
		long routeId = getRouteId(gRoute);
		if (routeId >= 700l) {
			return COLOR_BLUE;
		}
		if (routeId >= 400l) {
			return COLOR_GREEEN;
		}
		if (routeId >= 300l) {
			return COLOR_BLACK;
		}
		return COLOR_BLUE;
	}

	private static final String COLOR_WHITE = "FFFFFF";

	@Override
	public String getRouteTextColor(GRoute gRoute) {
		return COLOR_WHITE;
	}

	@Override
	public void setTripHeadsign(MRoute route, MTrip mTrip, GTrip gTrip) {
		String directionString = gTrip.trip_headsign.substring(gTrip.trip_headsign.length() - 1);
		MDirectionType directionType = MDirectionType.parse(directionString);
		mTrip.setHeadsignDirection(directionType);
	}


	@Override
	public String cleanStopName(String result) {

		result = CLEAN_SUBWAY.matcher(result).replaceAll(CLEAN_SUBWAY_REPLACEMENT);
		result = CLEAN_SUBWAY2.matcher(result).replaceAll(CLEAN_SUBWAY2_REPLACEMENT);
		result = MSpec.CLEAN_SLASHES.matcher(result).replaceAll(MSpec.CLEAN_SLASHES_REPLACEMENT);
		result = Utils.replaceAll(result.trim(), START_WITH_ST, StringUtils.EMPTY);
		result = Utils.replaceAll(result, SPACE_ST, MSpec.SPACE);
		return super.cleanStopNameFR(result); // MSpec.cleanLabel(result);
	}

	private static final String PARENTHESE1 = "\\(";
	private static final String PARENTHESE2 = "\\)";
	private static final String SLASH = "/";
	private static final Pattern CLEAN_SUBWAY = Pattern.compile("(station)([^" + PARENTHESE1 + "]*)" + PARENTHESE1 + "([^" + SLASH + "]*)" + SLASH + "([^"
			+ PARENTHESE2 + "]*)" + PARENTHESE2, Pattern.CASE_INSENSITIVE);
	private static final String CLEAN_SUBWAY_REPLACEMENT = "$3 " + SLASH + " $4 " + PARENTHESE1 + "$2" + PARENTHESE2 + "";
	private static final Pattern CLEAN_SUBWAY2 = Pattern.compile("(station)([^" + SLASH + "]*)" + SLASH + "(.*)", Pattern.CASE_INSENSITIVE);
	private static final String CLEAN_SUBWAY2_REPLACEMENT = "$3 " + PARENTHESE1 + "$2" + PARENTHESE2 + "";

	private static final String CHARS_NO = "no ";

	private static final String CHARS_VERS = "vers "; // , Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

	private static final String CHARS_STAR = "\\*";

	private static final String CHARS_SLASH = "/";

	private static final String CHARS_DASH = "-";

	private static final Pattern[] START_WITH_ST = new Pattern[] { //
	Pattern.compile("(^" + CHARS_NO + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("(^" + CHARS_VERS + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("(^" + CHARS_STAR + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("(^" + CHARS_SLASH + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("(^" + CHARS_DASH + ")", Pattern.CASE_INSENSITIVE) //
	};

	private static final Pattern[] SPACE_ST = new Pattern[] { //
	Pattern.compile("( " + CHARS_NO + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("( " + CHARS_VERS + ")", Pattern.CASE_INSENSITIVE) //
	};

}
