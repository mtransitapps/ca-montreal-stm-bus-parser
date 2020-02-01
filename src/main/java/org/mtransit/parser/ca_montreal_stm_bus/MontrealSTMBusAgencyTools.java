package org.mtransit.parser.ca_montreal_stm_bus;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.Utils;
import org.mtransit.parser.gtfs.data.GCalendar;
import org.mtransit.parser.gtfs.data.GCalendarDate;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GSpec;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.gtfs.data.GTrip;
import org.mtransit.parser.mt.data.MAgency;
import org.mtransit.parser.mt.data.MDirectionType;
import org.mtransit.parser.mt.data.MRoute;
import org.mtransit.parser.CleanUtils;
import org.mtransit.parser.mt.data.MTrip;

// http://www.stm.info/en/about/developers
// http://www.stm.info/sites/default/files/gtfs/gtfs_stm.zip
public class MontrealSTMBusAgencyTools extends DefaultAgencyTools {

	public static void main(String[] args) {
		if (args == null || args.length == 0) {
			args = new String[3];
			args[0] = "input/gtfs.zip";
			args[1] = "../../mtransitapps/ca-montreal-stm-bus-android/res/raw/";
			args[2] = ""; // files-prefix
		}
		new MontrealSTMBusAgencyTools().start(args);
	}

	private HashSet<String> serviceIds;

	@Override
	public void start(String[] args) {
		System.out.printf("\nGenerating STM bus data...");
		long start = System.currentTimeMillis();
		this.serviceIds = extractUsefulServiceIds(args, this, true);
		super.start(args);
		System.out.printf("\nGenerating STM bus data... DONE in %s.\n", Utils.getPrettyDuration(System.currentTimeMillis() - start));
	}

	@Override
	public boolean excludingAll() {
		return this.serviceIds != null && this.serviceIds.isEmpty();
	}

	@Override
	public Integer getAgencyRouteType() {
		return MAgency.ROUTE_TYPE_BUS;
	}

	@Override
	public boolean excludeTrip(GTrip gTrip) {
		if (this.serviceIds != null) {
			return excludeUselessTrip(gTrip, this.serviceIds);
		}
		return super.excludeTrip(gTrip);
	}

	@Override
	public boolean excludeCalendarDate(GCalendarDate gCalendarDates) {
		if (this.serviceIds != null) {
			return excludeUselessCalendarDate(gCalendarDates, this.serviceIds);
		}
		return super.excludeCalendarDate(gCalendarDates);
	}

	@Override
	public boolean excludeCalendar(GCalendar gCalendar) {
		if (this.serviceIds != null) {
			return excludeUselessCalendar(gCalendar, this.serviceIds);
		}
		return super.excludeCalendar(gCalendar);
	}

	@Override
	public long getRouteId(GRoute gRoute) {
		return Integer.valueOf(gRoute.getRouteShortName()); // use route short name instead of route ID
	}

	@Override
	public int getStopId(GStop gStop) {
		return Integer.valueOf(getStopCode(gStop)); // use stop code instead of stop ID
	}

	private static final Pattern P1NUITP2 = Pattern.compile("(\\(nuit\\))", Pattern.CASE_INSENSITIVE);

	private static final String RTS_809 = "809";
	private static final String RLN_809 = "Navette";

	@Override
	public String getRouteLongName(GRoute gRoute) {
		String gRouteLongName = gRoute.getRouteLongName();
		if (StringUtils.isEmpty(gRouteLongName)) {
			if (RTS_809.equals(gRoute.getRouteShortName())) {
				return RLN_809;
			}
		}
		return cleanRouteLongName(gRouteLongName);
	}

	private static final Pattern EXPRESS = Pattern.compile("(express)", Pattern.CASE_INSENSITIVE);
	private static final String EXPRESS_REPLACEMENT = " ";

	private static final Pattern NAVETTE = Pattern.compile("(navette )", Pattern.CASE_INSENSITIVE);
	private static final String NAVETTE_REPLACEMENT = " ";

	private String cleanRouteLongName(String result) {
		result = P1NUITP2.matcher(result).replaceAll(StringUtils.EMPTY);
		result = EXPRESS.matcher(result).replaceAll(EXPRESS_REPLACEMENT);
		result = NAVETTE.matcher(result).replaceAll(NAVETTE_REPLACEMENT);
		result = Utils.replaceAll(result.trim(), START_WITH_ST, StringUtils.EMPTY);
		result = Utils.replaceAll(result, SPACE_ST, CleanUtils.SPACE);
		return CleanUtils.cleanLabelFR(result);
	}

