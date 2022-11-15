package org.mtransit.parser.ca_montreal_stm_bus;

import static org.mtransit.commons.Constants.EMPTY;
import static org.mtransit.commons.Constants.SPACE;
import static org.mtransit.commons.Constants.SPACE_;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mtransit.commons.CleanUtils;
import org.mtransit.commons.HtmlSymbols;
import org.mtransit.commons.RegexUtils;
import org.mtransit.commons.StringUtils;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.mt.data.MAgency;
import org.mtransit.parser.mt.data.MTrip;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

// https://www.stm.info/en/about/developers
// https://www.stm.info/sites/default/files/gtfs/gtfs_stm.zip
public class MontrealSTMBusAgencyTools extends DefaultAgencyTools {

	public static void main(@NotNull String[] args) {
		new MontrealSTMBusAgencyTools().start(args);
	}

	@Nullable
	@Override
	public List<Locale> getSupportedLanguages() {
		return LANG_FR_EN;
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
	public boolean defaultRouteIdEnabled() {
		return true;
	}

	@Override
	public boolean useRouteShortNameForRouteId() {
		return true;
	}

	@Override
	public int getStopId(@NotNull GStop gStop) {
		return Integer.parseInt(getStopCode(gStop)); // use stop code instead of stop ID
	}

	private static final Pattern P1NUITP2 = Pattern.compile("(\\(nuit\\))", Pattern.CASE_INSENSITIVE);

	@Override
	public boolean defaultRouteLongNameEnabled() {
		return true;
	}

	private static final Pattern EXPRESS_ = CleanUtils.cleanWord("express");

	private static final Pattern NAVETTE_ = CleanUtils.cleanWord("navette");

	@NotNull
	@Override
	public String cleanRouteLongName(@NotNull String result) {
		result = P1NUITP2.matcher(result).replaceAll(EMPTY);
		result = EXPRESS_.matcher(result).replaceAll(SPACE_);
		result = NAVETTE_.matcher(result).replaceAll(SPACE_);
		result = RegexUtils.replaceAllNN(result.trim(), START_WITH_ST, EMPTY);
		result = RegexUtils.replaceAllNN(result, SPACE_ST, CleanUtils.SPACE);
		return CleanUtils.cleanLabelFR(result);
	}

	public boolean defaultAgencyColorEnabled() {
		return true;
	}

	private static final String COLOR_GREEN_EXPRESS = "007339";
	private static final String COLOR_BLACK_NIGHT = "000000";
	private static final String COLOR_BLUE_REGULAR = "0060AA";

	@Nullable
	@Override
	public String provideMissingRouteColor(@NotNull GRoute gRoute) {
		final String rln = gRoute.getRouteLongNameOrDefault();
		final long routeID = getRouteId(gRoute);
		if (rln.contains("express")
				|| 400L <= routeID && routeID <= 499L) {
			return COLOR_GREEN_EXPRESS;
		} else if (300L <= routeID && routeID <= 399L) {
			return COLOR_BLACK_NIGHT;
		} else {
			return COLOR_BLUE_REGULAR;
		}
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
		stopName = CLEAN_SUBWAY_P2_MISSING.matcher(stopName).replaceAll(CLEAN_SUBWAY_P2_MISSING_REPLACEMENT);
		stopName = CLEAN_SUBWAY2.matcher(stopName).replaceAll(CLEAN_SUBWAY2_REPLACEMENT);
		stopName = CLEAN_SUBWAY_ONLY.matcher(stopName).replaceAll(CLEAN_SUBWAY_ONLY_REPLACEMENT);
		stopName = EDICULE_NSEW_.matcher(stopName).replaceAll(EDICULE_NSEW_REPLACEMENT);
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

	private static final String P1 = "\\(";
	private static final String P2 = "\\)";
	private static final String SLASH = "/";

	private static final Pattern CLEAN_SUBWAY = Pattern.compile("(station)" +
					"([^" + P1 + "]*)" + P1 +
					"([^" + SLASH + "]*)" + SLASH +
					"([^" + P2 + "]*)" + P2
			, Pattern.CASE_INSENSITIVE);
	private static final String CLEAN_SUBWAY_REPLACEMENT = "$3 " + SLASH + " $4 " + P1 + HtmlSymbols.SUBWAY_ + "$2" + P2 + "";

	private static final Pattern CLEAN_SUBWAY_P2_MISSING = Pattern.compile("(station)" +
					"([^" + P1 + "]*)" + P1 +
					"([^" + SLASH + "]*)" + SLASH +
					"(.*)"
			, Pattern.CASE_INSENSITIVE);
	private static final String CLEAN_SUBWAY_P2_MISSING_REPLACEMENT = "$3 " + SLASH + " $4 " + P1 + HtmlSymbols.SUBWAY_ + "$2" + P2 + "";

	private static final Pattern CLEAN_SUBWAY2 = Pattern.compile("(station)([^" + SLASH + "]*)" + SLASH + "(.*)", Pattern.CASE_INSENSITIVE);
	private static final String CLEAN_SUBWAY2_REPLACEMENT = "$3 " + P1 + HtmlSymbols.SUBWAY_ + "$2" + P2 + "";

	private static final Pattern CLEAN_SUBWAY_ONLY = Pattern.compile("(^(station) (.*))", Pattern.CASE_INSENSITIVE);
	private static final String CLEAN_SUBWAY_ONLY_REPLACEMENT = "$2" + SPACE + HtmlSymbols.SUBWAY_ + "$3";

	private static final Pattern EDICULE_NSEW_ = Pattern.compile(P1 + "[eé]dicule " + "([^" + P2 + "]*)" + P2, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS);
	private static final String EDICULE_NSEW_REPLACEMENT = P1 + "$1" + P2;

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
