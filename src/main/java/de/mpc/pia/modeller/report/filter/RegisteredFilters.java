package de.mpc.pia.modeller.report.filter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import de.mpc.pia.intermediate.Accession;
import de.mpc.pia.intermediate.Modification;
import de.mpc.pia.modeller.peptide.ReportPeptide;
import de.mpc.pia.modeller.protein.ReportProtein;
import de.mpc.pia.modeller.psm.PSMReportItem;
import de.mpc.pia.modeller.psm.ReportPSM;
import de.mpc.pia.modeller.psm.ReportPSMSet;
import de.mpc.pia.modeller.report.filter.impl.PSMScoreFilter;
import de.mpc.pia.modeller.report.filter.impl.PSMTopIdentificationFilter;
import de.mpc.pia.modeller.report.filter.impl.PeptideScoreFilter;
import de.mpc.pia.modeller.report.filter.impl.SimpleTypeFilter;

/**
 * All the filters should be registered in this enum.
 *
 * @author julian
 *
 */
public enum RegisteredFilters {

    /* -------------------------------------------------------------------------
    /* PSM based filters
    /* -----------------------------------------------------------------------*/

    CHARGE_FILTER(FilterType.numerical, Number.class, "Charge Filter", "Charge (PSM)") {
        @Override
        public SimpleTypeFilter<Number> newInstanceOf(FilterComparator arg, Object value, boolean negate) {
            return new SimpleTypeFilter<>(arg, this, negate, ((Number) value).intValue());
        }

        @Override
        public Number getObjectsValue(Object o) {
            if (o instanceof PSMReportItem) {
                // if we get a ReportItem, return its charge value
                return ((PSMReportItem)o).getCharge();
            } else if (o instanceof Number) {
                // if we get an Number, just return it
                return (Number)o;
            } else {
                // nothing supported
                return null;
            }
        }

        @Override
        public boolean supportsClass(Object obj) {
            return (obj instanceof PSMReportItem);
        }
    },
    DELTA_MASS_FILTER(FilterType.numerical, Number.class, "dMass Filter for PSM", "dMass (PSM)") {
        @Override
        public SimpleTypeFilter<Number> newInstanceOf(FilterComparator arg, Object value, boolean negate) {
            return new SimpleTypeFilter<>(arg, this, negate, ((Number) value).doubleValue());
        }

        @Override
        public Object getObjectsValue(Object o) {
            if (o instanceof PSMReportItem) {
                return ((PSMReportItem)o).getDeltaMass();
            } else if (o instanceof Number) {
                return o;
            }

            // nothing supported
            return null;
        }

        @Override
        public boolean supportsClass(Object c) {
            return ((c instanceof PSMReportItem) || (c instanceof Number));
        }
    },
    DELTA_PPM_FILTER(FilterType.numerical, Number.class, "dPPM Filter for PSM", "dPPM (PSM)") {
        @Override
        public SimpleTypeFilter<Number> newInstanceOf(FilterComparator arg, Object value, boolean negate) {
            return new SimpleTypeFilter<>(arg, this, negate, ((Number) value).doubleValue());
        }

        @Override
        public Object getObjectsValue(Object o) {
            if (o instanceof PSMReportItem) {
                return ((PSMReportItem)o).getDeltaPPM();
            } else if (o instanceof Number) {
                return o;
            }

            // nothing supported
            return null;
        }

        @Override
        public boolean supportsClass(Object c) {
            return (c instanceof PSMReportItem);
        }
    },
    MZ_FILTER(FilterType.numerical, Number.class, "m/z Filter for PSM", "m/z (PSM)") {
        @Override
        public SimpleTypeFilter<Number> newInstanceOf(FilterComparator arg, Object value, boolean negate) {
            return new SimpleTypeFilter<>(arg, this, negate, ((Number) value).doubleValue());
        }

        @Override
        public Object getObjectsValue(Object o) {
            if (o instanceof PSMReportItem) {
                return ((PSMReportItem)o).getMassToCharge();
            } else if (o instanceof Number) {
                return o;
            }

            // nothing supported
            return null;
        }

        @Override
        public boolean supportsClass(Object c) {
            return (c instanceof PSMReportItem);
        }
    },
    PSM_ACCESSIONS_FILTER(FilterType.literal_list, String.class, "Accessions Filter for PSM", "Accessions (PSM)") {
        @Override
        public SimpleTypeFilter<String> newInstanceOf(FilterComparator arg, Object value, boolean negate) {
            return new SimpleTypeFilter<>(arg, this, negate, (String) value);
        }

        @Override
        public Object getObjectsValue(Object o) {
            if (o instanceof PSMReportItem) {
                return ((PSMReportItem) o).getAccessions();
            } else if (o instanceof List<?>) {
                return ((List<?>) o).stream().filter(obj -> obj instanceof String).map(obj -> (String) obj).collect(Collectors.toList());
            }

            // nothing supported
            return null;
        }

        @Override
        public boolean supportsClass(Object obj) {
            return (obj instanceof PSMReportItem);
        }

        @Override
        public boolean valueNeedsFileRefinement() {
            return true;
        }

        @Override
        public Object doFileRefinement(Long fileID, Object o) {
            // converts the list of strings or accessions into a List<String>
            List<String> strList = new ArrayList<>();

            if (o instanceof List<?>) {
                for (Object obj : (List<?>)o) {
                    if (obj instanceof Accession) {
                        if (((fileID > 0) && (((Accession)obj).foundInFile(fileID))) ||
                                (fileID == 0)) {
                            strList.add(((Accession)obj).getAccession());
                        }
                    } else if (obj instanceof String) {
                        strList.add((String)obj);
                    }
                }
            }

            return strList;
        }
    },
    PSM_DESCRIPTION_FILTER(FilterType.literal_list, String.class, "Description Filter for PSM", "Description (PSM)") {
        @Override
        public SimpleTypeFilter<String> newInstanceOf(FilterComparator arg, Object value, boolean negate) {
            return new SimpleTypeFilter<>(arg, this, negate, (String) value);
        }

        @Override
        public Object getObjectsValue(Object o) {
            if (o instanceof PSMReportItem) {
                return ((PSMReportItem) o).getAccessions();
            } else if (o instanceof List<?>) {
                return ((List<?>) o).stream().filter(obj -> obj instanceof String).map(obj -> (String) obj).collect(Collectors.toList());
            }

            // nothing supported
            return null;
        }

        @Override
        public boolean supportsClass(Object c) {
            return (c instanceof PSMReportItem);
        }

        @Override
        public boolean valueNeedsFileRefinement() {
            return true;
        }

        @Override
        public Object doFileRefinement(Long fileID, Object o) {
            List<String> strList = new ArrayList<>();

            if (o instanceof List<?>) {
                for (Object obj : (List<?>)o) {
                    if (obj instanceof Accession) {
                        if ((fileID > 0) && (((Accession)obj).foundInFile(fileID))) {
                            strList.add(((Accession)obj).getDescription(fileID));
                        } else if (fileID == 0) {
                            strList.addAll(((Accession) obj).getDescriptions().entrySet().stream().map(Map.Entry::getValue).collect(Collectors.toList()));
                        }
                    } else if (obj instanceof String) {
                        strList.add((String)obj);
                    }
                }
            }

            return strList;
        }
    },
    PSM_FILE_LIST_FILTER(FilterType.literal_list, String.class, "File List Filter for PSM", "File List (PSM)") {
        @Override
        public SimpleTypeFilter<String> newInstanceOf(FilterComparator arg, Object value, boolean negate) {
            return new SimpleTypeFilter<>(arg, this, negate, (String) value);
        }

        @Override
        public Object getObjectsValue(Object o) {
            if (o instanceof ReportPSM) {
                return o;
            } else if (o instanceof ReportPSMSet) {
                // if we get an ReportPSM, return its PSMs
                return ((ReportPSMSet)o).getPSMs();
            } else if (o instanceof List<?>) {
                return ((List<?>) o).stream().filter(obj -> obj instanceof ReportPSM).map(obj -> (ReportPSM) obj).collect(Collectors.toList());
            } else {
                // nothing supported
                return null;
            }
        }

        @Override
        public boolean supportsClass(Object c) {
            return (c instanceof ReportPSMSet) || (c instanceof ReportPSM);
        }

        @Override
        public boolean valueNeedsFileRefinement() {
            return true;
        }

        @Override
        public Object doFileRefinement(Long fileID, Object o) {
            List<String> strList = new ArrayList<>();

            if (o instanceof List<?>) {
                for (Object obj : (List<?>)o) {
                    if (obj instanceof ReportPSM) {
                        strList.add(((ReportPSM)obj).getFile().getName());
                    } else if (obj instanceof String) {
                        strList.add((String)obj);
                    }
                }
            } else if (o instanceof ReportPSM) {
                strList.add(((ReportPSM) o).getFile().getName());
            }

            return strList;
        }
    },
    PSM_MISSED_CLEAVAGES_FILTER(FilterType.numerical, Number.class, "Missed Cleavages Filter for PSM", "Missed Cleavages (PSM)") {
        @Override
        public SimpleTypeFilter<Number> newInstanceOf(FilterComparator arg, Object value, boolean negate) {
            return new SimpleTypeFilter<>(arg, this, negate, ((Number) value).intValue());
        }

        @Override
        public Object getObjectsValue(Object o) {
            if (o instanceof PSMReportItem) {
                return ((PSMReportItem)o).getMissedCleavages();
            } else if (o instanceof Number) {
                return ((Number) o).intValue();
            } else {
                // nothing supported
                return null;
            }
        }

        @Override
        public boolean supportsClass(Object c) {
            return (c instanceof PSMReportItem);
        }
    },
    PSM_MODIFICATIONS_FILTER(FilterType.modification, String.class, "Modifications Filter for PSM", "Modifications (PSM)") {
        @Override
        public SimpleTypeFilter<String> newInstanceOf(FilterComparator arg, Object value, boolean negate) {
            return new SimpleTypeFilter<>(arg, this, negate, (String) value);
        }

        @Override
        public Object getObjectsValue(Object o) {
            if (o instanceof PSMReportItem) {
                return ((PSMReportItem) o).getModifications().entrySet().stream().map(Map.Entry::getValue).collect(Collectors.toList());
            } else if (o instanceof List<?>) {
                return ((List<?>) o).stream().filter(obj -> obj instanceof Modification).map(obj -> (Modification) obj).collect(Collectors.toList());
            }

            // nothing supported
            return null;
        }

        @Override
        public boolean supportsClass(Object c) {
            return (c instanceof PSMReportItem);
        }
    },
    PSM_RANK_FILTER(FilterType.numerical, Number.class, "Rank Filter for PSM", "Rank (PSM)") {
        @Override
        public SimpleTypeFilter<Number> newInstanceOf(FilterComparator arg, Object value, boolean negate) {
            return new SimpleTypeFilter<>(arg, this, negate, ((Number) value).intValue());
        }

        @Override
        public Object getObjectsValue(Object o) {
            if (o instanceof PSMReportItem) {
                return ((PSMReportItem)o).getRank();
            } else if (o instanceof Number) {
                return o;
            } else {
                // nothing supported
                return null;
            }
        }

        @Override
        public boolean supportsClass(Object c) {
            return (c instanceof PSMReportItem);
        }
    },
    PSM_SEQUENCE_FILTER(FilterType.literal, String.class, "Sequence Filter for PSM", "Sequence (PSM)") {
        @Override
        public SimpleTypeFilter<String> newInstanceOf(FilterComparator arg, Object value, boolean negate) {
            return new SimpleTypeFilter<>(arg, this, negate, (String) value);
        }

        @Override
        public Object getObjectsValue(Object o) {
            if (o instanceof PSMReportItem) {
                return ((PSMReportItem)o).getSequence();
            } else if (o instanceof String) {
                return o;
            } else {
                // nothing supported
                return null;
            }
        }

        @Override
        public boolean supportsClass(Object c) {
            return (c instanceof PSMReportItem);
        }
    },
    PSM_UNIQUE_FILTER(FilterType.bool, Boolean.class, "Unique Filter for PSM", "Unique (PSM)") {
        @Override
        public SimpleTypeFilter<Boolean> newInstanceOf(FilterComparator arg, Object value, boolean negate) {
            return new SimpleTypeFilter<>(arg, this, negate, (Boolean) value);
        }

        @Override
        public Object getObjectsValue(Object o) {
            if (o instanceof PSMReportItem) {
                Boolean isUnique = null;
                if (o instanceof ReportPSMSet) {
                    isUnique = ((ReportPSMSet) o).getPSMs().get(0).getSpectrum().getIsUnique();
                } else if (o instanceof ReportPSM) {
                    isUnique = ((ReportPSM) o).getSpectrum().getIsUnique();
                }

                if (isUnique != null) {
                    return isUnique;
                } else {
                    return Boolean.FALSE;
                }
            } else if (o instanceof Boolean) {
                return o;
            }

            // nothing supported
            return null;
        }

        @Override
        public boolean supportsClass(Object c) {
            return (c instanceof PSMReportItem);
        }

    },
    NR_ACCESSIONS_PER_PSM_FILTER(FilterType.numerical, Number.class, "#accessions per PSM", "#accessions (PSM)") {
        @Override
        public SimpleTypeFilter<Number> newInstanceOf(FilterComparator arg, Object value, boolean negate) {
            return new SimpleTypeFilter<>(arg, this, negate, ((Number) value).intValue());
        }

        @Override
        public Object getObjectsValue(Object o) {
            if (o instanceof PSMReportItem) {
                return ((PSMReportItem) o).getAccessions();
            } else if (o instanceof List<?>) {
                return ((List<?>) o).stream().filter(obj -> obj instanceof String).map(obj -> (String) obj).collect(Collectors.toList());
            }

            // nothing supported
            return null;
        }

        @Override
        public boolean supportsClass(Object c) {
            return (c instanceof PSMReportItem);
        }

        @Override
        public boolean valueNeedsFileRefinement() {
                return true;
        }

        @Override
        public Object doFileRefinement(Long fileID, Object o) {
            List<String> strList = new ArrayList<>();

            if (o instanceof List<?>) {
                for (Object obj : (List<?>)o) {
                    if (obj instanceof Accession) {
                        if (((fileID > 0) && ((Accession)obj).foundInFile(fileID)) ||
                                (fileID == 0)) {
                            strList.add(((Accession)obj).getAccession());
                        }
                    } else if (obj instanceof String) {
                        strList.add((String)obj);
                    }
                }
            }

            return strList.size();
        }
    },
    NR_PSMS_PER_PSM_SET_FILTER(FilterType.numerical, Number.class, "#PSMs per PSM Set", "#PSMs (PSM Set)") {
        @Override
        public SimpleTypeFilter<Number> newInstanceOf(FilterComparator arg, Object value, boolean negate) {
            return new SimpleTypeFilter<>(arg, this, negate, ((Number) value).intValue());
        }

        @Override
        public Object getObjectsValue(Object o) {
            if (o instanceof ReportPSMSet) {
                return ((ReportPSMSet) o).getPSMs().size();
            } else if (o instanceof Number) {
                // if we get a Number, just return it
                return o;
            }

            // nothing supported
            return null;
        }

        @Override
        public boolean supportsClass(Object c) {
            return (c instanceof ReportPSMSet);
        }
    },
    PSM_SOURCE_ID_FILTER(FilterType.literal, String.class, "Source ID Filter for PSM", "Source ID (PSM)") {
        @Override
        public SimpleTypeFilter<String> newInstanceOf(FilterComparator arg, Object value, boolean negate) {
            return new SimpleTypeFilter<>(arg, this, negate, (String) value);
        }

        @Override
        public Object getObjectsValue(Object o) {
            if (o instanceof PSMReportItem) {
                return ((PSMReportItem)o).getSourceID();
            } else if (o instanceof String) {
                return o;
            }

            // nothing supported
            return null;
        }

        @Override
        public boolean supportsClass(Object c) {
            return (c instanceof PSMReportItem);
        }
    },

