package org.mtransit.parser.ca_levis_stl_bus;

import org.apache.commons.lang3.StringUtils;
import org.mtransit.parser.CleanUtils;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.MTLog;
import org.mtransit.parser.Pair;
import org.mtransit.parser.SplitUtils;
import org.mtransit.parser.SplitUtils.RouteTripSpec;
import org.mtransit.parser.Utils;
import org.mtransit.parser.gtfs.data.GCalendar;
import org.mtransit.parser.gtfs.data.GCalendarDate;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GSpec;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.gtfs.data.GTrip;
import org.mtransit.parser.gtfs.data.GTripStop;
import org.mtransit.parser.mt.data.MAgency;
import org.mtransit.parser.mt.data.MRoute;
import org.mtransit.parser.mt.data.MTrip;
import org.mtransit.parser.mt.data.MTripStop;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// https://www.stlevis.ca/stlevis/donnees-ouvertes
// https://www.stlevis.ca/sites/default/files/public/assets/gtfs/transit/gtfs_stlevis.zip
public class LevisSTLBusAgencyTools extends DefaultAgencyTools {

	public static void main(String[] args) {
		if (args == null || args.length == 0) {
			args = new String[3];
			args[0] = "input/gtfs.zip";
			args[1] = "../../mtransitapps/ca-levis-stl-bus-android/res/raw/";
			args[2] = ""; // files-prefix
		}
		new LevisSTLBusAgencyTools().start(args);
	}

	private HashSet<String> serviceIds;

	@Override
	public void start(String[] args) {
		MTLog.log("Generating STLévis bus data...");
		long start = System.currentTimeMillis();
		boolean isNext = "next_".equalsIgnoreCase(args[2]);
		if (isNext) {
			setupNext();
		}
		this.serviceIds = extractUsefulServiceIds(args, this, true);
		super.start(args);
		MTLog.log("Generating STLévis bus data... DONE in %s.", Utils.getPrettyDuration(System.currentTimeMillis() - start));
	}

	private void setupNext() {
	}

	@Override
	public boolean excludingAll() {
		return this.serviceIds != null && this.serviceIds.isEmpty();
	}

	@Override
	public boolean excludeCalendar(GCalendar gCalendar) {
		if (this.serviceIds != null) {
			return excludeUselessCalendar(gCalendar, this.serviceIds);
		}
		return super.excludeCalendar(gCalendar);
	}

	@Override
	public boolean excludeCalendarDate(GCalendarDate gCalendarDates) {
		if (this.serviceIds != null) {
			return excludeUselessCalendarDate(gCalendarDates, this.serviceIds);
		}
		return super.excludeCalendarDate(gCalendarDates);
	}

	@Override
	public boolean excludeTrip(GTrip gTrip) {
		if (this.serviceIds != null) {
			return excludeUselessTrip(gTrip, this.serviceIds);
		}
		return super.excludeTrip(gTrip);
	}

	@Override
	public Integer getAgencyRouteType() {
		return MAgency.ROUTE_TYPE_BUS;
	}

	private static final Pattern DIGITS = Pattern.compile("[\\d]+");

	private static final long RID_L = 12 * 1000L;
	private static final long RID_T = 20 * 1000L;

	@SuppressWarnings("PointlessArithmeticExpression")
	private static final long RID__A = 1 * 100_000L;
	private static final long RID__E = 5 * 100_000L;
	private static final long RID__R = 18 * 100_000L;

	private static final String RSN_ECQ = "ECQ";
	private static final String RSN_ELQ = "ELQ";
	private static final String RSN_EOQ = "EOQ";
	private static final String RSN_ESQ = "ESQ";

	private static final long RID_ECQ = 9_050_317L;
	private static final long RID_ELQ = 9_051_217L;
	private static final long RID_EOQ = 9_051_517L;
	private static final long RID_ESQ = 9_051_917L;

	@Override
	public long getRouteId(GRoute gRoute) {
		if (Utils.isDigitsOnly(gRoute.getRouteShortName())) {
			return Long.parseLong(gRoute.getRouteShortName()); // using route short name as route ID
		}
		if (RSN_ECQ.equals(gRoute.getRouteShortName())) {
			return RID_ECQ;
		} else if (RSN_ELQ.equals(gRoute.getRouteShortName())) {
			return RID_ELQ;
		} else if (RSN_EOQ.equals(gRoute.getRouteShortName())) {
			return RID_EOQ;
		} else if (RSN_ESQ.equals(gRoute.getRouteShortName())) {
			return RID_ESQ;
		}
		Matcher matcher = DIGITS.matcher(gRoute.getRouteShortName());
		if (matcher.find()) {
			long digits = Long.parseLong(matcher.group());
			if (gRoute.getRouteShortName().startsWith("L")) {
				return digits + RID_L;
			} else if (gRoute.getRouteShortName().startsWith("T")) {
				return digits + RID_T;
			} else if (gRoute.getRouteShortName().endsWith("A")) {
				return digits + RID__A;
			} else if (gRoute.getRouteShortName().endsWith("E")) {
				return digits + RID__E;
			} else if (gRoute.getRouteShortName().endsWith("R")) {
				return digits + RID__R;
			}
		}
		MTLog.logFatal("Unexpected route ID for %s!", gRoute);
		return -1L;

	}

	private static final String _DASH_ = " - ";
	private static final String P1 = "(";
	private static final String P2 = ")";
	private static final String _SLASH_ = " / ";
	private static final String SPACE = " ";
	private static final String TO = " > ";
	private static final String _VIA_ = " Via ";
	private static final String OUEST = "Ouest";
	private static final String EST = "Est";
	private static final String NORD = "Nord";

