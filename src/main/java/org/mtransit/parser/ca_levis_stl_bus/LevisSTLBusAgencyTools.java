package org.mtransit.parser.ca_levis_stl_bus;

import static org.mtransit.commons.StringUtils.EMPTY;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mtransit.commons.CharUtils;
import org.mtransit.commons.CleanUtils;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.MTLog;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.mt.data.MAgency;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

// https://www.stlevis.ca/stlevis/donnees-ouvertes
// https://www.stlevis.ca/sites/default/files/public/assets/gtfs/transit/gtfs_stlevis.zip
public class LevisSTLBusAgencyTools extends DefaultAgencyTools {

	public static void main(@NotNull String[] args) {
		new LevisSTLBusAgencyTools().start(args);
	}

	@Nullable
	@Override
	public List<Locale> getSupportedLanguages() {
		return LANG_FR;
	}

	@NotNull
	public String getAgencyName() {
		return "STLévis";
	}

	@Override
	public boolean defaultExcludeEnabled() {
		return true;
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

	@Nullable
	@Override
	public Long convertRouteIdFromShortNameNotSupported(@NotNull String routeShortName) {
		switch (routeShortName) {
		case "ECQ":
			return 9_050_317L;
		case "ELQ":
			return 9_051_217L;
		case "EOQ":
			return 9_051_517L;
		case "ESQ":
			return 9_051_917L;
		}
		return super.convertRouteIdFromShortNameNotSupported(routeShortName);
	}

	private static final String P1 = "(";
	private static final String P2 = ")";
	private static final String SPACE = " ";

	private static final String ABRAHAM_MARTIN_SHORT = "A-Martin";
	private static final String BERNIERES = "Bernières";
	private static final String BREAKEYVILLE = "Breakeyville";
	private static final String CENTRE_SHORT = "Ctr";
	private static final String JUVENAT_NOTRE_DAME_SHORT = "JND"; // "Juvénat Notre-Dame";
	private static final String PARC_RELAIS_BUS_SHORT = "PRB"; // "P+R";
	private static final String QUEBEC = "Québec";
	private static final String RENE_LEVESQUE_SHORT = "R-Lévesque";
	private static final String ST_JEAN_SHORT = "St-J";
	private static final String ST_NICOLAS = "St-Nicolas";
	private static final String ST_LAMBERT = "St-Lambert";
	private static final String UNIVERSITE_SHORT = "U.";
	private static final String VILLAGE = "Village";

	private static final String VILLAGE_ST_NICOLAS = VILLAGE + SPACE + P1 + ST_NICOLAS + P2;
	private static final String BERNIERES_ST_NICOLAS = BERNIERES + SPACE + P1 + ST_NICOLAS + P2;

	@Override
	public boolean defaultRouteLongNameEnabled() {
		return true;
	}

	@NotNull
	@Override
	public String cleanRouteLongName(@NotNull String routeLongName) {
		routeLongName = CleanUtils.SAINT.matcher(routeLongName).replaceAll(CleanUtils.SAINT_REPLACEMENT);
		routeLongName = CleanUtils.CLEAN_PARENTHESIS1.matcher(routeLongName).replaceAll(CleanUtils.CLEAN_PARENTHESIS1_REPLACEMENT);
		routeLongName = CleanUtils.CLEAN_PARENTHESIS2.matcher(routeLongName).replaceAll(CleanUtils.CLEAN_PARENTHESIS2_REPLACEMENT);
		return CleanUtils.cleanLabel(routeLongName);
	}

	@Override
	public boolean defaultAgencyColorEnabled() {
		return true;
	}

	private static final String AGENCY_COLOR = "009CBE"; // from PDF

	private static final String SCHOOL_BUS_COLOR = "FFD800"; // YELLOW (from Wikipedia)

	@NotNull
	@Override
	public String getAgencyColor() {
		return AGENCY_COLOR;
	}

	@Nullable
	@Override
	public String provideMissingRouteColor(@NotNull GRoute gRoute) {
		final String rsnS = gRoute.getRouteShortName();
		if (CharUtils.isDigitsOnly(rsnS)) {
			final int rsn = Integer.parseInt(rsnS);
			if (rsn >= 100 && rsn <= 999) {
				return SCHOOL_BUS_COLOR;
			}
		}
		if ("T65".equalsIgnoreCase(rsnS)) {
			return "C7B24C";
		}
		throw new MTLog.Fatal("Unexpected route color for %s!", gRoute);
	}

	@NotNull
	@Override
	public String cleanStopOriginalId(@NotNull String gStopId) {
		gStopId = CleanUtils.cleanMergedID(gStopId);
		return gStopId;
	}

	@Override
	public boolean directionFinderEnabled() {
		return true;
	}

	@NotNull
	@Override
	public String cleanDirectionHeadsign(boolean fromStopName, @NotNull String directionHeadSign) {
		if (directionHeadSign.endsWith(" (AM)")) {
			return "AM";
		} else if (directionHeadSign.endsWith(" (PM)")) {
			return "PM";
		}
		directionHeadSign = super.cleanDirectionHeadsign(fromStopName, directionHeadSign);
		return directionHeadSign;
	}

	private static final Pattern STATION = Pattern.compile("((^|\\W)(station)(\\W|$))", Pattern.CASE_INSENSITIVE);
	private static final String STATION_REPLACEMENT = "$2$4"; // "$2" + STATION_SHORT; // + "$4"

	private static final Pattern TERMINUS_ = Pattern.compile("((^|\\W)(terminus)(\\W|$))", Pattern.CASE_INSENSITIVE);
	private static final String TERMINUS_REPLACEMENT = "$2$4"; // "$2" + TERMINUS_SHORT; // + "$4"

	private static final Pattern STE_HELENE_DE_BREAKEYVILLE_ = Pattern.compile("((^|\\W)(ste-hélène-de-breakeyville)(\\W|$))",
			Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.CANON_EQ);
	private static final String STE_HELENE_DE_BREAKEYVILLE_REPLACEMENT = "$2" + BREAKEYVILLE + "$4";

	private static final Pattern ST_LAMBERT_ = Pattern.compile("((^|\\W)(st-lambert-de-lauzon)(\\W|$))", Pattern.CASE_INSENSITIVE);
	private static final String ST_LAMBERT_REPLACEMENT = "$2" + ST_LAMBERT + "$4";

	// St-Nicolas - Bernières (Direct)
	private static final Pattern ST_NICOLAS_BERNIERES_ = Pattern.compile("((^|\\W)(" //
			+ "st-nicolas - bernières" //
			+ "|" //
			+ "st-nicolas - bernières" //
			+ ")(\\W|$))", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.CANON_EQ);
	private static final String ST_NICOLAS_BERNIERES_REPLACEMENT = "$2" + BERNIERES_ST_NICOLAS + "$4";

	private static final Pattern ST_NICOLAS_VILLAGE_ = Pattern.compile("((^|\\W)(st-nicolas - village)(\\W|$))", Pattern.CASE_INSENSITIVE);
	private static final String ST_NICOLAS_VILLAGE_REPLACEMENT = "$2" + VILLAGE_ST_NICOLAS + "$4";

	private static final Pattern UNIVERSITE_ = Pattern.compile("((^|\\W)(universit[é|e])(\\W|$))", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.CANON_EQ);
	private static final String UNIVERSITE_REPLACEMENT = "$2" + UNIVERSITE_SHORT; // + "$4"

	private static final Pattern CENTRE_ = Pattern.compile("((^|\\W)(centre)(\\W|$))", Pattern.CASE_INSENSITIVE);
	private static final String CENTRE_REPLACEMENT = "$2" + CENTRE_SHORT + "$4";

	private static final Pattern PARC_RELAIS_BUS_ = Pattern.compile("((^|\\W)(parc-relais-bus)(\\W|$))", Pattern.CASE_INSENSITIVE);
	private static final String PARC_RELAIS_BUS_REPLACEMENT = "$2" + PARC_RELAIS_BUS_SHORT + "$4";

	private static final Pattern QUEBEC_CTR_ = Pattern.compile("((^|\\W)(qu[é|e]bec centre-ville - SAAQ)(\\W|$))", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.CANON_EQ);
	private static final String QUEBEC_CTR_REPLACEMENT = "$2" + QUEBEC + SPACE + CENTRE_SHORT + "$4";

	private static final Pattern ST_JEAN_ = Pattern.compile("((^|\\W)(st-jean))", Pattern.CASE_INSENSITIVE);
	private static final String ST_JEAN_REPLACEMENT = "$2" + ST_JEAN_SHORT;

	private static final Pattern JUVENAT_NOTRE_DAME_ = Pattern.compile("((^|\\W)(juv[e|é]nat notre-dame)(\\W|$))", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.CANON_EQ);
	private static final String JUVENAT_NOTRE_DAME_REPLACEMENT = "$2" + JUVENAT_NOTRE_DAME_SHORT + "$4";

	private static final Pattern DASH_ = Pattern.compile("((^|\\W)(–)(\\W|$))", Pattern.CASE_INSENSITIVE);
	private static final String DASH_REPLACEMENT = "$2-$4";

	private static final Pattern RENE_LEVESQUE_ = Pattern.compile("((^|\\W)(rené-lévesque)(\\W|$))", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.CANON_EQ);
	private static final String RENE_LEVESQUE_REPLACEMENT = "$2" + RENE_LEVESQUE_SHORT + "$4";

	private static final Pattern ABRAHAM_MARTIN_ = Pattern.compile("((^|\\W)(abraham-martin)(\\W|$))", Pattern.CASE_INSENSITIVE);
	private static final String ABRAHAM_MARTIN_REPLACEMENT = "$2" + ABRAHAM_MARTIN_SHORT + "$4";

	private static final Pattern ENDS_WITH_ARRETS_LIMITES_ = Pattern.compile("( \\(arr[e|ê]ts limit[e|é]s\\))", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.CANON_EQ);
	private static final Pattern ENDS_WITH_DIRECT_ = Pattern.compile("( \\(direct\\))", Pattern.CASE_INSENSITIVE);

	@NotNull
	@Override
	public String cleanTripHeadsign(@NotNull String tripHeadsign) {
		tripHeadsign = CleanUtils.removeVia(tripHeadsign);
		tripHeadsign = CleanUtils.SAINT.matcher(tripHeadsign).replaceAll(CleanUtils.SAINT_REPLACEMENT);
		tripHeadsign = DASH_.matcher(tripHeadsign).replaceAll(DASH_REPLACEMENT);
		tripHeadsign = TERMINUS_.matcher(tripHeadsign).replaceAll(TERMINUS_REPLACEMENT);
		tripHeadsign = RENE_LEVESQUE_.matcher(tripHeadsign).replaceAll(RENE_LEVESQUE_REPLACEMENT);
		tripHeadsign = ABRAHAM_MARTIN_.matcher(tripHeadsign).replaceAll(ABRAHAM_MARTIN_REPLACEMENT);
		tripHeadsign = STATION.matcher(tripHeadsign).replaceAll(STATION_REPLACEMENT);
		tripHeadsign = ST_JEAN_.matcher(tripHeadsign).replaceAll(ST_JEAN_REPLACEMENT);
		tripHeadsign = ST_LAMBERT_.matcher(tripHeadsign).replaceAll(ST_LAMBERT_REPLACEMENT);
		tripHeadsign = ST_NICOLAS_BERNIERES_.matcher(tripHeadsign).replaceAll(ST_NICOLAS_BERNIERES_REPLACEMENT);
		tripHeadsign = ST_NICOLAS_VILLAGE_.matcher(tripHeadsign).replaceAll(ST_NICOLAS_VILLAGE_REPLACEMENT);
		tripHeadsign = STE_HELENE_DE_BREAKEYVILLE_.matcher(tripHeadsign).replaceAll(STE_HELENE_DE_BREAKEYVILLE_REPLACEMENT);
		tripHeadsign = PARC_RELAIS_BUS_.matcher(tripHeadsign).replaceAll(PARC_RELAIS_BUS_REPLACEMENT);
		tripHeadsign = JUVENAT_NOTRE_DAME_.matcher(tripHeadsign).replaceAll(JUVENAT_NOTRE_DAME_REPLACEMENT);
		tripHeadsign = QUEBEC_CTR_.matcher(tripHeadsign).replaceAll(QUEBEC_CTR_REPLACEMENT);
		tripHeadsign = CENTRE_.matcher(tripHeadsign).replaceAll(CENTRE_REPLACEMENT);
		tripHeadsign = UNIVERSITE_.matcher(tripHeadsign).replaceAll(UNIVERSITE_REPLACEMENT);
		tripHeadsign = ENDS_WITH_DIRECT_.matcher(tripHeadsign).replaceAll(EMPTY);
		tripHeadsign = ENDS_WITH_ARRETS_LIMITES_.matcher(tripHeadsign).replaceAll(EMPTY);
		tripHeadsign = CleanUtils.cleanStreetTypesFRCA(tripHeadsign);
		return CleanUtils.cleanLabelFR(tripHeadsign);
	}

	@NotNull
	@Override
	public String cleanStopName(@NotNull String gStopName) {
		gStopName = CleanUtils.cleanStreetTypesFRCA(gStopName);
		return CleanUtils.cleanLabelFR(gStopName);
	}

	@NotNull
	@Override
	public String getStopCode(@NotNull GStop gStop) {
		return String.valueOf(getStopId(gStop)); // using stop ID as stop code
	}

	@Override
	public int getStopId(@NotNull GStop gStop) {
		//noinspection deprecation
		String stopId = gStop.getStopId();
		stopId = CleanUtils.cleanMergedID(stopId);
		return Integer.parseInt(stopId);
	}
}