    /* -------------------------------------------------------------------------
    /* peptide based filters
    /* -----------------------------------------------------------------------*/

    PEPTIDE_ACCESSIONS_FILTER(FilterType.literal_list, String.class, "Accessions Filter for Peptide", "Accessions (Peptide)") {
        @Override
        public SimpleTypeFilter<String> newInstanceOf(FilterComparator arg, Object value, boolean negate) {
            return new SimpleTypeFilter<>(arg, this, negate, (String) value);
        }

        @Override
        public Object getObjectsValue(Object o) {
            if (o instanceof ReportPeptide) {
                return ((ReportPeptide) o).getAccessions();
            } else if (o instanceof List<?>) {
                return ((List<?>) o).stream().filter(obj -> obj instanceof String).map(obj -> (String) obj).collect(Collectors.toList());
            }

            // nothing supported
            return null;
        }

        @Override
        public boolean supportsClass(Object c) {
            return (c instanceof ReportPeptide);
        }

        @Override
        public boolean valueNeedsFileRefinement() {
            return true;
        }

        @Override
        public Object doFileRefinement(Long fileID, Object o) {
            List<String> strList = new ArrayList<>();

            if (o instanceof List<?>) {
                for (Object obj : (List<?>)o) {
                    if (obj instanceof Accession) {
                        if (((fileID > 0) && (((Accession)obj).foundInFile(fileID))) ||
                                (fileID == 0)) {
                            strList.add(((Accession)obj).getAccession());
                        }
                    } else if (obj instanceof String) {
                        strList.add((String)obj);
                    }
                }
            }

            return strList;
        }
    },
    PEPTIDE_DESCRIPTION_FILTER(FilterType.literal_list, String.class, "Description Filter for Peptide", "Description (Peptide)") {
        @Override
        public SimpleTypeFilter<String> newInstanceOf(FilterComparator arg, Object value, boolean negate) {
            return new SimpleTypeFilter<>(arg, this, negate, (String) value);
        }

        @Override
        public Object getObjectsValue(Object o) {
            if (o instanceof ReportPeptide) {
                return ((ReportPeptide) o).getAccessions();
            } else if (o instanceof List<?>) {
                return ((List<?>) o).stream().filter(obj -> obj instanceof String).map(obj -> (String) obj).collect(Collectors.toList());
            }

            // nothing supported
            return null;
        }

        @Override
        public boolean supportsClass(Object c) {
            return (c instanceof ReportPeptide);
        }

        @Override
        public boolean valueNeedsFileRefinement() {
            return true;
        }

        @Override
        public Object doFileRefinement(Long fileID, Object o) {
            List<String> strList = new ArrayList<>();

            if (o instanceof List<?>) {
                for (Object obj : (List<?>)o) {
                    if (obj instanceof Accession) {
                        if ((fileID > 0) && (((Accession)obj).foundInFile(fileID))) {
                            strList.add(((Accession)obj).getDescription(fileID));
                        } else if (fileID == 0) {
                            strList.addAll(((Accession) obj).getDescriptions().entrySet().stream().map(Map.Entry::getValue).collect(Collectors.toList()));
                        }
                    } else if (obj instanceof String) {
                        strList.add((String)obj);
                    }
                }
            }

            return strList;
        }
    },
    PEPTIDE_FILE_LIST_FILTER(FilterType.literal_list, String.class, "File List Filter for Peptide", "File List (Peptide)") {
        @Override
        public SimpleTypeFilter<String> newInstanceOf(FilterComparator arg, Object value, boolean negate) {
            return new SimpleTypeFilter<>(arg, this, negate, (String) value);
        }

        @Override
        public Object getObjectsValue(Object o) {
            if (o instanceof ReportPeptide) {
                return ((ReportPeptide) o).getFileNames();
            } else {
                // nothing supported
                return null;
            }
        }

        @Override
        public boolean supportsClass(Object c) {
            return (c instanceof ReportPeptide);
        }
    },
    PEPTIDE_MISSED_CLEAVAGES_FILTER(FilterType.numerical, Number.class, "Missed Cleavages Filter for Peptide", "Missed Cleavages (Peptide)") {
        @Override
        public SimpleTypeFilter<Number> newInstanceOf(FilterComparator arg, Object value, boolean negate) {
            return new SimpleTypeFilter<>(arg, this, negate, ((Number) value).intValue());
        }

        @Override
        public Object getObjectsValue(Object o) {
            if (o instanceof ReportPeptide) {
                return ((ReportPeptide) o).getMissedCleavages();
            } else if (o instanceof Number) {
                // if we get an Number, just return it
                return o;
            }

            // nothing supported
            return null;
        }

        @Override
        public boolean supportsClass(Object c) {
            return (c instanceof ReportPeptide);
        }
    },
    PEPTIDE_MODIFICATIONS_FILTER(FilterType.modification, String.class, "Modifications Filter for Peptide", "Modifications (Peptide)") {
        @Override
        public SimpleTypeFilter<String> newInstanceOf(FilterComparator arg, Object value, boolean negate) {
            return new SimpleTypeFilter<>(arg, this, negate, (String) value);
        }

        @Override
        public Object getObjectsValue(Object o) {
            if (o instanceof ReportPeptide) {
                return ((ReportPeptide) o).getModificationsList();
            } else if (o instanceof List<?>) {
                return ((List<?>) o).stream().filter(obj -> obj instanceof Modification).map(obj -> (Modification) obj).collect(Collectors.toList());
            }

            // nothing supported
            return null;
        }

        @Override
        public boolean supportsClass(Object c) {
            return (c instanceof ReportPeptide);
        }
    },
    PEPTIDE_SEQUENCE_FILTER(FilterType.literal, String.class, "Sequence Filter for Peptide", "Sequence (Peptide)") {
        @Override
        public SimpleTypeFilter<String> newInstanceOf(FilterComparator arg, Object value, boolean negate) {
            return new SimpleTypeFilter<>(arg, this, negate, (String) value);
        }

        @Override
        public Object getObjectsValue(Object o) {
            if (o instanceof ReportPeptide) {
                return ((ReportPeptide) o).getSequence();
            } else if (o instanceof String) {
                return o;
            }

            // nothing supported
            return null;
        }

        @Override
        public boolean supportsClass(Object c) {
            return (c instanceof ReportPeptide);
        }
    },
    PEPTIDE_SOURCE_ID_LIST_FILTER(FilterType.literal, String.class, "Source ID Filter for Peptide", "Source ID (Peptide)") {
        @Override
        public SimpleTypeFilter<String> newInstanceOf(FilterComparator arg, Object value, boolean negate) {
            return new SimpleTypeFilter<>(arg, this, negate, (String) value);
        }

        @Override
        public Object getObjectsValue(Object o) {
            if (o instanceof ReportPeptide) {
                return ((ReportPeptide) o).getSourceIDs();
            }

            // nothing supported
            return null;
        }

        @Override
        public boolean supportsClass(Object c) {
            return (c instanceof ReportPeptide);
        }
    },
    PEPTIDE_UNIQUE_FILTER(FilterType.bool, Boolean.class, "Unique Filter for Peptide", "Unique (Peptide)") {
        @Override
        public SimpleTypeFilter<Boolean> newInstanceOf(FilterComparator arg, Object value, boolean negate) {
            return new SimpleTypeFilter<>(arg, this, negate, (Boolean) value);
        }

        @Override
        public Object getObjectsValue(Object o) {
            if (o instanceof ReportPeptide) {

                for (PSMReportItem psmReportItem : ((ReportPeptide) o).getPSMs()) {
                    Boolean isUnique = null;

                    if (psmReportItem instanceof ReportPSMSet) {
                        isUnique = ((ReportPSMSet) psmReportItem).getPSMs().iterator().next().getSpectrum().getIsUnique();
                    } else if (psmReportItem instanceof ReportPSM) {
                        isUnique = ((ReportPSM) psmReportItem).getSpectrum().getIsUnique();
                    }

                    if ((isUnique == null) || (!isUnique)) {
                        return false;
                    }
                }
                // all PSMs were unique
                return Boolean.TRUE;
            } else if (o instanceof Boolean) {
                return o;
            }

            // nothing supported
            return null;
        }

        @Override
        public boolean supportsClass(Object c) {
            return (c instanceof ReportPeptide);
        }
    },
    NR_PSMS_PER_PEPTIDE_FILTER(FilterType.numerical, Number.class, "#PSMs per Peptide Set", "#PSMs (Peptide)") {
        @Override
        public SimpleTypeFilter<Number> newInstanceOf(FilterComparator arg, Object value, boolean negate) {
            return new SimpleTypeFilter<>(arg, this, negate, ((Number) value).intValue());
        }

        @Override
        public Object getObjectsValue(Object o) {
            if (o instanceof ReportPeptide) {
                return ((ReportPeptide) o).getNrPSMs();
            } else if (o instanceof Number) {
                return o;
            }

            // nothing supported
            return null;
        }

        @Override
        public boolean supportsClass(Object c) {
            return (c instanceof ReportPeptide);
        }
    },
    NR_SPECTRA_PER_PEPTIDE_FILTER(FilterType.numerical, Number.class, "#Spectra per Peptide Set", "#spectra (Peptide)") {
        @Override
        public SimpleTypeFilter<Number> newInstanceOf(FilterComparator arg, Object value, boolean negate) {
            return new SimpleTypeFilter<>(arg, this, negate, ((Number) value).intValue());
        }

        @Override
        public Object getObjectsValue(Object o) {
            if (o instanceof ReportPeptide) {
                return ((ReportPeptide) o).getNrSpectra();
            } else if (o instanceof Number) {
                return o;
            }

            // nothing supported
            return null;
        }

        @Override
        public boolean supportsClass(Object c) {
            return (c instanceof ReportPeptide);
        }
    },

