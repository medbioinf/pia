package de.mpc.pia.modeller.score;

/**
 * This class models all scores in PIA.
 * 
 * @author julian
 *
 */
public class ScoreModel implements Comparable<ScoreModel> {
	
	/** the actual score value */
	private Double score;
	
	/** the type of the score, if known */
	private ScoreModelEnum type;
	
	/** the cvAccession, if possible. only used for unknown type */
	private String cvAccession;
	
	/** the cvName, if possible, or just a name. only used for unknown type */
	private String name;
	
	
	/**
	 * Basic constructor.
	 */
	public ScoreModel(Double score, String cvAccession, String name) {
		ScoreModelEnum modelEnum =
				ScoreModelEnum.getModelByDescription(cvAccession);
		
		this.score = score;
		this.type = modelEnum;
		this.cvAccession = cvAccession;
		this.name = name;
	}
	
	
	/**
	 * Basic constructor, if the type is known.
	 */
	public ScoreModel(Double score, ScoreModelEnum type) {
		if ((type == null) || ScoreModelEnum.UNKNOWN_SCORE.equals(type)) {
			throw new IllegalArgumentException("type must not be null or of " +
					"type UNKNOWN_SCORE");
		}
		
		this.score = score;
		this.type = type;
		this.cvAccession = null;
		this.name = null;
	}
	
	
	/**
	 * Returns the human readable name of the score, either from the type or, if
	 * the type is SCORE_UNKNOWN, the instantiating name.
	 * @return
	 */
	public String getName() {
		if (!type.equals(ScoreModelEnum.UNKNOWN_SCORE)) {
			return type.getName();
		} else {
			return name;
		}
	}
	
	
	/**
	 * Returns the machine readable of the score.
	 * @return
	 */
	public String getShortName() {
		if (!type.equals(ScoreModelEnum.UNKNOWN_SCORE)) {
			return type.getShortName();
		} else {
			if (cvAccession.contains(":")) {
				String[] splitted = cvAccession.split(":");
				return splitted[splitted.length-1];
			} else {
				return cvAccession;
			}
		}
	}
	
	
	/**
	 * Returns the accession. Either of the type or, it it is UNKNOWN_SCORE, the
	 * instantiating cvAccession.
	 * 
	 * @return
	 */
	public String getAccession() {
		if (!type.equals(ScoreModelEnum.UNKNOWN_SCORE)) {
			return type.getCvAccession();
		} else {
			return cvAccession;
		}
	}
	
	
	/**
	 * Getter for the score value.
	 * 
	 * @return
	 */
	public Double getValue() {
		return score;
	}
	
	
	/**
	 * Getter for the {@link ScoreModelEnum}
	 * @return
	 */
	public ScoreModelEnum getType() {
		return type;
	}
	
	
	/**
	 * Setter for the score value.
	 * @param score
	 */
	public void setValue(Double score) {
		this.score = score;
	}
	
	@Override
	public final String toString() {
		return getName() + ": " + score;
	}
	
	@Override
	public final int hashCode() {
		if (score != null) {
			return score.hashCode() +
				getName().hashCode();
		} else {
			return getName().hashCode();
		}
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ScoreModel) {
			if ((this.type.equals(((ScoreModel) obj).type)) &&	// same type
					(((this.score == null) && (((ScoreModel) obj).score == null)) ||
							((this.score != null) && this.score.equals(((ScoreModel) obj).score))) &&	// same score
					(((this.cvAccession == null) && (((ScoreModel) obj).cvAccession == null)) ||
							((this.cvAccession != null) && this.cvAccession.equals(((ScoreModel) obj).cvAccession))) && // same accession
					(((this.name == null) && (((ScoreModel) obj).name == null)) ||
							((this.name != null) && this.name.equals(((ScoreModel) obj).name))) // same name
					) {	// same instantiatingName
				return true;
			}
		}
		
		return false;
	}
	
	@Override
	public int compareTo(ScoreModel s) {
		return compareTo(s, type.higherScoreBetter());
	}
	
	
	/**
	 * Compares to the {@link ScoreModel} s, but uses the given value for
	 * higherScoreBetter instead of the one given by the {@link ScoreModelEnum}
	 * for this model.
	 * 
	 * @param s
	 * @param higherScoreBetter
	 * @return
	 */
	public int compareTo(ScoreModel s, Boolean higherScoreBetter) {
		if ((s == null) || !type.equals(s.type)) {
			// s.type is null or not this.type, therefore this is better
			return -1;
		}
		
		if ((this.score == null) || (this.score.equals(Double.NaN))) {
			
			if ((s.score != null) && !s.score.equals(Double.NaN)) {
				// s.score is a valid double
				// this.score is null or NaN, which is always worse!
				return 1;
			} else {
				// s.score is no valid double
				if (this.score == null) {
					if (s.score != null) {
						// this.score == null AND s.score == NaN
						return 1;
					} else {
						// both null
						return 0;
					}
				} else { 
					// this.score is NaN
					if (s.score != null) {
						// both NaN
						return 0;
					} else {
						// this.score ==  NaN, s.score == null
						return -1;
					}
				}
			}
			
		} else {
			
			if ((s.score != null) && !s.score.equals(Double.NaN)) {
				// both are valid doubles
				int factor = 1;
				
				if (higherScoreBetter == null) {
					// don't know, what's better, sorry
					return 0;
				} else if (!higherScoreBetter) {
					factor = -1;
				}
				
				if (this.score < s.score) {
					return factor;
				} else if (this.score > s.score) {
					return -factor;
				} else {
					return 0;
				}
			} else {
				// s.score is NaN or null, this.score is ok, therefore better
				return -1;
			}
			
		}
	}
	
	/**
	 * Compares the two score models and returns values according to the
	 * compareTo method.<br/>
	 * Actually, the compareModels function for the type of the first
	 * {@link ScoreModel} is performed.<br/>
	 * If score1 is null or the scores are not instances of the same
	 * classes, null is returned.
	 * 
	 * @param score1
	 * @param score2
	 * @return
	 */
	public static Integer compareScoreModels(ScoreModel score1, ScoreModel score2) {
		if ((score1 != null) && (score2 != null)) {
			if (score1.type.equals(score2.type)) {
				return score1.compareTo(score2);
			}
		}
		
		return null;
	}
	
	
	/**
	 * Compares two ScoreModels of the type given by the  scoreModelDescription.
	 * 
	 * @param score1
	 * @param score2
	 * @param scoreModelDescription
	 * @return
	 */
	public static Integer compareScoreModels(Double score1, Double score2,
			String scoreModelDescription) {
		ScoreModelEnum modelEnum =
				ScoreModelEnum.getModelByDescription(scoreModelDescription);
		
		if (ScoreModelEnum.UNKNOWN_SCORE.equals(modelEnum)) {
			// UNKNOWN_SCORE is not comparable
			return score1.compareTo(score2);
		} else {
			ScoreModel sm1 = new ScoreModel(score1, modelEnum);
			ScoreModel sm2 = new ScoreModel(score2, modelEnum);
			
			return sm1.compareTo(sm2);
		}
	}
}