	private static final String A_DESJARDINS = "A-Desjardins";
	private static final String ABRAHAM_MARTIN_SHORT = "A-Martin";
	private static final String BARONET = "Baronet";
	private static final String BERNIERES = "Bernières";
	private static final String BERNIERES_EST = BERNIERES + SPACE + EST;
	private static final String BREAKEYVILLE = "Breakeyville";
	private static final String CEGEP = "Cégep";
	private static final String CENTRE_SHORT = "Ctr";
	private static final String CHAGNON = "Chagnon";
	private static final String CHARNY = "Charny";
	private static final String CHARNY_OUEST = CHARNY + SPACE + OUEST;
	private static final String CHARNY_EST = CHARNY + SPACE + EST;
	private static final String CHAUDIERE_OUEST = "Chaudière-" + OUEST;
	private static final String CHEMIN_DES_ILES = "Chemin des Îles";
	private static final String CHEMIN_VIRE_CREPES = "Chemin Vire-Crêpes";
	private static final String DERNIER_ARRET = "Dernier Arrêt";
	private static final String DORVAL = "Dorval";
	private static final String DU_PRESIDENT_KENNEDY = "Du Président-Kennedy";
	private static final String DU_SAULT = "Du Sault";
	private static final String EMILE_COTE = "Émile-Côté";
	private static final String ETIENNE_DALLAIRE = "Étienne-Dallaire";
	private static final String EXPLORATEURS = "Explorateurs";
	private static final String EXPRESS = "Express";
	private static final String GALERIES = "Galeries";
	private static final String GALERIES_CHAGNON = GALERIES + SPACE + CHAGNON;
	private static final String GRAVEL = "Gravel";
	private static final String JUVENAT_NOTRE_DAME_LONG = "Juvénat Notre-Dame";
	private static final String JUVENAT_NOTRE_DAME_SHORT = "JND"; // "Juvénat Notre-Dame";
	private static final String LAUZON = "Lauzon";
	private static final String LEVIS = "Lévis";
	private static final String LEVIS_CENTRE = LEVIS + SPACE + CENTRE_SHORT;
	private static final String MANIC = "Manic";
	private static final String MARCELLE_MALLET = "Marcelle-Mallet";
	private static final String MONTCALM = "Montcalm";
	private static final String PARC_RELAIS_BUS_SHORT = "PRB"; // "P+R";
	private static final String PARC_RELAIS_BUS_DES_RIVIERES = PARC_RELAIS_BUS_SHORT + " Des Rivières";
	private static final String PINTENDRE = "Pintendre";
	private static final String PIONNIERS = "Pionniers";
	private static final String POINTE_DE_LA_MARTINIERE = "Pte de la Martinière";
	private static final String POINTE_LEVY = "Pointe-Lévy";
	private static final String PRESQU_ILE = "Presqu’Île";
	private static final String QUEBEC = "Québec";
	private static final String QUEBEC_CENTRE = QUEBEC + SPACE + CENTRE_SHORT;
	private static final String PLACE = "Place";
	private static final String PLACE_QUEBEC = PLACE + SPACE + QUEBEC;
	private static final String PROVENCE = "Provence";
	private static final String RACCOURCI = "Raccourci";
	private static final String RENE_LEVESQUE_SHORT = "R-Lévesque";
	private static final String RIVE_NORD = "Rive-" + NORD;
	private static final String ST_AUG = "St-Aug";
	private static final String ST_DAVID = "St-David";
	private static final String ST_ETIENNE = "St-Étienne";
	private static final String ST_ETIENNE_DE_LAUZON = ST_ETIENNE + "-De-" + LAUZON;
	private static final String ST_FOY = "St-Foy";
	private static final String ST_JEAN_SHORT = "St-J";
	private static final String ST_JEAN_CHRYSOSTOME = ST_JEAN_SHORT + "-Chrysostome";
	private static final String ST_LAURENT = "St-Laurent";
	private static final String ST_NICOLAS = "St-Nicolas";
	private static final String ST_LAMBERT = "St-Lambert";
	private static final String ST_LAMBERT_DE_LAUZON = ST_LAMBERT + "-de-Lauzon";
	private static final String ST_REDEMPTEUR = "St-Rédempteur";
	private static final String ST_ROMUALD = "St-Romuald";
	private static final String STE_FOY = "Ste-Foy";
	private static final String STATION_DE_LA_CONCORDE = "Concorde"; // STATION_SHORT + "De La Concorde";
	private static final String STATION_PAVILLON_DESJARDINS = "Pavillon Desjardins"; // STATION_SHORT + "Pavillon Desjardins";
	private static final String STATION_PLANTE = "Plante"; // STATION_SHORT + "Plante";
	private static final String TANIATA = "Taniata";
	private static final String TERMINUS_LAGUEUX = "Lagueux"; // TERMINUS_SHORT + "Lagueux";
	private static final String TERMINUS_DE_LA_TRAVERSE = "Traverse"; // TERMINUS_SHORT + "De La Traverse";
	private static final String TRAVERSE_DE_LEVIS = "Traverse de " + LEVIS;
	private static final String UQAR = "UQAR";
	private static final String UNIVERSITE_SHORT = "U.";
	private static final String UNIVERSITE_LAVAL = UNIVERSITE_SHORT + "Laval";
	private static final String V_CHEMIN = "V-Chemin";
	private static final String VIEUX_LEVIS = "Vieux-" + LEVIS;
	private static final String VILLAGE = "Village";

	private static final String CEGEP_LEVIS_LAUZON = CEGEP + SPACE + LEVIS + "/" + LAUZON;
	private static final String CEGEP_GARNEAU = CEGEP + " Garneau";
	private static final String COLLEGE_DE_LEVIS = "Collège de " + LEVIS;
	private static final String HOTEL_DIEU_DE_LEVIS = "Hôtel-Dieu De " + LEVIS;
	private static final String RUE_ST_LAURENT = "Rue " + ST_LAURENT;
	private static final String VILLAGE_ST_NICOLAS = VILLAGE + SPACE + P1 + ST_NICOLAS + P2;
	private static final String BERNIERES_ST_NICOLAS = BERNIERES + SPACE + P1 + ST_NICOLAS + P2;