    /* -------------------------------------------------------------------------
    /* protein based filters
    /* -----------------------------------------------------------------------*/

    PROTEIN_ACCESSIONS_FILTER(FilterType.literal_list, String.class, "Accessions Filter for Protein", "Accessions (Protein)") {
        @Override
        public SimpleTypeFilter<String> newInstanceOf(FilterComparator arg, Object value, boolean negate) {
            return new SimpleTypeFilter<>(arg, this, negate, (String) value);
        }

        @Override
        public Object getObjectsValue(Object o) {
            if (o instanceof ReportProtein) {
                return ((ReportProtein) o).getAccessions();
            } else if (o instanceof List<?>) {
                return ((List<?>) o).stream().filter(obj -> obj instanceof String).map(obj -> (String) obj).collect(Collectors.toList());
            }

            // nothing supported
            return null;
        }

        @Override
        public boolean supportsClass(Object c) {
            return (c instanceof ReportProtein);
        }

        @Override
        public boolean valueNeedsFileRefinement() {
            return true;
        }

        @Override
        public Object doFileRefinement(Long fileID, Object o) {
            List<String> strList = new ArrayList<>();

            if (o instanceof List<?>) {
                for (Object obj : (List<?>)o) {
                    if (obj instanceof Accession) {
                        if (((fileID > 0) && (((Accession)obj).foundInFile(fileID))) ||
                                (fileID == 0)) {
                            strList.add(((Accession)obj).getAccession());
                        }
                    } else if (obj instanceof String) {
                        strList.add((String)obj);
                    }
                }
            }

            return strList;
        }
    },
    PROTEIN_DESCRIPTION_FILTER(FilterType.literal_list, String.class, "Description Filter for Protein", "Description (Protein)") {
        @Override
        public SimpleTypeFilter<String> newInstanceOf(FilterComparator arg, Object value, boolean negate) {
            return new SimpleTypeFilter<>(arg, this, negate, (String) value);
        }

        @Override
        public Object getObjectsValue(Object o) {
            if (o instanceof ReportProtein) {
                ((ReportProtein) o).getAccessions();
            } else if (o instanceof List<?>) {
                return ((List<?>) o).stream().filter(obj -> obj instanceof String).map(obj -> (String) obj).collect(Collectors.toList());
            }

            // nothing supported
            return null;
        }

        @Override
        public boolean supportsClass(Object c) {
            return (c instanceof ReportProtein);
        }

        @Override
        public boolean valueNeedsFileRefinement() {
            return true;
        }

        @Override
        public Object doFileRefinement(Long fileID, Object o) {
            List<String> strList = new ArrayList<>();

            if (o instanceof List<?>) {
                for (Object obj : (List<?>)o) {
                    if (obj instanceof Accession) {
                        if ((fileID > 0) && (((Accession)obj).foundInFile(fileID))) {
                            strList.add(((Accession)obj).getDescription(fileID));
                        } else if (fileID == 0) {
                            strList.addAll(((Accession) obj).getDescriptions().entrySet().stream().map(Map.Entry::getValue).collect(Collectors.toList()));
                        }
                    } else if (obj instanceof String) {
                        strList.add((String)obj);
                    }
                }
            }

            return strList;
        }
    },
    PROTEIN_FILE_LIST_FILTER(FilterType.literal_list, String.class, "File List Filter for Protein", "File List (Protein)") {
        @Override
        public SimpleTypeFilter<String> newInstanceOf(FilterComparator arg, Object value, boolean negate) {
            return new SimpleTypeFilter<>(arg, this, negate, (String) value);
        }

        @Override
        public Object getObjectsValue(Object o) {
            if (o instanceof ReportProtein) {
                Set<String> fileNames = new HashSet<>();

                for (ReportPeptide pepIt :((ReportProtein) o).getPeptides()) {
                    fileNames.addAll(pepIt.getFileNames());
                }

                return new ArrayList<>(fileNames);
            } else {
                // nothing supported
                return null;
            }
        }

        @Override
        public boolean supportsClass(Object c) {
            return (c instanceof ReportProtein);
        }
    },
    PROTEIN_MODIFICATIONS_FILTER(FilterType.modification, String.class, "Modifications Filter for Protein", "Modifications (Protein)") {
        @Override
        public SimpleTypeFilter<String> newInstanceOf(FilterComparator arg, Object value, boolean negate) {
            return new SimpleTypeFilter<>(arg, this, negate, (String) value);
        }

        @Override
        public Object getObjectsValue(Object o) {
            if (o instanceof ReportProtein) {
                List<Modification> modList = new ArrayList<>();
                for (ReportPeptide pep : ((ReportProtein) o).getPeptides()) {
                    modList.addAll(pep.getModificationsList());
                }
                return modList;
            } else if (o instanceof List<?>) {
                return ((List<?>) o).stream().filter(obj -> obj instanceof Modification).map(obj -> (Modification) obj).collect(Collectors.toList());
            }

            // nothing supported
            return null;
        }

        @Override
        public boolean supportsClass(Object c) {
            return (c instanceof ReportProtein);
        }
    },
    PROTEIN_RANK_FILTER(FilterType.numerical, Number.class, "Rank Filter for Protein", "Rank (Protein)") {
        @Override
        public SimpleTypeFilter<Number> newInstanceOf(FilterComparator arg, Object value, boolean negate) {
            return new SimpleTypeFilter<>(arg, this, negate, ((Number) value).intValue());
        }

        @Override
        public Object getObjectsValue(Object o) {
            if (o instanceof ReportProtein) {
                return ((ReportProtein) o).getRank();
            } else if (o instanceof Number) {
                return o;
            }

            // nothing supported
            return null;
        }

        @Override
        public boolean supportsClass(Object c) {
            return (c instanceof ReportProtein);
        }
    },
    PROTEIN_SEQUENCE_LIST_FILTER(FilterType.literal_list, String.class, "Sequence List Filter for Protein", "Sequence List (Protein)") {
        @Override
        public SimpleTypeFilter<String> newInstanceOf(FilterComparator arg, Object value, boolean negate) {
            return new SimpleTypeFilter<>(arg, this, negate, (String) value);
        }

        @Override
        public Object getObjectsValue(Object o) {
            if (o instanceof ReportProtein) {
                return ((ReportProtein) o).getPeptides();
            }

            // nothing supported
            return null;
        }

        @Override
        public boolean supportsClass(Object c) {
            return (c instanceof ReportProtein);
        }

        @Override
        public boolean valueNeedsFileRefinement() {
            return true;
        }

        @Override
        public Object doFileRefinement(Long fileID, Object o) {
            List<String> strList = new ArrayList<>();

            if (o instanceof List<?>) {
                for (Object obj : (List<?>)o) {
                    if (obj instanceof ReportPeptide) {
                        strList.add(((ReportPeptide) obj).getSequence());
                    } else if (obj instanceof String) {
                        strList.add((String)obj);
                    }
                }
            }

            return strList;
        }
    },
    PROTEIN_SCORE_FILTER(FilterType.numerical, Number.class, "Protein Score filter", "score (Protein)") {
        @Override
        public SimpleTypeFilter<Number> newInstanceOf(FilterComparator arg, Object value, boolean negate) {
            return new SimpleTypeFilter<>(arg, this, negate, ((Number) value).doubleValue());
        }

        @Override
        public Object getObjectsValue(Object o) {
            if (o instanceof ReportProtein) {
                return ((ReportProtein) o).getScore();
            } else if (o instanceof Number) {
                return o;
            }

            // nothing supported
            return null;
        }

        @Override
        public boolean supportsClass(Object c) {
            return (c instanceof ReportProtein);
        }
    },
    NR_PEPTIDES_PER_PROTEIN_FILTER(FilterType.numerical, Number.class, "#Peptides per Protein Filter", "#peptides (Protein)") {
        @Override
        public SimpleTypeFilter<Number> newInstanceOf(FilterComparator arg, Object value, boolean negate) {
            return new SimpleTypeFilter<>(arg, this, negate, ((Number) value).intValue());
        }

        @Override
        public Object getObjectsValue(Object o) {
            if (o instanceof ReportProtein) {
                return ((ReportProtein) o).getNrPeptides();
            } else if (o instanceof Number) {
                return o;
            }

            // nothing supported
            return null;
        }

        @Override
        public boolean supportsClass(Object c) {
            return (c instanceof ReportProtein);
        }
    },
    NR_PSMS_PER_PROTEIN_FILTER(FilterType.numerical, Number.class, "#PSMs per Protein Filter", "#PSMs (Protein)") {
        @Override
        public SimpleTypeFilter<Number> newInstanceOf(FilterComparator arg, Object value, boolean negate) {
            return new SimpleTypeFilter<>(arg, this, negate, ((Number) value).intValue());
        }

        @Override
        public Object getObjectsValue(Object o) {
            if (o instanceof ReportProtein) {
                return ((ReportProtein) o).getNrPSMs();
            } else if (o instanceof Number) {
                return o;
            }

            // nothing supported
            return null;
        }

        @Override
        public boolean supportsClass(Object c) {
            return (c instanceof ReportProtein);
        }
    },
    NR_SPECTRA_PER_PROTEIN_FILTER(FilterType.numerical, Number.class, "#Spectra per Protein Filter", "#spectra (Protein)") {
        @Override
        public SimpleTypeFilter<Number> newInstanceOf(FilterComparator arg, Object value, boolean negate) {
            return new SimpleTypeFilter<>(arg, this, negate, ((Number) value).intValue());
        }

        @Override
        public Object getObjectsValue(Object o) {
            if (o instanceof ReportProtein) {
                return ((ReportProtein) o).getNrSpectra();
            } else if (o instanceof Number) {
                return o;
            }

            // nothing supported
            return null;
        }

        @Override
        public boolean supportsClass(Object c) {
            return (c instanceof ReportProtein);
        }
    },
    NR_UNIQUE_PEPTIDES_PER_PROTEIN_FILTER(FilterType.numerical, Number.class, "#Unique Peptides per Protein Filter", "#unique peptides (Protein)") {
        @Override
        public SimpleTypeFilter<Number> newInstanceOf(FilterComparator arg, Object value, boolean negate) {
            return new SimpleTypeFilter<>(arg, this, negate, ((Number) value).intValue());
        }

        @Override
        public Object getObjectsValue(Object o) {
            if (o instanceof ReportProtein) {
                Integer nrUnique = 0;

                for (ReportPeptide reportPeptide : ((ReportProtein) o).getPeptides()) {
                    for (PSMReportItem psmItem : reportPeptide.getPSMs()) {
                        Boolean isUnique = null;

                        if (psmItem instanceof ReportPSMSet) {
                            for (ReportPSM psm : ((ReportPSMSet) psmItem).getPSMs()) {
                                isUnique = psm.getSpectrum().getIsUnique();
                                if (isUnique != null) {
                                    // one definitely set PSM is enough
                                    break;
                                }
                            }
                        } else if (psmItem instanceof ReportPSM) {
                            isUnique = ((ReportPSM) psmItem).getSpectrum().getIsUnique();
                        }

                        if ((isUnique != null) && isUnique) {
                            // peptide is unique: increase and go to next peptide
                            nrUnique++;
                            break;
                        }
                    }
                }

                return nrUnique;
            } else if (o instanceof Number) {
                // if we get a Number, simply return it
                return o;
            }

            // nothing supported
            return null;
        }

        @Override
        public boolean supportsClass(Object c) {
            return (c instanceof ReportProtein);
        }
    },
    NR_GROUP_UNIQUE_PEPTIDES_PER_PROTEIN_FILTER(FilterType.numerical, Number.class, "#Unique Peptides per Protein Group Filter", "#unique peptides for group (Protein)") {
        @Override
        public SimpleTypeFilter<Number> newInstanceOf(FilterComparator arg, Object value, boolean negate) {
            return new SimpleTypeFilter<>(arg, this, negate, ((Number) value).intValue());
        }

        @Override
        public Object getObjectsValue(Object o) {
            if (o instanceof ReportProtein) {
                Integer nrGroupUnique = 0;
                Set<Accession> protAccesssions =
                        new HashSet<>(((ReportProtein) o).getAccessions());

                for (ReportPeptide reportPeptide : ((ReportProtein) o).getPeptides()) {
                    if (protAccesssions.equals(new HashSet<>(reportPeptide.getAccessions()))) {
                        nrGroupUnique++;
                    }
                }

                return nrGroupUnique;
            } else if (o instanceof Number) {
                return o;
            }

            // nothing supported
            return null;
        }

        @Override
        public boolean supportsClass(Object c) {
            return (c instanceof ReportProtein);
        }
    },

