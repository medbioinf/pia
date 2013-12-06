package de.mpc.pia.modeller.report.filter;

import java.util.HashSet;
import java.util.Set;

import de.mpc.pia.modeller.report.filter.peptide.NrPSMsPerPeptideFilter;
import de.mpc.pia.modeller.report.filter.peptide.NrSpectraPerPeptideFilter;
import de.mpc.pia.modeller.report.filter.peptide.PeptideAccessionsFilter;
import de.mpc.pia.modeller.report.filter.peptide.PeptideDescriptionFilter;
import de.mpc.pia.modeller.report.filter.peptide.PeptideFileListFilter;
import de.mpc.pia.modeller.report.filter.peptide.PeptideMissedCleavagesFilter;
import de.mpc.pia.modeller.report.filter.peptide.PeptideModificationsFilter;
import de.mpc.pia.modeller.report.filter.peptide.PeptideRankFilter;
import de.mpc.pia.modeller.report.filter.peptide.PeptideScoreFilter;
import de.mpc.pia.modeller.report.filter.peptide.PeptideSequenceFilter;
import de.mpc.pia.modeller.report.filter.peptide.PeptideSourceIDListFilter;
import de.mpc.pia.modeller.report.filter.peptide.PeptideSpectrumTitleListFilter;
import de.mpc.pia.modeller.report.filter.peptide.PeptideUniqueFilter;
import de.mpc.pia.modeller.report.filter.protein.NrPSMsPerProteinFilter;
import de.mpc.pia.modeller.report.filter.protein.NrPeptidesPerProteinFilter;
import de.mpc.pia.modeller.report.filter.protein.NrSpectraPerProteinFilter;
import de.mpc.pia.modeller.report.filter.protein.NrUniquePeptidesPerProteinFilter;
import de.mpc.pia.modeller.report.filter.protein.ProteinAccessionsFilter;
import de.mpc.pia.modeller.report.filter.protein.ProteinDescriptionFilter;
import de.mpc.pia.modeller.report.filter.protein.ProteinFileListFilter;
import de.mpc.pia.modeller.report.filter.protein.ProteinModificationsFilter;
import de.mpc.pia.modeller.report.filter.protein.ProteinRankFilter;
import de.mpc.pia.modeller.report.filter.protein.ProteinScoreFilter;
import de.mpc.pia.modeller.report.filter.protein.SequenceListFilter;
import de.mpc.pia.modeller.report.filter.psm.ChargeFilter;
import de.mpc.pia.modeller.report.filter.psm.DeltaMassFilter;
import de.mpc.pia.modeller.report.filter.psm.DeltaPPMFilter;
import de.mpc.pia.modeller.report.filter.psm.MZFilter;
import de.mpc.pia.modeller.report.filter.psm.NrAccessionsPerPSMFilter;
import de.mpc.pia.modeller.report.filter.psm.NrPSMsPerPSMSetFilter;
import de.mpc.pia.modeller.report.filter.psm.PSMAccessionsFilter;
import de.mpc.pia.modeller.report.filter.psm.PSMDescriptionFilter;
import de.mpc.pia.modeller.report.filter.psm.PSMFileListFilter;
import de.mpc.pia.modeller.report.filter.psm.PSMMissedCleavagesFilter;
import de.mpc.pia.modeller.report.filter.psm.PSMModificationsFilter;
import de.mpc.pia.modeller.report.filter.psm.PSMRankFilter;
import de.mpc.pia.modeller.report.filter.psm.PSMScoreFilter;
import de.mpc.pia.modeller.report.filter.psm.PSMSequenceFilter;
import de.mpc.pia.modeller.report.filter.psm.PSMTopIdentificationFilter;
import de.mpc.pia.modeller.report.filter.psm.PSMUniqueFilter;
import de.mpc.pia.modeller.report.filter.psm.SourceIDFilter;

/**
 * All the filters should be registered in this enum.
 * 
 * @author julian
 *
 */
public enum RegisteredFilters {
	
