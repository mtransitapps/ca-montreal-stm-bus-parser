package org.mtransit.parser.ca_montreal_stm_bus;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.Utils;
import org.mtransit.parser.gtfs.data.GCalendar;
import org.mtransit.parser.gtfs.data.GCalendarDate;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.gtfs.data.GTrip;
import org.mtransit.parser.mt.data.MDirectionType;
import org.mtransit.parser.mt.data.MSpec;
import org.mtransit.parser.mt.data.MTrip;

// http:www.stm.info/en/about/developers
// http://www.stm.info/sites/default/files/gtfs/gtfs_stm.zip
public class MontrealSTMBusAgencyTools extends DefaultAgencyTools {

	public static final String ROUTE_TYPE_FILTER = "3"; // bus only

	public static final String SERVICE_ID_FILTER = "14S"; // TODO use calendar

	public static void main(String[] args) {
		if (args == null || args.length == 0) {
			args = new String[3];
			args[0] = "input/gtfs.zip";
			args[1] = "../ca-montreal-stm-bus-android/res/raw/";
			args[2] = ""; // files-prefix
		}
		new MontrealSTMBusAgencyTools().start(args);
	}

	@Override
	public void start(String[] args) {
		System.out.printf("Generating STM bus data...\n");
		long start = System.currentTimeMillis();
		super.start(args);
		System.out.printf("Generating STM bus data... DONE in %s.\n", Utils.getPrettyDuration(System.currentTimeMillis() - start));
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
		if (SERVICE_ID_FILTER != null && !gTrip.service_id.contains(SERVICE_ID_FILTER)) {
			return true;
		}
		return false;
	}

	@Override
	public boolean excludeCalendarDate(GCalendarDate gCalendarDates) {
		if (SERVICE_ID_FILTER != null && !gCalendarDates.service_id.contains(SERVICE_ID_FILTER)) {
			return true;
		}
		return false;
	}

	@Override
	public boolean excludeCalendar(GCalendar gCalendar) {
		if (SERVICE_ID_FILTER != null && !gCalendar.service_id.contains(SERVICE_ID_FILTER)) {
			return true;
		}
		return false;
	}

	@Override
	public int getRouteId(GRoute gRoute) {
		return Integer.valueOf(gRoute.route_short_name); // use route short name instead of route ID
	}

	@Override
	public String getRouteShortName(GRoute gRoute) {
		return StringUtils.leftPad(gRoute.route_short_name, 3); // route short name length = 3
	}

	@Override
	public int getStopId(GStop gStop) {
		return Integer.valueOf(getStopCode(gStop)); // use stop code instead of stop ID
	}

	private static final String P1 = "(";
	private static final String P1S = "( ";
	private static final String P2 = ")";
	private static final String SP2 = "  )";
	private static final String P1NUITP2 = "(nuit)";
	private static final String S = "/";
	private static final String SSS = " / ";

	@Override
	public String getRouteLongName(GRoute gRoute) {
		String result = gRoute.route_long_name;
		result = result.replace(P1S, P1);
		result = result.replace(SP2, P2);
		result = result.replace(P1NUITP2, StringUtils.EMPTY);
		result = result.replace(S, SSS);
		return MSpec.cleanLabel(result);
	}

	public static final String COLOR_GREEEN = "007339";
	public static final String COLOR_BLACK = "000000";
	public static final String COLOR_BLUE = "0060AA";
	public static final String COLOR_RED = "ff0000";

	public static final String COLOR_LOCAL = "009ee0";
	public static final String COLOR_10MIN = "97be0d";
	public static final String COLOR_NIGHT = "646567";
	public static final String COLOR_EXPRESS = "e4368a";
	public static final String COLOR_SHUTTLE = "781b7d";

	public static final List<Integer> ROUTES_10MIN = Arrays.asList(new Integer[] { //
			18, 24, 32, 33, 44, 45, 48, 49, 51, 55, 64, 67, 69, 80, 90, 97, // 0
					103, 105, 106, 121, 136, 139, 141, 161, 165, 171, 187, 193, // 1
					211, // 2
					406, 470 }); // 4