    /*
     * The score and top identifications filters are special types of filters
     */
    PSM_SCORE_FILTER(FilterType.numerical, Number.class, "Score Filter", "Score (PSM)") {
        @Override
        public PSMScoreFilter newInstanceOf(FilterComparator arg, Object value, boolean negate) {
            throw new NullPointerException("Wrong way to instantiate PSMScoreFilter. "
                    + "Instantiate directly!");
        }

        @Override
        public Object getObjectsValue(Object o) {
            throw new NullPointerException("This should never be called. "
                    + "PSMScoreFilter must overwrite getObjectsValue() method.");
        }

        @Override
        public boolean supportsClass(Object c) {
            throw new NullPointerException("This should never be called. "
                    + "PSMScoreFilter must overwrite supportsClass() method.");
        }
    },
    PSM_TOP_IDENTIFICATION_FILTER(FilterType.numerical, Number.class, "Top Identifications for PSMs", "PSM Top Identifications") {
        @Override
        public PSMScoreFilter newInstanceOf(FilterComparator arg, Object value, boolean negate) {
            throw new NullPointerException("Wrong way to instantiate PSMTopIdentificationFilter. "
                    + "Instantiate directly!");
        }

        @Override
        public Object getObjectsValue(Object o) {
            throw new NullPointerException("This should never be called. "
                    + "PSMTopIdentificationFilter must overwrite getObjectsValue() method.");
        }

        @Override
        public boolean supportsClass(Object c) {
            throw new NullPointerException("This should never be called. "
                    + "PSMTopIdentificationFilter must overwrite supportsClass() method.");
        }
    },
    PEPTIDE_SCORE_FILTER(FilterType.numerical, Number.class, "Score Filter", "Score (peptide)") {
        @Override
        public Object getObjectsValue(Object o) {
            throw new NullPointerException("Wrong way to instantiate PeptideScoreFilter. "
                    + "Instantiate directly!");
        }

        @Override
        public boolean supportsClass(Object c) {
            throw new NullPointerException("This should never be called. "
                    + "PeptideScoreFilter must overwrite getObjectsValue() method.");
        }

        @Override
        public AbstractFilter newInstanceOf(FilterComparator arg, Object value, boolean negate) {
            throw new NullPointerException("This should never be called. "
                    + "PeptideScoreFilter must overwrite getObjectsValue() method.");
        }

    },
    ;