	CHARGE_FILTER {
		@Override
		public FilterType getFilterType() {
			return ChargeFilter.filterType;
		}
		
		@Override
		public boolean isCorrectValueInstance(Object value) {
			return ChargeFilter.isCorrectValueInstance(value);
		}
		
		@Override
		public ChargeFilter newInstanceOf(FilterComparator arg,
				Object value, boolean negate) {
			return new ChargeFilter(arg, ((Number)value).intValue(), negate);
		}
	},
	DELTA_MASS_FILTER {
		@Override
		public FilterType getFilterType() {
			return DeltaMassFilter.filterType;
		}
		
		@Override
		public boolean isCorrectValueInstance(Object value) {
			return DeltaMassFilter.isCorrectValueInstance(value);
		}
		
		@Override
		public DeltaMassFilter newInstanceOf(FilterComparator arg,
				Object value, boolean negate) {
			return new DeltaMassFilter(arg, ((Number)value).doubleValue(), negate);
		}
	},
	DELTA_PPM_FILTER {
		@Override
		public FilterType getFilterType() {
			return DeltaPPMFilter.filterType;
		}
		
		@Override
		public boolean isCorrectValueInstance(Object value) {
			return DeltaPPMFilter.isCorrectValueInstance(value);
		}
		
		@Override
		public DeltaPPMFilter newInstanceOf(FilterComparator arg,
				Object value, boolean negate) {
			return new DeltaPPMFilter(arg, ((Number)value).doubleValue(), negate);
		}
	},
	MZ_FILTER {
		@Override
		public FilterType getFilterType() {
			return MZFilter.filterType;
		}
		
		@Override
		public boolean isCorrectValueInstance(Object value) {
			return MZFilter.isCorrectValueInstance(value);
		}
		
		@Override
		public MZFilter newInstanceOf(FilterComparator arg,
				Object value, boolean negate) {
			return new MZFilter(arg, ((Number)value).doubleValue(), negate);
		}
	},
	NR_ACCESSIONS_PER_PSM_FILTER {
		@Override
		public FilterType getFilterType() {
			return NrAccessionsPerPSMFilter.filterType;
		}
		
		@Override
		public boolean isCorrectValueInstance(Object value) {
			return NrAccessionsPerPSMFilter.isCorrectValueInstance(value);
		}
		
		@Override
		public NrAccessionsPerPSMFilter newInstanceOf(FilterComparator arg,
				Object value, boolean negate) {
			return new NrAccessionsPerPSMFilter(arg, ((Number)value).intValue(), negate);
		}
	},
	NR_PEPTIDES_FILTER {
		@Override
		public FilterType getFilterType() {
			return NrPeptidesPerProteinFilter.filterType;
		}
		
		@Override
		public boolean isCorrectValueInstance(Object value) {
			return NrPeptidesPerProteinFilter.isCorrectValueInstance(value);
		}
		
		@Override
		public NrPeptidesPerProteinFilter newInstanceOf(FilterComparator arg,
				Object value, boolean negate) {
			return new NrPeptidesPerProteinFilter(arg, ((Number)value).intValue(), negate);
		}
	},
	NR_PSMS_PER_PEPTIDE_FILTER {
		@Override
		public FilterType getFilterType() {
			return NrPSMsPerPeptideFilter.filterType;
		}
		
		@Override
		public boolean isCorrectValueInstance(Object value) {
			return NrPSMsPerPeptideFilter.isCorrectValueInstance(value);
		}
		
		@Override
		public NrPSMsPerPeptideFilter newInstanceOf(FilterComparator arg,
				Object value, boolean negate) {
			return new NrPSMsPerPeptideFilter(arg, ((Number)value).intValue(), negate);
		}
	},
	NR_PSMS_PER_PROTEIN_FILTER {
		@Override
		public FilterType getFilterType() {
			return NrPSMsPerProteinFilter.filterType;
		}
		
		@Override
		public boolean isCorrectValueInstance(Object value) {
			return NrPSMsPerProteinFilter.isCorrectValueInstance(value);
		}
		
		@Override
		public NrPSMsPerProteinFilter newInstanceOf(FilterComparator arg,
				Object value, boolean negate) {
			return new NrPSMsPerProteinFilter(arg, ((Number)value).intValue(), negate);
		}
	},
	NR_PSMS_PER_PSM_SET_FILTER {
		@Override
		public FilterType getFilterType() {
			return NrPSMsPerPSMSetFilter.filterType;
		}
		
		@Override
		public boolean isCorrectValueInstance(Object value) {
			return NrPSMsPerPSMSetFilter.isCorrectValueInstance(value);
		}
		
		@Override
		public NrPSMsPerPSMSetFilter newInstanceOf(FilterComparator arg,
				Object value, boolean negate) {
			return new NrPSMsPerPSMSetFilter(arg, ((Number)value).intValue(), negate);
		}
	},
	NR_SPECTRA_PER_PEPTIDE_FILTER {
		@Override
		public FilterType getFilterType() {
			return NrSpectraPerPeptideFilter.filterType;
		}
		
		@Override
		public boolean isCorrectValueInstance(Object value) {
			return NrSpectraPerPeptideFilter.isCorrectValueInstance(value);
		}
		
		@Override
		public NrSpectraPerPeptideFilter newInstanceOf(FilterComparator arg,
				Object value, boolean negate) {
			return new NrSpectraPerPeptideFilter(arg, ((Number)value).intValue(), negate);
		}
	},
	NR_SPECTRA_PER_PROTEIN_FILTER {
		@Override
		public FilterType getFilterType() {
			return NrSpectraPerProteinFilter.filterType;
		}
		
		@Override
		public boolean isCorrectValueInstance(Object value) {
			return NrSpectraPerProteinFilter.isCorrectValueInstance(value);
		}
		
		@Override
		public NrSpectraPerProteinFilter newInstanceOf(FilterComparator arg,
				Object value, boolean negate) {
			return new NrSpectraPerProteinFilter(arg, ((Number)value).intValue(), negate);
		}
	},
	NR_UNIQUE_PEPTIDES_PER_PROTEIN_FILTER {
		@Override
		public FilterType getFilterType() {
			return NrUniquePeptidesPerProteinFilter.filterType;
		}
		
		@Override
		public boolean isCorrectValueInstance(Object value) {
			return NrUniquePeptidesPerProteinFilter.isCorrectValueInstance(value);
		}
		
		@Override
		public NrUniquePeptidesPerProteinFilter newInstanceOf(FilterComparator arg,
				Object value, boolean negate) {
			return new NrUniquePeptidesPerProteinFilter(arg, ((Number)value).intValue(), negate);
		}
	},
	PEPTIDE_ACCESSIONS_FILTER {
		@Override
		public FilterType getFilterType() {
			return PeptideAccessionsFilter.filterType;
		}
		
		@Override
		public boolean isCorrectValueInstance(Object value) {
			return PeptideAccessionsFilter.isCorrectValueInstance(value);
		}
		
		@Override
		public PeptideAccessionsFilter newInstanceOf(FilterComparator arg,
				Object value, boolean negate) {
			return new PeptideAccessionsFilter(arg, (String)value, negate);
		}
	},
	PEPTIDE_DESCRIPTION_FILTER {
		@Override
		public FilterType getFilterType() {
			return PeptideDescriptionFilter.filterType;
		}
		
		@Override
		public boolean isCorrectValueInstance(Object value) {
			return PeptideDescriptionFilter.isCorrectValueInstance(value);
		}
		
		@Override
		public PeptideDescriptionFilter newInstanceOf(FilterComparator arg,
				Object value, boolean negate) {
			return new PeptideDescriptionFilter(arg, (String)value, negate);
		}
	},
	PEPTIDE_FILE_LIST_FILTER {
		@Override
		public FilterType getFilterType() {
			return PeptideFileListFilter.filterType;
		}
		
		@Override
		public boolean isCorrectValueInstance(Object value) {
			return PeptideFileListFilter.isCorrectValueInstance(value);
		}
		
		@Override
		public PeptideFileListFilter newInstanceOf(FilterComparator arg,
				Object value, boolean negate) {
			return new PeptideFileListFilter(arg, (String)value, negate);
		}
	},
	PEPTIDE_MISSED_CLEAVAGES_FILTER {
		@Override
		public FilterType getFilterType() {
			return PeptideMissedCleavagesFilter.filterType;
		}
		
		@Override
		public boolean isCorrectValueInstance(Object value) {
			return PeptideMissedCleavagesFilter.isCorrectValueInstance(value);
		}
		
		@Override
		public PeptideMissedCleavagesFilter newInstanceOf(FilterComparator arg,
				Object value, boolean negate) {
			return new PeptideMissedCleavagesFilter(arg, ((Number)value).intValue(), negate);
		}
	},
	PEPTIDE_MODIFICATIONS_FILTER {
		@Override
		public FilterType getFilterType() {
			return PeptideModificationsFilter.filterType;
		}
		
		@Override
		public boolean isCorrectValueInstance(Object value) {
			return PeptideModificationsFilter.isCorrectValueInstance(value);
		}
		
		@Override
		public PeptideModificationsFilter newInstanceOf(FilterComparator arg,
				Object value, boolean negate) {
			return new PeptideModificationsFilter(arg, (String)value, negate);
		}
	},
	PEPTIDE_RANK_FILTER {
		@Override
		public FilterType getFilterType() {
			return PeptideRankFilter.filterType;
		}
		
		@Override
		public boolean isCorrectValueInstance(Object value) {
			return PeptideRankFilter.isCorrectValueInstance(value);
		}
		
		@Override
		public PeptideRankFilter newInstanceOf(FilterComparator arg,
				Object value, boolean negate) {
			return new PeptideRankFilter(arg, ((Number)value).intValue(), negate);
		}
	},
	PEPTIDE_SEQUENCE_FILTER {
		@Override
		public FilterType getFilterType() {
			return PeptideSequenceFilter.filterType;
		}
		
		@Override
		public boolean isCorrectValueInstance(Object value) {
			return PeptideSequenceFilter.isCorrectValueInstance(value);
		}
		
		@Override
		public PeptideSequenceFilter newInstanceOf(FilterComparator arg,
				Object value, boolean negate) {
			return new PeptideSequenceFilter(arg, (String)value, negate);
		}
	},
	PEPTIDE_SOURCE_ID_LIST_FILTER {
		@Override
		public FilterType getFilterType() {
			return PeptideSourceIDListFilter.filterType;
		}
		
		@Override
		public boolean isCorrectValueInstance(Object value) {
			return PeptideSourceIDListFilter.isCorrectValueInstance(value);
		}
		
		@Override
		public PeptideSourceIDListFilter newInstanceOf(FilterComparator arg,
				Object value, boolean negate) {
			return new PeptideSourceIDListFilter(arg, (String)value, negate);
		}
	},
	PEPTIDE_SPECTRUM_TITLE_LIST_FILTER {
		@Override
		public FilterType getFilterType() {
			return PeptideSpectrumTitleListFilter.filterType;
		}
		
		@Override
		public boolean isCorrectValueInstance(Object value) {
			return PeptideSpectrumTitleListFilter.isCorrectValueInstance(value);
		}
		
		@Override
		public PeptideSpectrumTitleListFilter newInstanceOf(FilterComparator arg,
				Object value, boolean negate) {
			return new PeptideSpectrumTitleListFilter(arg, (String)value, negate);
		}
	},
	PEPTIDE_UNIQUE_FILTER {
		@Override
		public FilterType getFilterType() {
			return PeptideUniqueFilter.filterType;
		}
		
		@Override
		public boolean isCorrectValueInstance(Object value) {
			return PeptideUniqueFilter.isCorrectValueInstance(value);
		}
		
		@Override
		public PeptideUniqueFilter newInstanceOf(FilterComparator arg,
				Object value, boolean negate) {
			return new PeptideUniqueFilter(arg, (Boolean)value, negate);
		}
	},
	PROTEIN_ACCESSIONS_FILTER {
		@Override
		public FilterType getFilterType() {
			return ProteinAccessionsFilter.filterType;
		}
		
		@Override
		public boolean isCorrectValueInstance(Object value) {
			return ProteinAccessionsFilter.isCorrectValueInstance(value);
		}
		
		@Override
		public ProteinAccessionsFilter newInstanceOf(FilterComparator arg,
				Object value, boolean negate) {
			return new ProteinAccessionsFilter(arg, (String)value, negate);
		}
	},
	PROTEIN_DESCRIPTION_FILTER {
		@Override
		public FilterType getFilterType() {
			return ProteinDescriptionFilter.filterType;
		}
		
		@Override
		public boolean isCorrectValueInstance(Object value) {
			return ProteinDescriptionFilter.isCorrectValueInstance(value);
		}
		
		@Override
		public ProteinDescriptionFilter newInstanceOf(FilterComparator arg,
				Object value, boolean negate) {
			return new ProteinDescriptionFilter(arg, (String)value, negate);
		}
	},
	PROTEIN_FILE_LIST_FILTER {
		@Override
		public FilterType getFilterType() {
			return ProteinFileListFilter.filterType;
		}
		
		@Override
		public boolean isCorrectValueInstance(Object value) {
			return ProteinFileListFilter.isCorrectValueInstance(value);
		}
		
		@Override
		public ProteinFileListFilter newInstanceOf(FilterComparator arg,
				Object value, boolean negate) {
			return new ProteinFileListFilter(arg, (String)value, negate);
		}
	},
	PROTEIN_MODIFICATIONS_FILTER {
		@Override
		public FilterType getFilterType() {
			return ProteinModificationsFilter.filterType;
		}
		
		@Override
		public boolean isCorrectValueInstance(Object value) {
			return ProteinModificationsFilter.isCorrectValueInstance(value);
		}
		
		@Override
		public ProteinModificationsFilter newInstanceOf(FilterComparator arg,
				Object value, boolean negate) {
			return new ProteinModificationsFilter(arg, (String)value, negate);
		}
	},
	PROTEIN_RANK_FILTER {
		@Override
		public FilterType getFilterType() {
			return ProteinRankFilter.filterType;
		}
		
		@Override
		public boolean isCorrectValueInstance(Object value) {
			return ProteinRankFilter.isCorrectValueInstance(value);
		}
		
		@Override
		public ProteinRankFilter newInstanceOf(FilterComparator arg,
				Object value, boolean negate) {
			return new ProteinRankFilter(arg, ((Number)value).intValue(), negate);
		}
	},
	PSM_ACCESSIONS_FILTER {
		@Override
		public FilterType getFilterType() {
			return PSMAccessionsFilter.filterType;
		}
		
		@Override
		public boolean isCorrectValueInstance(Object value) {
			return PSMAccessionsFilter.isCorrectValueInstance(value);
		}
		
		@Override
		public PSMAccessionsFilter newInstanceOf(FilterComparator arg,
				Object value, boolean negate) {
			return new PSMAccessionsFilter(arg, (String)value, negate);
		}
	},
	PSM_DESCRIPTION_FILTER {
		@Override
		public FilterType getFilterType() {
			return PSMDescriptionFilter.filterType;
		}
		
		@Override
		public boolean isCorrectValueInstance(Object value) {
			return PSMDescriptionFilter.isCorrectValueInstance(value);
		}
		
		@Override
		public PSMDescriptionFilter newInstanceOf(FilterComparator arg,
				Object value, boolean negate) {
			return new PSMDescriptionFilter(arg, (String)value, negate);
		}
	},
	PSM_FILE_LIST_FILTER {
		@Override
		public FilterType getFilterType() {
			return PSMFileListFilter.filterType;
		}
		
		@Override
		public boolean isCorrectValueInstance(Object value) {
			return PSMFileListFilter.isCorrectValueInstance(value);
		}
		
		@Override
		public PSMFileListFilter newInstanceOf(FilterComparator arg,
				Object value, boolean negate) {
			return new PSMFileListFilter(arg, (String)value, negate);
		}
	},
	PSM_MISSED_CLEAVAGES_FILTER {
		@Override
		public FilterType getFilterType() {
			return PSMMissedCleavagesFilter.filterType;
		}
		
		@Override
		public boolean isCorrectValueInstance(Object value) {
			return PSMMissedCleavagesFilter.isCorrectValueInstance(value);
		}
		
		@Override
		public PSMMissedCleavagesFilter newInstanceOf(FilterComparator arg,
				Object value, boolean negate) {
			return new PSMMissedCleavagesFilter(arg, ((Number)value).intValue(), negate);
		}
	},
	PSM_MODIFICATIONS_FILTER {
		@Override
		public FilterType getFilterType() {
			return PSMModificationsFilter.filterType;
		}
		
		@Override
		public boolean isCorrectValueInstance(Object value) {
			return PSMModificationsFilter.isCorrectValueInstance(value);
		}
		
		@Override
		public PSMModificationsFilter newInstanceOf(FilterComparator arg,
				Object value, boolean negate) {
			return new PSMModificationsFilter(arg, (String)value, negate);
		}
	},
	PSM_RANK_FILTER {
		@Override
		public FilterType getFilterType() {
			return PSMRankFilter.filterType;
		}
		
		@Override
		public boolean isCorrectValueInstance(Object value) {
			return PSMRankFilter.isCorrectValueInstance(value);
		}
		
		@Override
		public PSMRankFilter newInstanceOf(FilterComparator arg,
				Object value, boolean negate) {
			return new PSMRankFilter(arg, ((Number)value).intValue(), negate);
		}
	},
	PSM_SEQUENCE_FILTER {
		@Override
		public FilterType getFilterType() {
			return PSMSequenceFilter.filterType;
		}
		
		@Override
		public boolean isCorrectValueInstance(Object value) {
			return PSMSequenceFilter.isCorrectValueInstance(value);
		}
		
		@Override
		public PSMSequenceFilter newInstanceOf(FilterComparator arg,
				Object value, boolean negate) {
			return new PSMSequenceFilter(arg, (String)value, negate);
		}
	},
	PSM_UNIQUE_FILTER {
		@Override
		public FilterType getFilterType() {
			return PSMUniqueFilter.filterType;
		}
		
		@Override
		public boolean isCorrectValueInstance(Object value) {
			return PSMUniqueFilter.isCorrectValueInstance(value);
		}
		
		@Override
		public PSMUniqueFilter newInstanceOf(FilterComparator arg,
				Object value, boolean negate) {
			return new PSMUniqueFilter(arg, (Boolean)value, negate);
		}
	},
	SEQUENCE_LIST_FILTER {
		@Override
		public FilterType getFilterType() {
			return SequenceListFilter.filterType;
		}
		
		@Override
		public boolean isCorrectValueInstance(Object value) {
			return SequenceListFilter.isCorrectValueInstance(value);
		}
		
		@Override
		public SequenceListFilter newInstanceOf(FilterComparator arg,
				Object value, boolean negate) {
			return new SequenceListFilter(arg, (String)value, negate);
		}
	},
	SOURCE_ID_FILTER {
		@Override
		public FilterType getFilterType() {
			return SourceIDFilter.filterType;
		}
		
		@Override
		public boolean isCorrectValueInstance(Object value) {
			return SourceIDFilter.isCorrectValueInstance(value);
		}
		
		@Override
		public SourceIDFilter newInstanceOf(FilterComparator arg,
				Object value, boolean negate) {
			return new SourceIDFilter(arg, (String)value, negate);
		}
	},
	;
	
	
	/**
	 * Returns the short name of this filter.
	 * @return
	 */
	public final String getShortName() {
		return this.name().toLowerCase();
	}
	
	
	/**
	 * Returns the {@link FilterType} of this filter.
	 * @return
	 */
	public abstract FilterType getFilterType();
	
	
	/**
	 * Checks wether the given value has the correct instance type for this
	 * filter.
	 * @return
	 */
	public abstract boolean isCorrectValueInstance(Object value);
	
	
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
	 * Returns a set of (descriptive) shorts for the registered PSM filters
	 */
	public static Set<String> getPSMFilters() {
		Set<String> filterShorts = new HashSet<String>();
		
		filterShorts.add(ChargeFilter.shortName());
		filterShorts.add(DeltaMassFilter.shortName());
		filterShorts.add(DeltaPPMFilter.shortName());
		filterShorts.add(MZFilter.shortName());
		filterShorts.add(NrAccessionsPerPSMFilter.shortName());
		filterShorts.add(NrPSMsPerPSMSetFilter.shortName());
		filterShorts.add(PSMAccessionsFilter.shortName());
		filterShorts.add(PSMDescriptionFilter.shortName());
		filterShorts.add(PSMFileListFilter.shortName());
		filterShorts.add(PSMMissedCleavagesFilter.shortName());
		filterShorts.add(PSMModificationsFilter.shortName());
		filterShorts.add(PSMRankFilter.shortName());
		filterShorts.add(PSMScoreFilter.prefix + "[scoreShort]");
		filterShorts.add(PSMSequenceFilter.shortName());
		filterShorts.add(PSMTopIdentificationFilter.prefix + "[scoreShort]");
		filterShorts.add(PSMUniqueFilter.shortName());
		filterShorts.add(SourceIDFilter.shortName());
		
		return filterShorts;
	}
	
	
	/**
	 * Returns a set of (descriptive) shorts for the registered peptide filters
	 */
	public static Set<String> getPeptideFilters() {
		Set<String> filterShorts = new HashSet<String>();
		
		filterShorts.add(NrPSMsPerPeptideFilter.shortName());
		filterShorts.add(NrSpectraPerPeptideFilter.shortName());
		filterShorts.add(PeptideAccessionsFilter.shortName());
		filterShorts.add(PeptideDescriptionFilter.shortName());
		filterShorts.add(PeptideFileListFilter.shortName());
		filterShorts.add(PeptideMissedCleavagesFilter.shortName());
		filterShorts.add(PeptideModificationsFilter.shortName());
		filterShorts.add(PeptideRankFilter.shortName());
		filterShorts.add(PSMScoreFilter.prefix + "[scoreShort]");
		filterShorts.add(PeptideScoreFilter.prefix + "[scoreShort]");
		filterShorts.add(PeptideSequenceFilter.shortName());
		filterShorts.add(PeptideSourceIDListFilter.shortName());
		filterShorts.add(PeptideSpectrumTitleListFilter.shortName());
		filterShorts.add(PeptideUniqueFilter.shortName());
		
		return filterShorts;
	}
	
	
	/**
	 * Returns a set of (descriptive) shorts for the registered protein filters
	 */
	public static Set<String> getProteinFilters() {
		Set<String> filterShorts = new HashSet<String>();
		
		filterShorts.add(NrPeptidesPerProteinFilter.shortName());
		filterShorts.add(NrPSMsPerProteinFilter.shortName());
		filterShorts.add(NrSpectraPerProteinFilter.shortName());
		filterShorts.add(NrUniquePeptidesPerProteinFilter.shortName());
		filterShorts.add(ProteinAccessionsFilter.shortName());
		filterShorts.add(ProteinDescriptionFilter.shortName());
		filterShorts.add(ProteinFileListFilter.shortName());
		filterShorts.add(ProteinModificationsFilter.shortName());
		filterShorts.add(ProteinRankFilter.shortName());
		filterShorts.add(PSMScoreFilter.prefix + "[scoreShort]");
		filterShorts.add(ProteinScoreFilter.shortName());
		filterShorts.add(SequenceListFilter.shortName());
		
		return filterShorts;
	}
}