	@SuppressWarnings("DuplicateBranchesInSwitch")
	@Override
	public String getRouteLongName(GRoute gRoute) {
		String routeLongName = gRoute.getRouteLongName();
		if (StringUtils.isEmpty(routeLongName)) {
			if (Utils.isDigitsOnly(gRoute.getRouteShortName())) {
				int rsn = Integer.parseInt(gRoute.getRouteShortName());
				switch (rsn) {
				case 11:
					return LAUZON + _DASH_ + LEVIS_CENTRE;
				case 12:
					return LAUZON + TO + VIEUX_LEVIS;
				case 13:
					return ST_DAVID + _DASH_ + LEVIS_CENTRE;
				case 14:
					return LEVIS_CENTRE;
				case 15:
					return PINTENDRE;
				case 19:
					return BREAKEYVILLE;
				case 22:
					return BERNIERES_ST_NICOLAS;
				case 23:
					return VILLAGE_ST_NICOLAS;
				case 24:
					return ST_REDEMPTEUR;
				case 32:
					return ST_ROMUALD;
				case 65:
					return ST_LAMBERT + _DASH_ + ST_NICOLAS;
				case 111:
					return POINTE_LEVY + _SLASH_ + JUVENAT_NOTRE_DAME_SHORT + _DASH_ + ST_ROMUALD + _SLASH_ + LAUZON;
				case 115:
					return LAUZON + _DASH_ + LEVIS_CENTRE;
				case 125:
					return LAUZON + _DASH_ + LEVIS_CENTRE;
				case 126:
					return ETIENNE_DALLAIRE + _DASH_ + LEVIS_CENTRE;
				case 131:
					return ST_JEAN_CHRYSOSTOME + _SLASH_ + MANIC + _DASH_ + JUVENAT_NOTRE_DAME_LONG;
				case 135:
					return COLLEGE_DE_LEVIS + _SLASH_ + MARCELLE_MALLET + _DASH_ + ST_JEAN_CHRYSOSTOME;
				case 136:
					return ST_DAVID + _DASH_ + LEVIS_CENTRE;
				case 137:
					return ST_DAVID + _SLASH_ + "Hadlow" + _DASH_ + LEVIS_CENTRE;
				case 141:
					return JUVENAT_NOTRE_DAME_LONG + _DASH_ + CHARNY_EST;
				case 151:
					return JUVENAT_NOTRE_DAME_LONG + _DASH_ + CHARNY_OUEST;
				case 155:
					return PINTENDRE + _DASH_ + LEVIS_CENTRE;
				case 156:
					return PINTENDRE + _VIA_ + "Massenet" + _DASH_ + LEVIS_CENTRE;
				case 161:
					return JUVENAT_NOTRE_DAME_LONG + _DASH_ + ST_JEAN_CHRYSOSTOME + _SLASH_ + ST_ROMUALD;
				case 165:
					return COLLEGE_DE_LEVIS + _SLASH_ + MARCELLE_MALLET + _DASH_ + ST_JEAN_CHRYSOSTOME;
				case 171:
					return ST_JEAN_CHRYSOSTOME + _SLASH_ + GRAVEL + _DASH_ + JUVENAT_NOTRE_DAME_LONG;
				case 175:
					return ST_JEAN_CHRYSOSTOME + _SLASH_ + MANIC + _SLASH_ + COLLEGE_DE_LEVIS + _DASH_ + MARCELLE_MALLET;
				case 181:
					return JUVENAT_NOTRE_DAME_LONG + _DASH_ + ST_ROMUALD;
				case 185:
					return COLLEGE_DE_LEVIS + _SLASH_ + MARCELLE_MALLET + _DASH_ + ST_ROMUALD;
				case 221:
					return JUVENAT_NOTRE_DAME_LONG + _DASH_ + BERNIERES_ST_NICOLAS;
				case 222:
					return JUVENAT_NOTRE_DAME_LONG + _DASH_ + ST_NICOLAS + _SLASH_ + PRESQU_ILE;
				case 225:
					return COLLEGE_DE_LEVIS + _SLASH_ + MARCELLE_MALLET + _DASH_ + ST_NICOLAS + _SLASH_ + PRESQU_ILE;
				case 231:
					return JUVENAT_NOTRE_DAME_LONG + _DASH_ + ST_NICOLAS;
				case 232:
					return JUVENAT_NOTRE_DAME_LONG + _DASH_ + ST_NICOLAS;
				case 235:
					return COLLEGE_DE_LEVIS + _SLASH_ + MARCELLE_MALLET + _DASH_ + ST_NICOLAS + SPACE + P1 + VILLAGE + P2;
				case 241:
					return JUVENAT_NOTRE_DAME_LONG + _DASH_ + ST_REDEMPTEUR + _SLASH_ + BERNIERES_EST;
				case 242:
					return JUVENAT_NOTRE_DAME_LONG + _DASH_ + ST_REDEMPTEUR;
				case 243:
					return JUVENAT_NOTRE_DAME_LONG + _DASH_ + ST_NICOLAS + _SLASH_ + ST_REDEMPTEUR;
				case 245:
					return COLLEGE_DE_LEVIS + _SLASH_ + MARCELLE_MALLET + _DASH_ + ST_REDEMPTEUR + _SLASH_ + BERNIERES_EST;
				case 246:
					return COLLEGE_DE_LEVIS + _SLASH_ + MARCELLE_MALLET + _DASH_ + ST_REDEMPTEUR + _SLASH_ + ST_NICOLAS;
				case 325:
					return ST_ROMUALD + _DASH_ + LEVIS_CENTRE;
				case 345:
					return ST_ROMUALD + _SLASH_ + PRESQU_ILE + _SLASH_ + QUEBEC + _DASH_ + LEVIS_CENTRE;
				case 351:
					return ST_ROMUALD + _SLASH_ + JUVENAT_NOTRE_DAME_SHORT + _DASH_ + CHARNY;
				case 355:
					return CHARNY + _DASH_ + LEVIS_CENTRE;
				case 371:
					return ST_ROMUALD + _SLASH_ + JUVENAT_NOTRE_DAME_SHORT + _DASH_ + ST_JEAN_CHRYSOSTOME;
				case 375:
					return ST_JEAN_CHRYSOSTOME + _DASH_ + LEVIS_CENTRE;
				case 381:
					return ST_ROMUALD + _SLASH_ + JUVENAT_NOTRE_DAME_SHORT + _DASH_ + ST_JEAN_CHRYSOSTOME;
				case 385:
					return ST_JEAN_CHRYSOSTOME + _DASH_ + LEVIS_CENTRE;
				case 386:
					return ST_JEAN_CHRYSOSTOME + _DASH_ + LEVIS_CENTRE;
				case 391:
					return ST_ROMUALD + _SLASH_ + JUVENAT_NOTRE_DAME_SHORT + _DASH_ + ST_JEAN_CHRYSOSTOME;
				case 395:
					return ST_JEAN_CHRYSOSTOME + _DASH_ + LEVIS_CENTRE;
				case 915:
					return COLLEGE_DE_LEVIS + _SLASH_ + MARCELLE_MALLET + _DASH_ + PINTENDRE;
				case 916:
					return COLLEGE_DE_LEVIS + _SLASH_ + MARCELLE_MALLET + _DASH_ + PINTENDRE;
				}
			}
			if (RSN_ECQ.equalsIgnoreCase(gRoute.getRouteShortName())) {
				return EXPRESS + SPACE + CHARNY;
			} else if (RSN_ELQ.equalsIgnoreCase(gRoute.getRouteShortName())) {
				return EXPRESS + SPACE + LEVIS_CENTRE;
			} else if (RSN_EOQ.equalsIgnoreCase(gRoute.getRouteShortName())) {
				return EXPRESS + SPACE + CHAUDIERE_OUEST;
			} else if (RSN_ESQ.equalsIgnoreCase(gRoute.getRouteShortName())) {
				return EXPRESS + SPACE + ST_JEAN_CHRYSOSTOME;
			}
			if ("L1".equalsIgnoreCase(gRoute.getRouteShortName())) {
				return ST_ETIENNE + _DASH_ + LEVIS;
			} else if ("L2".equalsIgnoreCase(gRoute.getRouteShortName())) {
				return LEVIS + _DASH_ + UNIVERSITE_LAVAL;
			} else if ("L3".equalsIgnoreCase(gRoute.getRouteShortName())) {
				return ST_ETIENNE + _DASH_ + UNIVERSITE_LAVAL;
			}
			if ("T1".equalsIgnoreCase(gRoute.getRouteShortName())) {
				return CHEMIN_DES_ILES + _DASH_ + LEVIS;
			} else if ("T2".equalsIgnoreCase(gRoute.getRouteShortName())) {
				return RUE_ST_LAURENT + _DASH_ + TRAVERSE_DE_LEVIS;
			} else if ("T11".equalsIgnoreCase(gRoute.getRouteShortName())) {
				return POINTE_DE_LA_MARTINIERE + _DASH_ + LEVIS;
			} else if ("T16".equalsIgnoreCase(gRoute.getRouteShortName())) {
				return BREAKEYVILLE + _DASH_ + ST_JEAN_CHRYSOSTOME;
			} else if ("T22".equalsIgnoreCase(gRoute.getRouteShortName())) {
				return BERNIERES_ST_NICOLAS;
			} else if ("T23".equalsIgnoreCase(gRoute.getRouteShortName())) {
				return VILLAGE_ST_NICOLAS;
			} else if ("T25".equalsIgnoreCase(gRoute.getRouteShortName())) {
				return CHEMIN_VIRE_CREPES + _DASH_ + STATION_PLANTE;
			} else if ("T65".equalsIgnoreCase(gRoute.getRouteShortName())) {
				return ST_LAMBERT + _DASH_ + "Secteur des Éperviers";
			} else if ("T66".equalsIgnoreCase(gRoute.getRouteShortName())) {
				return ST_LAMBERT_DE_LAUZON;
			}
			if ("11A".equalsIgnoreCase(gRoute.getRouteShortName())) {
				return LEVIS_CENTRE + _DASH_ + LAUZON;
			} else if ("13A".equalsIgnoreCase(gRoute.getRouteShortName())) {
				return ST_DAVID + _DASH_ + LEVIS_CENTRE;
			} else if ("27E".equalsIgnoreCase(gRoute.getRouteShortName())) {
				return ST_JEAN_CHRYSOSTOME + _DASH_ + RIVE_NORD;
			} else if ("27R".equalsIgnoreCase(gRoute.getRouteShortName())) {
				return ST_JEAN_CHRYSOSTOME + _DASH_ + ST_ROMUALD;
			} else if ("31E".equalsIgnoreCase(gRoute.getRouteShortName())) {
				return ST_JEAN_CHRYSOSTOME + _DASH_ + CEGEP_LEVIS_LAUZON;
			} else if ("33E".equalsIgnoreCase(gRoute.getRouteShortName())) {
				return ST_FOY + _DASH_ + CEGEP_GARNEAU;
			} else if ("34E".equalsIgnoreCase(gRoute.getRouteShortName())) {
				return ST_ROMUALD + _DASH_ + RIVE_NORD;
			} else if ("35E".equalsIgnoreCase(gRoute.getRouteShortName())) {
				return CHARNY + _DASH_ + RIVE_NORD;
			} else if ("35R".equalsIgnoreCase(gRoute.getRouteShortName())) {
				return CHARNY;
			} else if ("36E".equalsIgnoreCase(gRoute.getRouteShortName())) {
				return CHARNY + _DASH_ + UNIVERSITE_LAVAL;
			} else if ("37E".equalsIgnoreCase(gRoute.getRouteShortName())) {
				return ST_JEAN_CHRYSOSTOME + _DASH_ + UNIVERSITE_LAVAL;
			} else if ("38E".equalsIgnoreCase(gRoute.getRouteShortName())) {
				return ST_JEAN_CHRYSOSTOME + _DASH_ + UNIVERSITE_LAVAL;
			} else if ("41E".equalsIgnoreCase(gRoute.getRouteShortName())) {
				return CHARNY + _DASH_ + CEGEP_LEVIS_LAUZON;
			} else if ("42E".equalsIgnoreCase(gRoute.getRouteShortName())) {
				return LEVIS_CENTRE + _DASH_ + CEGEP_GARNEAU;
			} else if ("43E".equalsIgnoreCase(gRoute.getRouteShortName())) {
				return PARC_RELAIS_BUS_DES_RIVIERES + _DASH_ + RIVE_NORD;
			} else if ("60E".equalsIgnoreCase(gRoute.getRouteShortName())) {
				return PARC_RELAIS_BUS_DES_RIVIERES + _DASH_ + RIVE_NORD;
			}
			MTLog.logFatal("Unexpected route long name for %s!", gRoute);
			return null;
		}
		routeLongName = CleanUtils.SAINT.matcher(routeLongName).replaceAll(CleanUtils.SAINT_REPLACEMENT);
		routeLongName = CleanUtils.CLEAN_PARENTHESE1.matcher(routeLongName).replaceAll(CleanUtils.CLEAN_PARENTHESE1_REPLACEMENT);
		routeLongName = CleanUtils.CLEAN_PARENTHESE2.matcher(routeLongName).replaceAll(CleanUtils.CLEAN_PARENTHESE2_REPLACEMENT);
		return CleanUtils.cleanLabel(routeLongName);
	}