    /** the actual {@link FilterType} of this filter */
    private FilterType filterType;

    /** the values which are compared by his filter */
    private Class<?> valueInstance;

    /** a longer, more descriptive name */
    private String longName;

    /** getter for the name, which should be displayed at a filter list */
    private String filteringListName;

    /** list of filters for PSMs */
    private static final List<RegisteredFilters> psmFilters = Arrays.asList(
            CHARGE_FILTER,
            DELTA_MASS_FILTER,
            DELTA_PPM_FILTER,
            MZ_FILTER,
            PSM_ACCESSIONS_FILTER,
            PSM_DESCRIPTION_FILTER,
            PSM_FILE_LIST_FILTER,
            PSM_MISSED_CLEAVAGES_FILTER,
            PSM_MODIFICATIONS_FILTER,
            PSM_RANK_FILTER,
            PSM_SEQUENCE_FILTER,
            PSM_UNIQUE_FILTER,
            PSM_SOURCE_ID_FILTER,
            NR_ACCESSIONS_PER_PSM_FILTER,
            NR_PSMS_PER_PSM_SET_FILTER);

    /** list of filters for peptides */
    private static final List<RegisteredFilters> peptideFilters = Arrays.asList(
            PEPTIDE_ACCESSIONS_FILTER,
            PEPTIDE_DESCRIPTION_FILTER,
            PEPTIDE_FILE_LIST_FILTER,
            PEPTIDE_MISSED_CLEAVAGES_FILTER,
            PEPTIDE_MODIFICATIONS_FILTER,
            PEPTIDE_SEQUENCE_FILTER,
            PEPTIDE_SOURCE_ID_LIST_FILTER,
            PEPTIDE_UNIQUE_FILTER,
            NR_PSMS_PER_PEPTIDE_FILTER,
            NR_SPECTRA_PER_PEPTIDE_FILTER);