	private static final String AGENCY_COLOR = "009EE0";

	@Override
	public String getAgencyColor() {
		return AGENCY_COLOR;
	}

	public static final String COLOR_GREEEN = "007339";
	public static final String COLOR_BLACK = "000000";
	public static final String COLOR_BLUE = "0060AA";
	public static final String COLOR_OR = "FFD700";

	public static final List<Long> ROUTES_OR = Arrays.asList(new Long[] { //
			252L, 253L, 254L, 256L, 257L, 258L, 259L, //
					260L, 262L, 263L //
			});

	@Override
	public String getRouteColor(GRoute gRoute) {
		long routeId = getRouteId(gRoute);
		if (400L <= routeId && routeId <= 499L) {
			return COLOR_GREEEN;
		}
		if (300L <= routeId && routeId <= 399L) {
			return COLOR_BLACK;
		}
		return COLOR_BLUE;
	}

	@Override
	public void setTripHeadsign(MRoute mRoute, MTrip mTrip, GTrip gTrip, GSpec gtfs) {
		if (gTrip.getTripHeadsign().endsWith("-N")) {
			mTrip.setHeadsignDirection(MDirectionType.NORTH);
			return;
		} else if (gTrip.getTripHeadsign().endsWith("-S")) {
			mTrip.setHeadsignDirection(MDirectionType.SOUTH);
			return;
		} else if (gTrip.getTripHeadsign().endsWith("-E")) {
			mTrip.setHeadsignDirection(MDirectionType.EAST);
			return;
		} else if (gTrip.getTripHeadsign().endsWith("-O")) {
			mTrip.setHeadsignDirection(MDirectionType.WEST);
			return;
		}
		System.out.printf("\nUnexpected trip %s.\n", gTrip);
		System.exit(-1);
	}

	@Override
	public boolean mergeHeadsign(MTrip mTrip, MTrip mTripToMerge) {
		System.out.printf("\nUnexpected trips to merge %s & %s!\n", mTrip, mTripToMerge);
		System.exit(-1);
		return false;
	}

	@Override
	public String cleanTripHeadsign(String tripHeadsign) {
		tripHeadsign = tripHeadsign.toLowerCase(Locale.ENGLISH);
		tripHeadsign = CleanUtils.cleanStreetTypesFRCA(tripHeadsign);
		return super.cleanTripHeadsign(tripHeadsign);
	}

	@Override
	public String cleanStopName(String stopName) {
		stopName = CLEAN_SUBWAY.matcher(stopName).replaceAll(CLEAN_SUBWAY_REPLACEMENT);
		stopName = CLEAN_SUBWAY2.matcher(stopName).replaceAll(CLEAN_SUBWAY2_REPLACEMENT);
		stopName = CleanUtils.cleanSlashes(stopName);
		stopName = Utils.replaceAll(stopName.trim(), START_WITH_ST, StringUtils.EMPTY);
		stopName = Utils.replaceAll(stopName, SPACE_ST, CleanUtils.SPACE);
		stopName = CleanUtils.cleanLabelFR(stopName);
		StringBuilder resultSB = new StringBuilder();
		String[] words = stopName.split(SLASH);
		for (String word : words) {
			if (!resultSB.toString().contains(word.trim())) {
				if (resultSB.length() > 0) {
					resultSB.append(SPACE).append(SLASH).append(SPACE);
				}
				resultSB.append(word.trim());
			}
		}
		return resultSB.toString();
	}

	private static final String SPACE = " ";

	private static final String PARENTHESE1 = "\\(";
	private static final String PARENTHESE2 = "\\)";
	private static final String SLASH = "/";
	private static final Pattern CLEAN_SUBWAY = Pattern.compile("(station)([^" + PARENTHESE1 + "]*)" + PARENTHESE1 + "([^" + SLASH + "]*)" + SLASH + "([^"
			+ PARENTHESE2 + "]*)" + PARENTHESE2, Pattern.CASE_INSENSITIVE);
	private static final String CLEAN_SUBWAY_REPLACEMENT = "$3 " + SLASH + " $4 " + PARENTHESE1 + "$2" + PARENTHESE2 + "";
	private static final Pattern CLEAN_SUBWAY2 = Pattern.compile("(station)([^" + SLASH + "]*)" + SLASH + "(.*)", Pattern.CASE_INSENSITIVE);
	private static final String CLEAN_SUBWAY2_REPLACEMENT = "$3 " + PARENTHESE1 + "$2" + PARENTHESE2 + "";

	private static final String CHARS_NO = "no ";

	private static final String CHARS_VERS = "vers ";

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