	private static final String AGENCY_COLOR = "009CBE"; // from PDF

	private static final String SCHOOL_BUS_COLOR = "FFD800"; // YELLOW (from Wikipedia)

	@Override
	public String getAgencyColor() {
		return AGENCY_COLOR;
	}

	@Override
	public String getRouteColor(GRoute gRoute) {
		if (StringUtils.isEmpty(gRoute.getRouteColor())) {
			if (Utils.isDigitsOnly(gRoute.getRouteShortName())) {
				int rsn = Integer.parseInt(gRoute.getRouteShortName());
				if (rsn >= 100 && rsn <= 999) {
					return SCHOOL_BUS_COLOR;
				}
			}
			if ("T65".equalsIgnoreCase(gRoute.getRouteShortName())) {
				return "C7B24C";
			}
			if (isGoodEnoughAccepted()) {
				return null;
			}
			MTLog.logFatal("Unexpected route color for %s!", gRoute);
			return null;
		}
		return super.getRouteColor(gRoute);
	}

	private static HashMap<Long, RouteTripSpec> ALL_ROUTE_TRIPS2;

	static {
		HashMap<Long, RouteTripSpec> map2 = new HashMap<>();
		map2.put(11L, new RouteTripSpec(11L, //
				0, MTrip.HEADSIGN_TYPE_STRING, LEVIS_CENTRE, //
				1, MTrip.HEADSIGN_TYPE_STRING, LAUZON) //
				.addTripSort(0, //
						Arrays.asList(//
								"110245", // Lallemand / des Riveurs
								"110535", // Galeries Chagnon / Alph.-Desjardins
								"110003" // Terminus de la Traverse
						)) //
				.addTripSort(1, //
						Arrays.asList(//
								"110003", // Terminus de la Traverse
								"110115", // ++
								"110245" // Lallemand / des Riveurs
						)) //
				.compileBothTripSort());
		map2.put(11L + RID__A, new RouteTripSpec(11L + RID__A, // 11A
				0, MTrip.HEADSIGN_TYPE_STRING, LEVIS_CENTRE, //
				1, MTrip.HEADSIGN_TYPE_STRING, LAUZON) //
				.addTripSort(0, //
						Arrays.asList(//
								"110288", // Lallemand / des Riveurs
								"110130", // ++
								"110002" // Terminus de la Traverse
						)) //
				.addTripSort(1, //
						Arrays.asList(//
								"110002", // Terminus de la Traverse
								"110580", // Galeries Chagnon - A.-Desjardins
								"110450", // Station du Vieux-Fort
								"110288" // Lallemand / des Riveurs
						)) //
				.compileBothTripSort());
		map2.put(27L + RID__R, new RouteTripSpec(27L + RID__R, // 27R
				0, MTrip.HEADSIGN_TYPE_STRING, ST_JEAN_CHRYSOSTOME, //
				1, MTrip.HEADSIGN_TYPE_STRING, ST_ROMUALD) //
				.addTripSort(0, //
						Arrays.asList(//
								"20395", // Station de Saint-Romuald
								"160070", // Taniata / de St-J.-Chrysostome
								"160360", // ++ des Champs / Figaro
								"160650", // Taniata / de Saint-Jean-Chrysostome
								"20410", // Station de Saint-Romuald
								"990041" // Saint-Jacques / de l'Abbaye
						)) //
				.addTripSort(1, //
						Arrays.asList(//
								"990041", // Saint-Jacques / de l'Abbaye
								"170130", // ++ du Sault / de l'Élizabeth
								"20395" // Station de Saint-Romuald
						)) //
				.compileBothTripSort());
		map2.put(35L + RID__R, new RouteTripSpec(35L + RID__R, // 35R
				0, MTrip.HEADSIGN_TYPE_STRING, STATION_DE_LA_CONCORDE, //
				1, MTrip.HEADSIGN_TYPE_STRING, CHARNY) //
				.addTripSort(0, //
						Arrays.asList(//
								"180190", // des Générations / de Charny
								"180280", // ++
								"180320", // des Églises / du C.-Hospitalier
								"180010" // Station de la Concorde
						)) //
				.addTripSort(1, //
						Arrays.asList(//
								"180010", // Station de la Concorde
								"180012", // E.-Lacasse / de la Concorde
								"180030", // des Églises / du C.-Hospitalier
								"180080", // ++
								"180190" // des Générations / de Charny
						)) //
				.compileBothTripSort());
		ALL_ROUTE_TRIPS2 = map2;
	}

