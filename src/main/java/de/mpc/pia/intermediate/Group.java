package de.mpc.pia.intermediate;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;


public class Group implements Serializable {

    private static final long serialVersionUID = -8592708450814552133L;


    /** ID of the group */
    private long id;
    /** ID of the group's tree */
    private long treeID;
    /** List of the direct peptides in the group. */
    private Map<String, Peptide> peptides;
    /** Children groups of this group, i.e. groups where this group points to. */
    private TreeMap<Long, Group> children;
    /** Parents of this group, i.e. groups pointing to this group. */
    private TreeMap<Long, Group> parents;
    /** List of the direct accessions of this group. */
    private Map<String, Accession> accessions;
    /** List of all parents' and own accession. */
    private Map<String, Accession> allAccessions;


    /**
     * Basic Constructor, sets all the maps to null and score to NaN.
     *
     * @param id
     */
    public Group(long id) {
        this.id = id;
        this.treeID = -1;
        this.peptides = null;
        this.children = new TreeMap<>();
        this.parents = new TreeMap<>();
        this.accessions = new HashMap<>();
        this.allAccessions = new HashMap<>();
    }

    @Override
    public int hashCode() {
        int result = (int) (id ^ (id >>> 32));
        result = 31 * result + (int) (treeID ^ (treeID >>> 32));
        result = 31 * result + (peptides != null ? peptides.hashCode() : 0);
        if (children != null) {
            for (Group group : children.values()) {
                result = result * 31 + Long.hashCode(group.getID());
            }
        } else {
            result = 31 * result;
        }

        if(parents != null) {
            for (Group group : parents.values()) {
                result = result * 31 + Long.hashCode(group.getID());
            }
        } else {
            result = 31 * result;
        }

        result = 31 * result + (accessions != null ? accessions.hashCode() : 0);
        result = 31 * result + (allAccessions != null ? allAccessions.hashCode() : 0);
        return result;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Group group = (Group) o;

        if (id != group.id) return false;
        if (treeID != group.treeID) return false;
        if (peptides != null ? !peptides.equals(group.peptides) : group.peptides != null) return false;
        if (children != null ? !children.equals(group.children) : group.children != null) return false;
        if (parents != null ? !parents.equals(group.parents) : group.parents != null) return false;
        if (accessions != null ? !accessions.equals(group.accessions) : group.accessions != null) return false;
        return allAccessions != null ? allAccessions.equals(group.allAccessions) : group.allAccessions == null;
    }

    /**
     * Getter for the ID.
     *
     * @return
     */
    public long getID() {
        return this.id;
    }


    /**
     * Setter for the treeID.
     *
     * @param id
     */
    public void setTreeID(long id) {
        this.treeID = id;
    }


    /**
     * Getter for the group's treeID.
     *
     * @return
     */
    public long getTreeID() {
        return treeID;
    }


    /**
     * Setter for the peptides.
     *
     * @param peptides
     */
    public void setPeptides(Map<String, Peptide> peptides) {
        this.peptides = peptides;
    }


    /**
     * Adds a single peptide to the group.
     *
     */
    public void addPeptide(Peptide peptide) {
        if (peptides == null) {
            peptides = new HashMap<>();
        }

        peptides.put(peptide.getSequence(), peptide);
    }


    /**
     * Getter for the peptides.
     *
     * @return
     */
    public Map<String, Peptide> getPeptides() {
        if (peptides == null) {
            peptides = new HashMap<>();
        }

        return peptides;
    }


    /**
     * getter for all peptides, including children's peptides.
     * @return
     */
    public Map<String, Peptide> getAllPeptides() {
        Map<String, Peptide> ret = new HashMap<>();

        if (peptides != null) {
            for (Map.Entry<String, Peptide> pep : peptides.entrySet()) {
                ret.put(pep.getKey(), pep.getValue());
            }
        }

        for (Map.Entry<Long, Group> child : getAllPeptideChildren().entrySet()) {
            Map<String, Peptide> childPepMap = child.getValue().getPeptides();
            if (childPepMap != null) {
                for (Map.Entry<String, Peptide> childPeps : childPepMap.entrySet()) {
                    ret.put(childPeps.getKey(), childPeps.getValue());
                }
            }
        }

        return ret;
    }

    /**
     * Adds a child to the children map.
     * If the map is not yet initialized, initialize it.
     *
     */
    public void addChild(Group child) {
        children.put(child.getID(), child);
        if (allAccessions != null) {
            allAccessions.values().stream().forEach(child::addToAllAccessions);
        }
    }


    /**
     * Getter for the children.
     *
     * @return
     */
    public Map<Long, Group> getChildren() {
        return children;
    }


    /**
     * Getter for all children groups of this group, including children's
     * children and so on.
     */
    public Map<Long, Group> getAllChildren(){
        Map<Long, Group> allChildren = new HashMap<>();

        for (Map.Entry<Long, Group> cIt : children.entrySet()) {
            allChildren.put(cIt.getKey(), cIt.getValue());

            Map<Long, Group> childChildren = cIt.getValue().getAllChildren();
            for (Map.Entry<Long, Group> ccIt : childChildren.entrySet()) {
                allChildren.put(ccIt.getKey(), ccIt.getValue());
            }
        }

        return allChildren;
    }