	public static final List<Integer> ROUTES_RED = Arrays.asList(new Integer[] { //
			13, 25, 39, 46, 52, 73, 74, 75, // 0
					101, 115, 116, 135, 188, // 1
					213, 216, 218, 219, 225 }); // 2

	@Override
	public String getRouteColor(GRoute gRoute) {
		int routeId = getRouteId(gRoute);
		if (routeId == 747) {
			return COLOR_BLUE;
		}
		if (ROUTES_RED.contains(routeId)) {
			return COLOR_RED;
		}
		if (routeId >= 400) {
			return COLOR_GREEEN;
		}
		if (routeId >= 300) {
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
	public void setTripHeadsign(MTrip mTrip, GTrip gTrip) {
		final String directionString = gTrip.trip_headsign.substring(gTrip.trip_headsign.length() - 1);
		final MDirectionType directionType = MDirectionType.parse(directionString);
		mTrip.setHeadsignDirection(directionType);
	}

	private static final String PLACE_CHAR_DE = "de ";
	private static final int PLACE_CHAR_DE_LENGTH = PLACE_CHAR_DE.length();

	private static final String PLACE_CHAR_DES = "des ";
	private static final int PLACE_CHAR_DES_LENGTH = PLACE_CHAR_DES.length();

	private static final String PLACE_CHAR_DU = "du ";
	private static final int PLACE_CHAR_DU_LENGTH = PLACE_CHAR_DU.length();

	private static final String PLACE_CHAR_LA = "la ";
	private static final int PLACE_CHAR_LA_LENGTH = PLACE_CHAR_LA.length();

	private static final String PLACE_CHAR_LE = "le ";
	private static final int PLACE_CHAR_LE_LENGTH = PLACE_CHAR_LE.length();

	private static final String PLACE_CHAR_L = "l'";
	private static final int PLACE_CHAR_L_LENGTH = PLACE_CHAR_L.length();

	private static final String PLACE_CHAR_D = "d'";
	private static final int PLACE_CHAR_D_LENGTH = PLACE_CHAR_D.length();

	private static final String PLACE_CHAR_IN = "/ ";
	private static final String PLACE_CHAR_IN_DE = PLACE_CHAR_IN + PLACE_CHAR_DE;
	private static final String PLACE_CHAR_IN_DES = PLACE_CHAR_IN + PLACE_CHAR_DES;
	private static final String PLACE_CHAR_IN_DU = PLACE_CHAR_IN + PLACE_CHAR_DU;
	private static final String PLACE_CHAR_IN_LA = PLACE_CHAR_IN + PLACE_CHAR_LA;
	private static final String PLACE_CHAR_IN_LE = PLACE_CHAR_IN + PLACE_CHAR_LE;
	private static final String PLACE_CHAR_IN_L = PLACE_CHAR_IN + PLACE_CHAR_L;
	private static final String PLACE_CHAR_IN_D = PLACE_CHAR_IN + PLACE_CHAR_D;

	private static final String PLACE_CHAR_PARENTHESE = "(";
	private static final String PLACE_CHAR_PARENTHESE_DE = PLACE_CHAR_PARENTHESE + PLACE_CHAR_DE;
	private static final String PLACE_CHAR_PARENTHESE_DES = PLACE_CHAR_PARENTHESE + PLACE_CHAR_DES;
	private static final String PLACE_CHAR_PARENTHESE_DU = PLACE_CHAR_PARENTHESE + PLACE_CHAR_DU;
	private static final String PLACE_CHAR_PARENTHESE_LA = PLACE_CHAR_PARENTHESE + PLACE_CHAR_LA;
	private static final String PLACE_CHAR_PARENTHESE_LE = PLACE_CHAR_PARENTHESE + PLACE_CHAR_LE;
	private static final String PLACE_CHAR_PARENTHESE_L = PLACE_CHAR_PARENTHESE + PLACE_CHAR_L;
	private static final String PLACE_CHAR_PARENTHESE_D = PLACE_CHAR_PARENTHESE + PLACE_CHAR_D;

	private static final String PLACE_CHAR_PARENTHESE_STATION = PLACE_CHAR_PARENTHESE + "station ";
	private static final String PLACE_CHAR_PARENTHESE_STATION_BIG = PLACE_CHAR_PARENTHESE + "Station ";

	@Override
	public String cleanStopName(String result) {
		if (result.startsWith(PLACE_CHAR_DE)) {
			result = result.substring(PLACE_CHAR_DE_LENGTH);
		} else if (result.startsWith(PLACE_CHAR_DES)) {
			result = result.substring(PLACE_CHAR_DES_LENGTH);
		} else if (result.startsWith(PLACE_CHAR_DU)) {
			result = result.substring(PLACE_CHAR_DU_LENGTH);
		}
		if (result.startsWith(PLACE_CHAR_LA)) {
			result = result.substring(PLACE_CHAR_LA_LENGTH);
		} else if (result.startsWith(PLACE_CHAR_LE)) {
			result = result.substring(PLACE_CHAR_LE_LENGTH);
		} else if (result.startsWith(PLACE_CHAR_L)) {
			result = result.substring(PLACE_CHAR_L_LENGTH);
		} else if (result.startsWith(PLACE_CHAR_D)) {
			result = result.substring(PLACE_CHAR_D_LENGTH);
		}

		if (result.contains(PLACE_CHAR_IN_DE)) {
			result = result.replace(PLACE_CHAR_IN_DE, PLACE_CHAR_IN);
		} else if (result.contains(PLACE_CHAR_IN_DES)) {
			result = result.replace(PLACE_CHAR_IN_DES, PLACE_CHAR_IN);
		} else if (result.contains(PLACE_CHAR_IN_DU)) {
			result = result.replace(PLACE_CHAR_IN_DU, PLACE_CHAR_IN);
		}
		if (result.contains(PLACE_CHAR_IN_LA)) {
			result = result.replace(PLACE_CHAR_IN_LA, PLACE_CHAR_IN);
		} else if (result.contains(PLACE_CHAR_IN_LE)) {
			result = result.replace(PLACE_CHAR_IN_LE, PLACE_CHAR_IN);
		} else if (result.contains(PLACE_CHAR_IN_L)) {
			result = result.replace(PLACE_CHAR_IN_L, PLACE_CHAR_IN);
		} else if (result.contains(PLACE_CHAR_IN_D)) {
			result = result.replace(PLACE_CHAR_IN_D, PLACE_CHAR_IN);
		}

		if (result.contains(PLACE_CHAR_PARENTHESE_DE)) {
			result = result.replace(PLACE_CHAR_PARENTHESE_DE, PLACE_CHAR_PARENTHESE);
		} else if (result.contains(PLACE_CHAR_PARENTHESE_DES)) {
			result = result.replace(PLACE_CHAR_PARENTHESE_DES, PLACE_CHAR_PARENTHESE);
		} else if (result.contains(PLACE_CHAR_PARENTHESE_DU)) {
			result = result.replace(PLACE_CHAR_PARENTHESE_DU, PLACE_CHAR_PARENTHESE);
		}
		if (result.contains(PLACE_CHAR_PARENTHESE_LA)) {
			result = result.replace(PLACE_CHAR_PARENTHESE_LA, PLACE_CHAR_PARENTHESE);
		} else if (result.contains(PLACE_CHAR_PARENTHESE_LE)) {
			result = result.replace(PLACE_CHAR_PARENTHESE_LE, PLACE_CHAR_PARENTHESE);
		} else if (result.contains(PLACE_CHAR_PARENTHESE_L)) {
			result = result.replace(PLACE_CHAR_PARENTHESE_L, PLACE_CHAR_PARENTHESE);
		} else if (result.contains(PLACE_CHAR_PARENTHESE_D)) {
			result = result.replace(PLACE_CHAR_PARENTHESE_D, PLACE_CHAR_PARENTHESE);
		}

		if (result.contains(PLACE_CHAR_PARENTHESE_STATION)) {
			result = result.replace(PLACE_CHAR_PARENTHESE_STATION, PLACE_CHAR_PARENTHESE);
		}
		if (result.contains(PLACE_CHAR_PARENTHESE_STATION_BIG)) {
			result = result.replace(PLACE_CHAR_PARENTHESE_STATION_BIG, PLACE_CHAR_PARENTHESE);
		}
		return super.cleanStopName(result);
	}
}
