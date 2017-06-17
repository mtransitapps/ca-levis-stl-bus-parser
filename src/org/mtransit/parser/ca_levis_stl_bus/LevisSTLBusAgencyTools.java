package org.mtransit.parser.ca_levis_stl_bus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.mtransit.parser.CleanUtils;
import org.mtransit.parser.DefaultAgencyTools;
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

// https://www.stlevis.ca/stlevis/donnees-ouvertes
// https://www.stlevis.ca/sites/default/files/public/assets/donnees-ouvertes/stlevis-ete17.zip
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
		System.out.printf("\nGenerating STL bus data...");
		long start = System.currentTimeMillis();
		this.serviceIds = extractUsefulServiceIds(args, this);
		super.start(args);
		System.out.printf("\nGenerating STL bus data... DONE in %s.\n", Utils.getPrettyDuration(System.currentTimeMillis() - start));
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

	private static final long RID__A = 1 * 100000L;
	private static final long RID__E = 5 * 100000L;
	private static final long RID__R = 18 * 100000L;

	private static final String RSN_ECQ = "ECQ";
	private static final String RSN_ELQ = "ELQ";
	private static final String RSN_EOQ = "EOQ";
	private static final String RSN_ESQ = "ESQ";

	private static final long RID_ECQ = 9050317L;
	private static final long RID_ELQ = 9051217L;
	private static final long RID_EOQ = 9051517L;
	private static final long RID_ESQ = 9051917L;

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
		System.out.printf("\nUnexpected route ID for %s!\n", gRoute);
		System.exit(-1);
		return -1l;

	}

	private static final String DASH = " - ";
	private static final String P1 = "(";
	private static final String P2 = ")";
	private static final String SLASH = " / ";
	private static final String SPACE = " ";
	private static final String TO = " > ";
	private static final String VIA = " Via ";
	private static final String OUEST = "Ouest";
	private static final String EST = "Est";
	private static final String NORD = "Nord";

	private static final String A_DESJARDINS = "A-Desjardins";
	private static final String BARONET = "Baronet";
	private static final String BERNIERES = "Bernières";
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
	private static final String CHEMIN_VIRE_CRÊPES = "Chemin Vire-Crêpes";
	private static final String DERNIER_ARRET = "Dernier Arrêt";
	private static final String DIRECT = "Direct";
	private static final String DORVAL = "Dorval";
	private static final String DU_PRESIDENT_KENNEDY = "Du Président-Kennedy";
	private static final String DU_SAULT = "Du Sault";
	private static final String EMILE_COTE = "Émile-Côté";
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
	private static final String PARC_RELAIS_BUS_SHORT = "P+R";
	private static final String PARC_RELAIS_BUS_DES_RIVIÈRES = PARC_RELAIS_BUS_SHORT + " Des Rivières";
	private static final String PINTENDRE = "Pintendre";
	private static final String PIONNIERS = "Pionniers";
	private static final String POINTE_DE_LA_MARTINIÈRE = "Pte de la Martinière";
	private static final String PRESQU_ILE = "Presqu’Île";
	private static final String QUEBEC = "Québec";
	private static final String QUEBEC_CENTRE = QUEBEC + SPACE + CENTRE_SHORT;
	private static final String PLACE = "Place";
	private static final String PLACE_QUEBEC = PLACE + SPACE + QUEBEC;
	private static final String PROVENCE = "Provence";
	private static final String RACCOURCI = "Raccourci";
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
	private static final String _P1_DIRECT_P2 = SPACE + P1 + DIRECT + P2;

	@Override
	public String getRouteLongName(GRoute gRoute) {
		String routeLongName = gRoute.getRouteLongName();
		if (StringUtils.isEmpty(routeLongName)) {
			if (Utils.isDigitsOnly(gRoute.getRouteShortName())) {
				int rsn = Integer.parseInt(gRoute.getRouteShortName());
				switch (rsn) {
				case 11:
					return LAUZON + DASH + LEVIS_CENTRE;
				case 12:
					return LAUZON + TO + VIEUX_LEVIS;
				case 13:
					return ST_DAVID + DASH + LEVIS_CENTRE;
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
				case 65:
					return ST_LAMBERT + DASH + ST_NICOLAS;
				case 131:
					return ST_JEAN_CHRYSOSTOME + SLASH + MANIC + DASH + JUVENAT_NOTRE_DAME_LONG;
				case 135:
					return COLLEGE_DE_LEVIS + SLASH + MARCELLE_MALLET + DASH + ST_JEAN_CHRYSOSTOME;
				case 141:
					return JUVENAT_NOTRE_DAME_LONG + DASH + CHARNY_EST;
				case 151:
					return JUVENAT_NOTRE_DAME_LONG + DASH + CHARNY_OUEST;
				case 161:
					return JUVENAT_NOTRE_DAME_LONG + DASH + ST_JEAN_CHRYSOSTOME + SLASH + ST_ROMUALD;
				case 165:
					return COLLEGE_DE_LEVIS + SLASH + MARCELLE_MALLET + DASH + ST_JEAN_CHRYSOSTOME;
				case 171:
					return ST_JEAN_CHRYSOSTOME + SLASH + GRAVEL + DASH + JUVENAT_NOTRE_DAME_LONG;
				case 175:
					return ST_JEAN_CHRYSOSTOME + SLASH + MANIC + SLASH + COLLEGE_DE_LEVIS + DASH + MARCELLE_MALLET;
				case 185:
					return COLLEGE_DE_LEVIS + SLASH + MARCELLE_MALLET + DASH + ST_ROMUALD;
				case 221:
					return JUVENAT_NOTRE_DAME_LONG + DASH + BERNIERES_ST_NICOLAS;
				case 222:
					return JUVENAT_NOTRE_DAME_LONG + DASH + ST_NICOLAS + SLASH + PRESQU_ILE;
				case 225:
					return COLLEGE_DE_LEVIS + SLASH + MARCELLE_MALLET + DASH + ST_NICOLAS + SLASH + PRESQU_ILE;
				case 231:
					return JUVENAT_NOTRE_DAME_LONG + DASH + ST_NICOLAS;
				case 232:
					return JUVENAT_NOTRE_DAME_LONG + DASH + ST_NICOLAS;
				case 235:
					return COLLEGE_DE_LEVIS + SLASH + MARCELLE_MALLET + DASH + ST_NICOLAS + SPACE + P1 + VILLAGE + P2;
				case 241:
					return JUVENAT_NOTRE_DAME_LONG + DASH + ST_REDEMPTEUR + SLASH + BERNIERES_EST;
				case 242:
					return JUVENAT_NOTRE_DAME_LONG + DASH + ST_REDEMPTEUR;
				case 243:
					return JUVENAT_NOTRE_DAME_LONG + DASH + ST_NICOLAS + SLASH + ST_REDEMPTEUR;
				case 245:
					return COLLEGE_DE_LEVIS + SLASH + MARCELLE_MALLET + DASH + ST_REDEMPTEUR + SLASH + BERNIERES_EST;
				case 246:
					return COLLEGE_DE_LEVIS + SLASH + MARCELLE_MALLET + DASH + ST_REDEMPTEUR + SLASH + ST_NICOLAS;
				case 915:
					return COLLEGE_DE_LEVIS + SLASH + MARCELLE_MALLET + DASH + PINTENDRE;
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
				return ST_ETIENNE + DASH + LEVIS;
			} else if ("L2".equalsIgnoreCase(gRoute.getRouteShortName())) {
				return LEVIS + DASH + UNIVERSITE_LAVAL;
			} else if ("L3".equalsIgnoreCase(gRoute.getRouteShortName())) {
				return ST_ETIENNE + DASH + UNIVERSITE_LAVAL;
			}
			if ("T1".equalsIgnoreCase(gRoute.getRouteShortName())) {
				return CHEMIN_DES_ILES + DASH + LEVIS;
			} else if ("T2".equalsIgnoreCase(gRoute.getRouteShortName())) {
				return RUE_ST_LAURENT + DASH + TRAVERSE_DE_LEVIS;
			} else if ("T11".equalsIgnoreCase(gRoute.getRouteShortName())) {
				return POINTE_DE_LA_MARTINIÈRE + DASH + LEVIS;
			} else if ("T16".equalsIgnoreCase(gRoute.getRouteShortName())) {
				return BREAKEYVILLE + DASH + ST_JEAN_CHRYSOSTOME;
			} else if ("T22".equalsIgnoreCase(gRoute.getRouteShortName())) {
				return BERNIERES_ST_NICOLAS;
			} else if ("T23".equalsIgnoreCase(gRoute.getRouteShortName())) {
				return VILLAGE_ST_NICOLAS;
			} else if ("T25".equalsIgnoreCase(gRoute.getRouteShortName())) {
				return CHEMIN_VIRE_CRÊPES + DASH + STATION_PLANTE;
			} else if ("T65".equalsIgnoreCase(gRoute.getRouteShortName())) {
				return ST_LAMBERT + DASH + "Secteur des Éperviers";
			}
			if ("11A".equalsIgnoreCase(gRoute.getRouteShortName())) {
				return LEVIS_CENTRE + DASH + LAUZON;
			} else if ("13A".equalsIgnoreCase(gRoute.getRouteShortName())) {
				return ST_DAVID + DASH + LEVIS_CENTRE;
			} else if ("27E".equalsIgnoreCase(gRoute.getRouteShortName())) {
				return ST_JEAN_CHRYSOSTOME + DASH + RIVE_NORD;
			} else if ("27R".equalsIgnoreCase(gRoute.getRouteShortName())) {
				return ST_JEAN_CHRYSOSTOME + DASH + ST_ROMUALD;
			} else if ("31E".equalsIgnoreCase(gRoute.getRouteShortName())) {
				return ST_JEAN_CHRYSOSTOME + DASH + CEGEP_LEVIS_LAUZON;
			} else if ("33E".equalsIgnoreCase(gRoute.getRouteShortName())) {
				return ST_FOY + DASH + CEGEP_GARNEAU;
			} else if ("34E".equalsIgnoreCase(gRoute.getRouteShortName())) {
				return ST_ROMUALD + DASH + RIVE_NORD;
			} else if ("35E".equalsIgnoreCase(gRoute.getRouteShortName())) {
				return CHARNY + DASH + RIVE_NORD;
			} else if ("35R".equalsIgnoreCase(gRoute.getRouteShortName())) {
				return CHARNY;
			} else if ("41E".equalsIgnoreCase(gRoute.getRouteShortName())) {
				return CHARNY + DASH + CEGEP_LEVIS_LAUZON;
			} else if ("43E".equalsIgnoreCase(gRoute.getRouteShortName())) {
				return PARC_RELAIS_BUS_DES_RIVIÈRES + DASH + RIVE_NORD;
			} else if ("60E".equalsIgnoreCase(gRoute.getRouteShortName())) {
				return PARC_RELAIS_BUS_DES_RIVIÈRES + DASH + RIVE_NORD;
			}
			System.out.printf("\nUnexpected route long name for %s!\n", gRoute);
			System.exit(-1);
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
				switch (rsn) {
				// @formatter:off
				case 131: return SCHOOL_BUS_COLOR;
				case 135: return SCHOOL_BUS_COLOR;
				case 141: return SCHOOL_BUS_COLOR;
				case 151: return SCHOOL_BUS_COLOR;
				case 161: return SCHOOL_BUS_COLOR;
				case 165: return SCHOOL_BUS_COLOR;
				case 171: return SCHOOL_BUS_COLOR;
				case 175: return SCHOOL_BUS_COLOR;
				case 185: return SCHOOL_BUS_COLOR;
				case 221: return SCHOOL_BUS_COLOR;
				case 222: return SCHOOL_BUS_COLOR;
				case 225: return SCHOOL_BUS_COLOR;
				case 231: return SCHOOL_BUS_COLOR;
				case 232: return SCHOOL_BUS_COLOR;
				case 235: return SCHOOL_BUS_COLOR;
				case 241: return SCHOOL_BUS_COLOR;
				case 242: return SCHOOL_BUS_COLOR;
				case 243: return SCHOOL_BUS_COLOR;
				case 245: return SCHOOL_BUS_COLOR;
				case 246: return SCHOOL_BUS_COLOR;
				case 915: return SCHOOL_BUS_COLOR;
				// @formatter:on
				}
			}
			if ("T65".equalsIgnoreCase(gRoute.getRouteShortName())) {
				return "C7B24C";
			}
			System.out.printf("\nUnexpected route color for %s!\n", gRoute);
			System.exit(-1);
			return null;
		}
		return super.getRouteColor(gRoute);
	}

	private static HashMap<Long, RouteTripSpec> ALL_ROUTE_TRIPS2;
	static {
		HashMap<Long, RouteTripSpec> map2 = new HashMap<Long, RouteTripSpec>();
		map2.put(RID_L + 1L, new RouteTripSpec(RID_L + 1L, // L1
				0, MTrip.HEADSIGN_TYPE_STRING, ST_ETIENNE, //
				1, MTrip.HEADSIGN_TYPE_STRING, LEVIS) //
				.addTripSort(0, //
						Arrays.asList(new String[] { //
						"10005", // Terminus de la Traverse
								"20495", // Station de la Concorde
								"10010", // Terminus Lagueux
						})) //
				.addTripSort(1, //
						Arrays.asList(new String[] { //
						"10010", // Terminus Lagueux
								"20510", // Station de la Concorde
								"10005", // Terminus de la Traverse
						})) //
				.compileBothTripSort());
		map2.put(RID_L + 2L, new RouteTripSpec(RID_L + 2L, // L2
				0, MTrip.HEADSIGN_TYPE_STRING, UNIVERSITE_LAVAL, //
				1, MTrip.HEADSIGN_TYPE_STRING, LEVIS) //
				.addTripSort(0, //
						Arrays.asList(new String[] { //
						"20005", // Terminus de la Traverse
								"20215", // Station Galeries Chagnon
								"20585", // Terminus de la Médecine-U.Laval
						})) //
				.addTripSort(1, //
						Arrays.asList(new String[] { //
						"20585", // Terminus de la Médecine-U.Laval
								"20595", // Terminus de la Médecine-U.Laval
								"20580", // Station Pavillon Desjardins-U.Laval
								"20510", // Station de la Concorde
								"20005", // Terminus de la Traverse
						})) //
				.compileBothTripSort());
		map2.put(RID_L + 3L, new RouteTripSpec(RID_L + 3L, // L3
				0, MTrip.HEADSIGN_TYPE_STRING, UNIVERSITE_LAVAL, //
				1, MTrip.HEADSIGN_TYPE_STRING, ST_ETIENNE) //
				.addTripSort(0, //
						Arrays.asList(new String[] { //
						"30020", // Terminus Lagueux
								"30242", // ++
								"20585", // Terminus de la Médecine-U.Laval
						})) //
				.addTripSort(1, //
						Arrays.asList(new String[] { //
						"20585", // Terminus de la Médecine-U.Laval
								"20595", // Terminus de la Médecine-U.Laval
								"20580", // Station Pavillon Desjardins-U.Laval
								"30020", // Terminus Lagueux
						})) //
				.compileBothTripSort());
		map2.put(11L, new RouteTripSpec(11L, //
				0, MTrip.HEADSIGN_TYPE_STRING, LEVIS_CENTRE, //
				1, MTrip.HEADSIGN_TYPE_STRING, LAUZON) //
				.addTripSort(0, //
						Arrays.asList(new String[] { //
						"110245", // Lallemand / des Riveurs
								"110535", // Galeries Chagnon / Alph.-Desjardins
								"110003", // Terminus de la Traverse
						})) //
				.addTripSort(1, //
						Arrays.asList(new String[] { //
						"110003", // Terminus de la Traverse
								"110115", // ++
								"110245", // Lallemand / des Riveurs
						})) //
				.compileBothTripSort());
		map2.put(11L + RID__A, new RouteTripSpec(11L + RID__A, // 11A
				0, MTrip.HEADSIGN_TYPE_STRING, LEVIS_CENTRE, //
				1, MTrip.HEADSIGN_TYPE_STRING, LAUZON) //
				.addTripSort(0, //
						Arrays.asList(new String[] { //
						"110288", // Lallemand / des Riveurs
								"110130", // ++
								"110002", // Terminus de la Traverse
						})) //
				.addTripSort(1, //
						Arrays.asList(new String[] { //
						"110002", // Terminus de la Traverse
								"110580", // Galeries Chagnon - A.-Desjardins
								"110450", // Station du Vieux-Fort
								"110288", // Lallemand / des Riveurs
						})) //
				.compileBothTripSort());
		map2.put(12L, new RouteTripSpec(12L, //
				0, MTrip.HEADSIGN_TYPE_STRING, VIEUX_LEVIS, //
				1, MTrip.HEADSIGN_TYPE_STRING, LAUZON) //
				.addTripSort(0, //
						Arrays.asList(new String[] { //
						"110288", // Lallemand / des Riveurs
								"120046", // Caron / des Laquiers
								"120380", // Déziel / Marie-Rollet
						})) //
				.addTripSort(1, //
						Arrays.asList(new String[] { //
						"120380", // Déziel / Marie-Rollet
								"120046", // Caron / des Laquiers
								"110245", // Lallemand / des Riveurs
						})) //
				.compileBothTripSort());
		map2.put(13L, new RouteTripSpec(13L, //
				0, MTrip.HEADSIGN_TYPE_STRING, DORVAL, // TODO LEVIS_CENTRE
				1, MTrip.HEADSIGN_TYPE_STRING, ST_DAVID) //
				.addTripSort(0, //
						Arrays.asList(new String[] { //
						"130145", // Ston des Rubis
								"130143", // ++
								"20232", // ++
								"130645", // Station Galeries Chagnon #DORVAL
						})) //
				.addTripSort(1, //
						Arrays.asList(new String[] { //
						"130645", // Station Galeries Chagnon #DORVAL
								"130635", // ++
								"20035", // ++
								"20045", // Ston Hôtel-Dieu de Lévis
								"110830", // ++
								"110527", // Alphonse-Desjardins / Octave-J.-Morin
								"110535", // Galeries Chagnon / Alph.-Desjardins
								"130155", // ++
								"130145", // Ston des Rubis
						})) //
				.compileBothTripSort());
		map2.put(13L + RID__A, new RouteTripSpec(13L + RID__A, // 13A
				0, MTrip.HEADSIGN_TYPE_STRING, DORVAL, // TODO LEVIS_CENTRE
				1, MTrip.HEADSIGN_TYPE_STRING, ST_DAVID) //
				.addTripSort(0, //
						Arrays.asList(new String[] { //
						"130150", // Ston des Rubis
								"130160", // ++
								"110580", // Galeries Chagnon / Alph.-Desjardins
								"110572", // Alphonse-Desjardins / Octave-J.-Morin
								"130720", // ++
								"130002", // Station Galeries Chagnon #DORVAL
						})) //
				.addTripSort(1, //
						Arrays.asList(new String[] { //
						"130002", // Station Galeries Chagnon
								"20217", // ++
								"130140", // ++
								"130150", // Ston des Rubis
						})) //
				.compileBothTripSort());
		map2.put(27L + RID__R, new RouteTripSpec(27L + RID__R, // 27R
				0, MTrip.HEADSIGN_TYPE_STRING, ST_JEAN_CHRYSOSTOME, //
				1, MTrip.HEADSIGN_TYPE_STRING, ST_ROMUALD) //
				.addTripSort(0, //
						Arrays.asList(new String[] { //
						"20395", // Station de Saint-Romuald
								"160360", // ++ des Champs / Figaro
								"160650", // Taniata / de Saint-Jean-Chrysostome
								"20410", // Station de Saint-Romuald
								"990041", // Saint-Jacques / de l'Abbaye
						})) //
				.addTripSort(1, //
						Arrays.asList(new String[] { //
						"990041", // Saint-Jacques / de l'Abbaye
								"170130", // ++ du Sault / de l'Élizabeth
								"20395", // Station de Saint-Romuald
						})) //
				.compileBothTripSort());
		map2.put(33L + RID__E, new RouteTripSpec(33L + RID__E, // 33E
				0, MTrip.HEADSIGN_TYPE_STRING, CEGEP_GARNEAU, //
				1, MTrip.HEADSIGN_TYPE_STRING, LEVIS) //
				.addTripSort(0, //
						Arrays.asList(new String[] { //
						"20215", // Station Galeries Chagnon
								"20375", // ++
								"990109", // Sainte-Foy / Émile-Côté
						})) //
				.addTripSort(1, //
						Arrays.asList(new String[] { //
						/* no stops */
						})) //
				.compileBothTripSort());
		map2.put(35L + RID__R, new RouteTripSpec(35L + RID__R, // 35R
				0, MTrip.HEADSIGN_TYPE_STRING, STATION_DE_LA_CONCORDE, //
				1, MTrip.HEADSIGN_TYPE_STRING, CHARNY) //
				.addTripSort(0, //
						Arrays.asList(new String[] { //
						"180190", // des Générations / de Charny
								"180280", // ++
								"180010", // Station de la Concorde
						})) //
				.addTripSort(1, //
						Arrays.asList(new String[] { //
						"180010", // Station de la Concorde
								"180080", // ++
								"180190", // des Générations / de Charny

						})) //
				.compileBothTripSort());
		map2.put(43L + RID__E, new RouteTripSpec(43L + RID__E, // 43E
				0, MTrip.HEADSIGN_TYPE_STRING, RIVE_NORD, //
				1, MTrip.HEADSIGN_TYPE_STRING, PARC_RELAIS_BUS_DES_RIVIÈRES) //
				.addTripSort(0, //
						Arrays.asList(new String[] { //
						"240345", // Parc-Relais-Bus des Rivières
								"30382", // ++
								"30390", // !=
								"20505", // <>
								"990107", // !=
								"990109", // Sainte-Foy / Émile-Côté
						})) //
				.addTripSort(1, //
						Arrays.asList(new String[] { //
						"990110", // Sainte-Foy / Émile-Côté
								"990111", // !=
								"990112", // !=
								"20505", // == <>
								"30385", // !=
								"990123", // Parc-Relais-Bus des Rivières
						})) //
				.compileBothTripSort());
		map2.put(60L + RID__E, new RouteTripSpec(60L + RID__E, // 60E
				0, MTrip.HEADSIGN_TYPE_STRING, RIVE_NORD, //
				1, MTrip.HEADSIGN_TYPE_STRING, PARC_RELAIS_BUS_DES_RIVIÈRES) //
				.addTripSort(0, //
						Arrays.asList(new String[] { //
						"650670", // Parc-Relais-Bus des Rivières
								"990224", // !=
								"20510", // <>
								"180010", // <>
								"20505", // <>
								"30385", // !=
								"990221", // des Quatre-Bourgeois / de Marly
						})) //
				.addTripSort(1, //
						Arrays.asList(new String[] { //
						"990221", // des Quatre-Bourgeois / de Marly
								"990224", // !=
								"20510", // <>
								"180010", // <>
								"20505", // <>
								"30385", // !=
								"990123", // Parc-Relais-Bus des Rivières
						})) //
				.compileBothTripSort());
		ALL_ROUTE_TRIPS2 = map2;
	}

	@Override
	public int compareEarly(long routeId, List<MTripStop> list1, List<MTripStop> list2, MTripStop ts1, MTripStop ts2, GStop ts1GStop, GStop ts2GStop) {
		if (ALL_ROUTE_TRIPS2.containsKey(routeId)) {
			return ALL_ROUTE_TRIPS2.get(routeId).compare(routeId, list1, list2, ts1, ts2, ts1GStop, ts2GStop);
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
			return SplitUtils.splitTripStop(mRoute, gTrip, gTripStop, routeGTFS, ALL_ROUTE_TRIPS2.get(mRoute.getId()));
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
					QUEBEC_CENTRE + VIA + DU_SAULT //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(QUEBEC_CENTRE, mTrip.getHeadsignId());
				return true;
			} else if (Arrays.asList( //
					LEVIS_CENTRE, //
					LEVIS_CENTRE + VIA + DU_SAULT //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(LEVIS_CENTRE, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == RID_EOQ) { // EOQ
			if (Arrays.asList( //
					PARC_RELAIS_BUS_DES_RIVIÈRES, //
					PARC_RELAIS_BUS_DES_RIVIÈRES + SLASH + ST_ETIENNE_DE_LAUZON //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(PARC_RELAIS_BUS_DES_RIVIÈRES, mTrip.getHeadsignId());
				return true;
			} else if (Arrays.asList( //
					PLACE_QUEBEC, //
					QUEBEC_CENTRE //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(QUEBEC_CENTRE, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == RID_L + 1L) { // L1
			if (Arrays.asList( //
					TERMINUS_DE_LA_TRAVERSE, //
					STATION_DE_LA_CONCORDE //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(TERMINUS_DE_LA_TRAVERSE, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == RID_L + 2L) { // L2
			if (Arrays.asList( //
					STATION_PAVILLON_DESJARDINS + DASH + UNIVERSITE_LAVAL, //
					TERMINUS_DE_LA_TRAVERSE //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(TERMINUS_DE_LA_TRAVERSE, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == RID_L + 3L) { // L3
			if (Arrays.asList( //
					TERMINUS_LAGUEUX, //
					STATION_PAVILLON_DESJARDINS + DASH + UNIVERSITE_LAVAL //
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
					GALERIES_CHAGNON + DASH + A_DESJARDINS, //
					(DORVAL + TO + HOTEL_DIEU_DE_LEVIS + TO + UQAR + TO + ST_DAVID + TO).trim()//
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString((DORVAL + TO + HOTEL_DIEU_DE_LEVIS + TO + UQAR + TO + ST_DAVID + TO).trim(), mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 14L) {
			if (Arrays.asList( //
					UQAR, //
					"Wilfrid-Carrier" + SLASH + "Arpents" // LEVIS_CENTRE
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Wilfrid-Carrier" + SLASH + "Arpents", mTrip.getHeadsignId()); // LEVIS_CENTRE
				return true;
			}
		} else if (mTrip.getRouteId() == 15L) {
			if (Arrays.asList( //
					PINTENDRE, //
					PINTENDRE + SPACE + RACCOURCI, //
					PINTENDRE + VIA + DU_PRESIDENT_KENNEDY, //
					PINTENDRE + VIA + PROVENCE //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(PINTENDRE, mTrip.getHeadsignId());
				return true;
			} else if (Arrays.asList( //
					TERMINUS_DE_LA_TRAVERSE, //
					TERMINUS_DE_LA_TRAVERSE + SPACE + RACCOURCI, //
					TERMINUS_DE_LA_TRAVERSE + VIA + DU_PRESIDENT_KENNEDY, //
					TERMINUS_DE_LA_TRAVERSE + VIA + PROVENCE //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(TERMINUS_DE_LA_TRAVERSE, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 19l) {
			if (Arrays.asList( //
					ST_AUG + SLASH + V_CHEMIN, //
					BREAKEYVILLE, //
					BREAKEYVILLE + _P1_DIRECT_P2, //
					BREAKEYVILLE + SLASH + ST_LAMBERT //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(BREAKEYVILLE, mTrip.getHeadsignId());
				return true;
			} else if (Arrays.asList( //
					STATION_DE_LA_CONCORDE, //
					STATION_DE_LA_CONCORDE + _P1_DIRECT_P2 //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(STATION_DE_LA_CONCORDE, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 22L) {
			if (Arrays.asList( //
					PARC_RELAIS_BUS_DES_RIVIÈRES, //
					PARC_RELAIS_BUS_DES_RIVIÈRES + _P1_DIRECT_P2 //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(PARC_RELAIS_BUS_DES_RIVIÈRES, mTrip.getHeadsignId());
				return true;
			} else if (Arrays.asList( //
					BERNIERES_ST_NICOLAS, //
					BERNIERES_ST_NICOLAS + _P1_DIRECT_P2 //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(BERNIERES_ST_NICOLAS, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 23L) {
			if (Arrays.asList( //
					PARC_RELAIS_BUS_DES_RIVIÈRES, //
					PARC_RELAIS_BUS_DES_RIVIÈRES + _P1_DIRECT_P2 //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(PARC_RELAIS_BUS_DES_RIVIÈRES, mTrip.getHeadsignId());
				return true;
			} else if (Arrays.asList( //
					PIONNIERS + SLASH + BARONET, //
					VILLAGE_ST_NICOLAS, //
					VILLAGE_ST_NICOLAS + _P1_DIRECT_P2 //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(VILLAGE_ST_NICOLAS, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 27L + RID__E) { // 27E
			if (Arrays.asList( //
					STE_FOY + DASH + UNIVERSITE_LAVAL + SLASH + EMILE_COTE, //
					UNIVERSITE_LAVAL //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(UNIVERSITE_LAVAL, mTrip.getHeadsignId());
				return true;
			} else if (Arrays.asList( //
					ST_JEAN_CHRYSOSTOME, //
					ST_JEAN_CHRYSOSTOME + VIA + EMILE_COTE //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(ST_JEAN_CHRYSOSTOME, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 27L + RID__R) { // 27R
			if (Arrays.asList( //
					DERNIER_ARRET + SPACE + TANIATA + SLASH + ST_JEAN_CHRYSOSTOME, //
					(ST_ROMUALD + TO + ST_JEAN_CHRYSOSTOME + TO).trim() //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString((ST_ROMUALD + TO + ST_JEAN_CHRYSOSTOME + TO).trim(), mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 65l) {
			if (Arrays.asList( //
					ST_AUG + SLASH + V_CHEMIN, //
					PARC_RELAIS_BUS_DES_RIVIÈRES //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(PARC_RELAIS_BUS_DES_RIVIÈRES, mTrip.getHeadsignId());
				return true;
			} else if (Arrays.asList( //
					EXPLORATEURS + SLASH + MONTCALM, //
					ST_LAMBERT //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(ST_LAMBERT, mTrip.getHeadsignId());
				return true;
			}
		}
		System.out.printf("\nUnexpected trips to merge %s & %s!\n", mTrip, mTripToMerge);
		System.exit(-1);
		return false;
	}

	private static final Pattern STATION = Pattern.compile("((^|\\W){1}(station)(\\W|$){1})", Pattern.CASE_INSENSITIVE);
	private static final String STATION_REPLACEMENT = "$2$4"; // "$2" + STATION_SHORT; // + "$4"

	private static final Pattern TERMINUS_ = Pattern.compile("((^|\\W){1}(terminus)(\\W|$){1})", Pattern.CASE_INSENSITIVE);
	private static final String TERMINUS_REPLACEMENT = "$2$4"; // "$2" + TERMINUS_SHORT; // + "$4"

	private static final Pattern STE_HELENE_DE_BREAKEYVILLE_ = Pattern.compile("((^|\\W){1}(ste\\-hélène\\-de\\-breakeyville)(\\W|$){1})",
			Pattern.CASE_INSENSITIVE);
	private static final String STE_HELENE_DE_BREAKEYVILLE_REPLACEMENT = "$2" + BREAKEYVILLE + "$4";

	private static final Pattern ST_LAMBERT_ = Pattern.compile("((^|\\W){1}(st\\-lambert\\-de\\-lauzon)(\\W|$){1})", Pattern.CASE_INSENSITIVE);
	private static final String ST_LAMBERT_REPLACEMENT = "$2" + ST_LAMBERT + "$4";

	private static final Pattern ST_NICOLAS_BERNIERES_ = Pattern.compile("((^|\\W){1}(st\\-nicolas \\- bernières)(\\W|$){1})", Pattern.CASE_INSENSITIVE);
	private static final String ST_NICOLAS_BERNIERES_REPLACEMENT = "$2" + BERNIERES_ST_NICOLAS + "$4";

	private static final Pattern ST_NICOLAS_VILLAGE_ = Pattern.compile("((^|\\W){1}(st\\-nicolas \\- village)(\\W|$){1})", Pattern.CASE_INSENSITIVE);
	private static final String ST_NICOLAS_VILLAGE_REPLACEMENT = "$2" + VILLAGE_ST_NICOLAS + "$4";

	private static final Pattern UNIVERSITE_ = Pattern.compile("((^|\\W){1}(universit[é|e])(\\W|$){1})", Pattern.CASE_INSENSITIVE);
	private static final String UNIVERSITE_REPLACEMENT = "$2" + UNIVERSITE_SHORT; // + "$4"

	private static final Pattern CENTRE_ = Pattern.compile("((^|\\W){1}(centre)(\\W|$){1})", Pattern.CASE_INSENSITIVE);
	private static final String CENTRE_REPLACEMENT = "$2" + CENTRE_SHORT + "$4";

	private static final Pattern PARC_RELAIS_BUS_ = Pattern.compile("((^|\\W){1}(parc\\-relais\\-bus)(\\W|$){1})", Pattern.CASE_INSENSITIVE);
	private static final String PARC_RELAIS_BUS_REPLACEMENT = "$2" + PARC_RELAIS_BUS_SHORT + "$4";

	private static final Pattern QUEBEC_CTR_ = Pattern.compile("((^|\\W){1}(qu[é|e]bec centre\\-ville \\- SAAQ)(\\W|$){1})", Pattern.CASE_INSENSITIVE);
	private static final String QUEBEC_CTR_REPLACEMENT = "$2" + QUEBEC + SPACE + CENTRE_SHORT + "$4";

	private static final Pattern ST_JEAN_ = Pattern.compile("((^|\\W){1}(st\\-jean))", Pattern.CASE_INSENSITIVE);
	private static final String ST_JEAN_REPLACEMENT = "$2" + ST_JEAN_SHORT;

	private static final Pattern JUVENAT_NOTRE_DAME_ = Pattern.compile("((^|\\W){1}(juv[e|é]nat notre\\-dame)(\\W|$){1})", Pattern.CASE_INSENSITIVE);
	private static final String JUVENAT_NOTRE_DAME_REPLACEMENT = "$2" + JUVENAT_NOTRE_DAME_SHORT + "$4";

	private static final Pattern DASH_ = Pattern.compile("((^|\\W){1}(–)(\\W|$){1})", Pattern.CASE_INSENSITIVE);
	private static final String DASH_REPLACEMENT = "$2-$4";

	@Override
	public String cleanTripHeadsign(String tripHeadsign) {
		tripHeadsign = DASH_.matcher(tripHeadsign).replaceAll(DASH_REPLACEMENT);
		tripHeadsign = TERMINUS_.matcher(tripHeadsign).replaceAll(TERMINUS_REPLACEMENT);
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
		return gStop.getStopId(); // using stop ID as stop code
	}
}
