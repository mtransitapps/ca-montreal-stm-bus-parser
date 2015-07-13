package org.mtransit.parser.ca_montreal_stm_bus;

import java.util.HashSet;
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
		this.serviceIds = extractUsefulServiceIds(args, this);
		super.start(args);
		System.out.printf("\nGenerating STM bus data... DONE in %s.\n", Utils.getPrettyDuration(System.currentTimeMillis() - start));
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
		return Integer.valueOf(gRoute.route_short_name); // use route short name instead of route ID
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
		String gRouteLongName = gRoute.route_long_name;
		if (StringUtils.isEmpty(gRouteLongName)) {
			if (RTS_809.equals(gRoute.route_short_name)) {
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

	@Override
	public void setTripHeadsign(MRoute mRoute, MTrip mTrip, GTrip gTrip, GSpec gtfs) {
		if (gTrip.trip_headsign.endsWith("-N")) {
			mTrip.setHeadsignDirection(MDirectionType.NORTH);
			return;
		} else if (gTrip.trip_headsign.endsWith("-S")) {
			mTrip.setHeadsignDirection(MDirectionType.SOUTH);
			return;
		} else if (gTrip.trip_headsign.endsWith("-E")) {
			mTrip.setHeadsignDirection(MDirectionType.EAST);
			return;
		} else if (gTrip.trip_headsign.endsWith("-O")) {
			mTrip.setHeadsignDirection(MDirectionType.WEST);
			return;
		}
		if (mRoute.id == 10l) {
			if (gTrip.direction_id == 1) {
				mTrip.setHeadsignDirection(MDirectionType.SOUTH);
				return;
			}
		} else if (mRoute.id == 12l) {
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignDirection(MDirectionType.NORTH);
				return;
			}
		} else if (mRoute.id == 13l) {
			if (gTrip.direction_id == 1) {
				mTrip.setHeadsignDirection(MDirectionType.SOUTH);
				return;
			}
		} else if (mRoute.id == 14l) {
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignDirection(MDirectionType.NORTH);
				return;
			} else if (gTrip.direction_id == 1) {
				mTrip.setHeadsignDirection(MDirectionType.SOUTH);
				return;
			}
		} else if (mRoute.id == 15l) {
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			} else if (gTrip.direction_id == 1) {
				mTrip.setHeadsignDirection(MDirectionType.WEST);
				return;
			}
		} else if (mRoute.id == 16l) {
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.id == 17l) {
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignDirection(MDirectionType.NORTH);
				return;
			} else if (gTrip.direction_id == 1) {
				mTrip.setHeadsignDirection(MDirectionType.SOUTH);
				return;
			}
		} else if (mRoute.id == 18l) {
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.id == 19l) {
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.id == 21l) {
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignDirection(MDirectionType.NORTH);
				return;
			}
		} else if (mRoute.id == 22l) {
			if (gTrip.direction_id == 1) {
				mTrip.setHeadsignDirection(MDirectionType.WEST);
				return;
			}
		} else if (mRoute.id == 24l) {
			if (gTrip.direction_id == 1) {
				mTrip.setHeadsignDirection(MDirectionType.WEST);
				return;
			}
		} else if (mRoute.id == 25l) {
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignDirection(MDirectionType.NORTH);
				return;
			}
		} else if (mRoute.id == 26l) {
			if (gTrip.direction_id == 1) {
				mTrip.setHeadsignDirection(MDirectionType.WEST);
				return;
			}
		} else if (mRoute.id == 27l) {
			if (gTrip.direction_id == 1) {
				mTrip.setHeadsignDirection(MDirectionType.WEST);
				return;
			}
		} else if (mRoute.id == 29l) {
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.id == 30l) {
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignDirection(MDirectionType.NORTH);
				return;
			}
		} else if (mRoute.id == 31l) {
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignDirection(MDirectionType.NORTH);
				return;
			} else if (gTrip.direction_id == 1) {
				mTrip.setHeadsignDirection(MDirectionType.SOUTH);
				return;
			}
		} else if (mRoute.id == 34l) {
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			} else if (gTrip.direction_id == 1) {
				mTrip.setHeadsignDirection(MDirectionType.WEST);
				return;
			}
		} else if (mRoute.id == 36l) {
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			} else if (gTrip.direction_id == 1) {
				mTrip.setHeadsignDirection(MDirectionType.WEST);
				return;
			}
		} else if (mRoute.id == 37l) {
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignDirection(MDirectionType.NORTH);
				return;
			} else if (gTrip.direction_id == 1) {
				mTrip.setHeadsignDirection(MDirectionType.SOUTH);
				return;
			}
		} else if (mRoute.id == 41l) {
			if (gTrip.direction_id == 1) {
				mTrip.setHeadsignDirection(MDirectionType.WEST);
				return;
			}
		} else if (mRoute.id == 44l) {
			if (gTrip.direction_id == 1) {
				mTrip.setHeadsignDirection(MDirectionType.SOUTH);
				return;
			}
		} else if (mRoute.id == 45l) {
			if (gTrip.direction_id == 1) {
				mTrip.setHeadsignDirection(MDirectionType.SOUTH);
				return;
			}
		} else if (mRoute.id == 46l) {
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.id == 47l) {
			if (gTrip.direction_id == 1) {
				mTrip.setHeadsignDirection(MDirectionType.WEST);
				return;
			}
		} else if (mRoute.id == 48l) {
			if (gTrip.direction_id == 1) {
				mTrip.setHeadsignDirection(MDirectionType.WEST);
				return;
			}
		} else if (mRoute.id == 49l) {
			if (gTrip.direction_id == 1) {
				mTrip.setHeadsignDirection(MDirectionType.WEST);
				return;
			}
		} else if (mRoute.id == 51l) {
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.id == 52l) {
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.id == 53l) {
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignDirection(MDirectionType.NORTH);
				return;
			} else if (gTrip.direction_id == 1) {
				mTrip.setHeadsignDirection(MDirectionType.SOUTH);
				return;
			}
		} else if (mRoute.id == 57l) {
			if (gTrip.direction_id == 1) {
				mTrip.setHeadsignDirection(MDirectionType.WEST);
				return;
			}
		} else if (mRoute.id == 58l) {
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.id == 61l) {
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.id == 63l) {
			if (gTrip.direction_id == 1) {
				mTrip.setHeadsignDirection(MDirectionType.SOUTH);
				return;
			}
		} else if (mRoute.id == 64l) {
			if (gTrip.direction_id == 1) {
				mTrip.setHeadsignDirection(MDirectionType.SOUTH);
				return;
			}
		} else if (mRoute.id == 66l) {
			if (gTrip.direction_id == 1) {
				mTrip.setHeadsignDirection(MDirectionType.SOUTH);
				return;
			}
		} else if (mRoute.id == 67l) {
			if (gTrip.direction_id == 1) {
				mTrip.setHeadsignDirection(MDirectionType.SOUTH);
				return;
			}
		} else if (mRoute.id == 70l) {
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.id == 71l) {
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignDirection(MDirectionType.NORTH);
				return;
			} else if (gTrip.direction_id == 1) {
				mTrip.setHeadsignDirection(MDirectionType.SOUTH);
				return;
			}
		} else if (mRoute.id == 72l) {
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.id == 73l) {
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.id == 74l) {
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignDirection(MDirectionType.NORTH);
				return;
			}
		} else if (mRoute.id == 75l) {
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignDirection(MDirectionType.NORTH);
				return;
			} else if (gTrip.direction_id == 1) {
				mTrip.setHeadsignDirection(MDirectionType.SOUTH);
				return;
			}
		} else if (mRoute.id == 76l) {
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.id == 77l) {
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.id == 78l) {
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			} else if (gTrip.direction_id == 1) {
				mTrip.setHeadsignDirection(MDirectionType.WEST);
				return;
			}
		} else if (mRoute.id == 80l) {
			if (gTrip.direction_id == 1) {
				mTrip.setHeadsignDirection(MDirectionType.SOUTH);
				return;
			}
		} else if (mRoute.id == 85l) {
			if (gTrip.direction_id == 1) {
				mTrip.setHeadsignDirection(MDirectionType.WEST);
				return;
			}
		} else if (mRoute.id == 90l) {
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.id == 92l) {
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.id == 93l) {
			if (gTrip.direction_id == 1) {
				mTrip.setHeadsignDirection(MDirectionType.WEST);
				return;
			}
		} else if (mRoute.id == 95l) {
			if (gTrip.direction_id == 1) {
				mTrip.setHeadsignDirection(MDirectionType.WEST);
				return;
			}
		} else if (mRoute.id == 97l) {
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.id == 99l) {
			if (gTrip.direction_id == 1) {
				mTrip.setHeadsignDirection(MDirectionType.WEST);
				return;
			}
		} else if (mRoute.id == 100l) {
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.id == 101l) {
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.id == 102l) {
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.id == 103l) {
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.id == 104l) {
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.id == 105l) {
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.id == 106l) {
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.id == 108l) {
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.id == 109l) {
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignDirection(MDirectionType.NORTH);
				return;
			}
		} else if (mRoute.id == 110l) {
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.id == 112l) {
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.id == 113l) {
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.id == 115l) {
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.id == 117l) {
			if (gTrip.direction_id == 1) {
				mTrip.setHeadsignDirection(MDirectionType.SOUTH);
				return;
			}
		} else if (mRoute.id == 119l) {
			if (gTrip.direction_id == 1) {
				mTrip.setHeadsignDirection(MDirectionType.SOUTH);
				return;
			}
		} else if (mRoute.id == 124l) {
			if (gTrip.direction_id == 1) {
				mTrip.setHeadsignDirection(MDirectionType.SOUTH);
				return;
			}
		} else if (mRoute.id == 125l) {
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			} else if (gTrip.direction_id == 1) {
				mTrip.setHeadsignDirection(MDirectionType.WEST);
				return;
			}
		} else if (mRoute.id == 129l) {
			if (gTrip.direction_id == 1) {
				mTrip.setHeadsignDirection(MDirectionType.SOUTH);
				return;
			}
		} else if (mRoute.id == 131l) {
			if (gTrip.direction_id == 1) {
				mTrip.setHeadsignDirection(MDirectionType.SOUTH);
				return;
			}
		} else if (mRoute.id == 135l) {
			if (gTrip.direction_id == 1) {
				mTrip.setHeadsignDirection(MDirectionType.SOUTH);
				return;
			}
		} else if (mRoute.id == 136l) {
			if (gTrip.direction_id == 1) {
				mTrip.setHeadsignDirection(MDirectionType.SOUTH);
				return;
			}
		} else if (mRoute.id == 138l) {
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.id == 140l) {
			if (gTrip.direction_id == 1) {
				mTrip.setHeadsignDirection(MDirectionType.WEST);
				return;
			}
		} else if (mRoute.id == 141l) {
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			} else if (gTrip.direction_id == 1) {
				mTrip.setHeadsignDirection(MDirectionType.WEST);
				return;
			}
		} else if (mRoute.id == 144l) {
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			} else if (gTrip.direction_id == 1) {
				mTrip.setHeadsignDirection(MDirectionType.WEST);
				return;
			}
		} else if (mRoute.id == 146l) {
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.id == 150l) {
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			} else if (gTrip.direction_id == 1) {
				mTrip.setHeadsignDirection(MDirectionType.WEST);
				return;
			}
		} else if (mRoute.id == 160l) {
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.id == 161l) {
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.id == 162l) {
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.id == 164l) {
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.id == 165l) {
			if (gTrip.direction_id == 1) {
				mTrip.setHeadsignDirection(MDirectionType.SOUTH);
				return;
			}
		} else if (mRoute.id == 166l) {
			if (gTrip.direction_id == 1) {
				mTrip.setHeadsignDirection(MDirectionType.SOUTH);
				return;
			}
		} else if (mRoute.id == 168l) {
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignDirection(MDirectionType.NORTH);
				return;
			}
		} else if (mRoute.id == 170l) {
			if (gTrip.direction_id == 1) {
				mTrip.setHeadsignDirection(MDirectionType.SOUTH);
				return;
			}
		} else if (mRoute.id == 171l) {
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.id == 174l) {
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.id == 175l) {
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.id == 177l) {
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.id == 178l) {
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignDirection(MDirectionType.NORTH);
				return;
			}
		} else if (mRoute.id == 179l) {
			if (gTrip.direction_id == 1) {
				mTrip.setHeadsignDirection(MDirectionType.SOUTH);
				return;
			}
		} else if (mRoute.id == 180l) {
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.id == 185l) {
			if (gTrip.direction_id == 1) {
				mTrip.setHeadsignDirection(MDirectionType.WEST);
				return;
			}
		} else if (mRoute.id == 186l) {
			if (gTrip.direction_id == 1) {
				mTrip.setHeadsignDirection(MDirectionType.WEST);
				return;
			}
		} else if (mRoute.id == 187l) {
			if (gTrip.direction_id == 1) {
				mTrip.setHeadsignDirection(MDirectionType.WEST);
				return;
			}
		} else if (mRoute.id == 188l) {
			if (gTrip.direction_id == 1) {
				mTrip.setHeadsignDirection(MDirectionType.WEST);
				return;
			}
		} else if (mRoute.id == 189l) {
			if (gTrip.direction_id == 1) {
				mTrip.setHeadsignDirection(MDirectionType.WEST);
				return;
			}
		} else if (mRoute.id == 191l) {
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.id == 192l) {
			if (gTrip.direction_id == 1) {
				mTrip.setHeadsignDirection(MDirectionType.WEST);
				return;
			}
		} else if (mRoute.id == 195l) {
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.id == 196l) {
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignDirection(MDirectionType.NORTH);
				return;
			}
		} else if (mRoute.id == 197l) {
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			} else if (gTrip.direction_id == 1) {
				mTrip.setHeadsignDirection(MDirectionType.WEST);
				return;
			}
		} else if (mRoute.id == 202l) {
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.id == 211l) {
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.id == 213l) {
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.id == 215l) {
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.id == 216l) {
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.id == 217l) {
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.id == 220l) {
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.id == 225l) {
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.id == 258l) {
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.id == 259l) {
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.id == 350l) {
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.id == 353l) {
			if (gTrip.direction_id == 1) {
				mTrip.setHeadsignDirection(MDirectionType.WEST);
				return;
			}
		} else if (mRoute.id == 354l) {
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.id == 355l) {
			if (gTrip.direction_id == 1) {
				mTrip.setHeadsignDirection(MDirectionType.SOUTH);
				return;
			}
		} else if (mRoute.id == 356l) {
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.id == 357l) {
			if (gTrip.direction_id == 1) {
				mTrip.setHeadsignDirection(MDirectionType.SOUTH);
				return;
			}
		} else if (mRoute.id == 358l) {
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			} else if (gTrip.direction_id == 1) {
				mTrip.setHeadsignDirection(MDirectionType.WEST);
				return;
			}
		} else if (mRoute.id == 359l) {
			if (gTrip.direction_id == 1) {
				mTrip.setHeadsignDirection(MDirectionType.SOUTH);
				return;
			}
		} else if (mRoute.id == 360l) {
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			} else if (gTrip.direction_id == 1) {
				mTrip.setHeadsignDirection(MDirectionType.WEST);
				return;
			}
		} else if (mRoute.id == 361l) {
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignDirection(MDirectionType.NORTH);
				return;
			} else if (gTrip.direction_id == 1) {
				mTrip.setHeadsignDirection(MDirectionType.SOUTH);
				return;
			}
		} else if (mRoute.id == 362l) {
			if (gTrip.direction_id == 1) {
				mTrip.setHeadsignDirection(MDirectionType.WEST);
				return;
			}
		} else if (mRoute.id == 363l) {
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignDirection(MDirectionType.NORTH);
				return;
			} else if (gTrip.direction_id == 1) {
				mTrip.setHeadsignDirection(MDirectionType.SOUTH);
				return;
			}
		} else if (mRoute.id == 364l) {
			if (gTrip.direction_id == 1) {
				mTrip.setHeadsignDirection(MDirectionType.WEST);
				return;
			}
		} else if (mRoute.id == 368l) {
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			} else if (gTrip.direction_id == 1) {
				mTrip.setHeadsignDirection(MDirectionType.WEST);
				return;
			}
		} else if (mRoute.id == 369l) {
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignDirection(MDirectionType.NORTH);
				return;
			} else if (gTrip.direction_id == 1) {
				mTrip.setHeadsignDirection(MDirectionType.SOUTH);
				return;
			}
		} else if (mRoute.id == 370l) {
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.id == 371l) {
			if (gTrip.direction_id == 1) {
				mTrip.setHeadsignDirection(MDirectionType.SOUTH);
				return;
			}
		} else if (mRoute.id == 372l) {
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			} else if (gTrip.direction_id == 1) {
				mTrip.setHeadsignDirection(MDirectionType.WEST);
				return;
			}
		} else if (mRoute.id == 376l) {
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.id == 382l) {
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.id == 405l) {
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.id == 406l) {
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.id == 409l) {
			if (gTrip.direction_id == 1) {
				mTrip.setHeadsignDirection(MDirectionType.SOUTH);
				return;
			}
		} else if (mRoute.id == 410l) {
			if (gTrip.direction_id == 1) {
				mTrip.setHeadsignDirection(MDirectionType.WEST);
				return;
			}
		} else if (mRoute.id == 411l) {
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.id == 425l) {
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.id == 427l) {
			if (gTrip.direction_id == 1) {
				mTrip.setHeadsignDirection(MDirectionType.WEST);
				return;
			}
		} else if (mRoute.id == 430l) {
			if (gTrip.direction_id == 1) {
				mTrip.setHeadsignDirection(MDirectionType.WEST);
				return;
			}
		} else if (mRoute.id == 432l) {
			if (gTrip.direction_id == 1) {
				mTrip.setHeadsignDirection(MDirectionType.SOUTH);
				return;
			}
		} else if (mRoute.id == 439l) {
			if (gTrip.direction_id == 1) {
				mTrip.setHeadsignDirection(MDirectionType.SOUTH);
				return;
			}
		} else if (mRoute.id == 440l) {
			if (gTrip.direction_id == 1) {
				mTrip.setHeadsignDirection(MDirectionType.WEST);
				return;
			}
		} else if (mRoute.id == 448l) {
			if (gTrip.direction_id == 1) {
				mTrip.setHeadsignDirection(MDirectionType.WEST);
				return;
			}
		} else if (mRoute.id == 449l) {
			if (gTrip.direction_id == 1) {
				mTrip.setHeadsignDirection(MDirectionType.SOUTH);
				return;
			}
		} else if (mRoute.id == 467l) {
			if (gTrip.direction_id == 1) {
				mTrip.setHeadsignDirection(MDirectionType.SOUTH);
				return;
			}
		} else if (mRoute.id == 468l) {
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.id == 469l) {
			if (gTrip.direction_id == 1) {
				mTrip.setHeadsignDirection(MDirectionType.WEST);
				return;
			}
		} else if (mRoute.id == 470l) {
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.id == 475l) {
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.id == 485l) {
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.id == 486l) {
			if (gTrip.direction_id == 1) {
				mTrip.setHeadsignDirection(MDirectionType.WEST);
				return;
			}
		} else if (mRoute.id == 487l) {
			if (gTrip.direction_id == 1) {
				mTrip.setHeadsignDirection(MDirectionType.WEST);
				return;
			}
		} else if (mRoute.id == 491l) {
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.id == 496l) {
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.id == 715l) {
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.id == 747l) {
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.id == 767l) {
			if (gTrip.direction_id == 1) {
				mTrip.setHeadsignDirection(MDirectionType.WEST);
				return;
			}
		} else if (mRoute.id == 769l) {
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignDirection(MDirectionType.NORTH);
				return;
			}
		} else if (mRoute.id == 777l) {
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.id == 809l) {
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignDirection(MDirectionType.NORTH);
				return;
			} else if (gTrip.direction_id == 1) {
				mTrip.setHeadsignDirection(MDirectionType.SOUTH);
				return;
			}
		}
		System.out.printf("\nUnexpected trip %s.\n", gTrip);
		System.exit(-1);
	}

	@Override
	public String cleanTripHeadsign(String tripHeadsign) {
		tripHeadsign = tripHeadsign.toLowerCase(Locale.ENGLISH);
		tripHeadsign = CleanUtils.cleanStreetTypesFRCA(tripHeadsign);
		return super.cleanTripHeadsign(tripHeadsign);
	}


	@Override
	public String cleanStopName(String result) {

		result = CLEAN_SUBWAY.matcher(result).replaceAll(CLEAN_SUBWAY_REPLACEMENT);
		result = CLEAN_SUBWAY2.matcher(result).replaceAll(CLEAN_SUBWAY2_REPLACEMENT);
		result = CleanUtils.CLEAN_SLASHES.matcher(result).replaceAll(CleanUtils.CLEAN_SLASHES_REPLACEMENT);
		result = Utils.replaceAll(result.trim(), START_WITH_ST, StringUtils.EMPTY);
		result = Utils.replaceAll(result, SPACE_ST, CleanUtils.SPACE);
		result = super.cleanStopNameFR(result);
		StringBuilder resultSB = new StringBuilder();
		String[] words = result.split(SLASH);
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