    /** list of filters for proteins */
    private static final List<RegisteredFilters> proteinFilters = Arrays.asList(
            PROTEIN_SCORE_FILTER,
            PROTEIN_RANK_FILTER,
            NR_UNIQUE_PEPTIDES_PER_PROTEIN_FILTER,
            NR_GROUP_UNIQUE_PEPTIDES_PER_PROTEIN_FILTER,
            PROTEIN_ACCESSIONS_FILTER,
            PROTEIN_DESCRIPTION_FILTER,
            PROTEIN_FILE_LIST_FILTER,
            PROTEIN_MODIFICATIONS_FILTER,
            PROTEIN_SEQUENCE_LIST_FILTER,
            NR_PEPTIDES_PER_PROTEIN_FILTER,
            NR_PSMS_PER_PROTEIN_FILTER,
            NR_SPECTRA_PER_PROTEIN_FILTER);


    /** a copy of all filters */
    private static final List<RegisteredFilters> allFilters = Arrays.asList(values());


    /**
     * Basic constructor for a filter
     * @param filterType
     * @param valueInstance
     * @param longName
     * @param filteringListName
     */
    private RegisteredFilters(FilterType filterType, Class<?> valueInstance,
            String longName, String filteringListName) {
        this.filterType = filterType;
        this.valueInstance = valueInstance;
        this.longName = longName;
        this.filteringListName = filteringListName;
    }


