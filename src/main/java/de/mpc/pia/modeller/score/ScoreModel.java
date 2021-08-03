package de.mpc.pia.modeller.score;

import java.io.Serializable;
import java.util.Objects;

/**
 * This class models all scores in PIA.
 *
 * @author julian
 *
 */
public class ScoreModel implements Serializable, Comparable<ScoreModel> {

    private static final long serialVersionUID = 1697318886689764656L;

    /** the actual score value */
    private Double score;

    /** the type of the score, if known */
    private ScoreModelEnum type;

    /** the cvAccession, if possible. only used for unknown type */
    private String cvAccession;

    /** the cvName, if possible, or just a name. only used for unknown type */
    private String name;

    /** cvLabel is use to describe the id of the ontology **/
    private String cvLabel;


    /**
     * Basic constructor, creating the ScoreModel by accession or name. Accession may also be one of the valid descriptors of the score.
     */
    public ScoreModel(Double score, String cvAccession, String name) {
        ScoreModelEnum modelEnum = ScoreModelEnum.getModelByAccession(cvAccession);

        if(modelEnum == ScoreModelEnum.UNKNOWN_SCORE) {
            modelEnum =  ScoreModelEnum.getModelByDescription(cvAccession);
        }

        if(modelEnum == ScoreModelEnum.UNKNOWN_SCORE) {
            modelEnum =  ScoreModelEnum.getModelByDescription(name);
        }

        this.score = score;
        this.type = modelEnum;
        this.cvAccession = cvAccession;
        this.name = name;
    }

    /**
     * Basic contructor including the CvLabel
     * @param score
     * @param cvAccession
     * @param name
     * @param cvLabel
     */
    public ScoreModel(Double score, String cvAccession, String name, String cvLabel) {
        this.score = score;
        this.cvAccession = cvAccession;
        this.name = name;
        this.cvLabel = cvLabel;
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
            if (cvAccession != null) {
                if (cvAccession.contains(":")) {
                    String[] splitted = cvAccession.split(":");
                    return splitted[splitted.length-1];
                } else {
                    return cvAccession;
                }
            } else {
                return getName();
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

    public String getCvLabel() {
        return cvLabel;
    }

    public void setCvLabel(String cvLabel) {
        this.cvLabel = cvLabel;
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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ScoreModel that = (ScoreModel) o;

        if (!score.equals(that.score)) return false;
        if (type != that.type) return false;
        if (!Objects.equals(cvAccession, that.cvAccession)) return false;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        int result = score.hashCode();
        result = 31 * result + (name != null ? name.hashCode() : 0);
        return result;
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
        int compRet;
        if ((s == null) || !type.equals(s.type)) {
            // s.type is null or not this.type, therefore this one is better
            compRet = -1;
        } else {
            if (((this.score == null) || this.score.equals(Double.NaN))
                    && ((s.score == null) || s.score.equals(Double.NaN))) {
                // both are invalid
                if ((this.score == null) && (s.score == null)) {
                    // both null
                    compRet = 0;
                } else if (this.score == null) {
                    // this.score == null AND s.score == NaN
                    compRet = 1;
                } else if (s.score == null) {
                    // this.score ==  NaN, s.score == null
                    compRet = -1;
                } else {
                    // both NaN
                    compRet = 0;
                }
            } else if (((this.score == null) || this.score.equals(Double.NaN))
                    && ((s.score != null) && !s.score.equals(Double.NaN))) {
                    // s.score is a valid double
                    // this.score is null or NaN, which is always worse!
                compRet = 1;
            } else if (((this.score != null) && !this.score.equals(Double.NaN))
                    && ((s.score == null) || s.score.equals(Double.NaN))) {
                    // s.score is a valid double
                    // this.score is null or NaN, which is always worse!
                compRet = 1;
            } else {
                // both are valid doubles
                int factor = 1;

                if (higherScoreBetter == null) {
                    // don't know, what's better, sorry
                    factor = 0;
                } else if (!higherScoreBetter) {
                    factor = -1;
                }

                if (this.score < s.score) {
                    compRet = factor;
                } else if (this.score > s.score) {
                    compRet = -factor;
                } else {
                    compRet = 0;
                }
            }
        }

        return compRet;
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
        if ((score1 != null)
                && (score2 != null)
                && score1.type.equals(score2.type)) {
            return score1.compareTo(score2);
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
