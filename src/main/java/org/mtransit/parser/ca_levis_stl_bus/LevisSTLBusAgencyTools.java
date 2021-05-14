package org.mtransit.parser.ca_levis_stl_bus;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mtransit.commons.CharUtils;
import org.mtransit.commons.CleanUtils;
import org.mtransit.commons.StringUtils;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.MTLog;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.mt.data.MAgency;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.mtransit.commons.StringUtils.EMPTY;

// https://www.stlevis.ca/stlevis/donnees-ouvertes
// https://www.stlevis.ca/sites/default/files/public/assets/gtfs/transit/gtfs_stlevis.zip
public class LevisSTLBusAgencyTools extends DefaultAgencyTools {

	public static void main(@NotNull String[] args) {
		new LevisSTLBusAgencyTools().start(args);
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
	public long getRouteId(@NotNull GRoute gRoute) {
		if (CharUtils.isDigitsOnly(gRoute.getRouteShortName())) {
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
		final Matcher matcher = DIGITS.matcher(gRoute.getRouteShortName());
		if (matcher.find()) {
			final long digits = Long.parseLong(matcher.group());
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
		throw new MTLog.Fatal("Unexpected route ID for %s!", gRoute);
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

	private static final String ABRAHAM_MARTIN_SHORT = "A-Martin";
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
	private static final String ETIENNE_DALLAIRE = "Étienne-Dallaire";
	private static final String EXPRESS = "Express";
	private static final String GALERIES = "Galeries";
	private static final String GALERIES_CHAGNON = GALERIES + SPACE + CHAGNON;
	private static final String GRAVEL = "Gravel";
	private static final String JUVENAT = "Juvénat";
	private static final String JUVENAT_NOTRE_DAME_LONG = JUVENAT + " Notre-Dame";
	private static final String JUVENAT_NOTRE_DAME_SHORT = "JND"; // "Juvénat Notre-Dame";
	private static final String LAUZON = "Lauzon";
	private static final String LEVIS = "Lévis";
	private static final String LEVIS_CENTRE = LEVIS + SPACE + CENTRE_SHORT;
	private static final String MANIC = "Manic";
	private static final String MARCELLE_MALLET = "Marcelle-Mallet";
	private static final String PARC_RELAIS_BUS_SHORT = "PRB"; // "P+R";
	private static final String PARC_RELAIS_BUS_DES_RIVIERES = PARC_RELAIS_BUS_SHORT + " Des Rivières";
	private static final String PINTENDRE = "Pintendre";
	private static final String POINTE_DE_LA_MARTINIERE = "Pte de la Martinière";
	private static final String POINTE_LEVY = "Pointe-Lévy";
	private static final String PRESQU_ILE = "Presqu’Île";
	private static final String QUEBEC = "Québec";
	private static final String RENE_LEVESQUE_SHORT = "R-Lévesque";
	private static final String RIVE_NORD = "Rive-" + NORD;
	private static final String ST_DAVID = "St-David";
	private static final String ST_ETIENNE = "St-Étienne";
	private static final String ST_JEAN_SHORT = "St-J";
	private static final String ST_JEAN_CHRYSOSTOME = ST_JEAN_SHORT + "-Chrysostome";
	private static final String ST_LAURENT = "St-Laurent";
	private static final String ST_NICOLAS = "St-Nicolas";
	private static final String ST_LAMBERT = "St-Lambert";
	private static final String ST_LAMBERT_DE_LAUZON = ST_LAMBERT + "-de-Lauzon";
	private static final String ST_REDEMPTEUR = "St-Rédempteur";
	private static final String ST_ROMUALD = "St-Romuald";
	private static final String STATION_DE_LA_CONCORDE = "Concorde"; // STATION_SHORT + "De La Concorde";
	private static final String STATION_PLANTE = "Plante"; // STATION_SHORT + "Plante";
	private static final String STE_FOY = "Ste-Foy";
	private static final String TANIATA = "Taniata";
	private static final String TRAVERSE_DE_LEVIS = "Traverse de " + LEVIS;
	private static final String UNIVERSITE_SHORT = "U.";
	private static final String UNIVERSITE_LAVAL = UNIVERSITE_SHORT + "Laval";
	private static final String V_CHEMIN = "V-Chemin";
	private static final String VIEUX_LEVIS = "Vieux-" + LEVIS;
	private static final String VILLAGE = "Village";

	private static final String CEGEP_LEVIS_LAUZON = CEGEP + SPACE + LEVIS + "/" + LAUZON;
	private static final String CEGEP_GARNEAU = CEGEP + " Garneau";
	private static final String COLLEGE_DE_LEVIS = "Collège de " + LEVIS;
	private static final String RUE_ST_LAURENT = "Rue " + ST_LAURENT;
	private static final String VILLAGE_ST_NICOLAS = VILLAGE + SPACE + P1 + ST_NICOLAS + P2;
	private static final String BERNIERES_ST_NICOLAS = BERNIERES + SPACE + P1 + ST_NICOLAS + P2;

	@NotNull
	@SuppressWarnings("DuplicateBranchesInSwitch")
	@Override
	public String getRouteLongName(@NotNull GRoute gRoute) {
		final String routeLongName = gRoute.getRouteLongNameOrDefault();
		if (StringUtils.isEmpty(routeLongName)) {
			if (CharUtils.isDigitsOnly(gRoute.getRouteShortName())) {
				final int rsn = Integer.parseInt(gRoute.getRouteShortName());
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
				case 31:
					return STATION_DE_LA_CONCORDE + _DASH_ + BREAKEYVILLE;
				case 32:
					return ST_ROMUALD;
				case 34:
					return STATION_DE_LA_CONCORDE + _DASH_ + ST_ROMUALD;
				case 35:
					return STATION_DE_LA_CONCORDE + _DASH_ + CHARNY + _VIA_ + "Eau-Vive";
				case 36:
					return STATION_DE_LA_CONCORDE + _DASH_ + CHARNY + _VIA_ + "C.-Hospitalier";
				case 37:
					return TANIATA + _DASH_ + ST_JEAN_CHRYSOSTOME + _VIA_ + TANIATA;
				case 38:
					return TANIATA + _DASH_ + ST_JEAN_CHRYSOSTOME + _VIA_ + "Vanier";
				case 39:
					return STATION_DE_LA_CONCORDE + _DASH_ + CHARNY + _SLASH_ + ST_JEAN_CHRYSOSTOME;
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
				case 158:
					return PINTENDRE + _DASH_ + POINTE_LEVY;
				case 159:
					return PINTENDRE + _DASH_ + POINTE_LEVY;
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
				case 220:
					return ST_NICOLAS + _SLASH_ + LEVIS_CENTRE;
				case 221:
					return JUVENAT_NOTRE_DAME_LONG + _DASH_ + BERNIERES_ST_NICOLAS;
				case 222:
					return JUVENAT_NOTRE_DAME_LONG + _DASH_ + ST_NICOLAS + _SLASH_ + PRESQU_ILE;
				case 223:
					return ST_NICOLAS + _DASH_ + PRESQU_ILE + _SLASH_ + JUVENAT;
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
				case 311:
					return ST_ROMUALD + _DASH_ + JUVENAT_NOTRE_DAME_LONG + _SLASH_ + BREAKEYVILLE;
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
			} else if ("T13".equalsIgnoreCase(gRoute.getRouteShortName())) {
				return ST_DAVID + _SLASH_ + VIEUX_LEVIS;
			} else if ("T15".equalsIgnoreCase(gRoute.getRouteShortName())) {
				return GALERIES_CHAGNON + _DASH_ + PINTENDRE;
			} else if ("T16".equalsIgnoreCase(gRoute.getRouteShortName())) {
				return BREAKEYVILLE + _DASH_ + ST_JEAN_CHRYSOSTOME;
			} else if ("T22".equalsIgnoreCase(gRoute.getRouteShortName())) {
				return BERNIERES_ST_NICOLAS;
			} else if ("T23".equalsIgnoreCase(gRoute.getRouteShortName())) {
				return VILLAGE_ST_NICOLAS;
			} else if ("T24".equalsIgnoreCase(gRoute.getRouteShortName())) {
				return ST_REDEMPTEUR;
			} else if ("T25".equalsIgnoreCase(gRoute.getRouteShortName())) {
				return CHEMIN_VIRE_CREPES + _DASH_ + STATION_PLANTE;
			} else if ("T31".equalsIgnoreCase(gRoute.getRouteShortName())) {
				return BREAKEYVILLE + _DASH_ + V_CHEMIN;
			} else if ("T33".equalsIgnoreCase(gRoute.getRouteShortName())) {
				return "Vieux Chemin";
			} else if ("T34".equalsIgnoreCase(gRoute.getRouteShortName())) {
				return ST_ROMUALD;
			} else if ("T65".equalsIgnoreCase(gRoute.getRouteShortName())) {
				return ST_LAMBERT + _DASH_ + "Secteur des Éperviers";
			} else if ("T66".equalsIgnoreCase(gRoute.getRouteShortName())) {
				return ST_LAMBERT_DE_LAUZON;
			}
			if ("11A".equalsIgnoreCase(gRoute.getRouteShortName())) {
				return LEVIS_CENTRE + _DASH_ + LAUZON;
			} else if ("13A".equalsIgnoreCase(gRoute.getRouteShortName())) {
				return ST_DAVID + _DASH_ + LEVIS_CENTRE;
			} else if ("24E".equalsIgnoreCase(gRoute.getRouteShortName())) {
				return STE_FOY + _DASH_ + ST_REDEMPTEUR;
			} else if ("27E".equalsIgnoreCase(gRoute.getRouteShortName())) {
				return ST_JEAN_CHRYSOSTOME + _DASH_ + RIVE_NORD;
			} else if ("27R".equalsIgnoreCase(gRoute.getRouteShortName())) {
				return ST_JEAN_CHRYSOSTOME + _DASH_ + ST_ROMUALD;
			} else if ("31E".equalsIgnoreCase(gRoute.getRouteShortName())) {
				return ST_JEAN_CHRYSOSTOME + _DASH_ + CEGEP_LEVIS_LAUZON;
			} else if ("33E".equalsIgnoreCase(gRoute.getRouteShortName())) {
				return STE_FOY + _DASH_ + CEGEP_GARNEAU;
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
			throw new MTLog.Fatal("Unexpected route long name for %s!", gRoute.toStringPlus());
		}
		return cleanRouteLongName(routeLongName);
	}

	@NotNull
	@Override
	public String cleanRouteLongName(@NotNull String routeLongName) {
		routeLongName = CleanUtils.SAINT.matcher(routeLongName).replaceAll(CleanUtils.SAINT_REPLACEMENT);
		routeLongName = CleanUtils.CLEAN_PARENTHESIS1.matcher(routeLongName).replaceAll(CleanUtils.CLEAN_PARENTHESIS1_REPLACEMENT);
		routeLongName = CleanUtils.CLEAN_PARENTHESIS2.matcher(routeLongName).replaceAll(CleanUtils.CLEAN_PARENTHESIS2_REPLACEMENT);
		return CleanUtils.cleanLabel(routeLongName);
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
	public String getRouteColor(@NotNull GRoute gRoute) {
		if (StringUtils.isEmpty(gRoute.getRouteColor())) {
			if (CharUtils.isDigitsOnly(gRoute.getRouteShortName())) {
				final int rsn = Integer.parseInt(gRoute.getRouteShortName());
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
			throw new MTLog.Fatal("Unexpected route color for %s!", gRoute);
		}
		return super.getRouteColor(gRoute);
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