    /**
     * Returns the short name of this filter.
     * @return
     */
    public final String getShortName() {
        return this.name().toLowerCase();
    }


    /**
     * getter for a longer, more descriptive name
     * @return
     */
    public final String getLongName() {
        return longName;
    }


    /**
     * getter for the name, which should be displayed at a filter list
     * @return
     */
    public final String getFilteringListName() {
        return filteringListName;
    }


    /**
     * Returns the {@link FilterType} of this filter.
     * @return
     */
    public final FilterType getFilterType() {
        return filterType;
    }


    /**
     * Checks whether the given value has the correct instance type for this
     * filter.
     *
     * @return
     */
    public final boolean isCorrectValueInstance(Object value) {
        return valueInstance.isInstance(value);
    }


    /**
     * Returns the value of the Object o, which will be used for filtering. E.g.
     * it returns the actual numerical value for the "charge" of a PSM, if the
     * charge should be filtered.
     *
     * @param o the object, containing the variable which should be filtered
     *
     * @return the value of the object's variable
     */
    public abstract Object getObjectsValue(Object o);


    /**
     * Returns, whether the class of the given object is valid for an instance
     * of this filter.
     *
     * @param c
     * @return
     */
    public abstract boolean supportsClass(Object c);


    /**
     * returns true, if the objects need file refinement (e.g. filtering the
     * accessions).
     *
     * @return
     */
    public boolean valueNeedsFileRefinement() {
        return false;
    }


