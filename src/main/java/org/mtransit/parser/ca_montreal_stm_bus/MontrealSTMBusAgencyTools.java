package org.mtransit.parser.ca_montreal_stm_bus;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mtransit.commons.CleanUtils;
import org.mtransit.commons.RegexUtils;
import org.mtransit.commons.StringUtils;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.MTLog;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.mt.data.MAgency;
import org.mtransit.parser.mt.data.MTrip;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import static org.mtransit.parser.StringUtils.EMPTY;

// http://www.stm.info/en/about/developers
// http://www.stm.info/sites/default/files/gtfs/gtfs_stm.zip
public class MontrealSTMBusAgencyTools extends DefaultAgencyTools {

	public static void main(@NotNull String[] args) {
		new MontrealSTMBusAgencyTools().start(args);
	}

	@Override
	public boolean defaultExcludeEnabled() {
		return true;
	}

	@NotNull
	@Override
	public String getAgencyName() {
		return "STM";
	}

	@NotNull
	@Override
	public Integer getAgencyRouteType() {
		return MAgency.ROUTE_TYPE_BUS;
	}

	@Override
	public long getRouteId(@NotNull GRoute gRoute) {
		return Long.parseLong(gRoute.getRouteShortName()); // use route short name instead of route ID
	}

	@Override
	public int getStopId(@NotNull GStop gStop) {
		return Integer.parseInt(getStopCode(gStop)); // use stop code instead of stop ID
	}

	private static final Pattern P1NUITP2 = Pattern.compile("(\\(nuit\\))", Pattern.CASE_INSENSITIVE);

	private static final String RTS_809 = "809";
	private static final String RLN_809 = "Navette";

	@NotNull
	@Override
	public String getRouteLongName(@NotNull GRoute gRoute) {
		String gRouteLongName = gRoute.getRouteLongNameOrDefault();
		if (StringUtils.isEmpty(gRouteLongName)) {
			if (RTS_809.equals(gRoute.getRouteShortName())) {
				return RLN_809;
			}
			throw new MTLog.Fatal("Unexpected route long name for %s!", gRoute);
		}
		return cleanRouteLongName(gRouteLongName);
	}

	private static final Pattern EXPRESS = Pattern.compile("(express)", Pattern.CASE_INSENSITIVE);
	private static final String EXPRESS_REPLACEMENT = " ";

	private static final Pattern NAVETTE = Pattern.compile("(navette )", Pattern.CASE_INSENSITIVE);
	private static final String NAVETTE_REPLACEMENT = " ";

	@NotNull
	@Override
	public String cleanRouteLongName(@NotNull String result) {
		result = P1NUITP2.matcher(result).replaceAll(EMPTY);
		result = EXPRESS.matcher(result).replaceAll(EXPRESS_REPLACEMENT);
		result = NAVETTE.matcher(result).replaceAll(NAVETTE_REPLACEMENT);
		result = RegexUtils.replaceAllNN(result.trim(), START_WITH_ST, EMPTY);
		result = RegexUtils.replaceAllNN(result, SPACE_ST, CleanUtils.SPACE);
		return CleanUtils.cleanLabelFR(result);
	}

	private static final String AGENCY_COLOR = "009EE0";

	@NotNull
	@Override
	public String getAgencyColor() {
		return AGENCY_COLOR;
	}

	private static final String COLOR_GREEN = "007339";
	private static final String COLOR_BLACK = "000000";
	private static final String COLOR_BLUE = "0060AA";
	// private static final String COLOR_OR = "FFD700";

	//	private static final List<Long> ROUTES_OR = Arrays.asList(//
	//			252L, 253L, 254L, 256L, 257L, 258L, 259L, //
	//			260L, 262L, 263L //
	//	);

	@Nullable
	@Override
	public String getRouteColor(@NotNull GRoute gRoute) {
		String routeColor = gRoute.getRouteColor();
		if (AGENCY_COLOR.equalsIgnoreCase(routeColor)) {
			routeColor = null; // ignore agency color (light blue)
		}
		if (StringUtils.isEmpty(routeColor)) {
			long routeId = getRouteId(gRoute);
			if (400L <= routeId && routeId <= 499L) {
				return COLOR_GREEN;
			}
			if (300L <= routeId && routeId <= 399L) {
				return COLOR_BLACK;
			}
			return COLOR_BLUE;
		}
		return super.getRouteColor(gRoute);
	}

