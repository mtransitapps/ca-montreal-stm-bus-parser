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
		if (mRoute.getId() == 10l) {
			if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignDirection(MDirectionType.SOUTH);
				return;
			}
		} else if (mRoute.getId() == 12l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignDirection(MDirectionType.NORTH);
				return;
			}
		} else if (mRoute.getId() == 13l) {
			if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignDirection(MDirectionType.SOUTH);
				return;
			}
		} else if (mRoute.getId() == 14l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignDirection(MDirectionType.NORTH);
				return;
			} else if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignDirection(MDirectionType.SOUTH);
				return;
			}
		} else if (mRoute.getId() == 15l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			} else if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignDirection(MDirectionType.WEST);
				return;
			}
		} else if (mRoute.getId() == 16l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.getId() == 17l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignDirection(MDirectionType.NORTH);
				return;
			} else if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignDirection(MDirectionType.SOUTH);
				return;
			}
		} else if (mRoute.getId() == 18l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.getId() == 19l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.getId() == 21l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignDirection(MDirectionType.NORTH);
				return;
			}
		} else if (mRoute.getId() == 22l) {
			if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignDirection(MDirectionType.WEST);
				return;
			}
		} else if (mRoute.getId() == 24l) {
			if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignDirection(MDirectionType.WEST);
				return;
			}
		} else if (mRoute.getId() == 25l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignDirection(MDirectionType.NORTH);
				return;
			}
		} else if (mRoute.getId() == 26l) {
			if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignDirection(MDirectionType.WEST);
				return;
			}
		} else if (mRoute.getId() == 27l) {
			if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignDirection(MDirectionType.WEST);
				return;
			}
		} else if (mRoute.getId() == 29l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.getId() == 30l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignDirection(MDirectionType.NORTH);
				return;
			}
		} else if (mRoute.getId() == 31l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignDirection(MDirectionType.NORTH);
				return;
			} else if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignDirection(MDirectionType.SOUTH);
				return;
			}
		} else if (mRoute.getId() == 34l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			} else if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignDirection(MDirectionType.WEST);
				return;
			}
		} else if (mRoute.getId() == 36l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			} else if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignDirection(MDirectionType.WEST);
				return;
			}
		} else if (mRoute.getId() == 37l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignDirection(MDirectionType.NORTH);
				return;
			} else if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignDirection(MDirectionType.SOUTH);
				return;
			}
		} else if (mRoute.getId() == 41l) {
			if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignDirection(MDirectionType.WEST);
				return;
			}
		} else if (mRoute.getId() == 44l) {
			if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignDirection(MDirectionType.SOUTH);
				return;
			}
		} else if (mRoute.getId() == 45l) {
			if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignDirection(MDirectionType.SOUTH);
				return;
			}
		} else if (mRoute.getId() == 46l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.getId() == 47l) {
			if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignDirection(MDirectionType.WEST);
				return;
			}
		} else if (mRoute.getId() == 48l) {
			if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignDirection(MDirectionType.WEST);
				return;
			}
		} else if (mRoute.getId() == 49l) {
			if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignDirection(MDirectionType.WEST);
				return;
			}
		} else if (mRoute.getId() == 51l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.getId() == 52l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.getId() == 53l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignDirection(MDirectionType.NORTH);
				return;
			} else if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignDirection(MDirectionType.SOUTH);
				return;
			}
		} else if (mRoute.getId() == 57l) {
			if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignDirection(MDirectionType.WEST);
				return;
			}
		} else if (mRoute.getId() == 58l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.getId() == 61l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.getId() == 63l) {
			if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignDirection(MDirectionType.SOUTH);
				return;
			}
		} else if (mRoute.getId() == 64l) {
			if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignDirection(MDirectionType.SOUTH);
				return;
			}
		} else if (mRoute.getId() == 66l) {
			if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignDirection(MDirectionType.SOUTH);
				return;
			}
		} else if (mRoute.getId() == 67l) {
			if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignDirection(MDirectionType.SOUTH);
				return;
			}
		} else if (mRoute.getId() == 70l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.getId() == 71l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignDirection(MDirectionType.NORTH);
				return;
			} else if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignDirection(MDirectionType.SOUTH);
				return;
			}
		} else if (mRoute.getId() == 72l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.getId() == 73l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.getId() == 74l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignDirection(MDirectionType.NORTH);
				return;
			}
		} else if (mRoute.getId() == 75l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignDirection(MDirectionType.NORTH);
				return;
			} else if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignDirection(MDirectionType.SOUTH);
				return;
			}
		} else if (mRoute.getId() == 76l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.getId() == 77l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.getId() == 78l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			} else if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignDirection(MDirectionType.WEST);
				return;
			}
		} else if (mRoute.getId() == 80l) {
			if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignDirection(MDirectionType.SOUTH);
				return;
			}
		} else if (mRoute.getId() == 85l) {
			if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignDirection(MDirectionType.WEST);
				return;
			}
		} else if (mRoute.getId() == 90l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.getId() == 92l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.getId() == 93l) {
			if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignDirection(MDirectionType.WEST);
				return;
			}
		} else if (mRoute.getId() == 95l) {
			if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignDirection(MDirectionType.WEST);
				return;
			}
		} else if (mRoute.getId() == 97l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.getId() == 99l) {
			if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignDirection(MDirectionType.WEST);
				return;
			}
		} else if (mRoute.getId() == 100l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.getId() == 101l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.getId() == 102l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.getId() == 103l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.getId() == 104l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.getId() == 105l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.getId() == 106l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.getId() == 108l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.getId() == 109l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignDirection(MDirectionType.NORTH);
				return;
			}
		} else if (mRoute.getId() == 110l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.getId() == 112l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.getId() == 113l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.getId() == 115l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.getId() == 117l) {
			if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignDirection(MDirectionType.SOUTH);
				return;
			}
		} else if (mRoute.getId() == 119l) {
			if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignDirection(MDirectionType.SOUTH);
				return;
			}
		} else if (mRoute.getId() == 124l) {
			if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignDirection(MDirectionType.SOUTH);
				return;
			}
		} else if (mRoute.getId() == 125l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			} else if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignDirection(MDirectionType.WEST);
				return;
			}
		} else if (mRoute.getId() == 129l) {
			if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignDirection(MDirectionType.SOUTH);
				return;
			}
		} else if (mRoute.getId() == 131l) {
			if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignDirection(MDirectionType.SOUTH);
				return;
			}
		} else if (mRoute.getId() == 135l) {
			if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignDirection(MDirectionType.SOUTH);
				return;
			}
		} else if (mRoute.getId() == 136l) {
			if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignDirection(MDirectionType.SOUTH);
				return;
			}
		} else if (mRoute.getId() == 138l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.getId() == 140l) {
			if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignDirection(MDirectionType.WEST);
				return;
			}
		} else if (mRoute.getId() == 141l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			} else if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignDirection(MDirectionType.WEST);
				return;
			}
		} else if (mRoute.getId() == 144l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			} else if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignDirection(MDirectionType.WEST);
				return;
			}
		} else if (mRoute.getId() == 146l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.getId() == 150l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			} else if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignDirection(MDirectionType.WEST);
				return;
			}
		} else if (mRoute.getId() == 160l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.getId() == 161l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.getId() == 162l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.getId() == 164l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.getId() == 165l) {
			if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignDirection(MDirectionType.SOUTH);
				return;
			}
		} else if (mRoute.getId() == 166l) {
			if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignDirection(MDirectionType.SOUTH);
				return;
			}
		} else if (mRoute.getId() == 168l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignDirection(MDirectionType.NORTH);
				return;
			}
		} else if (mRoute.getId() == 170l) {
			if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignDirection(MDirectionType.SOUTH);
				return;
			}
		} else if (mRoute.getId() == 171l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.getId() == 174l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.getId() == 175l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.getId() == 177l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.getId() == 178l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignDirection(MDirectionType.NORTH);
				return;
			}
		} else if (mRoute.getId() == 179l) {
			if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignDirection(MDirectionType.SOUTH);
				return;
			}
		} else if (mRoute.getId() == 180l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.getId() == 185l) {
			if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignDirection(MDirectionType.WEST);
				return;
			}
		} else if (mRoute.getId() == 186l) {
			if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignDirection(MDirectionType.WEST);
				return;
			}
		} else if (mRoute.getId() == 187l) {
			if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignDirection(MDirectionType.WEST);
				return;
			}
		} else if (mRoute.getId() == 188l) {
			if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignDirection(MDirectionType.WEST);
				return;
			}
		} else if (mRoute.getId() == 189l) {
			if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignDirection(MDirectionType.WEST);
				return;
			}
		} else if (mRoute.getId() == 191l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.getId() == 192l) {
			if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignDirection(MDirectionType.WEST);
				return;
			}
		} else if (mRoute.getId() == 195l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.getId() == 196l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignDirection(MDirectionType.NORTH);
				return;
			}
		} else if (mRoute.getId() == 197l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			} else if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignDirection(MDirectionType.WEST);
				return;
			}
		} else if (mRoute.getId() == 202l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.getId() == 211l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.getId() == 213l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.getId() == 215l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.getId() == 216l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.getId() == 217l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.getId() == 220l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.getId() == 225l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.getId() == 258l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.getId() == 259l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.getId() == 350l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.getId() == 353l) {
			if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignDirection(MDirectionType.WEST);
				return;
			}
		} else if (mRoute.getId() == 354l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.getId() == 355l) {
			if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignDirection(MDirectionType.SOUTH);
				return;
			}
		} else if (mRoute.getId() == 356l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.getId() == 357l) {
			if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignDirection(MDirectionType.SOUTH);
				return;
			}
		} else if (mRoute.getId() == 358l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			} else if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignDirection(MDirectionType.WEST);
				return;
			}
		} else if (mRoute.getId() == 359l) {
			if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignDirection(MDirectionType.SOUTH);
				return;
			}
		} else if (mRoute.getId() == 360l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			} else if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignDirection(MDirectionType.WEST);
				return;
			}
		} else if (mRoute.getId() == 361l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignDirection(MDirectionType.NORTH);
				return;
			} else if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignDirection(MDirectionType.SOUTH);
				return;
			}
		} else if (mRoute.getId() == 362l) {
			if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignDirection(MDirectionType.WEST);
				return;
			}
		} else if (mRoute.getId() == 363l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignDirection(MDirectionType.NORTH);
				return;
			} else if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignDirection(MDirectionType.SOUTH);
				return;
			}
		} else if (mRoute.getId() == 364l) {
			if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignDirection(MDirectionType.WEST);
				return;
			}
		} else if (mRoute.getId() == 368l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			} else if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignDirection(MDirectionType.WEST);
				return;
			}
		} else if (mRoute.getId() == 369l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignDirection(MDirectionType.NORTH);
				return;
			} else if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignDirection(MDirectionType.SOUTH);
				return;
			}
		} else if (mRoute.getId() == 370l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.getId() == 371l) {
			if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignDirection(MDirectionType.SOUTH);
				return;
			}
		} else if (mRoute.getId() == 372l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			} else if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignDirection(MDirectionType.WEST);
				return;
			}
		} else if (mRoute.getId() == 376l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.getId() == 382l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.getId() == 405l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.getId() == 406l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.getId() == 409l) {
			if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignDirection(MDirectionType.SOUTH);
				return;
			}
		} else if (mRoute.getId() == 410l) {
			if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignDirection(MDirectionType.WEST);
				return;
			}
		} else if (mRoute.getId() == 411l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.getId() == 425l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.getId() == 427l) {
			if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignDirection(MDirectionType.WEST);
				return;
			}
		} else if (mRoute.getId() == 430l) {
			if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignDirection(MDirectionType.WEST);
				return;
			}
		} else if (mRoute.getId() == 432l) {
			if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignDirection(MDirectionType.SOUTH);
				return;
			}
		} else if (mRoute.getId() == 439l) {
			if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignDirection(MDirectionType.SOUTH);
				return;
			}
		} else if (mRoute.getId() == 440l) {
			if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignDirection(MDirectionType.WEST);
				return;
			}
		} else if (mRoute.getId() == 448l) {
			if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignDirection(MDirectionType.WEST);
				return;
			}
		} else if (mRoute.getId() == 449l) {
			if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignDirection(MDirectionType.SOUTH);
				return;
			}
		} else if (mRoute.getId() == 467l) {
			if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignDirection(MDirectionType.SOUTH);
				return;
			}
		} else if (mRoute.getId() == 468l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.getId() == 469l) {
			if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignDirection(MDirectionType.WEST);
				return;
			}
		} else if (mRoute.getId() == 470l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.getId() == 475l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.getId() == 485l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.getId() == 486l) {
			if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignDirection(MDirectionType.WEST);
				return;
			}
		} else if (mRoute.getId() == 487l) {
			if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignDirection(MDirectionType.WEST);
				return;
			}
		} else if (mRoute.getId() == 491l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.getId() == 496l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.getId() == 715l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.getId() == 747l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.getId() == 767l) {
			if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignDirection(MDirectionType.WEST);
				return;
			}
		} else if (mRoute.getId() == 769l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignDirection(MDirectionType.NORTH);
				return;
			}
		} else if (mRoute.getId() == 777l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			}
		} else if (mRoute.getId() == 809l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignDirection(MDirectionType.NORTH);
				return;
			} else if (gTrip.getDirectionId() == 1) {
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