    /**
     * performs the file refinement an the given object and returns the
     * refined object.
     *
     * @return
     */
    public Object doFileRefinement(Long fileID, Object o) {
        return null;
    }


    /**
     * Builds a new instance of this filter type.</br>
     * The arg must be a {@link FilterComparator} valid for this filter
     * type and the value of a valid type.
     *
     * @param arg
     * @param value
     * @param negate
     * @return
     */
    public abstract AbstractFilter newInstanceOf(FilterComparator arg,
            Object value, boolean negate);


    /**
     * Returns a set of registered filters for the PSM level
     */
    public static final List<RegisteredFilters> getPSMFilters() {
        return psmFilters;
    }


    /**
     * Returns a set of (descriptive) shorts for the registered PSM filters.
     * These are only used to print the help.
     */
    public static final List<String> getPSMFilterShortsForHelp() {
        List<String> filterShorts = new ArrayList<String>();

        filterShorts.add(PSMScoreFilter.PREFIX + "[scoreShort]");
        filterShorts.add(PSMTopIdentificationFilter.PREFIX + "[scoreShort]");

        filterShorts.addAll(getPSMFilters().stream().map(RegisteredFilters::getShortName).collect(Collectors.toList()));

        return filterShorts;
    }


    /**
     * Returns a set of registered filters for the peptide level
     */
    public static List<RegisteredFilters> getPeptideFilters() {
        return peptideFilters;
    }


    /**
     * Returns a set of (descriptive) shorts for the registered peptide filters.
     * These are only used to print the help.
     */
    public static final Set<String> getPeptideFilterShortsForHelp() {
        Set<String> filterShorts = new HashSet<String>();

        filterShorts.add(PSMScoreFilter.PREFIX + "[scoreShort]");
        filterShorts.add(PSMTopIdentificationFilter.PREFIX + "[scoreShort]");
        filterShorts.add(PeptideScoreFilter.PREFIX + "[scoreShort]");

        filterShorts.addAll(getPeptideFilters().stream().map(RegisteredFilters::getShortName).collect(Collectors.toList()));

        return filterShorts;
    }


    /**
     * Returns a set of registered filters for the protein level
     */
    public static final List<RegisteredFilters> getProteinFilters() {
        return proteinFilters;
    }


    /**
     * Returns a set of (descriptive) shorts for the registered protein filters.
     * These are only used to print the help.
     */
    public static Set<String> getProteinFilterShortsForHelp() {
        Set<String> filterShorts = new HashSet<>();

        filterShorts.add(PSMScoreFilter.PREFIX + "[scoreShort]");

        filterShorts.addAll(getProteinFilters().stream().map(RegisteredFilters::getShortName).collect(Collectors.toList()));

        return filterShorts;
    }



    /**
     * Returns the filter specified by the given SHORT_NAME or null, if no such
     * filter was found.
     *
     * @param filterShort
     * @return
     */
    public static RegisteredFilters getFilterByShortname(String filterShort) {
        if (filterShort == null) {
            return null;
        }

        for (RegisteredFilters filter : allFilters) {
            if (filter.getShortName().equals(filterShort)) {
                return filter;
            }
        }

        return null;
    }
}