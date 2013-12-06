package de.mpc.pia.modeller.score.comparator;

import java.util.Comparator;

/**
 * Compares two ranks and taking care, that ranks smaller zero are always
 * treated as worse than everything above zero.
 * 
 * @author julian
 *
 * @param <T>
 */
public class RankComparator<T extends Rankable> implements Comparator<T> {
	@Override
	public int compare(T o1, T o2) {
		if ((o1.getRank() < 0) || (o2.getRank() < 0)) {
			return -o1.getRank().compareTo(o2.getRank());
		} else {
			return o1.getRank().compareTo(o2.getRank());
		}
	}
}
