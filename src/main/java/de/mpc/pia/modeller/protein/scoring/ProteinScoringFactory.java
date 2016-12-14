package de.mpc.pia.modeller.protein.scoring;

import java.util.HashMap;
import java.util.Map;


public class ProteinScoringFactory {
	
	public enum ScoringType {
		
		ADDITIVE_SCORING {
			@Override
			public String getName() {
				return AdditiveScoring.NAME;
			}
			
			@Override
			public String getShortName() {
				return AdditiveScoring.SHORT_NAME;
			}
			
			@Override
			public AdditiveScoring newInstanceOfScoring(
					Map<String, String> scoreNameMap) {
				return new AdditiveScoring(scoreNameMap);
			}
		},
		
		MULTIPLICATIVE_SCORING {
			@Override
			public String getName() {
				return MultiplicativeScoring.name;
			}
			
			@Override
			public String getShortName() {
				return MultiplicativeScoring.shortName;
			}
			
			@Override
			public MultiplicativeScoring newInstanceOfScoring(
					Map<String, String> scoreNameMap) {
				return new MultiplicativeScoring(scoreNameMap);
			}
		},
		
		GEOMETRIC_MEAN_SCORING {
			@Override
			public String getName() {
				return GeometricMeanScoring.NAME;
			}
			
			@Override
			public String getShortName() {
				return GeometricMeanScoring.SHORT_NAME;
			}
			
			@Override
			public GeometricMeanScoring newInstanceOfScoring(
					Map<String, String> scoreNameMap) {
				return new GeometricMeanScoring(scoreNameMap);
			}
		},
		;
		
		/**
		 * Returns the human readable name for this scoring.
		 * @return
		 */
		public abstract String getName();
		
		
		/**
		 * Returns the machine readable name for this scoring.
		 * @return
		 */
		public abstract String getShortName();
		
		
		/**
		 * Returns a new instance of this scoring type
		 * 
		 * @param scoreNameMap
		 * @return
		 */
		public abstract AbstractScoring newInstanceOfScoring(
				Map<String, String> scoreNameMap);
	}
	
	
	/**
	 * We don't ever want to instantiate this class
	 */
	private ProteinScoringFactory() {
		throw new AssertionError();
	}
	
	
	/**
	 * Returns the scoring type with the given shortName.<br/>
	 * If no Scoring type with this name is found, returns <code>null</code>.
	 * 
	 * @param shortName
	 * @return
	 */
	public static ScoringType getScoringTypeByName(String shortName) {
		for (ScoringType type : ScoringType.values()) {
			if (type.getShortName().equals(shortName)) {
				return type;
			}
		}
		
		return null;
	}
	
	
	/**
	 * Returns a new instance of the scoring given by the shortName.
	 * 
	 * @param shortName
	 * @param scoreNameMap
	 * @return
	 */
	public static AbstractScoring getNewInstanceByName(String shortName,
			Map<String, String> scoreNameMap) {
		ScoringType type = getScoringTypeByName(shortName);
		
		if (type != null) {
			return type.newInstanceOfScoring(scoreNameMap);
		} else {
			return null;
		}
	}
	
	
	/**
	 * Returns a map from each scoring's shortName to the human readable name of
	 * the scoring.
	 * 
	 * @return
	 */
	public static Map<String, String> getAllScoringNames() {
		Map<String, String> scoringMap = new HashMap<>(ScoringType.values().length);
		
		for (ScoringType scoring : ScoringType.values()) {
			scoringMap.put(scoring.getShortName(), scoring.getName());
		}
		
		return scoringMap;
	}
}