	@Override
	public boolean directionFinderEnabled() {
		return true;
	}

	private static final Pattern STARTS_WITH_RSN_DASH_ = Pattern.compile("(^\\d+-)", Pattern.CASE_INSENSITIVE);

	@NotNull
	@Override
	public String cleanDirectionHeadsign(boolean fromStopName, @NotNull String directionHeadSign) {
		directionHeadSign = STARTS_WITH_RSN_DASH_.matcher(directionHeadSign).replaceAll(EMPTY); // keep E/W/N/S
		return directionHeadSign;
	}

	@NotNull
	@Override
	public List<Integer> getDirectionTypes() {
		return Collections.singletonList(
				MTrip.HEADSIGN_TYPE_DIRECTION
		);
	}

	private static final Pattern STARTS_WITH_RSN_DASH_BOUND_ = Pattern.compile("(^\\d+-[A-Z])");

	@NotNull
	@Override
	public String cleanTripHeadsign(@NotNull String tripHeadsign) {
		tripHeadsign = STARTS_WITH_RSN_DASH_BOUND_.matcher(tripHeadsign).replaceAll(EMPTY); // E/W/N/W used for direction, not trip head-sign
		tripHeadsign = CleanUtils.toLowerCaseUpperCaseWords(Locale.FRENCH, tripHeadsign);
		tripHeadsign = CleanUtils.cleanStreetTypesFRCA(tripHeadsign);
		return super.cleanTripHeadsign(tripHeadsign);
	}

	@NotNull
	@Override
	public String cleanStopName(@NotNull String stopName) {
		stopName = CLEAN_SUBWAY.matcher(stopName).replaceAll(CLEAN_SUBWAY_REPLACEMENT);
		stopName = CLEAN_SUBWAY2.matcher(stopName).replaceAll(CLEAN_SUBWAY2_REPLACEMENT);
		stopName = CleanUtils.cleanSlashes(stopName);
		stopName = RegexUtils.replaceAllNN(stopName.trim(), START_WITH_ST, StringUtils.EMPTY);
		stopName = RegexUtils.replaceAllNN(stopName, SPACE_ST, CleanUtils.SPACE);
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

	private static final String PARENTHESIS1 = "\\(";
	private static final String PARENTHESIS2 = "\\)";
	private static final String SLASH = "/";
	private static final Pattern CLEAN_SUBWAY = Pattern.compile("(station)([^" + PARENTHESIS1 + "]*)" + PARENTHESIS1 + "([^" + SLASH + "]*)" + SLASH + "([^"
			+ PARENTHESIS2 + "]*)" + PARENTHESIS2, Pattern.CASE_INSENSITIVE);
	private static final String CLEAN_SUBWAY_REPLACEMENT = "$3 " + SLASH + " $4 " + PARENTHESIS1 + "$2" + PARENTHESIS2 + "";
	private static final Pattern CLEAN_SUBWAY2 = Pattern.compile("(station)([^" + SLASH + "]*)" + SLASH + "(.*)", Pattern.CASE_INSENSITIVE);
	private static final String CLEAN_SUBWAY2_REPLACEMENT = "$3 " + PARENTHESIS1 + "$2" + PARENTHESIS2 + "";

	private static final String CHARS_NO = "no ";

	private static final String CHARS_VERS = "vers ";

	private static final String CHARS_STAR = "\\*";

	private static final String CHARS_SLASH = "/";

	private static final String CHARS_DASH = "-";

	private static final Pattern[] START_WITH_ST = new Pattern[]{ //
			Pattern.compile("(^" + CHARS_NO + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("(^" + CHARS_VERS + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("(^" + CHARS_STAR + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("(^" + CHARS_SLASH + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("(^" + CHARS_DASH + ")", Pattern.CASE_INSENSITIVE) //
	};

	private static final Pattern[] SPACE_ST = new Pattern[]{ //
			Pattern.compile("( " + CHARS_NO + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("( " + CHARS_VERS + ")", Pattern.CASE_INSENSITIVE) //
	};
}