    /**
     * Getter for all children groups of this group that have at least one
     * peptide, recursive, i.e. get the reporting peptide groups.
     */
    public Map<Long, Group> getAllPeptideChildren(){
        Map<Long, Group> allChildren = new HashMap<>();
        Map<Long, Group> childChildren;

        for (Map.Entry<Long, Group> cIt : children.entrySet()) {
            childChildren = cIt.getValue().getAllPeptideChildren();

            for (Map.Entry<Long, Group> ccIt : childChildren.entrySet()) {
                allChildren.put(ccIt.getKey(), ccIt.getValue());
            }

            if ((cIt.getValue().getPeptides() != null) &&
                    (cIt.getValue().getPeptides().size() > 0)) {
                allChildren.put(cIt.getKey(), cIt.getValue());
            }
        }

        return allChildren;
    }


    /**
     * Adds a new group to the map of parents.
     * If the map is not yet initialized, initialize it.
     *
     * @param parent
     */
    public void addParent(Group parent) {
        parents.put(parent.getID(), parent);
        if (parent.getAllAccessions() != null) {
            parent.getAllAccessions().values().stream().forEach(this::addToAllAccessions);
        }
    }


    /**
     * Getter for the parents.
     *
     * @return
     */
    public  Map<Long, Group> getParents() {
        return parents;
    }


    /**
     * Adds a new accession to the map of accessions.
     * If the map is not yet initialized, initialize it.
     *
     */
    public void addAccession(Accession accession) {
        accessions.put(accession.getAccession(), accession);
        addToAllAccessions(accession);
    }


    /**
     * Getter for the accessions.
     *
     * @return
     */
    public Map<String, Accession> getAccessions() {
        return accessions;
    }


    /**
     * Adds the given accession to the map of all accessions and also dates up
     * the children.
     *
     * @param accession
     */
    protected void addToAllAccessions(Accession accession) {
        allAccessions.put(accession.getAccession(), accession);
        children.values().stream().forEach(child -> child.addToAllAccessions(accession));
    }


    /**
     * Getter for the accessions of this group and all the parents.
     *
     * @return
     */
    public Map<String, Accession> getAllAccessions() {
        return allAccessions;
    }


    /**
     * String getter for the accessions.
     *
     * @return
     */
    public String getAccessionsStr() {
        StringBuilder sb = new StringBuilder();
        if (accessions != null) {
            accessions.keySet().stream().forEach( key -> sb.append(key).append(' '));
        }
        return sb.toString();
    }


    /**
     * Returns the accessions as an array of strings.
     *
     * @return
     */
    public String[] getAccessionsStrArray() {
        String[] accessionsArr = null;
        int i = 0;

        if (accessions != null) {
            accessionsArr = new String[accessions.size()];
            for (Map.Entry<String, Accession> acc : accessions.entrySet()) {
                accessionsArr[i] = acc.getKey();
                i++;
            }
        }

        return accessionsArr;
    }


    /**
     * String getter for the peptides.
     *
     * @return
     */
    public String getPeptidesStr() {
        StringBuilder sb = new StringBuilder();

        if (peptides != null) {
            for (Map.Entry<String, Peptide> pep : peptides.entrySet()) {
                sb.append(pep.getKey()).append(' ');
            }
        }

        for (Map.Entry<Long, Group> pepChild : getAllPeptideChildren().entrySet()) {
            sb.append(pepChild.getValue().getPeptidesStr());
        }

        return sb.toString();
    }


    /**
     * Adds the given offset to the own id and and the keys in the children and
     * parent maps.<br/>
     * This function should only be called, if all the IDs in a cluster are
     * updated.
     *
     * @param offset
     */
    public void setOffset(Long offset) {
        TreeMap<Long, Group> tmpMap;
        this.id += offset;

        // offset the children keys
        tmpMap = new TreeMap<>();
        for (Map.Entry<Long, Group> childrenIt : children.entrySet()) {
            tmpMap.put( childrenIt.getKey()+offset, childrenIt.getValue());
        }
        children = tmpMap;

        // offset the parents' keys
        tmpMap = new TreeMap<>();
        for (Map.Entry<Long, Group> parentsIt : parents.entrySet()) {
            tmpMap.put( parentsIt.getKey()+offset, parentsIt.getValue());
        }
        parents = tmpMap;
    }


    /**
     * Removes the given accession pointer from the direct accessions map and
     * dates up the all accessions map.
     *
     * @param accession
     */
    public void removeAccession(Accession accession) {
        accessions.remove(accession.getAccession());
        removeFromAllAccessions(accession);
    }


    /**
     * Removes the given accession from the map of all accessions and also dates
     * up the children.
     *
     * @param accession
     */
    private void removeFromAllAccessions(Accession accession) {
        allAccessions.remove(accession.getAccession());

        for (Group child : children.values()) {
            child.removeFromAllAccessions(accession);
        }
    }
}