	@Override
	public String cleanStopOriginalId(String gStopId) {
		gStopId = CleanUtils.cleanMergedID(gStopId);
		return gStopId;
	}

	@Override
	public int compareEarly(long routeId, List<MTripStop> list1, List<MTripStop> list2, MTripStop ts1, MTripStop ts2, GStop ts1GStop, GStop ts2GStop) {
		if (ALL_ROUTE_TRIPS2.containsKey(routeId)) {
			return ALL_ROUTE_TRIPS2.get(routeId).compare(routeId, list1, list2, ts1, ts2, ts1GStop, ts2GStop, this);
		}
		return super.compareEarly(routeId, list1, list2, ts1, ts2, ts1GStop, ts2GStop);
	}

	@Override
	public ArrayList<MTrip> splitTrip(MRoute mRoute, GTrip gTrip, GSpec gtfs) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return ALL_ROUTE_TRIPS2.get(mRoute.getId()).getAllTrips();
		}
		return super.splitTrip(mRoute, gTrip, gtfs);
	}

	@Override
	public Pair<Long[], Integer[]> splitTripStop(MRoute mRoute, GTrip gTrip, GTripStop gTripStop, ArrayList<MTrip> splitTrips, GSpec routeGTFS) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return SplitUtils.splitTripStop(mRoute, gTrip, gTripStop, routeGTFS, ALL_ROUTE_TRIPS2.get(mRoute.getId()), this);
		}
		return super.splitTripStop(mRoute, gTrip, gTripStop, splitTrips, routeGTFS);
	}

	@Override
	public void setTripHeadsign(MRoute mRoute, MTrip mTrip, GTrip gTrip, GSpec gtfs) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return; // split
		}
		mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), gTrip.getDirectionId());
	}

	@Override
	public boolean mergeHeadsign(MTrip mTrip, MTrip mTripToMerge) {
		List<String> headsignsValues = Arrays.asList(mTrip.getHeadsignValue(), mTripToMerge.getHeadsignValue());
		if (mTrip.getRouteId() == RID_ELQ) { // ELQ
			if (Arrays.asList( //
					QUEBEC_CENTRE, //
					QUEBEC_CENTRE + _VIA_ + DU_SAULT //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(QUEBEC_CENTRE, mTrip.getHeadsignId());
				return true;
			} else if (Arrays.asList( //
					LEVIS_CENTRE, //
					LEVIS_CENTRE + _VIA_ + DU_SAULT //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(LEVIS_CENTRE, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == RID_EOQ) { // EOQ
			if (Arrays.asList( //
					PARC_RELAIS_BUS_DES_RIVIERES, //
					PARC_RELAIS_BUS_DES_RIVIERES + _SLASH_ + ST_ETIENNE_DE_LAUZON //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(PARC_RELAIS_BUS_DES_RIVIERES, mTrip.getHeadsignId());
				return true;
			} else if (Arrays.asList( //
					PLACE_QUEBEC, //
					PLACE_QUEBEC + _DASH_ + RENE_LEVESQUE_SHORT, //
					QUEBEC_CENTRE, //
					"St-Paul" + _SLASH_ + ABRAHAM_MARTIN_SHORT //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(QUEBEC_CENTRE, mTrip.getHeadsignId());
				return true;
			}
			if (Arrays.asList( //
					"Colline Parlementaire", //
					"Colline Parlementaire" + _SLASH_ + "Gare Du Palais" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Colline Parlementaire", mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == RID_ESQ) { // ESQ
			if (Arrays.asList( //
					"St-Paul" + _SLASH_ + ABRAHAM_MARTIN_SHORT, //
					QUEBEC_CENTRE + _VIA_ + DU_SAULT, //
					QUEBEC_CENTRE //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(QUEBEC_CENTRE, mTrip.getHeadsignId());
				return true;
			} else if (Arrays.asList( //
					ST_JEAN_CHRYSOSTOME, //
					ST_JEAN_CHRYSOSTOME + _VIA_ + TANIATA, //
					ST_JEAN_CHRYSOSTOME + _VIA_ + DU_SAULT, //
					ST_JEAN_CHRYSOSTOME + _VIA_ + DU_SAULT + _SLASH_ + TANIATA, //
					ST_JEAN_CHRYSOSTOME + _VIA_ + "Vanier" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(ST_JEAN_CHRYSOSTOME, mTrip.getHeadsignId());
				return true;
			}
			if (Arrays.asList( //
					"Colline Parlementaire" + _SLASH_ + "Gare Du Palais", //
					"Colline Parlementaire" + _SLASH_ + "Gare Du Palais" + _VIA_ + "Du Sault" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Colline Parlementaire" + _SLASH_ + "Gare Du Palais", mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == RID_L + 1L) { // L1
			if (Arrays.asList( //
					STATION_DE_LA_CONCORDE + _SLASH_ + CHARNY , //
					ST_NICOLAS, //
					TERMINUS_LAGUEUX//
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(TERMINUS_LAGUEUX, mTrip.getHeadsignId());
				return true;
			}
			if (Arrays.asList( //
					TERMINUS_DE_LA_TRAVERSE, //
					STATION_DE_LA_CONCORDE //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(TERMINUS_DE_LA_TRAVERSE, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == RID_L + 2L) { // L2
			if (Arrays.asList( //
					STATION_DE_LA_CONCORDE, //
					UNIVERSITE_LAVAL //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(UNIVERSITE_LAVAL, mTrip.getHeadsignId());
				return true;
			}
			if (Arrays.asList( //
					UNIVERSITE_LAVAL, // <>
					GALERIES_CHAGNON, //
					TERMINUS_DE_LA_TRAVERSE //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(TERMINUS_DE_LA_TRAVERSE, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == RID_L + 3L) { // L3
			if (Arrays.asList( //
					UNIVERSITE_LAVAL, // <>
					TERMINUS_LAGUEUX //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(TERMINUS_LAGUEUX, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 13L) {
			if (Arrays.asList( //
					(DORVAL + TO + ST_DAVID + TO + UQAR + TO + HOTEL_DIEU_DE_LEVIS + TO).trim(), //
					(DORVAL + TO + HOTEL_DIEU_DE_LEVIS + TO + UQAR + TO + ST_DAVID + TO).trim() //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString((DORVAL + TO + HOTEL_DIEU_DE_LEVIS + TO + UQAR + TO + ST_DAVID + TO).trim(), mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 13L + RID__A) {
			if (Arrays.asList( //
					GALERIES_CHAGNON + _DASH_ + A_DESJARDINS, //
					(DORVAL + TO + HOTEL_DIEU_DE_LEVIS + TO + UQAR + TO + ST_DAVID + TO).trim() //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString((DORVAL + TO + HOTEL_DIEU_DE_LEVIS + TO + UQAR + TO + ST_DAVID + TO).trim(), mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 14L) {
			if (Arrays.asList( //
					LEVIS_CENTRE, //
					LEVIS_CENTRE + SPACE + P1 + UQAR + P2, //
					UQAR, //
					"Wilfrid-Carrier" + _SLASH_ + "Arpents" // LEVIS_CENTRE
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(LEVIS_CENTRE, mTrip.getHeadsignId()); // LEVIS_CENTRE
				return true;
			}
			if (Arrays.asList( //
					LEVIS_CENTRE, //
					LEVIS_CENTRE + _SLASH_ + "Innoparc", //
					UQAR //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(LEVIS_CENTRE, mTrip.getHeadsignId()); // LEVIS_CENTRE
				return true;
			}
		} else if (mTrip.getRouteId() == 15L) {
			if (Arrays.asList( //
					PINTENDRE, //
					PINTENDRE + SPACE + RACCOURCI, //
					PINTENDRE + _VIA_ + DU_PRESIDENT_KENNEDY, //
					PINTENDRE + _VIA_ + PROVENCE //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(PINTENDRE, mTrip.getHeadsignId());
				return true;
			} else if (Arrays.asList( //
					TERMINUS_DE_LA_TRAVERSE, //
					TERMINUS_DE_LA_TRAVERSE + SPACE + RACCOURCI, //
					TERMINUS_DE_LA_TRAVERSE + _VIA_ + DU_PRESIDENT_KENNEDY, //
					TERMINUS_DE_LA_TRAVERSE + _VIA_ + PROVENCE //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(TERMINUS_DE_LA_TRAVERSE, mTrip.getHeadsignId());
				return true;
			}
			if (Arrays.asList( //
					GALERIES_CHAGNON, //
					GALERIES_CHAGNON + _SLASH_ + POINTE_LEVY).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(GALERIES_CHAGNON, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 19L) {
			if (Arrays.asList( //
					STATION_DE_LA_CONCORDE, // <>
					BREAKEYVILLE //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(BREAKEYVILLE, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 22L) {
			if (Arrays.asList( //
					"Lisière" + _SLASH_ + "Charmilles", //
					BERNIERES_ST_NICOLAS //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(BERNIERES_ST_NICOLAS, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 23L) {
			if (Arrays.asList( //
					PIONNIERS + _SLASH_ + BARONET, //
					VILLAGE_ST_NICOLAS //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(VILLAGE_ST_NICOLAS, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 27L + RID__E) { // 27E
			if (Arrays.asList( //
					CEGEP_GARNEAU, //
					STE_FOY + _DASH_ + UNIVERSITE_LAVAL + _SLASH_ + EMILE_COTE, //
					UNIVERSITE_LAVAL //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(UNIVERSITE_LAVAL, mTrip.getHeadsignId());
				return true;
			} else if (Arrays.asList( //
					ST_JEAN_CHRYSOSTOME, //
					ST_JEAN_CHRYSOSTOME + _VIA_ + EMILE_COTE //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(ST_JEAN_CHRYSOSTOME, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 27L + RID__R) { // 27R
			if (Arrays.asList( //
					DERNIER_ARRET + SPACE + TANIATA + _SLASH_ + ST_JEAN_CHRYSOSTOME, //
					(ST_ROMUALD + TO + ST_JEAN_CHRYSOSTOME + TO).trim() //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString((ST_ROMUALD + TO + ST_JEAN_CHRYSOSTOME + TO).trim(), mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 43L + RID__E) { // 43E
			if (Arrays.asList( //
					PARC_RELAIS_BUS_DES_RIVIERES, //
					PARC_RELAIS_BUS_DES_RIVIERES + _SLASH_ + STATION_DE_LA_CONCORDE //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(PARC_RELAIS_BUS_DES_RIVIERES + _SLASH_ + STATION_DE_LA_CONCORDE, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 65L) {
			if (Arrays.asList( //
					ST_AUG + _SLASH_ + V_CHEMIN, //
					PARC_RELAIS_BUS_DES_RIVIERES //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(PARC_RELAIS_BUS_DES_RIVIERES, mTrip.getHeadsignId());
				return true;
			} else if (Arrays.asList( //
					EXPLORATEURS + _SLASH_ + MONTCALM, //
					ST_LAMBERT //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(ST_LAMBERT, mTrip.getHeadsignId());
				return true;
			}
		}
		MTLog.logFatal("Unexpected trips to merge %s & %s!", mTrip, mTripToMerge);
		return false;
	}

	private static final Pattern STATION = Pattern.compile("((^|\\W)(station)(\\W|$))", Pattern.CASE_INSENSITIVE);
	private static final String STATION_REPLACEMENT = "$2$4"; // "$2" + STATION_SHORT; // + "$4"

	private static final Pattern TERMINUS_ = Pattern.compile("((^|\\W)(terminus)(\\W|$))", Pattern.CASE_INSENSITIVE);
	private static final String TERMINUS_REPLACEMENT = "$2$4"; // "$2" + TERMINUS_SHORT; // + "$4"

	private static final Pattern STE_HELENE_DE_BREAKEYVILLE_ = Pattern.compile("((^|\\W)(ste-hélène-de-breakeyville)(\\W|$))",
			Pattern.CASE_INSENSITIVE);
	private static final String STE_HELENE_DE_BREAKEYVILLE_REPLACEMENT = "$2" + BREAKEYVILLE + "$4";

	private static final Pattern ST_LAMBERT_ = Pattern.compile("((^|\\W)(st-lambert-de-lauzon)(\\W|$))", Pattern.CASE_INSENSITIVE);
	private static final String ST_LAMBERT_REPLACEMENT = "$2" + ST_LAMBERT + "$4";

	// St-Nicolas - Bernières (Direct)
	private static final Pattern ST_NICOLAS_BERNIERES_ = Pattern.compile("((^|\\W)(" //
			+ "st-nicolas - bernières" //
			+ "|" //
			+ "st-nicolas - bernières" //
			+ ")(\\W|$))", Pattern.CASE_INSENSITIVE);
	private static final String ST_NICOLAS_BERNIERES_REPLACEMENT = "$2" + BERNIERES_ST_NICOLAS + "$4";

	private static final Pattern ST_NICOLAS_VILLAGE_ = Pattern.compile("((^|\\W)(st-nicolas - village)(\\W|$))", Pattern.CASE_INSENSITIVE);
	private static final String ST_NICOLAS_VILLAGE_REPLACEMENT = "$2" + VILLAGE_ST_NICOLAS + "$4";

	private static final Pattern UNIVERSITE_ = Pattern.compile("((^|\\W)(universit[é|e])(\\W|$))", Pattern.CASE_INSENSITIVE);
	private static final String UNIVERSITE_REPLACEMENT = "$2" + UNIVERSITE_SHORT; // + "$4"

	private static final Pattern CENTRE_ = Pattern.compile("((^|\\W)(centre)(\\W|$))", Pattern.CASE_INSENSITIVE);
	private static final String CENTRE_REPLACEMENT = "$2" + CENTRE_SHORT + "$4";

	private static final Pattern PARC_RELAIS_BUS_ = Pattern.compile("((^|\\W)(parc-relais-bus)(\\W|$))", Pattern.CASE_INSENSITIVE);
	private static final String PARC_RELAIS_BUS_REPLACEMENT = "$2" + PARC_RELAIS_BUS_SHORT + "$4";

	private static final Pattern QUEBEC_CTR_ = Pattern.compile("((^|\\W)(qu[é|e]bec centre-ville - SAAQ)(\\W|$))", Pattern.CASE_INSENSITIVE);
	private static final String QUEBEC_CTR_REPLACEMENT = "$2" + QUEBEC + SPACE + CENTRE_SHORT + "$4";

	private static final Pattern ST_JEAN_ = Pattern.compile("((^|\\W)(st-jean))", Pattern.CASE_INSENSITIVE);
	private static final String ST_JEAN_REPLACEMENT = "$2" + ST_JEAN_SHORT;

	private static final Pattern JUVENAT_NOTRE_DAME_ = Pattern.compile("((^|\\W)(juv[e|é]nat notre-dame)(\\W|$))", Pattern.CASE_INSENSITIVE);
	private static final String JUVENAT_NOTRE_DAME_REPLACEMENT = "$2" + JUVENAT_NOTRE_DAME_SHORT + "$4";

	private static final Pattern DASH_ = Pattern.compile("((^|\\W)(–)(\\W|$))", Pattern.CASE_INSENSITIVE);
	private static final String DASH_REPLACEMENT = "$2-$4";

	private static final Pattern RENE_LEVESQUE_ = Pattern.compile("((^|\\W)(rené-lévesque)(\\W|$))", Pattern.CASE_INSENSITIVE);
	private static final String RENE_LEVESQUE_REPLACEMENT = "$2" + RENE_LEVESQUE_SHORT + "$4";

	private static final Pattern ABRAHAM_MARTIN_ = Pattern.compile("((^|\\W)(abraham-martin)(\\W|$))", Pattern.CASE_INSENSITIVE);
	private static final String ABRAHAM_MARTIN_REPLACEMENT = "$2" + ABRAHAM_MARTIN_SHORT + "$4";

	private static final Pattern ENDS_WITH_ARRETS_LIMITES_ = Pattern.compile("( \\(arr[e|ê]ts limit[e|é]s\\))", Pattern.CASE_INSENSITIVE);
	private static final Pattern ENDS_WITH_DIRECT_ = Pattern.compile("( \\(direct\\))", Pattern.CASE_INSENSITIVE);

	@Override
	public String cleanTripHeadsign(String tripHeadsign) {
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
		tripHeadsign = ENDS_WITH_DIRECT_.matcher(tripHeadsign).replaceAll(StringUtils.EMPTY);
		tripHeadsign = ENDS_WITH_ARRETS_LIMITES_.matcher(tripHeadsign).replaceAll(StringUtils.EMPTY);
		tripHeadsign = CleanUtils.removePoints(tripHeadsign);
		tripHeadsign = CleanUtils.cleanStreetTypesFRCA(tripHeadsign);
		return CleanUtils.cleanLabelFR(tripHeadsign);
	}

	@Override
	public String cleanStopName(String gStopName) {
		gStopName = CleanUtils.cleanStreetTypesFRCA(gStopName);
		return CleanUtils.cleanLabelFR(gStopName);
	}

	@Override
	public String getStopCode(GStop gStop) {
		return String.valueOf(getStopId(gStop)); // using stop ID as stop code
	}

	@Override
	public int getStopId(GStop gStop) {
		String stopId = gStop.getStopId();
		stopId = CleanUtils.cleanMergedID(stopId);
		return Integer.parseInt(stopId);
	}
}
